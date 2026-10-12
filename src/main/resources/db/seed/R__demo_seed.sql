-- 開発用デモデータ（PostgreSQL / local プロファイル専用）。
--
-- このファイルは classpath:db/seed を spring.flyway.locations に加えたときだけ実行される。
-- 本番・ステージングは db/migration だけを読むため、デモデータは入らない。
--
-- Flyway の「繰り返し実行マイグレーション」なので、チェックサムが変わるたびに再実行される。
-- 冪等性は delete してから insert することで担保する。削除対象は下記の予約ID範囲のみで、
-- 実データには一切触れない。
--
--   app_user 9001-9099 / project 9101-9199 / label 9201-9299 / task 9301-9399
--
-- シーケンスは 10050 以降に押し上げるので、アプリが採番するIDとは衝突しない（理由は末尾の注記）。
-- パスワードは全アカウント共通で Demo1234!（BCrypt strength 12）。開発用の既知の値であり、
-- 本番に取り込まれる経路はない。

-- ---------------------------------------------------------------------------
-- 1. 既存のシードを撤去（FKの順に）
-- ---------------------------------------------------------------------------
delete from task_label where task_id between 9301 and 9399;
delete from task       where id      between 9301 and 9399;
delete from label      where id      between 9201 and 9299;
delete from project    where id      between 9101 and 9199;
delete from app_user   where id      between 9001 and 9099;

-- ---------------------------------------------------------------------------
-- 2. アカウント: demo が主役、alex はデータ分離の確認用
-- ---------------------------------------------------------------------------
insert into app_user (id, username, email, password_hash, display_name, role, enabled,
                      created_at, updated_at, created_by, updated_by, deleted)
values
  (9001, 'demo', 'demo@example.com',
   '$2a$12$S4xH0VX/69ARlTuN/bfWwepDHQblWfdTd0EoQfDH/RP20KPDWoxgi',
   'Demo User', 'USER', true, current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9002, 'alex', 'alex@example.com',
   '$2a$12$S4xH0VX/69ARlTuN/bfWwepDHQblWfdTd0EoQfDH/RP20KPDWoxgi',
   'Alex Rivera', 'USER', true, current_timestamp, current_timestamp, 'seed', 'seed', false);

-- ---------------------------------------------------------------------------
-- 3. プロジェクト（9104 はアーカイブ済みの確認用）
-- ---------------------------------------------------------------------------
insert into project (id, name, color, archived, user_id,
                     created_at, updated_at, created_by, updated_by, deleted)
values
  (9101, 'Work',         '#2563EB', false, 9001, current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9102, 'Personal',     '#16A34A', false, 9001, current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9103, 'Study',        '#9333EA', false, 9001, current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9104, 'Old Side Gig', '#64748B', true,  9001, current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9105, 'Alex Private', '#DC2626', false, 9002, current_timestamp, current_timestamp, 'seed', 'seed', false);

-- ---------------------------------------------------------------------------
-- 4. ラベル
-- ---------------------------------------------------------------------------
insert into label (id, name, color, user_id,
                   created_at, updated_at, created_by, updated_by, deleted)
values
  (9201, 'office',    '#0EA5E9', 9001, current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9202, 'home',      '#F59E0B', 9001, current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9203, 'errand',    '#EC4899', 9001, current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9204, 'deep-work', '#14B8A6', 9001, current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9205, 'alex-only', '#DC2626', 9002, current_timestamp, current_timestamp, 'seed', 'seed', false);

-- ---------------------------------------------------------------------------
-- 5. タスク
--
-- 期限は current_date からの相対で入れるので、いつ seed しても
-- OVERDUE / TODAY / UPCOMING がそのまま成立する。
-- 9309 は「7日より先」= UPCOMING の境界外を確認するための1件。
-- ---------------------------------------------------------------------------
insert into task (id, title, description, status, priority, due_date, user_id, project_id,
                  urgent, important, start_time, end_time, alert_enabled,
                  created_at, updated_at, created_by, updated_by, deleted)
