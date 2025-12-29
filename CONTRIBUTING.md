# Contributing

Thanks for contributing!

## Development principles
- Production-minded defaults (secure-by-default, observable-by-default).
- Small PRs with clear scope.
- Prefer explicit APIs and versioned endpoints.

## Local development
- Infra: `infra/docker/docker-compose.yml`
- Services:
  - `gateway/` (Rust)
  - `*-service/` (Java/Spring Boot)

## Pull requests
- Add or update tests when changing behavior.
- Keep backward compatibility unless the change is part of a milestone.
- Include migration notes for DB changes.
