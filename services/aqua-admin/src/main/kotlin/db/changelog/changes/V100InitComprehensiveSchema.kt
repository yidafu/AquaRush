// 转换自 changes/v1.0.0-init-comprehensive-schema.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog
import java.math.BigDecimal

class V100InitComprehensiveSchema :
  KotlinCompiledDatabaseChangeLog({
    // =======================================================================
    // Reference Tables (No Dependencies)
    // =======================================================================
    changeSet(author = "aqua-rush", id = "1.0.0-create-aqua_regions-table") {
      comment("Create aqua_regions table for province/city/district data")
      createTable(tableName = "aqua_regions") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "name", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "code", type = "varchar(12)") {
          constraints(nullable = false, unique = true)
        }
        column(name = "parent_code", type = "varchar(12)") {
          constraints(nullable = true)
        }
        column(name = "level", type = "int") {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
      }
      createIndex(tableName = "aqua_regions", indexName = "idx_regions_parent_code") {
        column(name = "parent_code")
      }
      createIndex(tableName = "aqua_regions", indexName = "idx_regions_level") {
        column(name = "level")
      }
      createIndex(tableName = "aqua_regions", indexName = "idx_regions_parent_level") {
        column(name = "parent_code")
        column(name = "level")
      }
      createIndex(tableName = "aqua_regions", indexName = "idx_regions_deleted") {
        column(name = "is_deleted")
      }
    }
    changeSet(author = "aqua-rush", id = "1.0.0-insert-aqua_regions-data") {
      comment("Insert initial aqua_regions data (provinces, cities, districts)")
      sqlFile(path = "db/changelog/changes/insert-regions.sql", relativeToChangelogFile = false)
    }
    changeSet(author = "aqua-rush", id = "1.0.0-create-admin-table") {
      comment("Create admin table")
      createTable(tableName = "aqua_admins") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "username", type = "varchar(50)") {
          constraints(nullable = false, unique = true)
        }
        column(name = "password_hash", type = "varchar(255)") {
          constraints(nullable = false)
        }
        column(name = "real_name", type = "varchar(100)")
        column(name = "phone", type = "varchar(20)")
        column(name = "role", type = "varchar(20)", defaultValue = "ADMIN") {
          constraints(nullable = false)
        }
        column(name = "user_id", type = "bigint")
        column(name = "last_login_at", type = "timestamp")
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        // 配送业务字段(送水员档案并入 aqua_admins 单表,v1.2.1;role=DELIVERY_WORKER 区分身份)
        column(name = "avatar_url", type = "varchar(512)")
        column(name = "online_status", type = "varchar(20)", defaultValue = "OFFLINE") {
          constraints(nullable = false)
        }
        column(name = "coordinates", type = "json")
        column(name = "current_location", type = "json")
        column(name = "rating", type = "double")
        column(name = "total_orders", type = "integer", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "completed_orders", type = "integer", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "average_rating", type = "double")
        column(name = "earning_cents", type = "bigint")
        column(name = "is_available", type = "boolean", defaultValueBoolean = true) {
          constraints(nullable = false)
        }
        // 后台账号状态列(v1.2.2,ACTIVE/DISABLED)
        column(name = "account_status", type = "varchar(20)", defaultValue = "ACTIVE") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "aqua_admins", indexName = "idx_admin_username") {
        column(name = "username")
      }
      createIndex(tableName = "aqua_admins", indexName = "idx_admins_deleted") {
        column(name = "is_deleted")
      }
      addUniqueConstraint(tableName = "aqua_admins", columnNames = "user_id", constraintName = "uq_admins_user_id")
    }
    changeSet(author = "aqua-rush", id = "1.0.0-create-product-table") {
      comment("Create product table")
      createTable(tableName = "aqua_products") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "name", type = "varchar(255)") {
          constraints(nullable = false)
        }
        column(name = "price_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "thumbnail_url", type = "varchar(512)") {
          constraints(nullable = false)
        }
        column(name = "detail_images", type = "json")
        column(name = "description", type = "text")
        column(name = "stock_quantity", type = "integer", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(20)", defaultValue = "ACTIVE") {
          constraints(nullable = false)
        }
        // Enhanced product fields for water delivery business
        column(name = "subtitle", type = "varchar(500)")
        column(name = "original_price_cents", type = "bigint")
        column(name = "deposit_price_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "image_gallery", type = "json")
        column(name = "specification", type = "varchar(100)", defaultValue = "默认规格") {
          constraints(nullable = false)
        }
        column(name = "water_source", type = "varchar(200)")
        column(name = "mineral_content", type = "varchar(200)")
        column(name = "sales_volume", type = "integer", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "sort_order", type = "integer", defaultValueNumeric = 999) {
          constraints(nullable = false)
        }
        column(name = "tags", type = "json")
        column(name = "detail_content", type = "text")
        column(name = "certificate_images", type = "json")
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
      }
      // Indexes for performance
      createIndex(tableName = "aqua_products", indexName = "idx_product_status") {
        column(name = "status")
      }
      createIndex(tableName = "aqua_products", indexName = "idx_product_sales_volume") {
        column(name = "sales_volume")
      }
      createIndex(tableName = "aqua_products", indexName = "idx_product_sort_order") {
        column(name = "sort_order")
      }
      createIndex(tableName = "aqua_products", indexName = "idx_product_deleted") {
        column(name = "is_deleted")
      }
      createIndex(tableName = "aqua_products", indexName = "idx_product_water_source") {
        column(name = "water_source")
      }
      createIndex(tableName = "aqua_products", indexName = "idx_product_composite") {
        column(name = "is_deleted")
        column(name = "status")
        column(name = "sales_volume")
      }
      // Constraints for data integrity
      sql(
        sql = """
            
      ALTER TABLE aqua_products
        ADD CONSTRAINT chk_product_sales_volume_non_negative
          CHECK (sales_volume >= 0),
                ADD CONSTRAINT chk_product_sort_order_positive
                CHECK (sort_order >= 0),
                ADD CONSTRAINT chk_product_is_deleted_valid
                CHECK (is_deleted IN (false, true)),
                ADD CONSTRAINT chk_product_original_price_positive
                CHECK (original_price_cents IS NULL OR original_price_cents >= 0),
                ADD CONSTRAINT chk_product_deposit_price_positive
                CHECK (deposit_price_cents >= 0);
      
        
            """,
      )
    }
    // =======================================================================
    // System Tables (No Dependencies)
    // =======================================================================
    changeSet(author = "aqua-rush", id = "1.0.0-create-storage-schema") {
      comment("Create file metadata table for storage system")
      createTable(tableName = "aqua_file_metadata") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "file_name", type = "varchar(255)") {
          constraints(nullable = false)
        }
        column(name = "storage_path", type = "varchar(500)") {
          constraints(nullable = false, unique = true)
        }
        column(name = "file_type", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "file_size", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "mime_type", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "checksum", type = "varchar(64)") {
          constraints(nullable = false, unique = true)
        }
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "is_public", type = "boolean", defaultValueBoolean = true) {
          constraints(nullable = false)
        }
        column(name = "description", type = "varchar(1000)")
        column(name = "extension", type = "varchar(10)")
        column(name = "owner_id", type = "bigint")
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
      }
      createIndex(tableName = "aqua_file_metadata", indexName = "idx_file_metadata_checksum") {
        column(name = "checksum")
      }
      createIndex(tableName = "aqua_file_metadata", indexName = "idx_file_metadata_storage_path") {
        column(name = "storage_path")
      }
      createIndex(tableName = "aqua_file_metadata", indexName = "idx_file_metadata_file_type") {
        column(name = "file_type")
      }
      createIndex(tableName = "aqua_file_metadata", indexName = "idx_file_metadata_owner_id") {
        column(name = "owner_id")
      }
      createIndex(tableName = "aqua_file_metadata", indexName = "idx_file_metadata_created_at") {
        column(name = "created_at")
      }
      createIndex(tableName = "aqua_file_metadata", indexName = "idx_file_metadata_is_public") {
        column(name = "is_public")
      }
      createIndex(tableName = "aqua_file_metadata", indexName = "idx_file_metadata_deleted") {
        column(name = "is_deleted")
      }
      sql(
        sql = """
 ALTER TABLE aqua_file_metadata
      ADD CONSTRAINT chk_file_type CHECK (file_type IN ('IMAGE',
                                                        'VIDEO', 'AUDIO', 'DOCUMENT', 'SPREADSHEET', 'PRESENTATION',
                                                        'FRONTEND', 'ARCHIVE',
                                                        'EXECUTABLE', 'BACKUP', 'OTHER')), ADD CONSTRAINT chk_file_size_positive CHECK (file_size >
      0); 
            """,
      )
      sql(
        sql = """
 CREATE
    OR REPLACE FUNCTION update_file_metadata_updated_at() RETURNS TRIGGER AS ${'$'}${'$'}
    BEGIN
      NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
    END ${'$'}${'$'} LANGUAGE plpgsql;
    CREATE TRIGGER
      trigger_file_metadata_updated_at
      BEFORE UPDATE
      ON aqua_file_metadata
      FOR EACH ROW EXECUTE FUNCTION update_file_metadata_updated_at(); 
            """,
      )
    }
    changeSet(author = "aqua-rush", id = "1.0.0-create-event-table", dbms = "postgresql") {
      comment("Create outbox event table for async processing (half-year RANGE partitions, v3.4.0 替代 Artemis / v3.4.2 分区化)")
      sql(
        sql = """
        CREATE TABLE aqua_outbox_events (
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

        CREATE INDEX idx_outbox_status_next_run ON aqua_outbox_events (status, next_run_at);
        CREATE INDEX idx_outbox_destination ON aqua_outbox_events (destination);
        CREATE INDEX idx_outbox_subscriber_status ON aqua_outbox_events (subscriber, status);
        """,
      )
      // 初始建当前与下一半区(承接未来 6 个月 INSERT);滚动 DETACH 归档由服务侧维护
      sql(
        sql = """
        DO ${'$'}${'$'}
        DECLARE
          h timestamp;
          h2 timestamp;
        BEGIN
          SELECT CASE WHEN extract(month from now()) <= 6 THEN date_trunc('year', now())
                      ELSE date_trunc('year', now()) + interval '6 months' END
          INTO h;
          h2 := h + interval '6 months';
          EXECUTE format(
            'CREATE TABLE IF NOT EXISTS aqua_outbox_events_%s%s PARTITION OF aqua_outbox_events FOR VALUES FROM (%L) TO (%L)',
            to_char(h, 'YYYY'),
            CASE WHEN extract(month from h) = 7 THEN 'H2' ELSE 'H1' END,
            h, h2);
          EXECUTE format(
            'CREATE TABLE IF NOT EXISTS aqua_outbox_events_%s%s PARTITION OF aqua_outbox_events FOR VALUES FROM (%L) TO (%L)',
            to_char(h2, 'YYYY'),
            CASE WHEN extract(month from h2) = 7 THEN 'H2' ELSE 'H1' END,
            h2, h2 + interval '6 months');
        END ${'$'}${'$'};
        """,
        splitStatements = false,
      )
    }
    // =======================================================================
    // Base Entity Tables
    // =======================================================================
    changeSet(author = "aqua-rush", id = "1.0.0-create-user-table") {
      comment("Create user table")
      createTable(tableName = "aqua_users") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "wechat_openid", type = "varchar(128)") {
          constraints(nullable = false, unique = true)
        }
        column(name = "wechat_unionid", type = "varchar(128)") {
          constraints(unique = true)
        }
        column(name = "nickname", type = "varchar(255)")
        column(name = "phone", type = "varchar(20)")
        column(name = "avatar_url", type = "varchar(512)")
        column(name = "email", type = "varchar(255)", defaultValue = "") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(50)", defaultValue = "INACTIVE") {
          constraints(nullable = false)
        }
        column(name = "role", type = "varchar(50)", defaultValue = "NONE") {
          constraints(nullable = false)
        }
        // balance_cents 遗留列已删(v3.3.0:不做用户储值,PRD 2026-10-09 决策)
        column(name = "total_spent_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "last_login_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        // 客户分组(v1.0.9,单分组,空=未分组)
        column(name = "customer_group_id", type = "bigint")
      }
      createIndex(tableName = "aqua_users", indexName = "idx_users_openid") {
        column(name = "wechat_openid")
      }
      createIndex(tableName = "aqua_users", indexName = "idx_users_deleted") {
        column(name = "is_deleted")
      }
      createIndex(tableName = "aqua_users", indexName = "idx_users_customer_group_id") {
        column(name = "customer_group_id")
      }
    }
    changeSet(author = "aqua-rush", id = "1.0.0-create-delivery-area-table") {
      comment("Create delivery area table")
      createTable(tableName = "aqua_delivery_areas") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "name", type = "varchar(255)") {
          constraints(nullable = false)
        }
        column(name = "province", type = "varchar(50)") {
          constraints(nullable = false)
        }
        column(name = "city", type = "varchar(50)") {
          constraints(nullable = false)
        }
        column(name = "district", type = "varchar(50)") {
          constraints(nullable = false)
        }
        column(name = "is_enabled", type = "boolean", defaultValueBoolean = true) {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "aqua_delivery_areas", indexName = "idx_area_location") {
        column(name = "province")
        column(name = "city")
        column(name = "district")
      }
      createIndex(tableName = "aqua_delivery_areas", indexName = "idx_delivery_areas_deleted") {
        column(name = "is_deleted")
      }
    }
    changeSet(author = "aqua-rush", id = "1.0.0-create-reconciliation-tasks-table") {
      comment("Create reconciliation tasks table")
      createTable(tableName = "aqua_reconciliation_tasks") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "task_id", type = "varchar(100)") {
          constraints(nullable = false, unique = true)
        }
        column(name = "task_type", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "task_date", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "start_time", type = "timestamp")
        column(name = "end_time", type = "timestamp")
        column(name = "total_records", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "matched_records", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "unmatched_records", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "error_message", type = "text")
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        // 触发方式/执行人与微信、本地两侧汇总列(v3.4.7 G93 对账收尾,F-ORD-031/R-PAY-04)
        column(name = "trigger_type", type = "varchar(20)", defaultValue = "SCHEDULED") {
          constraints(nullable = false)
        }
        column(name = "executed_by", type = "bigint")
        column(name = "wechat_total_count", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "wechat_total_amount_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "local_total_count", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "local_total_amount_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "aqua_reconciliation_tasks", indexName = "idx_reconciliation_tasks_task_id") {
        column(name = "task_id")
      }
      createIndex(tableName = "aqua_reconciliation_tasks", indexName = "idx_reconciliation_tasks_task_type") {
        column(name = "task_type")
      }
      createIndex(tableName = "aqua_reconciliation_tasks", indexName = "idx_reconciliation_tasks_status") {
        column(name = "status")
      }
      createIndex(tableName = "aqua_reconciliation_tasks", indexName = "idx_reconciliation_tasks_task_date") {
        column(name = "task_date")
      }
      createIndex(tableName = "aqua_reconciliation_tasks", indexName = "idx_reconciliation_tasks_composite") {
        column(name = "task_date")
        column(name = "status")
        column(name = "created_at")
      }
      createIndex(tableName = "aqua_reconciliation_tasks", indexName = "idx_reconciliation_tasks_type_status") {
        column(name = "task_type")
        column(name = "status")
      }
      createIndex(tableName = "aqua_reconciliation_tasks", indexName = "idx_reconciliation_tasks_deleted") {
        column(name = "is_deleted")
      }
      createIndex(tableName = "aqua_reconciliation_tasks", indexName = "idx_reconciliation_tasks_trigger") {
        column(name = "trigger_type")
      }
      sql(
        sql = """
 ALTER TABLE aqua_reconciliation_tasks
      ADD CONSTRAINT chk_task_type CHECK (task_type IN
                                          ('PAYMENT', 'REFUND', 'SETTLEMENT')), ADD CONSTRAINT chk_task_status CHECK (status IN
      ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'SKIPPED')), ADD CONSTRAINT chk_total_records_non_negative
      CHECK (total_records >= 0), ADD CONSTRAINT chk_matched_records_non_negative CHECK
      (matched_records >= 0), ADD CONSTRAINT chk_unmatched_records_non_negative CHECK
      (unmatched_records >= 0), ADD CONSTRAINT chk_recon_trigger_type CHECK (trigger_type IN
      ('SCHEDULED', 'MANUAL'));
            """,
      )
    }
    // =======================================================================
    // Dependent Tables (Reference Base Entities)
    // =======================================================================
    changeSet(author = "aqua-rush", id = "1.0.1-create-user-address-table") {
      comment("Create user address table")
      createTable(tableName = "aqua_addresses") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "receiver_name", type = "varchar(50)") {
          constraints(nullable = false)
        }
        column(name = "phone", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "province", type = "varchar(50)") {
          constraints(nullable = false)
        }
        column(name = "province_code", type = "varchar(12)", remarks = "省份代码") {
          constraints(nullable = true)
        }
        column(name = "city", type = "varchar(50)") {
          constraints(nullable = false)
        }
        column(name = "city_code", type = "varchar(12)", remarks = "城市代码") {
          constraints(nullable = true)
        }
        column(name = "district", type = "varchar(50)") {
          constraints(nullable = false)
        }
        column(name = "district_code", type = "varchar(12)", remarks = "区县代码") {
          constraints(nullable = true)
        }
        column(name = "detail_address", type = "varchar(500)") {
          constraints(nullable = false)
        }
        column(name = "longitude", type = "double", remarks = "经度坐标") {
          constraints(nullable = true)
        }
        column(name = "latitude", type = "double", remarks = "纬度坐标") {
          constraints(nullable = true)
        }
        column(name = "is_default", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        // 地址楼层(v1.0.7,配送费楼层判定用,空=无楼层信息/平房)
        column(name = "floor", type = "int")
      }
      createIndex(tableName = "aqua_addresses", indexName = "idx_user_address_user_id") {
        column(name = "user_id")
      }
      createIndex(tableName = "aqua_addresses", indexName = "idx_addresses_phone") {
        column(name = "phone")
      }
      createIndex(tableName = "aqua_addresses", indexName = "idx_user_address_is_default") {
        column(name = "user_id")
        column(name = "is_default")
      }
      createIndex(tableName = "aqua_addresses", indexName = "idx_addresses_location") {
        column(name = "longitude")
        column(name = "latitude")
      }
      createIndex(tableName = "aqua_addresses", indexName = "idx_addresses_deleted") {
        column(name = "is_deleted")
      }
    }
    changeSet(author = "aqua-rush", id = "1.0.1-create-user-notification-settings-table") {
      comment("Create user notification settings table")
      createTable(tableName = "aqua_user_notification_settings") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false, unique = true)
        }
        column(name = "order_updates", type = "boolean", defaultValueBoolean = true) {
          constraints(nullable = false)
        }
        column(name = "payment_notifications", type = "boolean", defaultValueBoolean = true) {
          constraints(nullable = false)
        }
        column(name = "delivery_notifications", type = "boolean", defaultValueBoolean = true) {
          constraints(nullable = false)
        }
        column(name = "promotional_notifications", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
      }
      createIndex(
        tableName = "aqua_user_notification_settings",
        indexName = "idx_user_notification_settings_user_id",
      ) {
        column(name = "user_id")
      }
      createIndex(
        tableName = "aqua_user_notification_settings",
        indexName = "idx_user_notification_settings_deleted",
      ) {
        column(name = "is_deleted")
      }
    }
    changeSet(author = "aqua-rush", id = "1.0.1-create-message-history-table") {
      comment("Create message history table")
      createTable(tableName = "aqua_message_history") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "message_type", type = "varchar(50)") {
          constraints(nullable = false)
        }
        column(name = "template_id", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "content", type = "text")
        column(name = "status", type = "varchar(20)", defaultValue = "PENDING") {
          constraints(nullable = false)
        }
        column(name = "sent_at", type = "timestamp")
        column(name = "wx_message_id", type = "varchar(100)")
        column(name = "error_message", type = "text")
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "retry_count", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "aqua_message_history", indexName = "idx_message_history_user_id") {
        column(name = "user_id")
      }
      createIndex(tableName = "aqua_message_history", indexName = "idx_message_history_status") {
        column(name = "status")
      }
      createIndex(tableName = "aqua_message_history", indexName = "idx_message_history_message_type") {
        column(name = "message_type")
      }
      createIndex(tableName = "aqua_message_history", indexName = "idx_message_history_created_at") {
        column(name = "created_at")
      }
      createIndex(tableName = "aqua_message_history", indexName = "idx_message_history_wx_message_id") {
        column(name = "wx_message_id")
      }
      createIndex(tableName = "aqua_message_history", indexName = "idx_message_history_deleted") {
        column(name = "is_deleted")
      }
      sql(
        sql = """
 ALTER TABLE aqua_message_history
      ADD CONSTRAINT chk_message_status CHECK
        (status IN ('PENDING', 'SENT', 'FAILED', 'RETRYING')); 
            """,
        dbms = "postgresql",
      )
      sql(
        sql = """
 ALTER TABLE aqua_message_history
      ADD CONSTRAINT chk_message_status CHECK (status
        IN ('PENDING', 'SENT', 'FAILED', 'RETRYING')); 
            """,
        dbms = "mysql",
      )
      sql(
        sql = """
 CREATE
    OR REPLACE FUNCTION update_updated_at_column() RETURNS TRIGGER AS ${'$'}${'$'}
    BEGIN
      NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
    END ${'$'}${'$'} language 'plpgsql';
    CREATE TRIGGER
      update_user_notification_settings_updated_at
      BEFORE UPDATE
      ON aqua_user_notification_settings
      FOR
        EACH ROW EXECUTE FUNCTION update_updated_at_column(); 
            """,
      )
    }
    changeSet(author = "aqua-rush", id = "1.0.1-create-delivery-worker-statistics-table") {
      comment("Create delivery worker statistics table")
      createTable(tableName = "aqua_delivery_worker_statistics") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "delivery_worker_id", type = "bigint") {
          constraints(nullable = false, unique = true)
        }
        column(name = "average_rating", type = "decimal(3,2)", defaultValueNumeric = BigDecimal("0.00")) {
          constraints(nullable = false)
        }
        column(name = "total_reviews", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "one_star_reviews", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "two_star_reviews", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "three_star_reviews", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "four_star_reviews", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "five_star_reviews", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "last_updated", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "aqua_delivery_worker_statistics", indexName = "idx_delivery_worker_statistics_id") {
        column(name = "delivery_worker_id")
      }
      createIndex(
        tableName = "aqua_delivery_worker_statistics",
        indexName = "idx_delivery_worker_statistics_deleted",
      ) {
        column(name = "is_deleted")
      }
      sql(
        sql = """
 ALTER TABLE aqua_delivery_worker_statistics
      ADD CONSTRAINT chk_average_rating_range CHECK
        (average_rating >= 0.00 AND average_rating <= 5.00), ADD CONSTRAINT
      chk_total_reviews_non_negative CHECK (total_reviews >= 0), ADD CONSTRAINT
      chk_star_reviews_non_negative CHECK (one_star_reviews >= 0 AND two_star_reviews >= 0 AND
      three_star_reviews >= 0 AND four_star_reviews >= 0 AND five_star_reviews >= 0); 
            """,
      )
      sql(
        sql = """
 CREATE
    OR REPLACE FUNCTION update_delivery_worker_statistics_last_updated() RETURNS
      TRIGGER AS ${'$'}${'$'}
    BEGIN NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
    END ${'$'}${'$'} language
      'plpgsql';
    CREATE TRIGGER trigger_delivery_worker_statistics_last_updated
      BEFORE UPDATE
      ON
        aqua_delivery_worker_statistics
      FOR EACH ROW EXECUTE FUNCTION update_delivery_worker_statistics_last_updated(); 
            """,
      )
    }
    changeSet(author = "aqua-rush", id = "1.0.2-create-product-favorites-table") {
      comment("Create product favorites table for managing product favorites")
      createTable(tableName = "aqua_product_favorites") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "product_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "enable", type = "boolean", defaultValueBoolean = true) {
          constraints(nullable = false)
        }
      }
      // Composite unique constraint to prevent duplicate favorites
      addUniqueConstraint(
        tableName = "aqua_product_favorites",
        columnNames = "user_id, product_id",
        constraintName = "uk_user_product_favorite",
      )
      // Foreign key constraints
      addForeignKeyConstraint(
        baseTableName = "aqua_product_favorites",
        baseColumnNames = "user_id",
        referencedTableName = "aqua_users",
        referencedColumnNames = "id",
        constraintName = "fk_favorites_user",
      )
      addForeignKeyConstraint(
        baseTableName = "aqua_product_favorites",
        baseColumnNames = "product_id",
        referencedTableName = "aqua_products",
        referencedColumnNames = "id",
        constraintName = "fk_favorites_product",
      )
      // Basic performance indexes
      createIndex(tableName = "aqua_product_favorites", indexName = "idx_favorites_user_id") {
        column(name = "user_id")
      }
      createIndex(tableName = "aqua_product_favorites", indexName = "idx_favorites_product_id") {
        column(name = "product_id")
      }
      createIndex(tableName = "aqua_product_favorites", indexName = "idx_favorites_created_at") {
        column(name = "created_at")
      }
      createIndex(tableName = "aqua_product_favorites", indexName = "idx_product_favorites_deleted") {
        column(name = "is_deleted")
      }
      createIndex(tableName = "aqua_product_favorites", indexName = "idx_favorites_enable") {
        column(name = "enable")
      }
      createIndex(tableName = "aqua_product_favorites", indexName = "idx_favorites_user_product_enable") {
        column(name = "user_id")
        column(name = "product_id")
        column(name = "enable")
      }
      // Enhanced performance indexes
      createIndex(tableName = "aqua_product_favorites", indexName = "idx_favorites_user_created", unique = false) {
        column(name = "user_id")
        column(name = "created_at", descending = true)
      }
      createIndex(tableName = "aqua_product_favorites", indexName = "idx_favorites_product_created", unique = false) {
        column(name = "product_id")
        column(name = "created_at", descending = true)
      }
      createIndex(
        tableName = "aqua_product_favorites",
        indexName = "idx_favorites_date_user_product",
        unique = false,
      ) {
        column(name = "created_at")
        column(name = "user_id")
        column(name = "product_id")
      }
      createIndex(tableName = "aqua_product_favorites", indexName = "idx_favorites_created_at_desc", unique = false) {
        column(name = "created_at", descending = true)
      }
    }
    changeSet(
      author = "aqua-rush",
      id = "1.0.3-create-favorites-performance-views",
      context = "production,development",
    ) {
      preConditions(onFail = "MARK_RAN") {
        tableExists(tableName = "aqua_product_favorites")
        tableExists(tableName = "aqua_products")
        tableExists(tableName = "aqua_users")
      }
      // Materialized view for daily favorites statistics (PostgreSQL)
      // <sql dbms="postgresql">
      //             CREATE OR REPLACE MATERIALIZED VIEW mv_daily_favorites_stats AS
      //             SELECT
      //                 DATE(pf.created_at) as date,
      //                 COUNT(*) as total_favorites,
      //                 COUNT(DISTINCT pf.user_id) as active_users,
      //                 COUNT(DISTINCT pf.product_id) as unique_products
      //             FROM aqua_product_favorites pf
      //             WHERE pf.is_deleted = false
      //             GROUP BY DATE(pf.created_at)
      //             ORDER BY date DESC;
      //         </sql>
      // Regular view for most favorited aqua_products
      createView(viewName = "v_most_favorited_products", replaceIfExists = true) {
        """            
      SELECT p.id               as product_id,
             p.name             as product_name,
             p.price_cents,
             p.stock_quantity,
             p.sales_volume,
             COUNT(pf.id)       as favorite_count,
             MAX(pf.created_at) as last_favorite_at
      FROM aqua_products p
             INNER JOIN aqua_product_favorites pf ON p.id = pf.product_id
      WHERE p.is_deleted = false
        AND pf.is_deleted = false
        AND p.status IN ('ONLINE', 'ACTIVE')
      GROUP BY p.id, p.name, p.price_cents, p.stock_quantity, p.sales_volume
      ORDER BY favorite_count DESC
      
        
            """
      }
      // Regular view for user engagement statistics
      createView(viewName = "v_user_engagement_stats", replaceIfExists = true) {
        """            
      SELECT u.id                          as user_id,
             u.nickname,
             u.created_at                  as user_created_at,
             COUNT(pf.id)                  as total_favorites,
             MAX(pf.created_at)            as last_favorite_at,
             AVG(p.price_cents)            as avg_favorite_price,
             COUNT(DISTINCT pf.product_id) as unique_products_favorited
      FROM aqua_users u
             LEFT JOIN aqua_product_favorites pf ON u.id = pf.user_id AND pf.is_deleted = false
             LEFT JOIN aqua_products p ON pf.product_id = p.id AND p.is_deleted = false
      GROUP BY u.id, u.nickname, u.created_at
      ORDER BY total_favorites DESC
      
        
            """
      }
      comment(
        """
Performance views for admin analytics:
      - mv_daily_favorites_stats: Daily aggregate statistics (PostgreSQL materialized view)
      - v_most_favorited_products: Pre-computed product popularity ranking
      - v_user_engagement_stats: User favorite engagement metrics
""",
      )
    }
    changeSet(
      author = "aqua-rush",
      id = "1.0.3-create-favorites-view-indexes",
      context = "production,development",
    ) {
      preConditions(onFail = "MARK_RAN") {
        tableExists(tableName = "aqua_product_favorites")
      }
      // Note: Indexes cannot be created on regular views in PostgreSQL
      // The underlying tables already have indexes for optimal performance
      // Update table statistics for query optimizer
      sql(
        sql = """
 ANALYZE
    aqua_product_favorites; 
            """,
        dbms = "postgresql",
      )
      sql(
        sql = """
 ANALYZE
    aqua_products; 
            """,
        dbms = "postgresql",
      )
      sql(
        sql = """
 ANALYZE
    aqua_users; 
            """,
        dbms = "postgresql",
      )
      // Update table statistics for MySQL
      sql(
        sql = """
 ANALYZE
    TABLE aqua_product_favorites; 
            """,
        dbms = "mysql",
      )
      comment("Create supporting indexes for performance views and update statistics")
    }
    changeSet(author = "aqua-rush", id = "1.0.0-create-reconciliation-discrepancies-table") {
      comment("Create reconciliation discrepancies table")
      createTable(tableName = "aqua_reconciliation_discrepancies") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "task_id", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "discrepancy_type", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "source_system", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "record_id", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "record_details", type = "json")
        column(name = "status", type = "varchar(20)", defaultValue = "'UNRESOLVED'") {
          constraints(nullable = false)
        }
        column(name = "resolution_notes", type = "text")
        column(name = "resolved_by", type = "varchar(100)")
        column(name = "resolved_at", type = "timestamp")
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
      }
      createIndex(
        tableName = "aqua_reconciliation_discrepancies",
        indexName = "idx_reconciliation_discrepancies_task_id",
      ) {
        column(name = "task_id")
      }
      createIndex(
        tableName = "aqua_reconciliation_discrepancies",
        indexName = "idx_reconciliation_discrepancies_discrepancy_type",
      ) {
        column(name = "discrepancy_type")
      }
      createIndex(
        tableName = "aqua_reconciliation_discrepancies",
        indexName = "idx_reconciliation_discrepancies_source_system",
      ) {
        column(name = "source_system")
      }
      createIndex(
        tableName = "aqua_reconciliation_discrepancies",
        indexName = "idx_reconciliation_discrepancies_status",
      ) {
        column(name = "status")
      }
      createIndex(
        tableName = "aqua_reconciliation_discrepancies",
        indexName = "idx_reconciliation_discrepancies_record_id",
      ) {
        column(name = "record_id")
      }
      createIndex(
        tableName = "aqua_reconciliation_discrepancies",
        indexName = "idx_reconciliation_discrepancies_task_created",
      ) {
        column(name = "task_id")
        column(name = "created_at")
      }
      createIndex(
        tableName = "aqua_reconciliation_discrepancies",
        indexName = "idx_reconciliation_discrepancies_resolution",
      ) {
        column(name = "status")
        column(name = "discrepancy_type")
      }
      createIndex(
        tableName = "aqua_reconciliation_discrepancies",
        indexName = "idx_reconciliation_discrepancies_deleted",
      ) {
        column(name = "is_deleted")
      }
      addForeignKeyConstraint(
        baseTableName = "aqua_reconciliation_discrepancies",
        baseColumnNames = "task_id",
        referencedTableName = "aqua_reconciliation_tasks",
        referencedColumnNames = "task_id",
        constraintName = "fk_reconciliation_discrepancies_task_id",
        onDelete = "CASCADE",
      )
      sql(
        sql = """
 ALTER TABLE aqua_reconciliation_discrepancies
      ADD CONSTRAINT chk_discrepancy_type CHECK
        (discrepancy_type IN ('MISSING', 'MISMATCH', 'EXTRA')), ADD CONSTRAINT chk_source_system CHECK
      (source_system IN ('INTERNAL', 'WECHAT')), ADD CONSTRAINT chk_discrepancy_status CHECK (status
      IN ('UNRESOLVED', 'RESOLVED', 'IGNORED')); 
            """,
      )
    }
    changeSet(author = "aqua-rush", id = "1.0.0-create-reconciliation-reports-table") {
      comment("Create reconciliation reports table")
      createTable(tableName = "aqua_reconciliation_reports") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "task_id", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "report_type", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "report_data", type = "json") {
          constraints(nullable = false)
        }
        column(name = "file_path", type = "varchar(500)")
        column(name = "file_name", type = "varchar(255)")
        column(name = "file_size", type = "bigint")
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "generated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "aqua_reconciliation_reports", indexName = "idx_reconciliation_reports_task_id") {
        column(name = "task_id")
      }
      createIndex(tableName = "aqua_reconciliation_reports", indexName = "idx_reconciliation_reports_report_type") {
        column(name = "report_type")
      }
      createIndex(tableName = "aqua_reconciliation_reports", indexName = "idx_reconciliation_reports_generated_at") {
        column(name = "generated_at")
      }
      createIndex(tableName = "aqua_reconciliation_reports", indexName = "idx_reconciliation_reports_task_type") {
        column(name = "task_id")
        column(name = "report_type")
      }
      createIndex(tableName = "aqua_reconciliation_reports", indexName = "idx_reconciliation_reports_deleted") {
        column(name = "is_deleted")
      }
      addForeignKeyConstraint(
        baseTableName = "aqua_reconciliation_reports",
        baseColumnNames = "task_id",
        referencedTableName = "aqua_reconciliation_tasks",
        referencedColumnNames = "task_id",
        constraintName = "fk_reconciliation_reports_task_id",
        onDelete = "CASCADE",
      )
    }
    // =======================================================================
    // Transaction Tables (High Dependencies)
    // =======================================================================
    changeSet(author = "aqua-rush", id = "1.0.1-create-order-table") {
      comment("Create order table")
      createTable(tableName = "aqua_orders") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "order_no", type = "varchar(64)") {
          constraints(nullable = false, unique = true)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "total_amount_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "address_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(30)", defaultValue = "PENDING_PAYMENT") {
          constraints(nullable = false)
        }
        column(name = "payment_method", type = "varchar(20)")
        column(name = "payment_transaction_id", type = "varchar(128)")
        column(name = "paid_at", type = "timestamp")
        column(name = "delivery_worker_id", type = "bigint") {
          constraints(nullable = true)
        }
        // delivery_address_id 恒空列已删(缺陷 #7②,v3.4.1);订单地址为内联快照
        column(name = "delivery_photos", type = "json")
        column(name = "is_self_collect", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "payment_type", type = "varchar(20)")
        column(name = "remark", type = "varchar(500)")
        column(name = "delivery_started_at", type = "timestamp")
        // delivery_confirmed_at 已删(v3.4.3 订单 7 态:送达时间统一锚 completed_at)
        column(name = "completed_at", type = "timestamp")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        // 多商品下单(v1.2.0):单商品列 product_id/quantity 已移除,商品权威数据以 aqua_order_items 为准
        column(name = "delivery_remark", type = "varchar(500)")
        // 全文本搜索(v1.0.3)
        column(name = "search_content", type = "text")
        // 配送费快照(v1.0.7,R-MON-02,下单时按规则计算)
        column(name = "delivery_fee_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        // 营销快照(v1.0.9,下单时快照,已生成订单不受配置变更影响)
        column(name = "unit_price_cents", type = "bigint")
        column(name = "price_source", type = "varchar(30)")
        column(name = "promotion_activity_id", type = "bigint")
        column(name = "promotion_activity_name", type = "varchar(100)")
        column(name = "promotion_discount_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "coupon_id", type = "bigint")
        column(name = "coupon_name", type = "varchar(100)")
        column(name = "coupon_discount_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "ticket_redeem_count", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        // 首单立减快照(v1.1.0,R-MKT-10)
        column(name = "first_order_activity_id", type = "bigint")
        column(name = "first_order_activity_name", type = "varchar(100)")
        column(name = "first_order_discount_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        // 服务端幂等键与配送时段(v1.2.5,R-ORD-06/AC-ORD-001-05)
        column(name = "client_request_id", type = "varchar(64)")
        column(name = "delivery_time_slot", type = "varchar(20)")
        // 水票抵扣快照(v1.9.0,R-MKT-08)
        column(name = "ticket_discount_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        // 配送照片核验留痕(v2.0.0,G23)
        column(name = "delivery_photo_review_status", type = "varchar(20)")
        column(name = "delivery_photo_reviewed_at", type = "timestamp")
        column(name = "delivery_photo_reviewed_by", type = "bigint")
        column(name = "delivery_photo_review_remark", type = "varchar(500)")
        // 订单押金快照(v2.0.2,G06/R-MON-03,随单一并收取)
        column(name = "deposit_amount_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        // 派单/抢单记时(v3.4.4,缺陷 #9:履约时长锚点)
        column(name = "assigned_at", type = "timestamp")
        // 完成配送回收空桶总数快照(v3.4.6,01 §4.1)
        column(name = "empty_barrels_retrieved", type = "int")
      }
      createIndex(tableName = "aqua_orders", indexName = "idx_order_no") {
        column(name = "order_no")
      }
      createIndex(tableName = "aqua_orders", indexName = "idx_order_user_id") {
        column(name = "user_id")
      }
      createIndex(tableName = "aqua_orders", indexName = "idx_order_status") {
        column(name = "status")
      }
      createIndex(tableName = "aqua_orders", indexName = "idx_order_created_at") {
        column(name = "created_at")
      }
      createIndex(tableName = "aqua_orders", indexName = "idx_orders_deleted") {
        column(name = "is_deleted")
      }
      createIndex(tableName = "aqua_orders", indexName = "idx_orders_delivery_worker_id") {
        column(name = "delivery_worker_id")
      }
      createIndex(tableName = "aqua_orders", indexName = "idx_orders_is_self_collect") {
        column(name = "is_self_collect")
      }
      // 全文本搜索(v1.0.3)
      createIndex(tableName = "aqua_orders", indexName = "idx_orders_search_content") {
        column(name = "search_content")
      }
      // 服务端幂等唯一索引(v1.2.5,R-ORD-06:同一用户同一 clientRequestId 全局唯一)
      createIndex(indexName = "uk_orders_user_client_request", tableName = "aqua_orders", unique = true) {
        column(name = "user_id")
        column(name = "client_request_id")
      }
    }
    changeSet(author = "aqua-rush", id = "1.0.2-create-order-items-table") {
      comment("Create order items table for order line items")
      createTable(tableName = "aqua_order_items") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "order_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "product_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "quantity", type = "integer") {
          constraints(nullable = false)
        }
        column(name = "unit_price_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "total_price_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "product_snapshot", type = "json") {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
      }
      createIndex(tableName = "aqua_order_items", indexName = "idx_order_items_order_id") {
        column(name = "order_id")
      }
      createIndex(tableName = "aqua_order_items", indexName = "idx_order_items_product_id") {
        column(name = "product_id")
      }
      createIndex(tableName = "aqua_order_items", indexName = "idx_order_items_deleted") {
        column(name = "is_deleted")
      }
      addForeignKeyConstraint(
        baseTableName = "aqua_order_items",
        baseColumnNames = "order_id",
        referencedTableName = "aqua_orders",
        referencedColumnNames = "id",
        constraintName = "fk_order_items_order",
        onDelete = "CASCADE",
      )
      addForeignKeyConstraint(
        baseTableName = "aqua_order_items",
        baseColumnNames = "product_id",
        referencedTableName = "aqua_products",
        referencedColumnNames = "id",
        constraintName = "fk_order_items_product",
      )
      sql(
        sql = """
 ALTER TABLE aqua_order_items
      ADD CONSTRAINT chk_order_items_quantity_positive CHECK (quantity
        > 0), ADD CONSTRAINT chk_order_items_unit_price_cents_positive CHECK (unit_price_cents
      >= 0), ADD CONSTRAINT chk_order_items_total_price_cents_positive CHECK (total_price_cents
      >= 0); 
            """,
      )
    }
    changeSet(author = "aqua-rush", id = "1.0.0-create-aqua_payments-table") {
      comment("Create aqua_payments table")
      createTable(tableName = "aqua_payments") {
        column(name = "id", type = "BIGINT", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "order_id", type = "BIGINT") {
          constraints(nullable = false)
        }
        column(name = "user_id", type = "BIGINT") {
          constraints(nullable = false)
        }
        column(name = "transaction_id", type = "VARCHAR(255)") {
          constraints(unique = true, uniqueConstraintName = "uk_payments_transaction_id")
        }
        column(name = "prepay_id", type = "VARCHAR(255)") {
          constraints(unique = true, uniqueConstraintName = "uk_payments_prepay_id")
        }
        column(name = "amount_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "currency", type = "VARCHAR(3)") {
          constraints(nullable = false)
        }
        column(name = "status", type = "VARCHAR(20)") {
          constraints(nullable = false)
        }
        column(name = "payment_method", type = "VARCHAR(20)") {
          constraints(nullable = false)
        }
        column(name = "description", type = "TEXT")
        column(name = "failure_reason", type = "TEXT")
        column(name = "paid_at", type = "TIMESTAMP")
        column(name = "expired_at", type = "TIMESTAMP")
        column(name = "created_at", type = "TIMESTAMP", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "TIMESTAMP", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        // 乐观锁版本列(v2.7.0,R-ORD-04 支付超时 CAS 认领)
        column(name = "version", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
      }
      createIndex(indexName = "idx_payments_user_id", tableName = "aqua_payments") {
        column(name = "user_id")
      }
      createIndex(indexName = "idx_payments_status", tableName = "aqua_payments") {
        column(name = "status")
      }
      createIndex(indexName = "idx_payments_created_at", tableName = "aqua_payments") {
        column(name = "created_at")
      }
      createIndex(indexName = "idx_payments_order_id", tableName = "aqua_payments") {
        column(name = "order_id")
      }
      createIndex(indexName = "idx_payments_deleted", tableName = "aqua_payments") {
        column(name = "is_deleted")
      }
      addForeignKeyConstraint(
        baseTableName = "aqua_payments",
        baseColumnNames = "order_id",
        referencedTableName = "aqua_orders",
        referencedColumnNames = "id",
        constraintName = "fk_payments_order_id",
        onDelete = "CASCADE",
      )
      addForeignKeyConstraint(
        baseTableName = "aqua_payments",
        baseColumnNames = "user_id",
        referencedTableName = "aqua_users",
        referencedColumnNames = "id",
        constraintName = "fk_payments_user_id",
        onDelete = "CASCADE",
      )
      sql(
        sql = """
 ALTER TABLE aqua_payments
      ADD CONSTRAINT chk_payments_amount_cents_positive CHECK (amount_cents
        > 0), ADD CONSTRAINT chk_payments_currency_code CHECK (LENGTH(currency) = 3), ADD
      CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED',
      'CANCELLED', 'EXPIRED', 'REFUNDING', 'REFUNDED')); 
            """,
      )
    }
    changeSet(author = "aqua-rush", id = "1.0.0-create-payment-refunds-table") {
      comment("Create payment refunds table")
      createTable(tableName = "aqua_payment_refunds") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "payment_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "order_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "refund_id", type = "varchar(255)") {
          constraints(unique = true)
        }
        column(name = "refund_no", type = "varchar(100)") {
          constraints(unique = true)
        }
        column(name = "refund_amount_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "reason", type = "varchar(500)")
        column(name = "status", type = "varchar(20)", defaultValue = "PENDING") {
          constraints(nullable = false)
        }
        column(name = "refund_account", type = "varchar(100)")
        column(name = "failure_reason", type = "text")
        column(name = "processed_at", type = "timestamp")
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        // 退款审核流列(v2.0.1,R-PAY-02/G06;refund_id/refund_no 已放宽可空,申请阶段无微信退款单号)
        column(name = "refund_type", type = "varchar(20)", defaultValue = "ORDER_REFUND") {
          constraints(nullable = false)
        }
        column(name = "admin_note", type = "varchar(1000)")
        column(name = "processed_by", type = "bigint")
      }
      createIndex(tableName = "aqua_payment_refunds", indexName = "idx_payment_refunds_payment_id") {
        column(name = "payment_id")
      }
      createIndex(tableName = "aqua_payment_refunds", indexName = "idx_payment_refunds_order_id") {
        column(name = "order_id")
      }
      createIndex(tableName = "aqua_payment_refunds", indexName = "idx_payment_refunds_user_id") {
        column(name = "user_id")
      }
      createIndex(tableName = "aqua_payment_refunds", indexName = "idx_payment_refunds_status") {
        column(name = "status")
      }
      createIndex(tableName = "aqua_payment_refunds", indexName = "idx_payment_refunds_created_at") {
        column(name = "created_at")
      }
      createIndex(tableName = "aqua_payment_refunds", indexName = "idx_payment_refunds_deleted") {
        column(name = "is_deleted")
      }
      addForeignKeyConstraint(
        baseTableName = "aqua_payment_refunds",
        baseColumnNames = "payment_id",
        referencedTableName = "aqua_payments",
        referencedColumnNames = "id",
        constraintName = "fk_payment_refunds_payment",
      )
      addForeignKeyConstraint(
        baseTableName = "aqua_payment_refunds",
        baseColumnNames = "order_id",
        referencedTableName = "aqua_orders",
        referencedColumnNames = "id",
        constraintName = "fk_payment_refunds_order",
      )
      addForeignKeyConstraint(
        baseTableName = "aqua_payment_refunds",
        baseColumnNames = "user_id",
        referencedTableName = "aqua_users",
        referencedColumnNames = "id",
        constraintName = "fk_payment_refunds_user",
      )
      sql(
        sql = """
 ALTER TABLE aqua_payment_refunds
      ADD CONSTRAINT
        chk_payment_refunds_refund_amount_cents_positive CHECK (refund_amount_cents > 0), ADD
      CONSTRAINT chk_payment_refunds_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED',
      'COMPLETED', 'FAILED')); 
            """,
      )
    }
    changeSet(author = "aqua-rush", id = "1.0.0-create-aqua_reviews-table") {
      comment("Create review table")
      createTable(tableName = "aqua_reviews") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "order_id", type = "bigint") {
          constraints(nullable = false, unique = true)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(
          name = "delivery_worker_id",
          type = "bigint",
          remarks = "送水员账号 id(档案已并入 aqua_admins 单表,无外键约束)",
        ) {
          constraints(nullable = false)
        }
        column(name = "rating", type = "int") {
          constraints(nullable = false)
        }
        column(name = "comment", type = "text")
        column(name = "is_anonymous", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
      }
      createIndex(tableName = "aqua_reviews", indexName = "idx_reviews_order_id") {
        column(name = "order_id")
      }
      createIndex(tableName = "aqua_reviews", indexName = "idx_reviews_user_id") {
        column(name = "user_id")
      }
      createIndex(tableName = "aqua_reviews", indexName = "idx_reviews_delivery_worker_id") {
        column(name = "delivery_worker_id")
      }
      createIndex(tableName = "aqua_reviews", indexName = "idx_reviews_created_at") {
        column(name = "created_at")
      }
      createIndex(tableName = "aqua_reviews", indexName = "idx_reviews_rating") {
        column(name = "rating")
      }
      createIndex(tableName = "aqua_reviews", indexName = "idx_reviews_composite") {
        column(name = "delivery_worker_id")
        column(name = "created_at")
      }
      createIndex(tableName = "aqua_reviews", indexName = "idx_reviews_deleted") {
        column(name = "is_deleted")
      }
      addForeignKeyConstraint(
        baseTableName = "aqua_reviews",
        baseColumnNames = "order_id",
        referencedTableName = "aqua_orders",
        referencedColumnNames = "id",
        constraintName = "fk_reviews_order",
      )
      addForeignKeyConstraint(
        baseTableName = "aqua_reviews",
        baseColumnNames = "user_id",
        referencedTableName = "aqua_users",
        referencedColumnNames = "id",
        constraintName = "fk_reviews_user",
      )
      sql(
        sql = """
 ALTER TABLE aqua_reviews
      ADD CONSTRAINT chk_reviews_rating_range CHECK (rating >= 1 AND
                                                     rating <= 5), ADD CONSTRAINT chk_reviews_comment_length CHECK (LENGTH(COALESCE(comment,
      '')) <= 500); 
            """,
      )
      sql(
        sql = """
 CREATE
    OR REPLACE FUNCTION update_reviews_updated_at() RETURNS TRIGGER AS ${'$'}${'$'}
    BEGIN
      NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
    END ${'$'}${'$'} language 'plpgsql';
    CREATE TRIGGER
      trigger_reviews_updated_at
      BEFORE UPDATE
      ON aqua_reviews
      FOR EACH ROW EXECUTE FUNCTION update_reviews_updated_at(); 
            """,
      )
    }
    // =======================================================================
    // Triggers for Updated Timestamps
    // =======================================================================
    changeSet(author = "aqua-rush", id = "1.0.0-create-updated-at-triggers") {
      comment("Create triggers for automatic updated_at timestamp updates")
      sql(
        sql = """
 CREATE
    OR REPLACE FUNCTION update_updated_at_column() RETURNS TRIGGER AS ${'$'}${'$'}
    BEGIN
      NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
    END ${'$'}${'$'} LANGUAGE plpgsql;
    DROP TRIGGER IF EXISTS update_reconciliation_tasks_updated_at ON aqua_reconciliation_tasks;
    CREATE TRIGGER
      update_reconciliation_tasks_updated_at
      BEFORE UPDATE
      ON aqua_reconciliation_tasks
      FOR EACH ROW
      EXECUTE FUNCTION update_updated_at_column();
    DROP TRIGGER IF EXISTS
      update_reconciliation_discrepancies_updated_at ON aqua_reconciliation_discrepancies;
    CREATE TRIGGER
      update_reconciliation_discrepancies_updated_at
      BEFORE UPDATE
      ON aqua_reconciliation_discrepancies
      FOR EACH ROW EXECUTE FUNCTION update_updated_at_column(); 
            """,
      )
    }
    // =======================================================================
    // Bucket Deposit Management Tables
    // =======================================================================
    changeSet(author = "aqua-rush", id = "1.0.0-create-system-settings-table") {
      comment("Create system settings table (config params center, R-SYS-01)")
      createTable(tableName = "aqua_system_settings") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true)
        }
        column(name = "setting_key", type = "varchar(64)") {
          constraints(nullable = false, unique = true)
        }
        column(name = "setting_value", type = "varchar(255)")
        column(name = "description", type = "varchar(255)")
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        // 参数中心列扩展(v1.0.8, R-SYS-01:键 + 值 + 类型 + 分组 + 默认值)
        column(name = "setting_type", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "setting_group", type = "varchar(50)") {
          constraints(nullable = false)
        }
        column(name = "default_value", type = "varchar(255)")
        column(name = "min_value", type = "varchar(64)")
        column(name = "max_value", type = "varchar(64)")
        column(name = "options", type = "varchar(255)")
        column(name = "updated_by", type = "varchar(50)")
      }
    }
    changeSet(author = "aqua-rush", id = "1.0.0-create-bucket-deposits-table") {
      comment("Create bucket deposits table")
      createTable(tableName = "aqua_bucket_deposits") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "quantity", type = "int", defaultValueNumeric = 1) {
          constraints(nullable = false)
        }
        column(name = "amount_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "payment_transaction_id", type = "varchar(64)")
        column(name = "payment_time", type = "timestamp")
        column(name = "refunded_at", type = "timestamp")
        column(name = "refunded_by", type = "bigint")
        column(name = "remark", type = "varchar(255)")
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "aqua_bucket_deposits", indexName = "idx_bucket_deposits_user_id") {
        column(name = "user_id")
      }
      createIndex(tableName = "aqua_bucket_deposits", indexName = "idx_bucket_deposits_status") {
        column(name = "status")
      }
      createIndex(tableName = "aqua_bucket_deposits", indexName = "idx_bucket_deposits_created_at") {
        column(name = "created_at")
      }
    }
    // Drop legacy outbox tables - using Artemis MQ instead
    changeSet(author = "aqua-rush", id = "1.0.5-drop-order-domain-events-table") {
      comment("Drop order_domain_events table - using Artemis MQ instead")
      sql(" DROP TABLE IF EXISTS order_domain_events ")
    }
    // =======================================================================
    // Order Operations Table (merged from v1.0.3)
    // =======================================================================
    changeSet(author = "aqua-rush", id = "1.0.3-create-order-operations-table") {
      comment("Create aqua_order_operations table for order timeline tracking")
      createTable(tableName = "aqua_order_operations") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "order_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "operation_type", type = "varchar(50)") {
          constraints(nullable = false)
        }
        column(name = "operator_id", type = "bigint") {
          constraints(nullable = true)
        }
        column(name = "operator_type", type = "varchar(20)") {
          constraints(nullable = true)
        }
        column(name = "description", type = "varchar(500)") {
          constraints(nullable = true)
        }
        column(name = "extra_data", type = "json") {
          constraints(nullable = true)
        }
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "aqua_order_operations", indexName = "idx_order_operations_order_id") {
        column(name = "order_id")
      }
      createIndex(tableName = "aqua_order_operations", indexName = "idx_order_operations_created_at") {
        column(name = "created_at")
      }
      addForeignKeyConstraint(
        baseTableName = "aqua_order_operations",
        baseColumnNames = "order_id",
        referencedTableName = "aqua_orders",
        referencedColumnNames = "id",
        constraintName = "fk_order_operations_order_id",
      )
      sql(
        sql = """
      
        ALTER TABLE aqua_order_operations
          ADD CONSTRAINT chk_operation_type_valid
            CHECK (operation_type IN (
              'ORDER_CREATED',
              'ORDER_PAID',
              'ORDER_CANCELLED',
              'ORDER_URGING',
              'DELIVERY_ASSIGNED',
              'DELIVERY_STARTED',
              'DELIVERY_COMPLETED',
              'ORDER_COMPLETED',
              'REFUND_INITIATED',
              'REFUND_COMPLETED',
              'PAYMENT_TIMEOUT',
              'DELIVERY_PHOTO_APPROVED',
              'DELIVERY_PHOTO_REJECTED',
              'ADDRESS_UPDATED'
            )),
          ADD CONSTRAINT chk_operator_type_valid
            CHECK (operator_type IN (
              'USER',
              'ADMIN',
              'DELIVERY_WORKER',
              'SYSTEM'
            ));
      
    
            """,
      )
    }
    // =======================================================================
    // API Logs Table (merged from v1.0.4)
    // =======================================================================
    changeSet(author = "aqua-rush", id = "1.0.4-create-api-logs-table", dbms = "postgresql") {
      comment("Create aqua_api_logs table as monthly RANGE partition (v1.3.0 log partition F-SYS-006)")
      sql(
        sql = """
      
      CREATE TABLE aqua_api_logs (
        id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
        correlation_id varchar(50),
        request_type varchar(20) NOT NULL,
        method varchar(10),
        uri varchar(500),
        query text,
        request_body text,
        response_status int,
        duration_ms bigint,
        ip_address varchar(50),
        user_agent varchar(500),
        user_id bigint,
        username varchar(100),
        error_message text,
        operation_type varchar(20),
        operation_name varchar(100),
        created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT api_logs_pkey PRIMARY KEY (id, created_at)
      ) PARTITION BY RANGE (created_at);

      CREATE TABLE aqua_api_logs_default PARTITION OF aqua_api_logs DEFAULT;

      CREATE INDEX idx_api_logs_correlation_id ON aqua_api_logs (correlation_id);
      CREATE INDEX idx_api_logs_request_type ON aqua_api_logs (request_type);
      CREATE INDEX idx_api_logs_created_at ON aqua_api_logs (created_at);
      CREATE INDEX idx_api_logs_user_id ON aqua_api_logs (user_id);
      CREATE INDEX idx_api_logs_response_status ON aqua_api_logs (response_status);
      CREATE INDEX idx_api_logs_operation_type ON aqua_api_logs (operation_type);
      CREATE INDEX idx_api_logs_operation_name ON aqua_api_logs (operation_name);
      
    
            """,
      )
    }
    // =======================================================================
    // Business Logs Table (merged from v1.0.4)
    // =======================================================================
    changeSet(author = "aqua-rush", id = "1.0.4-create-business-logs-table", dbms = "postgresql") {
      comment("Create aqua_business_logs table as monthly RANGE partition (v1.3.0 log partition F-SYS-006; operation 列 v1.1.1)")
      sql(
        sql = """
      
      CREATE TABLE aqua_business_logs (
        id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
        correlation_id varchar(50),
        level varchar(10) NOT NULL,
        logger_name varchar(200),
        message text,
        stack_trace text,
        user_id bigint,
        username varchar(100),
        created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
        operation varchar(100),
        CONSTRAINT business_logs_pkey PRIMARY KEY (id, created_at),
        CONSTRAINT chk_log_level_valid CHECK (level IN ('TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'))
      ) PARTITION BY RANGE (created_at);

      CREATE TABLE aqua_business_logs_default PARTITION OF aqua_business_logs DEFAULT;

      CREATE INDEX idx_business_logs_correlation_id ON aqua_business_logs (correlation_id);
      CREATE INDEX idx_business_logs_level ON aqua_business_logs (level);
      CREATE INDEX idx_business_logs_created_at ON aqua_business_logs (created_at);
      CREATE INDEX idx_business_logs_user_id ON aqua_business_logs (user_id);
      CREATE INDEX idx_business_logs_logger_name ON aqua_business_logs (logger_name);
      
    
            """,
      )
    }
    // =======================================================================
    // User Action Logs Table (merged from v1.0.6)
    // =======================================================================
    changeSet(author = "aqua-rush", id = "1.0.6-create-user-action-logs-table", dbms = "postgresql") {
      comment("Create aqua_user_action_logs table as monthly RANGE partition (v1.3.0 log partition F-SYS-006)")
      sql(
        sql = """
      
      CREATE TABLE aqua_user_action_logs (
        id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
        user_id varchar(50),
        username varchar(100),
        action_type varchar(50) NOT NULL,
        target varchar(500),
        page_url varchar(1000),
        element_id varchar(100),
        element_type varchar(50),
        element_text varchar(200),
        client_ip varchar(50),
        user_agent varchar(500),
        properties text,
        created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
        CONSTRAINT user_action_logs_pkey PRIMARY KEY (id, created_at)
      ) PARTITION BY RANGE (created_at);

      CREATE TABLE aqua_user_action_logs_default PARTITION OF aqua_user_action_logs DEFAULT;

      CREATE INDEX idx_user_action_logs_user_id ON aqua_user_action_logs (user_id);
      CREATE INDEX idx_user_action_logs_action_type ON aqua_user_action_logs (action_type);
      CREATE INDEX idx_user_action_logs_created_at ON aqua_user_action_logs (created_at);
      
    
            """,
      )
    }
    // =======================================================================
    // 日志离线备份(F-SYS-006,v1.3.0):归档 schema + 备份记录表,仅 PostgreSQL
    // =======================================================================
    changeSet(author = "aqua-rush", id = "1.3.0-create-log-archive-schema-backup-records", dbms = "postgresql") {
      comment("Create log_archive schema and aqua_log_backup_records table (PRD 11-system-settings.md F-SYS-006)")
      sql(
        sql = """
      
      CREATE SCHEMA IF NOT EXISTS log_archive;

      CREATE TABLE aqua_log_backup_records (
        id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL PRIMARY KEY,
        log_table varchar(30) NOT NULL,
        partition_name varchar(100) NOT NULL,
        partition_month varchar(7) NOT NULL,
        cold_table_name varchar(200) NOT NULL,
        from_bound timestamp,
        to_bound timestamp,
        row_count bigint NOT NULL DEFAULT 0,
        file_path varchar(500),
        file_size_bytes bigint,
        file_checksum varchar(64),
        status varchar(20) NOT NULL,
        error_message text,
        operator_id bigint,
        operator_name varchar(100),
        started_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
        finished_at timestamp,
        created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
      );

      CREATE INDEX idx_log_backup_records_log_table ON aqua_log_backup_records (log_table);
      CREATE INDEX idx_log_backup_records_created_at ON aqua_log_backup_records (created_at);
      
    
            """,
      )
    }
  })
