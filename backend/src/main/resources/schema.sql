-- 实验室 · 试剂柜与领用
SET NAMES utf8mb4;

DROP TABLE IF EXISTS bench_order;
DROP TABLE IF EXISTS usage_log;
DROP TABLE IF EXISTS instrument;
DROP TABLE IF EXISTS reagent;
DROP TABLE IF EXISTS cabinet;

CREATE TABLE cabinet (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  cabinet_code   VARCHAR(24) NOT NULL,
  cabinet_name   VARCHAR(64) NOT NULL,
  cabinet_kind   VARCHAR(24) NULL,
  cabinet_status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cabinet_code (cabinet_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reagent (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  reagent_code   VARCHAR(24) NOT NULL,
  reagent_name   VARCHAR(64) NOT NULL,
  spec_text      VARCHAR(32) NULL,
  cabinet_id     BIGINT      NULL,
  balance        INT         NOT NULL DEFAULT 0,
  reserved       INT         NOT NULL DEFAULT 0,
  expire_date    DATE        NULL,
  reagent_status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reagent_code (reagent_code),
  KEY idx_reagent_cabinet (cabinet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE instrument (
  id                BIGINT      NOT NULL AUTO_INCREMENT,
  instrument_code   VARCHAR(24) NOT NULL,
  instrument_name   VARCHAR(64) NOT NULL,
  model_text        VARCHAR(32) NULL,
  keeper            VARCHAR(32) NULL,
  instrument_status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_instrument_code (instrument_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE usage_log (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  log_no     VARCHAR(24) NOT NULL,
  reagent_id BIGINT      NOT NULL,
  user_name  VARCHAR(32) NOT NULL,
  use_date   DATE        NULL,
  quantity   INT         NOT NULL,
  direction  VARCHAR(8)  NOT NULL,
  purpose    VARCHAR(64) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_log_no (log_no),
  KEY idx_log_reagent (reagent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 对照试验开台单：open_flag 未收口时恒为 1、收口后置空，
-- 靠 (instrument_id, open_flag) 唯一索引挡住「一台仪器两条开着的单」（MySQL 唯一索引里 NULL 不互斥）
CREATE TABLE bench_order (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  order_no      VARCHAR(24) NOT NULL,
  instrument_id BIGINT      NOT NULL,
  reagent_id    BIGINT      NOT NULL,
  user_name     VARCHAR(32) NOT NULL,
  quantity      INT         NOT NULL DEFAULT 1,
  order_status  VARCHAR(16) NOT NULL,
  open_time     DATETIME    NOT NULL,
  close_time    DATETIME    NULL,
  open_flag     INT         NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bench_order_no (order_no),
  UNIQUE KEY uk_bench_open (instrument_id, open_flag),
  KEY idx_bench_reagent (reagent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO cabinet (cabinet_code, cabinet_name, cabinet_kind, cabinet_status) VALUES
('CB-01', '通风柜 A', '通风柜', '可用'),
('CB-02', '普通试剂柜 B', '普通柜', '可用'),
('CB-03', '危化品防爆柜 C', '防爆柜', '可用'),
('CB-04', '备用柜 D', '普通柜', '停用');

-- RG-04 的有效期已经过去，领不动；RG-05 一个月内到期，会出现在临期清单里
INSERT INTO reagent (reagent_code, reagent_name, spec_text, cabinet_id, balance, expire_date, reagent_status) VALUES
('RG-01', '无水乙醇', '500mL', 1, 12, '2027-03-01', '可用'),
('RG-02', '氢氧化钠', '500g',  3,  6, '2026-12-01', '可用'),
('RG-03', '盐酸',     '500mL', 3,  4, '2027-01-15', '可用'),
('RG-04', '硫酸铜',   '250g',  2,  3, '2026-08-01', '可用'),
('RG-05', '酚酞指示剂', '100mL', 2,  8, '2026-10-05', '可用');

INSERT INTO instrument (instrument_code, instrument_name, model_text, keeper, instrument_status) VALUES
('IN-01', '电子天平', 'FA2004', '周老师', '可用'),
('IN-02', '紫外分光光度计', 'UV-1800', '周老师', '可用'),
('IN-03', '离心机', 'TG16', '李师姐', '维修中'),
('IN-04', '恒温水浴锅', 'HH-4', '李师姐', '可用'),
('IN-05', '马弗炉', 'SX2-4-10', '周老师', '停用');

INSERT INTO usage_log (log_no, reagent_id, user_name, use_date, quantity, direction, purpose) VALUES
('UL-01', 1, '张三', '2026-09-15', 2, '领用', '样品溶解'),
('UL-02', 2, '李四', '2026-09-16', 1, '领用', '配制溶液'),
('UL-03', 5, '张三', '2026-09-17', 1, '领用', '酸碱滴定'),
('UL-04', 1, '王五', '2026-09-18', 3, '领用', '清洗器皿'),
('UL-05', 1, '王五', '2026-09-18', 1, '退回', '多领了还回来');
