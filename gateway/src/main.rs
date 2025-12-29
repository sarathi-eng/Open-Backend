use axum::{
    body::Body,
    extract::{Path, State},
    http::{HeaderMap, Method, Request, StatusCode, Uri},
    middleware::{from_fn_with_state, Next},
    response::{IntoResponse, Response},
    routing::{any, get},
    Router,
};
use dashmap::DashMap;
use jsonwebtoken::{Algorithm, DecodingKey, Validation};
use reqwest::Client;
use serde::{Deserialize, Serialize};
use std::{
    net::SocketAddr,
    sync::Arc,
    time::{Instant},
};
use tracing::info;
use opentelemetry_otlp::WithExportConfig;
use opentelemetry::trace::TracerProvider as _;
use tracing_subscriber::layer::SubscriberExt;
use tracing_subscriber::util::SubscriberInitExt;

#[derive(Clone)]
struct AppState {
    http: Client,
    routes: Routes,
    auth: AuthConfig,
    rate_limit: RateLimitConfig,
    limiter: Arc<RateLimiter>,
}

#[derive(Clone)]
struct Routes {
    auth_base: String,
    user_base: String,
    billing_base: String,
    notification_base: String,
}

#[derive(Clone)]
struct AuthConfig {
    jwt_secret: Option<String>,
    jwt_issuer: String,
}

#[derive(Clone, Copy)]
struct RateLimitConfig {
    rps: f64,
    burst: f64,
}

struct RateLimiter {
    buckets: DashMap<String, Bucket>,
}

#[derive(Clone, Copy, Debug)]
struct Bucket {
    tokens: f64,
    last: Instant,
}

impl RateLimiter {
    fn new() -> Self {
        Self {
            buckets: DashMap::new(),
        }
    }

    fn allow(&self, key: &str, cfg: RateLimitConfig) -> bool {
        let now = Instant::now();
        let mut entry = self
            .buckets
            .entry(key.to_string())
            .or_insert(Bucket {
                tokens: cfg.burst,
                last: now,
            });

        let elapsed = now.duration_since(entry.last);
        entry.last = now;

        let refill = cfg.rps.max(0.0) * elapsed.as_secs_f64();
        entry.tokens = (entry.tokens + refill).min(cfg.burst.max(0.0));

        if entry.tokens >= 1.0 {
            entry.tokens -= 1.0;
            true
        } else {
            false
        }
    }
}

#[derive(Debug, Serialize, Deserialize)]
struct JwtClaims {
    iss: Option<String>,
    sub: Option<String>,
    exp: Option<u64>,
    iat: Option<u64>,
    typ: Option<String>,
    role: Option<String>,
}