values
  -- 期限切れ（OVERDUE）
  (9301, 'Submit quarterly tax return', '国税庁のポータルから電子申告する。', 'PENDING', 'HIGH',
   current_date - 5, 9001, 9101, true,  true,  null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9302, 'Fix broken deploy script', 'staging へのデプロイが権限エラーで落ちている。', 'IN_PROGRESS', 'HIGH',
   current_date - 1, 9001, 9101, true,  true,  null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),

  -- 今日（TODAY）
  (9303, 'Team stand-up notes', null, 'PENDING', 'MEDIUM',
   current_date, 9001, 9101, true,  false, time '09:00', time '09:15', true,
   current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9304, 'Review pull request #42', 'CI/CD パイプラインの追加分をレビューする。', 'PENDING', 'HIGH',
   current_date, 9001, 9101, true,  true,  time '10:00', time '11:00', false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9305, 'Dentist appointment', null, 'PENDING', 'MEDIUM',
   current_date, 9001, 9102, true,  false, time '14:30', time '15:30', true,
   current_timestamp, current_timestamp, 'seed', 'seed', false),

  -- 7日以内（UPCOMING）
  (9306, 'Write design doc for billing', '課金モジュールの設計をまとめる。', 'PENDING', 'HIGH',
   current_date + 2, 9001, 9101, false, true,  null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9307, 'Plan Q3 roadmap', null, 'PENDING', 'MEDIUM',
   current_date + 5, 9001, 9101, false, true,  null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9308, 'Renew passport', '有効期限が切れる前に窓口を予約する。', 'PENDING', 'HIGH',
   current_date + 6, 9001, 9102, false, true,  null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9310, 'Finish Spring Boot 4 chapter', null, 'IN_PROGRESS', 'MEDIUM',
   current_date + 3, 9001, 9103, false, true,  time '20:00', time '21:30', false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9315, 'Cancel unused subscriptions', null, 'PENDING', 'LOW',
   current_date + 4, 9001, 9102, false, false, null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),

  -- UPCOMING の境界外（8日以降）
  (9309, 'Book flights for holiday', null, 'PENDING', 'LOW',
   current_date + 10, 9001, 9102, false, false, null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),

  -- 期限なし
  (9311, 'Read Hibernate 7 release notes', null, 'PENDING', 'LOW',
   null, 9001, 9103, false, false, null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),

  -- プロジェクト未所属（INBOX）
  (9312, 'Buy milk and coffee', null, 'PENDING', 'LOW',
   current_date, 9001, null, true,  false, null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9313, 'Reply to recruiter email', null, 'PENDING', 'LOW',
   null, 9001, null, true,  false, null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9314, 'Scroll social media', '捨てる象限（ELIMINATE）の見本。', 'PENDING', 'LOW',
   null, 9001, null, false, false, null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),

  -- 完了済み（OVERDUE には出ない＝完了は除外されることの確認）
  (9316, 'Ship v1.0 release notes', null, 'COMPLETED', 'HIGH',
   current_date - 2, 9001, 9101, true,  true,  null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9317, 'Set up CI pipeline', null, 'COMPLETED', 'MEDIUM',
   current_date - 3, 9001, 9101, false, true,  null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9318, 'Water the plants', null, 'COMPLETED', 'LOW',
   current_date, 9001, 9102, false, false, null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),

  -- アーカイブ済みプロジェクトに残ったタスク
  (9319, 'Hand over old client site', null, 'PENDING', 'LOW',
   current_date + 3, 9001, 9104, false, false, null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),

  -- alex のデータ（demo からは一切見えないことの確認用）
  (9320, 'Alex quarterly review', null, 'PENDING', 'HIGH',
   current_date, 9002, 9105, true,  true,  null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false),
  (9321, 'Alex inbox item', null, 'PENDING', 'LOW',
   null, 9002, null, false, false, null, null, false,
   current_timestamp, current_timestamp, 'seed', 'seed', false);

-- ---------------------------------------------------------------------------
-- 6. タスクとラベルの紐付け
-- ---------------------------------------------------------------------------
insert into task_label (task_id, label_id)
values
  (9301, 9201),
  (9303, 9201),
  (9304, 9201), (9304, 9204),
  (9305, 9202),
  (9306, 9204),
  (9310, 9204),
  (9312, 9203),
  (9313, 9203),
  (9320, 9205);

-- ---------------------------------------------------------------------------
-- 7. シーケンスを予約ID範囲の上へ押し上げる
--
-- 10050 なのは allocationSize=50 のため。Hibernate の pooled オプティマイザは
-- nextval で得た値 hi に対して (hi-50, hi] をIDとして払い出すので、10000 にすると
-- 最初のブロックが 9951-10000 になり予約帯（9001-9399）のすぐ隣に食い込む。
-- 10050 にすれば払い出しは 10001 以降に限られる。
--
-- greatest を使うので、すでに先まで進んでいる場合は巻き戻さない。
-- 巻き戻すと採番済みIDと衝突して duplicate key になる。
-- ---------------------------------------------------------------------------
select setval('app_user_seq', greatest((select last_value from app_user_seq), 10050), true);
select setval('project_seq',  greatest((select last_value from project_seq),  10050), true);
select setval('label_seq',    greatest((select last_value from label_seq),    10050), true);
select setval('task_seq',     greatest((select last_value from task_seq),     10050), true);
