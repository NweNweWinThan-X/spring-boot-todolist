-- 認証モジュール: メールアドレスを追加（ログインIDとしても使用する）
alter table app_user add column email varchar(255);

create unique index ux_app_user_email on app_user (lower(email)) where deleted = false;

-- タスクモジュール
create sequence task_seq start with 1 increment by 50;

create table task (
  id          bigint        not null,
  title       varchar(200)  not null,
  description varchar(2000),
  status      varchar(20)   not null,
  priority    varchar(20)   not null,
  due_date    date,
  user_id     bigint        not null,
  created_at  timestamptz   not null,
  updated_at  timestamptz   not null,
  created_by  varchar(50),
  updated_by  varchar(50),
  deleted     boolean       not null default false,
  constraint pk_task primary key (id),
  constraint fk_task_user foreign key (user_id) references app_user (id),
  constraint ck_task_status check (status in ('PENDING', 'IN_PROGRESS', 'COMPLETED')),
  constraint ck_task_priority check (priority in ('LOW', 'MEDIUM', 'HIGH'))
);

create index ix_task_user_status on task (user_id, status) where deleted = false;
create index ix_task_user_due_date on task (user_id, due_date) where deleted = false;