#[tokio::main]
async fn main() {
    let otlp_endpoint = std::env::var("OTEL_EXPORTER_OTLP_ENDPOINT").unwrap_or_else(|_| "http://localhost:4317".to_string());

    let provider = opentelemetry_otlp::new_pipeline()
        .tracing()
        .with_exporter(
            opentelemetry_otlp::new_exporter()
                .tonic()
                .with_endpoint(otlp_endpoint),
        )
        .with_trace_config(
            opentelemetry_sdk::trace::Config::default().with_resource(
                opentelemetry_sdk::Resource::new(vec![opentelemetry::KeyValue::new(
                    "service.name",
                    "opencore-gateway",
                )]),
            ),
        )
        .install_batch(opentelemetry_sdk::runtime::Tokio)
        .expect("otel tracer provider");

    let tracer = provider.tracer("opencore-gateway");
    let otel_layer = tracing_opentelemetry::layer().with_tracer(tracer);

    tracing_subscriber::registry()
        .with(
            tracing_subscriber::fmt::layer()
                .json(),
        )
        .with(tracing_subscriber::EnvFilter::from_default_env())
        .with(otel_layer)
        .init();

    let routes = Routes {
        auth_base: std::env::var("AUTH_SERVICE_BASE_URL").unwrap_or_else(|_| "http://localhost:8081".to_string()),
        user_base: std::env::var("USER_SERVICE_BASE_URL").unwrap_or_else(|_| "http://localhost:8082".to_string()),
        billing_base: std::env::var("BILLING_SERVICE_BASE_URL").unwrap_or_else(|_| "http://localhost:8083".to_string()),
        notification_base: std::env::var("NOTIFICATION_SERVICE_BASE_URL").unwrap_or_else(|_| "http://localhost:8084".to_string()),
    };

    let auth = AuthConfig {
        jwt_secret: std::env::var("OPENCORE_JWT_SECRET").ok(),
        jwt_issuer: std::env::var("OPENCORE_JWT_ISSUER").unwrap_or_else(|_| "opencore".to_string()),
    };

    let rate_limit = RateLimitConfig {
        rps: std::env::var("OPENCORE_RATE_LIMIT_RPS")
            .ok()
            .and_then(|v| v.parse::<f64>().ok())
            .unwrap_or(25.0),
        burst: std::env::var("OPENCORE_RATE_LIMIT_BURST")
            .ok()
            .and_then(|v| v.parse::<f64>().ok())
            .unwrap_or(50.0),
    };

    let state = Arc::new(AppState {
        http: Client::new(),
        routes,
        auth,
        rate_limit,
        limiter: Arc::new(RateLimiter::new()),
    });

    let app = build_app(state);

    let addr: SocketAddr = "0.0.0.0:8080".parse().expect("valid addr");
    info!(%addr, "gateway listening");

    let listener = tokio::net::TcpListener::bind(addr).await.expect("bind");
    axum::serve(listener, app).await.expect("serve");
}

fn build_app(state: Arc<AppState>) -> Router {
    Router::new()
        .route("/health", get(health))
        .route("/auth/*path", any(proxy_auth))
        .route("/users/*path", any(proxy_user))
        .route("/billing/*path", any(proxy_billing))
        .route("/notifications/*path", any(proxy_notification))
        .layer(from_fn_with_state(state.clone(), rate_limit_middleware))
        .layer(from_fn_with_state(state.clone(), auth_middleware))
        .with_state(state)
}

async fn health() -> impl IntoResponse {
    (StatusCode::OK, "ok")
}

async fn auth_middleware(
    State(state): State<Arc<AppState>>,
    req: Request<Body>,
    next: Next,
) -> Result<Response, StatusCode> {
    let path = req.uri().path();

    // Allow health + actuator endpoints without auth.
    if path == "/health" || path.contains("/actuator/") {
        return Ok(next.run(req).await);
    }

    // Require auth only for protected surfaces.
    let is_protected =
        path.starts_with("/users/") || path.starts_with("/billing/") || path.starts_with("/notifications/");
    if !is_protected {
        return Ok(next.run(req).await);
    }

    let authz = req
        .headers()
        .get(axum::http::header::AUTHORIZATION)
        .and_then(|v| v.to_str().ok())
        .unwrap_or("");

    let token = authz.strip_prefix("Bearer ").unwrap_or("").trim();
    if token.is_empty() {
        return Err(StatusCode::UNAUTHORIZED);
    }

    if let Some(secret) = &state.auth.jwt_secret {
        let mut validation = Validation::new(Algorithm::HS384);
        validation.validate_exp = true;
        validation.set_issuer(&[state.auth.jwt_issuer.clone()]);

        let res = jsonwebtoken::decode::<JwtClaims>(
            token,
            &DecodingKey::from_secret(secret.as_bytes()),
            &validation,
        );
        if res.is_err() {
            return Err(StatusCode::UNAUTHORIZED);
        }
    }

    Ok(next.run(req).await)
}

