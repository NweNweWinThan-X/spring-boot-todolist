-- Todoist の概念モデル（Project 1:N Task、Label M:N Task）と
-- TaskTactic の アイゼンハワー・マトリクス（緊急度 × 重要度）を追加する。

-- プロジェクト: タスクを重ならない生活領域で束ねる
create sequence project_seq start with 1 increment by 50;

create table project (
  id         bigint       not null,
  name       varchar(100) not null,
  color      varchar(7)   not null default '#6366F1',
  archived   boolean      not null default false,
  user_id    bigint       not null,
  created_at timestamptz  not null,
  updated_at timestamptz  not null,
  created_by varchar(50),
  updated_by varchar(50),
  deleted    boolean      not null default false,
  constraint pk_project primary key (id),
  constraint fk_project_user foreign key (user_id) references app_user (id)
);

create unique index ux_project_user_name on project (user_id, lower(name)) where deleted = false;
create index ix_project_user on project (user_id) where deleted = false;

-- ラベル: タスクに付け外しできる再利用可能なメタデータ
create sequence label_seq start with 1 increment by 50;

create table label (
  id         bigint       not null,
  name       varchar(50)  not null,
  color      varchar(7)   not null default '#64748B',
  user_id    bigint       not null,
  created_at timestamptz  not null,
  updated_at timestamptz  not null,
  created_by varchar(50),
  updated_by varchar(50),
  deleted    boolean      not null default false,
  constraint pk_label primary key (id),
  constraint fk_label_user foreign key (user_id) references app_user (id)
);

create unique index ux_label_user_name on label (user_id, lower(name)) where deleted = false;
create index ix_label_user on label (user_id) where deleted = false;

-- タスクとラベルの多対多
create table task_label (
  task_id  bigint not null,
  label_id bigint not null,
  constraint pk_task_label primary key (task_id, label_id),
  constraint fk_task_label_task foreign key (task_id) references task (id),
  constraint fk_task_label_label foreign key (label_id) references label (id)
);

create index ix_task_label_label on task_label (label_id);

-- タスク: プロジェクト所属（null = Inbox）、アイゼンハワー軸、時間枠
alter table task add column project_id    bigint;
alter table task add column urgent        boolean not null default false;
alter table task add column important     boolean not null default false;
alter table task add column start_time    time;
alter table task add column end_time      time;
alter table task add column alert_enabled boolean not null default false;

-- 終了時刻は開始時刻より後であること（どちらも任意）
alter table task add constraint ck_task_time_range
  check (start_time is null or end_time is null or end_time > start_time);

alter table task add constraint fk_task_project foreign key (project_id) references project (id);

create index ix_task_project on task (project_id) where deleted = false;
create index ix_task_quadrant on task (user_id, urgent, important) where deleted = false;
