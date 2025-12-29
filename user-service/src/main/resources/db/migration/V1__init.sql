create schema if not exists user_service;

create table if not exists user_service.users (
  id uuid primary key,
  email text,
  created_at timestamptz not null default now()
);

create table if not exists user_service.organizations (
  id uuid primary key,
  name text not null,
  created_at timestamptz not null default now()
);

create table if not exists user_service.org_memberships (
  org_id uuid not null references user_service.organizations(id) on delete cascade,
  user_id uuid not null references user_service.users(id) on delete cascade,
  role text not null,
  created_at timestamptz not null default now(),
  primary key (org_id, user_id)
);

create table if not exists user_service.audit_logs (
  id uuid primary key,
  org_id uuid,
  actor_user_id uuid,
  action text not null,
  resource_type text,
  resource_id text,
  ip text,
  metadata_json jsonb,
  created_at timestamptz not null default now()
);

create index if not exists idx_audit_org_created_at on user_service.audit_logs(org_id, created_at desc);
create index if not exists idx_members_user on user_service.org_memberships(user_id);
