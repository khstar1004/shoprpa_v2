-- 테넌트까지정보테이블
CREATE TABLE IF NOT EXISTS `t_tenant_expiration` (
  `id` VARCHAR(64) NOT NULL COMMENT '기본 키ID',
  `tenant_id` VARCHAR(64) NOT NULL COMMENT '테넌트ID',
  `expiration_date` VARCHAR(64) DEFAULT NULL COMMENT '까지시간(형식: YYYY-MM-DD, 버전로암호화데이터, 버전로문서)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `is_delete` TINYINT(1) DEFAULT 0 COMMENT '삭제 여부(0-아니요, 1-예)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='테넌트까지정보테이블';
