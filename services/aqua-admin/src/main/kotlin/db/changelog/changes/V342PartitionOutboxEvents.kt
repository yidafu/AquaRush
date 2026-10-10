// outbox 分区化(spec/tech/design/outbox-messaging.md 修订):outbox_events 转按半年 RANGE 分区,
// 配合滚动维护「结束满 30 天的分区 DETACH 归档」,避免临时消息堆积导致慢查询。
// 迁移保留现有数据(在途 PENDING 不可丢):rename 旧表释放对象名 → 建分区父表(PK 含分区键)
// → DO $$ 动态建半区(覆盖 min(created_at) 所在半区至当前半区+1)→ 拷贝回流 → drop 旧表,
// 全部置于单个 changeSet 利用 PG 事务性 DDL 保证原子性。DO $$ 分区块必须 splitStatements=false
// (Liquibase SQL 分割器不识别 dollar 引用,同 V130LogPartitionBackup 的说明)。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V342PartitionOutboxEvents :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.4.2-partition-outbox-events", dbms = "postgresql") {
      comment("Convert outbox_events to half-year RANGE partitions (DETACH archive)")
      // 1) 旧表/约束/索引改名释放对象名(数据保留,走 legacy 回流;同 V130 模式)
      sql(
        sql = """
        ALTER TABLE outbox_events RENAME TO outbox_events_legacy;
        ALTER TABLE outbox_events_legacy RENAME CONSTRAINT outbox_events_pkey TO outbox_events_legacy_pkey;
        DROP INDEX IF EXISTS idx_outbox_status_next_run;
        DROP INDEX IF EXISTS idx_outbox_destination;
        DROP INDEX IF EXISTS idx_outbox_subscriber_status;
        """,
      )
      // 2) 分区父表:列与 V320 一致,主键改 (id, created_at)(分区表 PK 必须含分区键);
      //    三索引以原名建在父表上,自动下发到既有与未来分区
      sql(
        sql = """
        CREATE TABLE outbox_events (
          id bigint NOT NULL,
          event_type varchar(100) NOT NULL,
          destination varchar(50) NOT NULL,
          subscriber varchar(20),
          payload text NOT NULL,
          next_run_at timestamp NOT NULL,
          status varchar(20) NOT NULL DEFAULT 'PENDING',
          retry_count integer NOT NULL DEFAULT 0,
          error_message text,
          locked_by varchar(64),
          locked_at timestamp,
          created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
          updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
          CONSTRAINT outbox_events_pkey PRIMARY KEY (id, created_at)
        ) PARTITION BY RANGE (created_at);

        CREATE INDEX idx_outbox_status_next_run ON outbox_events (status, next_run_at);
        CREATE INDEX idx_outbox_destination ON outbox_events (destination);
        CREATE INDEX idx_outbox_subscriber_status ON outbox_events (subscriber, status);
        """,
      )
      // 3) 动态建半区:起点=LEAST(数据最早半区, 当前半区), 终点=GREATEST(...)+6个月
      //    (即覆盖全部既有数据,并保证「当前半区 + 下一半区」存在,承接未来 6 个月的 INSERT;
      //    空表时 min 为 NULL,COALESCE 回退 now() 只建当前+下一半区)
      sql(
        sql = """
        DO ${'$'}${'$'}
        DECLARE
          h date;
          start_ts timestamp;
          end_ts timestamp;
        BEGIN
          SELECT LEAST(al, nl), GREATEST(al, nl) + interval '6 months'
          INTO start_ts, end_ts
          FROM (
            SELECT
              CASE WHEN extract(month from mn) <= 6
                   THEN date_trunc('year', mn)
                   ELSE date_trunc('year', mn) + interval '6 months' END AS al,
              CASE WHEN extract(month from now()) <= 6
                   THEN date_trunc('year', now())
                   ELSE date_trunc('year', now()) + interval '6 months' END AS nl
            FROM (SELECT COALESCE((SELECT min(created_at) FROM outbox_events_legacy), now()) AS mn) t
          ) s;

          FOR h IN SELECT generate_series(start_ts, end_ts, interval '6 months')::date
          LOOP
            EXECUTE format(
              'CREATE TABLE IF NOT EXISTS outbox_events_%s%s PARTITION OF outbox_events FOR VALUES FROM (%L) TO (%L)',
              to_char(h, 'YYYY'),
              CASE WHEN extract(month from h) = 7 THEN 'H2' ELSE 'H1' END,
              h::timestamp,
              h::timestamp + interval '6 months');
          END LOOP;
        END ${'$'}${'$'};
        """,
        splitStatements = false,
      )
      // 4) 数据回流并删除旧表
      sql(
        sql = """
        INSERT INTO outbox_events
          (id, event_type, destination, subscriber, payload, next_run_at, status, retry_count,
           error_message, locked_by, locked_at, created_at, updated_at)
        SELECT id, event_type, destination, subscriber, payload, next_run_at, status, retry_count,
               error_message, locked_by, locked_at, created_at, updated_at
        FROM outbox_events_legacy;

        DROP TABLE outbox_events_legacy;
        """,
      )
    }
  })