async fn rate_limit_middleware(
    State(state): State<Arc<AppState>>,
    req: Request<Body>,
    next: Next,
) -> Result<Response, StatusCode> {
    let path = req.uri().path();
    if path == "/health" || path.contains("/actuator/") {
        return Ok(next.run(req).await);
    }

    let ip = req
        .headers()
        .get("x-forwarded-for")
        .and_then(|v| v.to_str().ok())
        .and_then(|s| s.split(',').next())
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
        .unwrap_or_else(|| "unknown".to_string());

    let key = format!("ip={ip}|path={path}");
    if !state.limiter.allow(&key, state.rate_limit) {
        return Err(StatusCode::TOO_MANY_REQUESTS);
    }

    Ok(next.run(req).await)
}

async fn proxy_auth(
    State(state): State<Arc<AppState>>,
    method: Method,
    headers: HeaderMap,
    uri: Uri,
    Path(path): Path<String>,
    body: axum::body::Bytes,
) -> impl IntoResponse {
    let base = state.routes.auth_base.clone();
    proxy(state, method, headers, uri, format!("{base}/{path}"), body).await
}

async fn proxy_user(
    State(state): State<Arc<AppState>>,
    method: Method,
    headers: HeaderMap,
    uri: Uri,
    Path(path): Path<String>,
    body: axum::body::Bytes,
) -> impl IntoResponse {
    let base = state.routes.user_base.clone();
    proxy(state, method, headers, uri, format!("{base}/{path}"), body).await
}

async fn proxy_billing(
    State(state): State<Arc<AppState>>,
    method: Method,
    headers: HeaderMap,
    uri: Uri,
    Path(path): Path<String>,
    body: axum::body::Bytes,
) -> impl IntoResponse {
    let base = state.routes.billing_base.clone();
    proxy(state, method, headers, uri, format!("{base}/{path}"), body).await
}

async fn proxy_notification(
    State(state): State<Arc<AppState>>,
    method: Method,
    headers: HeaderMap,
    uri: Uri,
    Path(path): Path<String>,
    body: axum::body::Bytes,
) -> impl IntoResponse {
    let base = state.routes.notification_base.clone();
    proxy(
        state,
        method,
        headers,
        uri,
        format!("{base}/{path}"),
        body,
    )
    .await
}

async fn proxy(
    state: Arc<AppState>,
    method: Method,
    headers: HeaderMap,
    uri: Uri,
    upstream: String,
    body: axum::body::Bytes,
) -> impl IntoResponse {
    // Minimal gateway skeleton: request logging + routing.
    // Auth verification + rate limiting will be added here.
    let query = uri.query().map(|q| format!("?{q}")).unwrap_or_default();
    let url = format!("{upstream}{query}");

    let mut request = state.http.request(method.clone(), &url).body(body);

    // Forward headers (excluding hop-by-hop handled by reqwest).
    for (name, value) in headers.iter() {
        request = request.header(name, value);
    }

    let resp = match request.send().await {
        Ok(r) => r,
        Err(e) => {
            tracing::warn!(error = %e, %url, "upstream request failed");
            return (StatusCode::BAD_GATEWAY, "bad gateway").into_response();
        }
    };

    let status = resp.status();
    let bytes = match resp.bytes().await {
        Ok(b) => b,
        Err(e) => {
            tracing::warn!(error = %e, %url, "reading upstream response failed");
            return (StatusCode::BAD_GATEWAY, "bad gateway").into_response();
        }
    };

    tracing::info!(
        method = %method,
        path = %uri.path(),
        upstream = %url,
        status = %status.as_u16(),
        "request"
    );

    (status, bytes).into_response()
}

#[cfg(test)]
mod tests {
    use super::*;
    use axum::{routing::any, Router};
    use jsonwebtoken::{EncodingKey, Header};
    use tower::ServiceExt;

    async fn start_mock(handler: Router) -> String {
        let listener = tokio::net::TcpListener::bind("127.0.0.1:0")
            .await
            .expect("bind ephemeral");
        let addr = listener.local_addr().expect("addr");

        tokio::spawn(async move {
            axum::serve(listener, handler).await.ok();
        });

        format!("http://{addr}")
    }

