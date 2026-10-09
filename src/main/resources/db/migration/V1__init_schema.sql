-- 認証モジュール: アカウントテーブル
create sequence app_user_seq start with 1 increment by 50;

create table app_user (
  id            bigint       not null,
  username      varchar(50)  not null,
  password_hash varchar(72)  not null,
  display_name  varchar(100) not null,
  role          varchar(20)  not null,
  enabled       boolean      not null default true,
  created_at    timestamptz  not null,
  updated_at    timestamptz  not null,
  created_by    varchar(50),
  updated_by    varchar(50),
  deleted       boolean      not null default false,
  constraint pk_app_user primary key (id),
  constraint uk_app_user_username unique (username),
  constraint ck_app_user_role check (role in ('USER', 'ADMIN'))
);

create index ix_app_user_deleted on app_user (deleted);