    fn test_state(routes: Routes, auth: AuthConfig, rate_limit: RateLimitConfig) -> Arc<AppState> {
        Arc::new(AppState {
            http: Client::new(),
            routes,
            auth,
            rate_limit,
            limiter: Arc::new(RateLimiter::new()),
        })
    }

    #[tokio::test]
    async fn health_endpoint_ok() {
        let state = test_state(
            Routes {
                auth_base: "http://127.0.0.1".into(),
                user_base: "http://127.0.0.1".into(),
                billing_base: "http://127.0.0.1".into(),
                notification_base: "http://127.0.0.1".into(),
            },
            AuthConfig {
                jwt_secret: None,
                jwt_issuer: "opencore".into(),
            },
            RateLimitConfig {
                rps: 1000.0,
                burst: 1000.0,
            },
        );

        let app = build_app(state);
        let res = app
            .oneshot(Request::builder().method("GET").uri("/health").body(Body::empty()).unwrap())
            .await
            .unwrap();
        assert_eq!(res.status(), StatusCode::OK);
    }

    #[tokio::test]
    async fn route_matching_preserves_path_and_query() {
        let upstream = start_mock(
            Router::new().route(
                "/*path",
                any(|uri: Uri| async move { (StatusCode::OK, uri.to_string()) }),
            ),
        )
        .await;

        let state = test_state(
            Routes {
                auth_base: upstream,
                user_base: "http://127.0.0.1".into(),
                billing_base: "http://127.0.0.1".into(),
                notification_base: "http://127.0.0.1".into(),
            },
            AuthConfig {
                jwt_secret: None,
                jwt_issuer: "opencore".into(),
            },
            RateLimitConfig {
                rps: 1000.0,
                burst: 1000.0,
            },
        );

        let app = build_app(state);
        let res = app
            .oneshot(
                Request::builder()
                    .method("GET")
                    .uri("/auth/ping?x=1")
                    .body(Body::empty())
                    .unwrap(),
            )
            .await
            .unwrap();
        assert_eq!(res.status(), StatusCode::OK);
        let body = axum::body::to_bytes(res.into_body(), usize::MAX).await.unwrap();
        let body = String::from_utf8(body.to_vec()).unwrap();
        assert_eq!(body, "/ping?x=1");
    }

    #[tokio::test]
    async fn auth_middleware_rejects_missing_token() {
        let upstream = start_mock(Router::new().route("/*path", any(|| async move { (StatusCode::OK, "ok") }))).await;

        let state = test_state(
            Routes {
                auth_base: "http://127.0.0.1".into(),
                user_base: upstream,
                billing_base: "http://127.0.0.1".into(),
                notification_base: "http://127.0.0.1".into(),
            },
            AuthConfig {
                jwt_secret: Some("dev-only-change-me-dev-only-change-me-dev-only-change-me".into()),
                jwt_issuer: "opencore".into(),
            },
            RateLimitConfig {
                rps: 1000.0,
                burst: 1000.0,
            },
        );

        let app = build_app(state);
        let res = app
            .oneshot(
                Request::builder()
                    .method("POST")
                    .uri("/users/v1/users")
                    .header(axum::http::header::CONTENT_TYPE, "application/json")
                    .body(Body::from("{}"))
                    .unwrap(),
            )
            .await
            .unwrap();
        assert_eq!(res.status(), StatusCode::UNAUTHORIZED);
    }

    #[tokio::test]
    async fn auth_middleware_rejects_invalid_token() {
        let upstream = start_mock(Router::new().route("/*path", any(|| async move { (StatusCode::OK, "ok") }))).await;

        let state = test_state(
            Routes {
                auth_base: "http://127.0.0.1".into(),
                user_base: upstream,
                billing_base: "http://127.0.0.1".into(),
                notification_base: "http://127.0.0.1".into(),
            },
            AuthConfig {
                jwt_secret: Some("dev-only-change-me-dev-only-change-me-dev-only-change-me".into()),
                jwt_issuer: "opencore".into(),
            },
            RateLimitConfig {
                rps: 1000.0,
                burst: 1000.0,
            },
        );

        let app = build_app(state);
        let res = app
            .oneshot(
                Request::builder()
                    .method("POST")
                    .uri("/users/v1/users")
                    .header(axum::http::header::AUTHORIZATION, "Bearer not-a-jwt")
                    .header(axum::http::header::CONTENT_TYPE, "application/json")
                    .body(Body::from("{}"))
                    .unwrap(),
            )
            .await
            .unwrap();
        assert_eq!(res.status(), StatusCode::UNAUTHORIZED);
    }

    #[tokio::test]
    async fn auth_middleware_allows_valid_token() {
        let upstream = start_mock(Router::new().route("/*path", any(|| async move { (StatusCode::OK, "upstream-ok") }))).await;

        let secret = "dev-only-change-me-dev-only-change-me-dev-only-change-me".to_string();
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_secs();
        let claims = JwtClaims {
            iss: Some("opencore".into()),
            sub: Some("user".into()),
            exp: Some(now + 3600),
            iat: Some(now),
            typ: Some("access".into()),
            role: Some("User".into()),
        };
        let token = jsonwebtoken::encode(
            &Header::new(Algorithm::HS384),
            &claims,
            &EncodingKey::from_secret(secret.as_bytes()),
        )
        .unwrap();

        let state = test_state(
            Routes {
                auth_base: "http://127.0.0.1".into(),
                user_base: upstream,
                billing_base: "http://127.0.0.1".into(),
                notification_base: "http://127.0.0.1".into(),
            },
            AuthConfig {
                jwt_secret: Some(secret),
                jwt_issuer: "opencore".into(),
            },
            RateLimitConfig {
                rps: 1000.0,
                burst: 1000.0,
            },
        );

        let app = build_app(state);
        let res = app
            .oneshot(
                Request::builder()
                    .method("POST")
                    .uri("/users/v1/users")
                    .header(axum::http::header::AUTHORIZATION, format!("Bearer {token}"))
                    .header(axum::http::header::CONTENT_TYPE, "application/json")
                    .body(Body::from("{}"))
                    .unwrap(),
            )
            .await
            .unwrap();
        assert_eq!(res.status(), StatusCode::OK);
        let body = axum::body::to_bytes(res.into_body(), usize::MAX).await.unwrap();
        assert_eq!(String::from_utf8(body.to_vec()).unwrap(), "upstream-ok");
    }

    #[tokio::test]
    async fn rate_limit_rejects_when_exceeded() {
        let upstream = start_mock(Router::new().route("/*path", any(|| async move { (StatusCode::OK, "ok") }))).await;

        let state = test_state(
            Routes {
                auth_base: upstream,
                user_base: "http://127.0.0.1".into(),
                billing_base: "http://127.0.0.1".into(),
                notification_base: "http://127.0.0.1".into(),
            },
            AuthConfig {
                jwt_secret: None,
                jwt_issuer: "opencore".into(),
            },
            // 0 rps means no refill; burst=1 means only first request passes.
            RateLimitConfig { rps: 0.0, burst: 1.0 },
        );

        let app = build_app(state);
        let res1 = app
            .clone()
            .oneshot(Request::builder().method("GET").uri("/auth/ping").body(Body::empty()).unwrap())
            .await
            .unwrap();
        assert_eq!(res1.status(), StatusCode::OK);

        let res2 = app
            .oneshot(Request::builder().method("GET").uri("/auth/ping").body(Body::empty()).unwrap())
            .await
            .unwrap();
        assert_eq!(res2.status(), StatusCode::TOO_MANY_REQUESTS);
    }
}
