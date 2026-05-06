-- global
SET GLOBAL character_set_client = utf8mb4;
SET GLOBAL character_set_connection = utf8mb4;
SET GLOBAL character_set_results = utf8mb4;
SET GLOBAL collation_connection = utf8mb4_general_ci;

-- casdoor database init

CREATE DATABASE IF NOT EXISTS casdoor COLLATE utf8mb4_general_ci;

-- rpa database init

CREATE DATABASE IF NOT EXISTS rpa COLLATE utf8mb4_general_ci;

USE rpa;
-- rpa.agent_table definition

CREATE TABLE `agent_table` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '항목증가기본 키',
  `agent_id` varchar(100) NOT NULL COMMENT 'RPA Agent ID',
  `content` mediumtext COMMENT 'Agent매칭항목정보(초과길이텍스트)',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제식별자: 0-삭제되지 않음, 1-삭제됨',
  `creator_id` varchar(36) DEFAULT NULL COMMENT '생성사람ID',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간, 삽입시항목항목성공',
  `updater_id` varchar(36) DEFAULT NULL COMMENT '업데이트사람ID',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간, 업데이트시항목업데이트',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_id` (`agent_id`) COMMENT 'AgentId전체영역항목일'
) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8mb4 COMMENT='RPA Agent매칭항목테이블';


-- rpa.alarm_rule definition

CREATE TABLE `alarm_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `enable` tinyint(3) DEFAULT NULL COMMENT '여부사용',
  `name` varchar(255) DEFAULT NULL COMMENT '항목이면이름',
  `condition` varchar(100) DEFAULT NULL COMMENT '항목파일JSON문자열: {"hours":23,"minutes":59,"count":10}',
  `duration` char(17) DEFAULT NULL COMMENT 'HH:MM:SS-HH:MM:SS  시간항목(열기항목-결과항목)',
  `role_id` char(36) DEFAULT NULL COMMENT '항목항목역할id',
  `process_id_list` mediumtext COMMENT 'processId',
  `event_module_code` int(11) DEFAULT NULL COMMENT '항목파일모듈항목코드',
  `event_module_name` varchar(255) DEFAULT NULL COMMENT '항목파일모듈',
  `event_type_code` int(11) DEFAULT NULL COMMENT '항목파일항목코드',
  `event_type_name` varchar(255) DEFAULT NULL COMMENT '항목파일유형',
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(6) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1992445240183595009 DEFAULT CHARSET=utf8mb4;


-- rpa.alarm_rule_user definition

CREATE TABLE `alarm_rule_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `alarm_rule_id` bigint(20) DEFAULT NULL COMMENT 'alarm_rule테이블id',
  `phone` varchar(200) DEFAULT NULL COMMENT '항목',
  `name` varchar(100) DEFAULT NULL COMMENT '사용자항목이름',
  `deleted` smallint(6) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1992445240238120961 DEFAULT CHARSET=utf8mb4;


-- rpa.app_application definition

CREATE TABLE `app_application` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `robot_id` varchar(100) NOT NULL COMMENT '봇ID',
  `robot_version` int(11) NOT NULL COMMENT '봇버전ID',
  `status` varchar(20) NOT NULL COMMENT '상태: 대기검토pending, 완료통신경과approved, 미완료통신경과rejected, 완료항목판매canceled, 항목nullify',
  `application_type` varchar(20) NOT NULL COMMENT '신청유형: release(위항목)/use(사용)',
  `security_level` varchar(10) DEFAULT NULL COMMENT '검토항목의비밀단계red,green,yellow',
  `allowed_dept` varchar(5000) DEFAULT NULL COMMENT '허용사용의부서ID목록',
  `expire_time` timestamp NULL DEFAULT NULL COMMENT '사용항목제한(항목중지날짜)',
  `audit_opinion` varchar(500) DEFAULT NULL COMMENT '검토항목',
  `creator_id` char(36) DEFAULT NULL COMMENT '신청사람ID',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자또는검토항목id',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` smallint(1) DEFAULT '0',
  `tenant_id` char(36) CHARACTER SET utf8 DEFAULT NULL,
  `client_deleted` smallint(1) DEFAULT '0' COMMENT '클라이언트의신청항목기록-삭제 여부',
  `cloud_deleted` smallint(1) DEFAULT '0' COMMENT '항목중항목의신청항목기록-삭제 여부',
  `default_pass` smallint(1) DEFAULT NULL COMMENT '선택항목색상비밀단계시, 후항목업데이트발송버전여부항목통신경과',
  `market_info` varchar(500) DEFAULT NULL COMMENT '팀마켓id대기정보, 사용항목일항목발송항목위항목신청, 검토통신경과후항목공유까지해당마켓',
  `publish_info` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_app_robot` (`robot_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2012152533336399875 DEFAULT CHARSET=utf8mb4 COMMENT='위항목/사용검토테이블';


-- rpa.app_application_tenant definition

CREATE TABLE `app_application_tenant` (
  `tenant_id` varchar(36) NOT NULL,
  `audit_enable` smallint(6) DEFAULT NULL COMMENT '여부열기시작검토, 1열기시작, 0아니오열기시작',
  `audit_enable_time` timestamp NULL DEFAULT NULL,
  `audit_enable_operator` char(36) DEFAULT NULL,
  `audit_enable_reason` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='테넌트여부열기시작검토매칭항목테이블';


-- rpa.app_market definition

CREATE TABLE `app_market` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tenant_id` char(36) DEFAULT NULL,
  `market_id` varchar(20) DEFAULT NULL COMMENT '팀마켓id',
  `market_name` varchar(60) DEFAULT NULL COMMENT '마켓이름',
  `market_describe` varchar(800) DEFAULT NULL COMMENT '마켓설명',
  `market_type` varchar(10) DEFAULT NULL COMMENT '마켓유형: team,official',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`),
  KEY `app_market_creator_id_IDX` (`creator_id`),
  KEY `app_market_market_id_IDX` (`market_id`),
  KEY `app_market_tenant_id_IDX` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=169 DEFAULT CHARSET=utf8mb4 COMMENT='팀마켓-팀테이블';


-- rpa.app_market_classification definition

CREATE TABLE `app_market_classification` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) DEFAULT NULL COMMENT '분유형이름',
  `source` smallint(1) DEFAULT NULL COMMENT '항목: 0-시스템시스템항목, 1-항목지정항목',
  `sort` int(11) DEFAULT NULL COMMENT '정렬',
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `creator_id` char(36) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updater_id` char(36) DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` smallint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `name_IDX` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=92371 DEFAULT CHARSET=utf8;


-- rpa.app_market_classification_map definition

CREATE TABLE `app_market_classification_map` (
  `english` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- rpa.app_market_dict definition

CREATE TABLE `app_market_dict` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `business_code` varchar(100) DEFAULT NULL COMMENT '항목서비스항목코드: 1, 행항목유형, 2, 역할공가능marketRoleFunc',
  `name` varchar(64) DEFAULT NULL COMMENT '행항목이름, 역할공가능이름',
  `dict_code` varchar(64) DEFAULT NULL COMMENT '행항목코드, 공가능항목코드',
  `dict_value` varchar(100) DEFAULT NULL COMMENT 'T있음권한, F없음권한',
  `user_type` varchar(100) DEFAULT NULL COMMENT 'owner,admin,acquirer,author',
  `description` varchar(256) DEFAULT NULL COMMENT '설명',
  `seq` int(11) DEFAULT NULL COMMENT '정렬',
  `creator_id` char(36) DEFAULT '73',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updater_id` char(36) DEFAULT '73',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` smallint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `app_market_dict_dict_code_IDX` (`dict_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=73 DEFAULT CHARSET=utf8;


-- rpa.app_market_invite definition

CREATE TABLE `app_market_invite` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키id',
  `invite_key` varchar(20) DEFAULT NULL COMMENT '초대항목연결key',
  `inviter_id` varchar(50) DEFAULT NULL COMMENT '초대사람id',
  `market_id` varchar(50) DEFAULT NULL COMMENT '마켓id',
  `current_join_count` int(11) DEFAULT NULL COMMENT '현재완료추가입력사람데이터',
  `max_join_count` int(11) DEFAULT NULL COMMENT '항목대추가입력사람데이터',
  `expire_time` timestamp NULL DEFAULT NULL COMMENT '실패항목시간',
  `expire_type` varchar(50) DEFAULT NULL COMMENT '실패항목유형',
  `creator_id` varchar(50) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` varchar(50) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` int(11) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invite_key` (`invite_key`)
) ENGINE=InnoDB AUTO_INCREMENT=83 DEFAULT CHARSET=utf8mb4 COMMENT='팀마켓-초대항목연결테이블';


-- rpa.app_market_resource definition

CREATE TABLE `app_market_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `market_id` varchar(20) DEFAULT NULL COMMENT '팀마켓id',
  `app_id` varchar(50) DEFAULT NULL COMMENT '항목사용id, 항목id, 컴포넌트id',
  `download_num` bigint(20) DEFAULT '0' COMMENT '다운로드항목데이터',
  `check_num` bigint(20) DEFAULT '0' COMMENT '조회항목데이터',
  `creator_id` char(36) DEFAULT NULL COMMENT '게시사람',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '게시시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `tenant_id` char(36) DEFAULT NULL,
  `robot_id` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '봇id',
  `app_name` varchar(64) CHARACTER SET utf8 DEFAULT NULL COMMENT '항목이름',
  PRIMARY KEY (`id`),
  KEY `app_market_resource_app_id_IDX` (`app_id`),
  KEY `app_market_resource_creator_id_IDX` (`creator_id`),
  KEY `app_market_resource_market_id_IDX` (`market_id`),
  KEY `app_market_resource_tenant_id_IDX` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=285 DEFAULT CHARSET=utf8mb4 COMMENT='팀마켓-항목항목테이블';


-- rpa.app_market_user definition

CREATE TABLE `app_market_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `market_id` varchar(20) DEFAULT NULL COMMENT '마켓id',
  `user_type` varchar(10) DEFAULT NULL COMMENT '구성원유형: owner,admin,acquirer,author',
  `creator_id` char(36) DEFAULT NULL COMMENT '구성원id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '추가입력시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`),
  KEY `app_market_user_creator_id_IDX` (`creator_id`),
  KEY `app_market_user_market_id_IDX` (`market_id`),
  KEY `app_market_user_tenant_id_IDX` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17233 DEFAULT CHARSET=utf8mb4 COMMENT='팀마켓-사람원테이블, n:n의닫기시스템';


-- rpa.app_market_version definition

CREATE TABLE `app_market_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `market_id` varchar(100) DEFAULT NULL COMMENT '마켓id',
  `app_id` varchar(50) DEFAULT NULL,
  `app_version` int(11) DEFAULT NULL COMMENT '항목사용버전, 항목봇버전',
  `edit_flag` tinyint(1) DEFAULT '1' COMMENT '항목생성의공유까지마켓, 여부지원항목/열기항목코드;0지원하지 않음, 1지원',
  `category` varchar(100) DEFAULT NULL COMMENT '공유까지마켓의봇행항목: 항목서비스, 항목, 상업항목대기',
  `creator_id` char(36) DEFAULT NULL COMMENT '게시사람',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '게시시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `category_id` bigint(20) DEFAULT NULL COMMENT '분유형id',
  PRIMARY KEY (`id`),
  KEY `app_market_version_app_id_IDX` (`app_id`) USING BTREE,
  KEY `app_market_version_market_id_IDX` (`market_id`) USING BTREE,
  KEY `idx_app_id_version_deleted` (`app_id`,`app_version`,`deleted`),
  KEY `idx_market_app_version` (`market_id`,`app_id`,`app_version`)
) ENGINE=InnoDB AUTO_INCREMENT=663 DEFAULT CHARSET=utf8mb4 COMMENT='팀마켓-항목사용버전테이블';


-- rpa.atom_like definition

CREATE TABLE `atom_like` (
  `id` int(20) NOT NULL AUTO_INCREMENT,
  `like_id` varchar(20) NOT NULL,
  `atom_key` varchar(100) NOT NULL COMMENT '기존항목가능항목의key, 전체영역항목일',
  `creator_id` char(36) NOT NULL,
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `is_deleted` smallint(1) NOT NULL DEFAULT '0',
  `updater_id` char(36) DEFAULT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=95 DEFAULT CHARSET=utf8 COMMENT='기존항목가능항목즐겨찾기';


-- rpa.atom_meta_duplicate_log definition

CREATE TABLE `atom_meta_duplicate_log` (
  `id` bigint(20) NOT NULL DEFAULT '0',
  `atom_key` varchar(100) DEFAULT NULL,
  `version` varchar(20) DEFAULT NULL COMMENT '기존항목가능항목버전',
  `request_body` mediumtext COMMENT '항목요청항목',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부',
  `creator_id` bigint(20) DEFAULT '73',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updater_id` bigint(20) DEFAULT '73',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- rpa.audit_checkpoint definition

CREATE TABLE `audit_checkpoint` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `audit_object_type` varchar(36) DEFAULT NULL COMMENT 'robot, dept',
  `last_processed_id` varchar(36) DEFAULT NULL,
  `audit_status` varchar(20) DEFAULT NULL COMMENT '시스템계획항목정도: counting, completed, pending,to_count',
  `count_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제식별자',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1261 DEFAULT CHARSET=utf8mb4 COMMENT='항목관리관리시스템계획항목테이블';


-- rpa.audit_record definition

CREATE TABLE `audit_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `event_module_code` int(11) DEFAULT NULL,
  `event_module_name` varchar(255) DEFAULT NULL COMMENT '항목파일모듈',
  `event_type_code` int(11) DEFAULT NULL,
  `event_type_name` varchar(255) DEFAULT NULL COMMENT '항목파일유형',
  `event_detail` varchar(255) DEFAULT NULL COMMENT '항목파일항목',
  `creator_id` char(36) DEFAULT NULL,
  `creator_name` varchar(255) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` char(36) DEFAULT NULL,
  `process_id_list` mediumtext,
  `role_id_list` mediumtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=157 DEFAULT CHARSET=utf8mb4;


-- rpa.c_atom_meta_new definition

CREATE TABLE `c_atom_meta_new` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `atom_key` varchar(100) DEFAULT NULL,
  `atom_content` mediumtext COMMENT '기존항목가능항목모든매칭항목정보, json',
  `sort` int(11) DEFAULT NULL COMMENT '기존항목가능항목항목순서',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_atom_key` (`atom_key`) COMMENT 'atom_key검색항목'
) ENGINE=InnoDB AUTO_INCREMENT=558 DEFAULT CHARSET=utf8mb4 COMMENT='클라이언트-새기존항목가능항목';


-- rpa.c_element definition

CREATE TABLE `c_element` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `element_id` varchar(100) DEFAULT NULL COMMENT '원항목id',
  `element_name` varchar(100) DEFAULT NULL COMMENT '원항목이름',
  `icon` varchar(100) DEFAULT NULL COMMENT '아이콘',
  `image_id` varchar(100) DEFAULT NULL COMMENT '이미지다운로드주소',
  `parent_image_id` varchar(100) DEFAULT NULL COMMENT '원항목의항목단계이미지다운로드주소',
  `element_data` mediumtext COMMENT '원항목내용',
  `deleted` smallint(6) DEFAULT '0',
  `creator_id` char(36) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updater_id` char(36) DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `robot_id` varchar(100) DEFAULT NULL,
  `robot_version` int(11) DEFAULT NULL,
  `group_id` varchar(30) DEFAULT NULL,
  `common_sub_type` varchar(50) DEFAULT NULL COMMENT 'cv이미지, sigle항목통신선택, batch데이터근거항목가져오기',
  `group_name` varchar(100) DEFAULT NULL,
  `element_type` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_element_robot_version` (`element_id`,`robot_id`,`robot_version`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_robot_info` (`robot_id`,`robot_version`),
  KEY `idx_element_name` (`element_name`),
  KEY `idx_element_id` (`element_id`)
) ENGINE=InnoDB AUTO_INCREMENT=102615 DEFAULT CHARSET=utf8mb4 COMMENT='클라이언트, 원항목정보';


-- rpa.c_global_var definition

CREATE TABLE `c_global_var` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) DEFAULT NULL,
  `global_id` varchar(100) DEFAULT NULL,
  `var_name` varchar(100) DEFAULT NULL,
  `var_type` varchar(100) DEFAULT NULL,
  `var_value` varchar(100) DEFAULT NULL,
  `var_describe` varchar(100) DEFAULT NULL,
  `deleted` smallint(6) DEFAULT NULL,
  `creator_id` char(36) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updater_id` char(36) DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `robot_id` varchar(100) DEFAULT NULL,
  `robot_version` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18477 DEFAULT CHARSET=utf8mb4 COMMENT='클라이언트-전역 변수';


-- rpa.c_group definition

CREATE TABLE `c_group` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `group_id` varchar(100) DEFAULT NULL,
  `group_name` varchar(100) DEFAULT NULL,
  `creator_id` char(36) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updater_id` char(36) DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` smallint(6) DEFAULT '0',
  `robot_id` varchar(100) DEFAULT NULL,
  `robot_version` int(11) DEFAULT NULL,
  `element_type` varchar(20) DEFAULT NULL COMMENT 'cv: cv선택; common:항목통신요소 선택',
  PRIMARY KEY (`id`),
  KEY `idx_robot_info` (`robot_id`,`robot_version`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_element_type` (`element_type`)
) ENGINE=InnoDB AUTO_INCREMENT=2013126979404636171 DEFAULT CHARSET=utf8mb4 COMMENT='원항목또는이미지의분그룹';


-- rpa.c_module definition

CREATE TABLE `c_module` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `module_id` varchar(100) DEFAULT NULL COMMENT '프로세스id',
  `module_content` mediumtext COMMENT '전체량python항목코드데이터근거',
  `module_name` varchar(100) DEFAULT NULL COMMENT 'python파일이름',
  `deleted` smallint(6) DEFAULT '0',
  `creator_id` char(36) DEFAULT '73',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updater_id` char(36) DEFAULT '73',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `robot_id` varchar(100) DEFAULT NULL,
  `robot_version` int(11) DEFAULT NULL,
  `breakpoint` mediumtext COMMENT '항목정보',
  PRIMARY KEY (`id`),
  KEY `c_module_module_id_IDX` (`module_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10831 DEFAULT CHARSET=utf8mb4 COMMENT='클라이언트-python모듈데이터근거';


-- rpa.c_param definition

CREATE TABLE `c_param` (
  `id` varchar(20) NOT NULL COMMENT '매개변수id',
  `var_direction` int(11) DEFAULT NULL COMMENT '입력/출력',
  `var_name` varchar(100) DEFAULT NULL COMMENT '매개변수이름',
  `var_type` varchar(100) DEFAULT NULL COMMENT '매개변수유형',
  `var_value` varchar(100) DEFAULT NULL COMMENT '매개변수내용',
  `var_describe` varchar(100) DEFAULT NULL COMMENT '매개변수설명',
  `process_id` varchar(100) DEFAULT NULL COMMENT '프로세스id',
  `creator_id` char(36) DEFAULT NULL,
  `updater_id` char(36) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT NULL,
  `update_time` timestamp NULL DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `robot_id` varchar(100) DEFAULT NULL,
  `robot_version` int(11) DEFAULT NULL,
  `module_id` varchar(100) DEFAULT NULL COMMENT 'python모듈id',
  KEY `c_param_id_IDX` (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- rpa.c_process definition

CREATE TABLE `c_process` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) DEFAULT NULL COMMENT '항목id',
  `process_id` varchar(100) DEFAULT NULL COMMENT '프로세스id',
  `process_content` mediumtext COMMENT '전체량프로세스데이터근거',
  `process_name` varchar(100) DEFAULT NULL COMMENT '프로세스이름',
  `deleted` smallint(6) DEFAULT '0',
  `creator_id` char(36) DEFAULT '73',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updater_id` char(36) DEFAULT '73',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `robot_id` varchar(100) DEFAULT NULL,
  `robot_version` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20533 DEFAULT CHARSET=utf8mb4 COMMENT='클라이언트-프로세스데이터근거';


-- rpa.c_project definition

CREATE TABLE `c_project` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) DEFAULT NULL,
  `project_name` varchar(200) CHARACTER SET utf8 DEFAULT NULL COMMENT '항목목록이름',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` datetime DEFAULT NULL COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` datetime DEFAULT NULL COMMENT '생성 시간',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '항목삭제 0: 삭제되지 않음 1: 삭제됨',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='항목테이블';


-- rpa.c_require definition

CREATE TABLE `c_require` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `project_id` varchar(100) DEFAULT NULL,
  `package_name` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '항목목록이름',
  `package_version` varchar(20) DEFAULT NULL,
  `mirror` varchar(100) DEFAULT NULL,
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` datetime DEFAULT NULL COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` datetime DEFAULT NULL COMMENT '생성 시간',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '항목삭제 0: 삭제되지 않음 1: 삭제됨',
  `robot_id` varchar(100) DEFAULT NULL,
  `robot_version` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=615 DEFAULT CHARSET=utf8mb4 COMMENT='python항목관리관리';


-- rpa.c_smart_version definition

CREATE TABLE `c_smart_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '항목증가기본 키',
  `smart_id` varchar(100) NOT NULL COMMENT '항목가능컴포넌트Id',
  `smart_type` varchar(100) DEFAULT NULL COMMENT '항목가능컴포넌트의유형',
  `content` mediumtext COMMENT '컴포넌트내용(초과길이텍스트)',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제식별자: 0-삭제되지 않음, 1-삭제됨',
  `robot_id` varchar(100) DEFAULT NULL COMMENT '봇Id',
  `robot_version` int(11) DEFAULT NULL COMMENT '봇버전항목',
  `creator_id` varchar(36) DEFAULT NULL COMMENT '생성사람ID',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간, 삽입시항목항목성공',
  `updater_id` varchar(36) DEFAULT NULL COMMENT '업데이트사람ID',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간, 업데이트시항목업데이트',
  PRIMARY KEY (`id`),
  KEY `idx_smart_id_robot_id` (`smart_id`,`robot_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=213 DEFAULT CHARSET=utf8mb4 COMMENT='항목가능컴포넌트버전테이블';


-- rpa.client_update_version definition

CREATE TABLE `client_update_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` char(15) NOT NULL COMMENT '버전',
  `version_num` mediumint(9) NOT NULL COMMENT '버전숫자',
  `download_url` varchar(255) NOT NULL COMMENT '다운로드항목연결',
  `update_info` mediumtext COMMENT '업데이트내용',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `os` varchar(255) DEFAULT NULL COMMENT '시스템시스템',
  `arch` varchar(255) DEFAULT NULL COMMENT '아키텍처',
  PRIMARY KEY (`id`),
  KEY `idx_version` (`version`),
  KEY `idx_version_num` (`version_num`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='클라이언트버전항목조회테이블';


-- rpa.cloud_terminal definition

CREATE TABLE `cloud_terminal` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `dept_id_path` varchar(100) DEFAULT NULL COMMENT '부서전체경로id',
  `name` varchar(100) DEFAULT NULL COMMENT '단말이름',
  `terminal_mac` varchar(100) DEFAULT NULL COMMENT '항목준비항목, 단말항목일식별자',
  `terminal_ip` varchar(100) DEFAULT NULL COMMENT 'ip',
  `terminal_status` varchar(100) DEFAULT NULL COMMENT '현재상태, 항목busy, 빈항목free, 항목offline',
  `terminal_des` varchar(100) DEFAULT NULL COMMENT '단말설명',
  `user_id` char(36) DEFAULT NULL COMMENT '항목항목사용자id',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '부서이름',
  `account_last` varchar(100) DEFAULT NULL COMMENT '항목항목계정',
  `user_name_last` varchar(100) DEFAULT NULL COMMENT '항목항목사용자명',
  `time_last` timestamp NULL DEFAULT NULL COMMENT '항목항목시간',
  `execute_time_total` bigint(20) DEFAULT '0' COMMENT '단일개단말항목계획실행시길이, 사용항목단말목록항목, 업데이트봇실행항목기록테이블시항목업데이트해당테이블',
  `execute_num` bigint(20) DEFAULT '0' COMMENT '단일개단말항목계획실행항목데이터, 업데이트봇실행항목기록테이블시항목업데이트해당테이블',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '단말항목기록생성 시간',
  `terminal_type` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `cloud_terminal_mac_tenant_index` (`terminal_mac`,`tenant_id`),
  KEY `cloud_terminal_tenant_id_IDX` (`tenant_id`,`dept_id_path`),
  KEY `cloud_terminal_terminal_mac_IDX` (`terminal_mac`),
  KEY `cloud_terminal_user_id_IDX` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='단말테이블';


-- rpa.component definition

CREATE TABLE `component` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키id',
  `component_id` varchar(100) NOT NULL COMMENT '봇항목일id, 가져오기의항목사용id',
  `name` varchar(100) NOT NULL COMMENT '현재이름문자, 사용항목목록항목',
  `creator_id` char(36) NOT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) NOT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `is_shown` smallint(1) NOT NULL DEFAULT '1' COMMENT '여부에서사용자목록항목항목 0: 아니오항목, 1: 항목',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `tenant_id` char(36) DEFAULT NULL,
  `app_id` varchar(50) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'appmarketResource중의항목사용id',
  `app_version` int(11) DEFAULT NULL COMMENT '가져오기의항목사용: 앱 마켓버전',
  `market_id` varchar(20) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '가져오기의항목사용: 마켓id',
  `resource_status` varchar(20) DEFAULT NULL COMMENT '항목상태: toObtain, obtained, toUpdate',
  `data_source` varchar(20) DEFAULT NULL COMMENT '항목: create 항목생성 ; market 마켓가져오기 ',
  `transform_status` varchar(20) DEFAULT NULL COMMENT 'editing 항목중, published 완료발송버전, shared 완료위항목, locked항목지정(없음법항목)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=177 DEFAULT CHARSET=utf8 COMMENT='컴포넌트테이블';


-- rpa.component_robot_block definition

CREATE TABLE `component_robot_block` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키id',
  `robot_id` varchar(100) CHARACTER SET utf8 NOT NULL COMMENT '봇id',
  `robot_version` int(10) NOT NULL COMMENT '봇버전항목',
  `component_id` varchar(100) CHARACTER SET utf8 NOT NULL COMMENT '컴포넌트id',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `tenant_id` char(36) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COMMENT='봇항목컴포넌트항목테이블';


-- rpa.component_robot_use definition

CREATE TABLE `component_robot_use` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키id',
  `robot_id` varchar(100) CHARACTER SET utf8 NOT NULL COMMENT '봇id',
  `robot_version` int(10) NOT NULL COMMENT '봇버전항목',
  `component_id` varchar(100) CHARACTER SET utf8 NOT NULL COMMENT '컴포넌트id',
  `component_version` int(10) NOT NULL COMMENT '컴포넌트버전항목',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `tenant_id` char(36) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2453 DEFAULT CHARSET=utf8mb4 COMMENT='봇항목컴포넌트항목사용테이블';


-- rpa.component_version definition

CREATE TABLE `component_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키id',
  `component_id` varchar(100) CHARACTER SET utf8 NOT NULL COMMENT '봇id',
  `version` int(10) NOT NULL COMMENT '버전항목',
  `introduction` longtext CHARACTER SET utf8 COMMENT '항목',
  `update_log` longtext CHARACTER SET utf8 COMMENT '변경 로그',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `tenant_id` char(36) DEFAULT NULL,
  `param` text CHARACTER SET utf8,
  `param_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '발송버전시항목의테이블단일매개변수정보',
  `icon` varchar(30) NOT NULL COMMENT '아이콘',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=287 DEFAULT CHARSET=utf8mb4 COMMENT='컴포넌트버전테이블';


-- rpa.consult_form definition

CREATE TABLE `consult_form` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `form_type` tinyint(4) DEFAULT NULL COMMENT '1=항목버전 2=항목버전 항목 3~99',
  `company_name` varchar(128) NOT NULL,
  `contact_name` varchar(64) NOT NULL,
  `mobile` varchar(20) NOT NULL,
  `email` varchar(128) DEFAULT NULL COMMENT '항목항목',
  `team_size` varchar(32) DEFAULT NULL COMMENT '사람데이터항목, 딕셔너리값',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0=대기항목관리 1=완료항목관리 2=완료항목',
  `remark` varchar(512) DEFAULT NULL COMMENT '항목서비스비고',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`form_type`,`status`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;


-- rpa.contact definition

CREATE TABLE `contact` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키ID',
  `name` varchar(100) NOT NULL COMMENT '항목이름',
  `phone` varchar(11) NOT NULL COMMENT '휴대폰 번호',
  `company_name` varchar(200) NOT NULL COMMENT '항목이름',
  `company_size` varchar(50) NOT NULL COMMENT '팀항목 매개항목CompanySizeEnum',
  `email` varchar(100) DEFAULT NULL COMMENT '메일함',
  `demand_desc` text COMMENT '필요항목설명',
  `contact_kind` varchar(50) NOT NULL COMMENT '항목문의유형 매개항목ContactKindEnum',
  `agreement` smallint(1) DEFAULT '1' COMMENT '여부항목항목 0-아니오항목 1-항목',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성사람ID',
  `updater_id` char(36) DEFAULT NULL COMMENT '업데이트사람ID',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0-삭제되지 않음 1-삭제됨',
  PRIMARY KEY (`id`),
  KEY `idx_company_name` (`company_name`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8 COMMENT='항목정보테이블';


-- rpa.dispatch_day_task_info definition

CREATE TABLE `dispatch_day_task_info` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `terminal_id` varchar(20) DEFAULT NULL COMMENT '단말id',
  `task_id` varchar(30) DEFAULT NULL COMMENT '작업ID',
  `task_name` varchar(30) DEFAULT NULL COMMENT '작업이름',
  `robot_id` varchar(30) DEFAULT NULL COMMENT '봇ID',
  `robot_name` varchar(30) DEFAULT NULL COMMENT '봇이름',
  `status` varchar(10) DEFAULT NULL COMMENT '현재상태 대기실행 todo /완료실행 done /에서실행 doing',
  `execute_time` varchar(10) DEFAULT NULL COMMENT '작업실행시간',
  `sort` int(11) DEFAULT NULL COMMENT '정렬, 항목소항목전',
  `tenant_id` varchar(36) DEFAULT NULL,
  `creator_id` varchar(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` varchar(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_task_id` (`task_id`),
  KEY `idx_robot_id` (`robot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='스케줄링항목방식:단말매일업로드의작업항목정보';


-- rpa.dispatch_task definition

CREATE TABLE `dispatch_task` (
  `dispatch_task_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '스케줄링항목방식예약 작업id',
  `status` varchar(10) NOT NULL DEFAULT '0' COMMENT '작업상태: 사용중active, 닫기stop, 완료경과항목expired',
  `name` varchar(50) DEFAULT NULL COMMENT '스케줄링항목방식예약 작업이름',
  `cron_json` mediumtext COMMENT '항목생성스케줄링예약 작업의항목매개변수;예약schedule저장계획계획실행의항목JSON',
  `type` varchar(10) DEFAULT NULL COMMENT '트리거항목파일: 항목트리거manual, 예약schedule, 예약트리거trigger',
  `exceptional` varchar(20) NOT NULL DEFAULT 'stop' COMMENT '항목오류예항목관리: 건너뛰기jump, 중지stop, 재시도후건너뛰기retry_jump, 재시도후중지retry_stop',
  `retry_num` int(11) DEFAULT NULL COMMENT '항목있음exceptional로retry시, 항목기록의재시도항목데이터',
  `timeout_enable` smallint(6) DEFAULT NULL COMMENT '여부사용시간 초과시간 1:사용 0:아니오사용',
  `timeout` int(11) DEFAULT '9999' COMMENT '시간 초과시간',
  `queue_enable` smallint(6) DEFAULT '0' COMMENT '여부사용정렬팀 1:사용 0:아니오사용',
  `screen_record_enable` smallint(6) DEFAULT '0' COMMENT '여부열기시작기록항목 1:사용 0:아니오사용',
  `virtual_desktop_enable` smallint(6) DEFAULT '0' COMMENT '여부열기시작항목항목 1:사용 0:아니오사용',
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(6) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`dispatch_task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1995762595529728001 DEFAULT CHARSET=utf8mb4;


-- rpa.dispatch_task_execute_record definition

CREATE TABLE `dispatch_task_execute_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `dispatch_task_id` bigint(20) DEFAULT NULL COMMENT '스케줄링항목방식예약 작업id',
  `dispatch_task_execute_id` bigint(20) DEFAULT NULL COMMENT '스케줄링항목방식예약 작업실행id',
  `count` int(11) DEFAULT NULL COMMENT '실행항목, 1, 2, 3....',
  `dispatch_task_type` varchar(20) DEFAULT NULL COMMENT '트리거항목파일: 항목트리거manual, 예약schedule, 예약트리거trigger',
  `result` varchar(20) DEFAULT NULL COMMENT '실행 결과항목:성공success, 실패error, 실행중executing, 중중지cancel, 아래발송실패dispatch_error, 실행실패exe_error',
  `start_time` datetime DEFAULT NULL COMMENT '실행시작 시간',
  `end_time` datetime DEFAULT NULL COMMENT '실행종료 시간',
  `execute_time` bigint(20) DEFAULT NULL COMMENT '실행항목시 단일위치초',
  `terminal_id` char(36) DEFAULT NULL COMMENT '단말항목일식별자, 예항목준비mac주소',
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `task_detail_json` mediumtext COMMENT '작업항목',
  `deleted` smallint(6) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`),
  KEY `idx_dispatch_task_teminal_task_id` (`dispatch_task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4;


-- rpa.dispatch_task_robot definition

CREATE TABLE `dispatch_task_robot` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `dispatch_task_id` bigint(20) DEFAULT NULL COMMENT '스케줄링항목방식예약 작업id',
  `robot_id` varchar(30) DEFAULT NULL COMMENT '봇ID',
  `online` tinyint(3) DEFAULT NULL COMMENT '여부사용버전:  0:미완료사용,1:완료사용',
  `version` int(11) DEFAULT NULL COMMENT '봇버전',
  `param_json` mediumtext COMMENT '봇구성 매개변수',
  `sort` int(11) DEFAULT NULL COMMENT '정렬, 항목소항목전',
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(6) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`),
  KEY `idx_dispatch_task_teminal_task_id` (`dispatch_task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4;


-- rpa.dispatch_task_robot_execute_record definition

CREATE TABLE `dispatch_task_robot_execute_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키id',
  `execute_id` bigint(20) DEFAULT NULL COMMENT '봇실행id',
  `dispatch_task_execute_id` bigint(20) DEFAULT NULL COMMENT '스케줄링항목방식예약 작업실행id',
  `robot_id` varchar(100) DEFAULT NULL COMMENT '봇id',
  `robot_version` int(11) DEFAULT NULL COMMENT '봇버전항목',
  `start_time` timestamp NULL DEFAULT NULL COMMENT '시작 시간',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '종료 시간',
  `execute_time` bigint(20) DEFAULT NULL COMMENT '실행항목시 단일위치초',
  `result` varchar(20) DEFAULT NULL COMMENT '실행 결과항목:: robotFail:실패,  robotSuccess:성공, robotCancel:가져오기항목(중중지), robotExecute:정상에서실행',
  `param_json` mediumtext COMMENT '봇실행매개변수',
  `error_reason` varchar(255) DEFAULT NULL COMMENT '오류원인',
  `execute_log` longtext COMMENT '로그내용',
  `video_local_path` varchar(200) DEFAULT NULL COMMENT '항목항목기록의본항목저장항목경로',
  `dept_id_path` varchar(100) DEFAULT NULL COMMENT '부서전체경로항목코드',
  `terminal_id` char(36) DEFAULT NULL COMMENT '단말항목일식별자, 예항목준비mac주소',
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(6) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `data_table_path` varchar(255) DEFAULT NULL COMMENT '데이터근거항목가져오기저장항목위치항목',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4;


-- rpa.dispatch_task_terminal definition

CREATE TABLE `dispatch_task_terminal` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `dispatch_task_id` bigint(20) DEFAULT NULL COMMENT '스케줄링항목방식예약 작업id',
  `terminal_or_group` varchar(10) DEFAULT NULL COMMENT '트리거항목파일: 단말teminal, 단말분그룹group',
  `execute_method` varchar(10) DEFAULT NULL COMMENT '실행방법방식: 항목기기일항목random_one, 전체실행all',
  `value` mediumtext COMMENT '항목값: 저장항목 list<id> ; 항목중단말항목: terminal_id(테이블terminal) 분그룹항목: id (terminal_group_name)',
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(6) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`),
  KEY `idx_dispatch_task_teminal_task_id` (`dispatch_task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4;


-- rpa.feedback_report definition

CREATE TABLE `feedback_report` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키ID',
  `report_no` varchar(32) NOT NULL COMMENT '항목일번호',
  `username` varchar(100) NOT NULL COMMENT '사용자로그인이름',
  `categories` text NOT NULL COMMENT '항목제목분유형목록(JSON형식)',
  `description` text NOT NULL COMMENT '항목제목설명',
  `image_ids` varchar(500) DEFAULT NULL COMMENT '이미지파일ID목록(항목분항목)',
  `create_time` datetime NOT NULL COMMENT '생성 시간',
  `update_time` datetime DEFAULT NULL COMMENT '수정 시간',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '항목삭제항목로그 0:삭제되지 않음 1:삭제됨',
  `processed` tinyint(4) DEFAULT '0' COMMENT '여부완료항목관리 0:미완료항목관리 1:완료항목관리',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_no` (`report_no`),
  KEY `idx_username` (`username`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_processed` (`processed`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='반대항목항목테이블';


-- rpa.file definition

CREATE TABLE `file` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '기본 키ID',
  `file_id` varchar(50) DEFAULT NULL COMMENT '파일항목의uuid',
  `path` varchar(100) DEFAULT NULL COMMENT '파일에서s3위항목의경로',
  `create_time` datetime DEFAULT NULL COMMENT '생성 시간',
  `update_time` datetime DEFAULT NULL COMMENT '수정 시간',
  `deleted` int(11) DEFAULT 0 COMMENT '항목삭제항목로그위치',
  `file_name` varchar(1000) DEFAULT NULL COMMENT '파일항목이름',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=283 DEFAULT CHARSET=utf8mb4 COMMENT='파일테이블';


-- rpa.his_base definition

CREATE TABLE `his_base` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `dept_id_path` varchar(100) DEFAULT NULL COMMENT '부서전체경로항목코드',
  `execute_success` bigint(20) DEFAULT NULL COMMENT '항목계획실행성공항목데이터',
  `execute_fail` bigint(20) DEFAULT NULL COMMENT '항목계획실행실패항목데이터',
  `execute_abort` bigint(20) DEFAULT NULL COMMENT '항목계획실행중중지항목데이터',
  `robot_num` bigint(20) DEFAULT NULL COMMENT '항목계획봇항목데이터',
  `execute_total` bigint(20) DEFAULT NULL COMMENT '봇항목계획실행항목데이터',
  `execute_time_total` bigint(20) DEFAULT NULL COMMENT '전체봇또는전체단말항목계획실행시길이, 단일위치초, 항목계획항목성공의',
  `execute_success_rate` decimal(5,2) DEFAULT NULL COMMENT '항목계획실행성공항목',
  `user_num` bigint(20) DEFAULT NULL COMMENT '항목계획사용자데이터량',
  `count_time` timestamp NULL DEFAULT NULL COMMENT '시스템계획시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) NOT NULL DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `terminal` bigint(20) DEFAULT NULL COMMENT '단말데이터량',
  `labor_save` bigint(20) DEFAULT NULL COMMENT '항목의사람항목',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1581 DEFAULT CHARSET=utf8 COMMENT='전체봇및전체단말항목테이블';


-- rpa.his_data_enum definition

CREATE TABLE `his_data_enum` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parent_code` varchar(100) DEFAULT NULL,
  `icon` varchar(100) DEFAULT NULL,
  `field` varchar(100) DEFAULT NULL,
  `text` varchar(100) DEFAULT NULL,
  `num` varchar(100) DEFAULT NULL,
  `unit` varchar(100) DEFAULT NULL,
  `percent` varchar(100) DEFAULT NULL,
  `tip` varchar(100) DEFAULT NULL,
  `order` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8 COMMENT='항목관리관리데이터근거항목항목조각매칭항목데이터근거항목';


-- rpa.his_robot definition

CREATE TABLE `his_robot` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `execute_num_total` bigint(20) DEFAULT NULL COMMENT '항목일실행항목데이터',
  `execute_success` bigint(20) DEFAULT NULL COMMENT '매일성공항목데이터',
  `execute_fail` bigint(20) DEFAULT NULL COMMENT '매일실패항목데이터',
  `execute_abort` bigint(20) DEFAULT NULL COMMENT '매일중중지항목데이터',
  `execute_success_rate` decimal(5,2) DEFAULT NULL COMMENT '매일성공항목',
  `execute_time` bigint(20) DEFAULT NULL COMMENT '매일실행시길이, 단일위치초',
  `count_time` timestamp NULL DEFAULT NULL COMMENT '시스템계획시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `robot_id` varchar(100) DEFAULT NULL,
  `user_id` char(36) DEFAULT NULL COMMENT '사용자id',
  `dept_id_path` varchar(100) DEFAULT NULL COMMENT '부서전체경로id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2571 DEFAULT CHARSET=utf8 COMMENT='단일개봇항목테이블,항목일데이터근거';


-- rpa.his_terminal definition

CREATE TABLE `his_terminal` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `dept_id_path` varchar(36) DEFAULT NULL COMMENT '부서전체경로id',
  `terminal_id` varchar(100) DEFAULT NULL COMMENT '항목준비mac',
  `execute_time` bigint(20) DEFAULT NULL COMMENT '매일실행시길이',
  `execute_num` bigint(20) DEFAULT NULL COMMENT '단말매일실행항목데이터',
  `count_time` timestamp NULL DEFAULT NULL COMMENT '시스템계획시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`),
  KEY `his_terminal_terminal_id_IDX` (`terminal_id`) USING BTREE,
  KEY `his_terminal_tenant_id_IDX` (`tenant_id`,`dept_id_path`) USING BTREE,
  KEY `his_terminal_count_time_IDX` (`count_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1241 DEFAULT CHARSET=utf8 COMMENT='단일개단말항목테이블';


-- rpa.install_package definition

CREATE TABLE `install_package` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키ID',
  `name` varchar(255) NOT NULL COMMENT '항목이름',
  `download_path` varchar(500) NOT NULL COMMENT '다운로드항목연결',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성사람ID',
  `updater_id` char(36) DEFAULT NULL COMMENT '업데이트사람ID',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '삭제 여부 0-삭제되지 않음 1-삭제됨',
  `is_online` tinyint(4) DEFAULT '0' COMMENT '여부위항목 0-아니오위항목 1-위항목',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='설치 패키지테이블';


-- rpa.notify_send definition

CREATE TABLE `notify_send` (
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(50) CHARACTER SET utf8 DEFAULT NULL COMMENT '수신항목',
  `message_info` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '메시지항목id',
  `message_type` varchar(20) CHARACTER SET utf8 DEFAULT NULL COMMENT '메시지유형: 항목사람메시지teamMarketInvite, 업데이트메시지teamMarketUpdate',
  `operate_result` smallint(2) DEFAULT NULL COMMENT '항목결과: 미완료항목1,  완료항목2, 완료추가입력3, 완료항목4',
  `market_id` varchar(500) CHARACTER SET utf8 DEFAULT NULL COMMENT '마켓id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(2) DEFAULT '0' COMMENT '삭제식별자',
  `user_type` varchar(10) CHARACTER SET utf8 DEFAULT NULL COMMENT '구성원유형: owner,admin,consumer',
  `app_name` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=375 DEFAULT CHARSET=utf8mb4 COMMENT='메시지알림-메시지테이블';


-- rpa.openapi_auth definition

CREATE TABLE `openapi_auth` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `user_id` char(36) DEFAULT NULL COMMENT '사용자id',
  `api_key` varchar(100) DEFAULT NULL,
  `prefix` varchar(10) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNIQUE` (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='openapi항목권한항목저장';


-- rpa.pypi_packages definition

CREATE TABLE `pypi_packages` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `package_name` varchar(255) NOT NULL,
  `oss_path` varchar(255) NOT NULL,
  `visibility` tinyint(1) DEFAULT '1' COMMENT 'visibility 1: 항목공유가능항목패키지 2: 개사람항목있음패키지 3: 항목정도패키지, 부서분사람가능항목',
  `user_id` char(36) DEFAULT '0' COMMENT '게시사용자id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_key` (`package_name`,`visibility`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- rpa.renewal_form definition

CREATE TABLE `renewal_form` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `form_type` tinyint(4) NOT NULL COMMENT '1=항목버전 2=항목버전 항목 3~99',
  `company_name` varchar(128) NOT NULL COMMENT '항목이름',
  `mobile` varchar(20) NOT NULL COMMENT '항목사람휴대폰 번호',
  `renewal_duration` varchar(32) NOT NULL COMMENT '항목시길이',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0=대기항목관리 1=완료항목관리 2=완료항목',
  `remark` varchar(512) DEFAULT NULL COMMENT '항목서비스비고',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`form_type`,`status`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='항목테이블단일테이블';


-- rpa.robot_design definition

CREATE TABLE `robot_design` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키id',
  `robot_id` varchar(100) DEFAULT NULL COMMENT '봇항목일id, 가져오기의항목사용id',
  `name` varchar(100) DEFAULT NULL COMMENT '현재이름문자, 사용항목목록항목',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `tenant_id` char(36) DEFAULT NULL,
  `app_id` varchar(50) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'appmarketResource중의항목사용id',
  `app_version` int(11) DEFAULT NULL COMMENT '가져오기의항목사용: 앱 마켓버전',
  `market_id` varchar(20) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '가져오기의항목사용: 마켓id',
  `resource_status` varchar(20) DEFAULT NULL COMMENT '항목상태: toObtain, obtained, toUpdate',
  `data_source` varchar(20) DEFAULT NULL COMMENT '항목: create 항목생성 ; market 마켓가져오기 ',
  `transform_status` varchar(20) DEFAULT NULL COMMENT 'editing 항목중, published 완료발송버전, shared 완료위항목, locked항목지정(없음법항목)',
  `edit_enable` varchar(100) DEFAULT NULL COMMENT '항목',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7929 DEFAULT CHARSET=utf8 COMMENT='항목단말봇테이블';


-- rpa.robot_execute definition

CREATE TABLE `robot_execute` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키id',
  `robot_id` varchar(100) DEFAULT NULL COMMENT '봇항목일id, 가져오기의항목사용id',
  `name` varchar(100) DEFAULT NULL COMMENT '현재이름문자, 사용항목목록항목',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `tenant_id` char(36) DEFAULT NULL,
  `app_id` varchar(50) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'appmarketResource중의항목사용id',
  `app_version` int(11) DEFAULT NULL COMMENT '가져오기의항목사용: 앱 마켓버전',
  `market_id` varchar(20) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '가져오기의항목사용: 마켓id',
  `resource_status` varchar(20) DEFAULT NULL COMMENT '항목상태: toObtain, obtained, toUpdate',
  `data_source` varchar(20) DEFAULT NULL COMMENT '항목: create 항목생성 ; market 마켓가져오기 ',
  `param_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '실행전사용자항목지정항목의테이블단일매개변수',
  `dept_id_path` varchar(200) DEFAULT NULL COMMENT '부서전체경로',
  `type` varchar(10) DEFAULT NULL COMMENT '항목새버전봇의유형, web, other',
  `latest_release_time` timestamp NULL DEFAULT NULL COMMENT '항목새버전발송버전시간',
  PRIMARY KEY (`id`),
  KEY `idx_robot_id` (`robot_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6239 DEFAULT CHARSET=utf8 COMMENT='항목단말봇테이블';


-- rpa.robot_execute_record definition

CREATE TABLE `robot_execute_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키id',
  `execute_id` varchar(30) DEFAULT NULL COMMENT '실행id',
  `robot_id` varchar(100) DEFAULT NULL COMMENT '봇id',
  `robot_version` int(10) DEFAULT NULL COMMENT '봇버전항목',
  `start_time` timestamp NULL DEFAULT NULL COMMENT '시작 시간',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '종료 시간',
  `execute_time` bigint(20) DEFAULT NULL COMMENT '실행항목시 단일위치초',
  `mode` varchar(60) DEFAULT NULL COMMENT '항목목록항목PROJECT_LIST ; 항목항목항목EDIT_PAGE; 예약 작업시작CRONTAB ; 실행기기실행 EXECUTOR',
  `task_execute_id` varchar(30) DEFAULT NULL COMMENT '예약 작업실행id, 항목schedule_task_execute의execute_id',
  `result` varchar(20) DEFAULT NULL COMMENT '실행 결과: robotFail:실패,  robotSuccess:성공, robotCancel:가져오기항목(중중지), robotExecute:정상에서실행',
  `error_reason` varchar(255) DEFAULT NULL COMMENT '오류원인',
  `execute_log` longtext COMMENT '로그내용',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `tenant_id` char(36) DEFAULT NULL,
  `video_local_path` varchar(200) DEFAULT NULL COMMENT '항목항목기록의본항목저장항목경로',
  `dept_id_path` varchar(100) DEFAULT NULL COMMENT '부서전체경로항목코드',
  `terminal_id` char(36) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '단말항목일식별자, 예항목준비mac주소',
  `data_table_path` varchar(255) DEFAULT NULL COMMENT '데이터근거항목가져오기저장항목위치항목',
  PRIMARY KEY (`id`),
  KEY `robot_execute_record_execute_id_IDX` (`execute_id`,`creator_id`,`tenant_id`) USING BTREE,
  KEY `idx_robot_id` (`robot_id`),
  KEY `idx_rer_task_execute_id` (`task_execute_id`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=66277 DEFAULT CHARSET=utf8 COMMENT='항목단말봇실행항목기록테이블';


-- rpa.robot_version definition

CREATE TABLE `robot_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키id',
  `robot_id` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '봇id',
  `version` int(10) DEFAULT NULL COMMENT '버전항목',
  `introduction` longtext CHARACTER SET utf8 COMMENT '항목',
  `update_log` longtext CHARACTER SET utf8 COMMENT '변경 로그',
  `use_description` longtext CHARACTER SET utf8 COMMENT '사용설명',
  `online` smallint(2) DEFAULT '0' COMMENT '여부사용 0:미완료사용,1:완료사용',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `tenant_id` char(36) DEFAULT NULL,
  `param` text CHARACTER SET utf8,
  `param_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '발송버전시항목의테이블단일매개변수정보',
  `video_id` varchar(100) DEFAULT NULL COMMENT '항목주소id',
  `appendix_id` varchar(100) DEFAULT NULL COMMENT '항목파일주소id',
  `icon` varchar(100) DEFAULT NULL COMMENT '아이콘',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7143 DEFAULT CHARSET=utf8mb4 COMMENT='항목단말봇버전테이블';


-- rpa.sample_templates definition

CREATE TABLE `sample_templates` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '기본 키',
  `sample_id` varchar(100) DEFAULT NULL COMMENT '항목id',
  `name` varchar(50) NOT NULL COMMENT '항목버전이름',
  `type` varchar(20) NOT NULL COMMENT '항목유형: robot_design, robot_execute, schedule_task 대기',
  `version` varchar(20) NOT NULL DEFAULT '1.0.0' COMMENT '항목항목항목버전항목(예 1.2.0)',
  `data` mediumtext NOT NULL COMMENT '항목매칭항목데이터근거(JSON 형식), 데이터근거라이브러리일행의데이터근거',
  `description` text COMMENT '항목설명',
  `is_active` tinyint(4) NOT NULL DEFAULT '1' COMMENT '여부사용(false 이면새사용자아니오비고입력)',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '항목삭제항목(항목물품관리삭제)',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COMMENT='시스템시스템항목지정항목의항목라이브러리, 사용항목비고입력사용자항목항목데이터근거.지원 robot, project, task 대기다중항목유형.';


-- rpa.sample_users definition

CREATE TABLE `sample_users` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '기본 키항목증가ID',
  `creator_id` char(36) NOT NULL COMMENT '사용자항목일식별자(예 UUID)',
  `tenant_id` varchar(36) DEFAULT NULL,
  `sample_id` varchar(100) NOT NULL COMMENT '닫기항목 sample_templates.sample_id',
  `name` varchar(100) NOT NULL COMMENT '사용자항목까지의이름(항목항목항목 name, 가능항목지정항목)',
  `data` mediumtext NOT NULL COMMENT '에서항목중비고입력의매칭항목데이터근거(JSON 문자열, 항목 Java 순서열항목)',
  `source` enum('system','user') NOT NULL DEFAULT 'system' COMMENT '항목: system(시스템시스템항목비고입력)또는 user(사용자항목생성/수정)',
  `version_injected` varchar(20) NOT NULL COMMENT '비고입력시항목사용항목의버전항목, 사용항목후항목업그레이드항목',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updated_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '항목후수정 시간',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1741 DEFAULT CHARSET=utf8mb4 COMMENT='항목기록사용자에서시스템시스템항목중비고입력의항목데이터근거, 예항목항목의항목중항목.';


-- rpa.schedule_task definition

CREATE TABLE `schedule_task` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `task_id` varchar(100) DEFAULT NULL COMMENT '예약 작업id',
  `name` varchar(64) DEFAULT NULL COMMENT '작업이름',
  `description` varchar(255) DEFAULT NULL COMMENT '설명',
  `exception_handle_way` varchar(64) DEFAULT NULL COMMENT '예외항목관리방법방식: stop중지  skip건너뛰기',
  `run_mode` varchar(64) DEFAULT NULL COMMENT '실행항목방식, 항목cycle, 예약fixed,항목지정항목custom',
  `cycle_frequency` varchar(10) DEFAULT NULL COMMENT '항목항목,단일위치초, -1로항목있음일항목, 3600, , , custom',
  `cycle_num` varchar(64) DEFAULT NULL COMMENT '항목지정항목항목, 항목유형, 매1시간, 매3시간, , 항목지정항목',
  `cycle_unit` varchar(20) DEFAULT NULL COMMENT '항목지정항목항목, 항목단일위치: minutes, hour',
  `status` varchar(64) DEFAULT NULL COMMENT '상태: doing실행중 close완료결과항목 ready대기실행',
  `enable` tinyint(1) DEFAULT NULL COMMENT '시작/사용 안 함',
  `schedule_type` varchar(64) DEFAULT NULL COMMENT '예약방법방식,day,month,week',
  `schedule_rule` varchar(255) DEFAULT NULL COMMENT '예약매칭항목(매칭항목객체)',
  `start_at` datetime DEFAULT NULL COMMENT '시작 시간',
  `end_at` datetime DEFAULT NULL COMMENT '종료 시간',
  `tenant_id` char(36) DEFAULT NULL,
  `enable_queue_execution` tinyint(1) DEFAULT NULL COMMENT '여부정렬팀실행',
  `cron_expression` varchar(50) DEFAULT NULL COMMENT 'cron테이블항목방식',
  `last_time` timestamp NULL DEFAULT NULL COMMENT '위항목가져오기시의nextTime',
  `next_time` timestamp NULL DEFAULT NULL COMMENT '아래항목실행시간',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성사람ID',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(6) DEFAULT NULL,
  `pull_time` timestamp NULL DEFAULT NULL COMMENT '위항목가져오기시간',
  `log_enable` varchar(5) CHARACTER SET utf8mb4 DEFAULT 'F' COMMENT '여부열기시작로그항목기록',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='스케줄링작업';


-- rpa.schedule_task_execute definition

CREATE TABLE `schedule_task_execute` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `task_id` varchar(20) DEFAULT NULL COMMENT '작업ID',
  `task_execute_id` varchar(20) DEFAULT NULL COMMENT '예약 작업실행id',
  `count` int(11) DEFAULT NULL COMMENT '실행항목, 1, 2, 3....',
  `result` varchar(20) DEFAULT NULL COMMENT '작업상태항목    성공  "success"     # 시작 실패     "start_error"     # 실행실패      "exe_error"     # 가져오기항목     CANCEL = "cancel"     # 실행중   "executing"',
  `start_time` datetime DEFAULT NULL COMMENT '실행시작 시간',
  `end_time` datetime DEFAULT NULL COMMENT '실행종료 시간',
  `tenant_id` char(36) DEFAULT NULL,
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_ste_query` (`tenant_id`,`creator_id`,`start_time`,`deleted`),
  KEY `idx_ste_status` (`tenant_id`,`creator_id`,`result`,`start_time`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=2317 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='예약 작업실행항목기록';


-- rpa.schedule_task_pull_log definition

CREATE TABLE `schedule_task_pull_log` (
  `id` bigint(20) DEFAULT NULL,
  `task_id` varchar(100) CHARACTER SET utf8 DEFAULT NULL COMMENT '예약 작업id',
  `pull_time` timestamp NULL DEFAULT NULL COMMENT '위항목가져오기시간',
  `last_time` timestamp NULL DEFAULT NULL COMMENT '위항목가져오기시의nextTime',
  `next_time` timestamp NULL DEFAULT NULL COMMENT '아래항목실행시간',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성사람ID',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- rpa.schedule_task_robot definition

CREATE TABLE `schedule_task_robot` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `task_id` varchar(30) DEFAULT NULL COMMENT '작업ID',
  `robot_id` varchar(30) DEFAULT NULL COMMENT '봇ID',
  `sort` int(11) DEFAULT NULL COMMENT '정렬, 항목소항목전',
  `tenant_id` char(36) DEFAULT NULL,
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `param_json` mediumtext COMMENT '예약 작업항목닫기매개변수',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=211 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='예약 작업봇목록';


-- rpa.shared_file definition

CREATE TABLE `shared_file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `file_id` bigint(20) DEFAULT NULL COMMENT '파일항목의uuid',
  `path` varchar(500) DEFAULT NULL COMMENT '파일에서s3위항목의경로',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `file_name` varchar(1000) DEFAULT NULL COMMENT '파일항목이름',
  `tags` varchar(512) DEFAULT NULL COMMENT '파일태그이름항목합치기',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자ID',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `file_type` tinyint(4) DEFAULT NULL COMMENT '파일유형: 0-위치항목유형 1-텍스트 2-WORD 3-PDF',
  `file_index_status` tinyint(4) DEFAULT NULL COMMENT '파일항목량항목상태:1-항목항목 2-항목성공 3-실패',
  `dept_id` varchar(100) DEFAULT NULL COMMENT '부서id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COMMENT='공유항목파일테이블';


-- rpa.shared_file_tag definition

CREATE TABLE `shared_file_tag` (
  `tag_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '태그id',
  `tag_name` varchar(255) DEFAULT NULL COMMENT '태그항목이름',
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자ID',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자ID',
  PRIMARY KEY (`tag_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1992443736764686337 DEFAULT CHARSET=utf8mb4 COMMENT='공유항목파일태그테이블';


-- rpa.shared_sub_var definition

CREATE TABLE `shared_sub_var` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '항목변수id',
  `shared_var_id` bigint(20) unsigned NOT NULL COMMENT '공유 변수id',
  `var_name` varchar(255) DEFAULT NULL COMMENT '항목변수이름',
  `var_type` varchar(20) DEFAULT NULL COMMENT '변수유형: text/password/array',
  `var_value` varchar(750) DEFAULT NULL COMMENT '변수항목값, 암호화이면로비밀문서, 아니오이면로항목문서',
  `encrypt` tinyint(1) DEFAULT NULL COMMENT '여부암호화:1-암호화',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_shared_var_id` (`shared_var_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='공유 변수-항목변수';


-- rpa.shared_var definition

CREATE TABLE `shared_var` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `shared_var_name` varchar(255) DEFAULT NULL COMMENT '공유 변수이름',
  `status` tinyint(1) DEFAULT NULL COMMENT '사용상태: 1사용',
  `remark` varchar(255) DEFAULT NULL COMMENT '변수설명',
  `dept_id` char(36) DEFAULT NULL COMMENT '항목부서ID',
  `usage_type` varchar(10) DEFAULT NULL COMMENT '가능사용계정유형항목(all/dept/select): 모든사람: all, 항목부서모든사람: dept, 항목지정사람: select',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `shared_var_type` varchar(20) DEFAULT NULL COMMENT '공유 변수유형: text/password/array/group',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_dept_id_path` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='공유 변수정보';


-- rpa.shared_var_key_tenant definition

CREATE TABLE `shared_var_key_tenant` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(36) NOT NULL,
  `key` varchar(500) DEFAULT NULL COMMENT '공유 변수테넌트키',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2012917907659145259 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='공유 변수테넌트키테이블';


-- rpa.shared_var_user definition

CREATE TABLE `shared_var_user` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shared_var_id` bigint(20) unsigned NOT NULL COMMENT '공유 변수id',
  `user_id` char(36) DEFAULT NULL COMMENT '사용자id',
  `user_name` varchar(100) DEFAULT NULL COMMENT '사용자항목이름',
  `user_phone` varchar(100) DEFAULT NULL COMMENT '사용자휴대폰 번호',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_shared_var_id` (`shared_var_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='공유 변수및사용자의항목테이블;N:N항목';


-- rpa.sms_record definition

CREATE TABLE `sms_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `receiver` varchar(30) CHARACTER SET utf8 DEFAULT NULL COMMENT '짧음정보수신항목',
  `send_type` varchar(30) CHARACTER SET utf8 DEFAULT NULL COMMENT '짧음정보유형',
  `send_result` varchar(20) CHARACTER SET utf8 DEFAULT NULL COMMENT '전송결과',
  `fail_reason` varchar(3000) CHARACTER SET utf8 DEFAULT NULL COMMENT '실패원인',
  `create_by` int(11) DEFAULT NULL COMMENT '생성자',
  `create_time` datetime DEFAULT NULL COMMENT '생성 시간',
  `update_by` int(11) DEFAULT NULL COMMENT '수정자',
  `update_time` datetime DEFAULT NULL COMMENT '수정 시간',
  `deleted` int(11) DEFAULT NULL COMMENT '여부삭제됨',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- rpa.sys_product_version definition

CREATE TABLE `sys_product_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키ID',
  `version_code` varchar(50) NOT NULL COMMENT '버전항목코드(예: personal, professional, enterprise)',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '삭제식별자: 0-삭제되지 않음, 1-삭제됨',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version_code` (`version_code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='제품품목버전테이블';


-- rpa.sys_tenant_config definition

CREATE TABLE `sys_tenant_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키ID',
  `tenant_id` varchar(64) NOT NULL COMMENT '테넌트ID',
  `version_id` bigint(20) NOT NULL COMMENT '버전ID, 닫기항목sys_product_version.id',
  `extra_config_json` text NOT NULL COMMENT '매칭항목빠름항목(JSON형식, 항목패키지항목type, base, final필드)',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '삭제식별자: 0-삭제되지 않음, 1-삭제됨',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=135 DEFAULT CHARSET=utf8mb4 COMMENT='테넌트매칭항목테이블';


-- rpa.sys_version_default_config definition

CREATE TABLE `sys_version_default_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키ID',
  `version_id` bigint(20) NOT NULL COMMENT '버전ID, 닫기항목sys_product_version.id',
  `resource_code` varchar(100) NOT NULL COMMENT '항목항목코드(예: designer_count, component_count대기)',
  `resource_type` tinyint(1) NOT NULL COMMENT '항목유형: 1-Quota(매칭금액), 2-Switch(열기닫기)',
  `parent_code` varchar(100) DEFAULT NULL COMMENT '항목단계항목항목코드(사용항목단계닫기시스템)',
  `default_value` int(11) NOT NULL COMMENT '항목값(항목Quota예데이터량, 항목Switch예0또는1)',
  `url_patterns` text COMMENT 'URL경로항목방식(JSON배열형식, 예: ["/api/v1/design/**"])',
  `description` varchar(500) DEFAULT NULL COMMENT '항목설명',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '삭제식별자: 0-삭제되지 않음, 1-삭제됨',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  PRIMARY KEY (`id`),
  KEY `idx_version_id` (`version_id`),
  KEY `idx_resource_code` (`resource_code`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COMMENT='버전항목매칭항목테이블';


-- rpa.t_tenant_expiration definition

CREATE TABLE `t_tenant_expiration` (
  `id` varchar(64) NOT NULL COMMENT '기본 키ID',
  `tenant_id` varchar(64) NOT NULL COMMENT '테넌트ID',
  `expiration_date` varchar(64) DEFAULT NULL COMMENT '까지항목시간(형식: YYYY-MM-DD, 항목항목항목버전로암호화데이터근거, 항목버전로항목문서)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `is_delete` tinyint(1) DEFAULT '0' COMMENT '삭제 여부(0-아니오, 1-예)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='테넌트까지항목정보테이블';


-- rpa.task_mail definition

CREATE TABLE `task_mail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` char(36) DEFAULT NULL,
  `tenant_id` char(36) DEFAULT NULL,
  `resource_id` varchar(255) DEFAULT NULL,
  `email_service` varchar(50) DEFAULT NULL COMMENT '메일함서비스서비스기기, 163Email, 126Email, qqEmail, customEmail',
  `email_protocol` varchar(50) DEFAULT NULL COMMENT '사용항목, POP3,IMAP',
  `email_service_address` varchar(255) DEFAULT NULL COMMENT '메일함서비스서비스기기주소',
  `port` varchar(50) DEFAULT NULL COMMENT '메일함서비스서비스기기단말항목',
  `enable_ssl` tinyint(1) DEFAULT NULL COMMENT '여부사용SSL',
  `email_account` varchar(255) DEFAULT NULL COMMENT '메일함계정',
  `authorization_code` varchar(255) DEFAULT NULL COMMENT '메일함권한 부여코드',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '삭제 여부',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4;


-- rpa.terminal definition

CREATE TABLE `terminal` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '기본 키id, 사용항목데이터근거예약시스템계획의항목정도관리관리',
  `terminal_id` char(36) NOT NULL COMMENT '단말항목일식별자, 예항목준비mac주소',
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `dept_id` varchar(100) DEFAULT NULL COMMENT '부서id',
  `dept_id_path` varchar(100) DEFAULT NULL COMMENT '부서전체경로id',
  `name` varchar(200) DEFAULT NULL COMMENT '단말이름',
  `account` varchar(100) DEFAULT NULL COMMENT '항목준비계정',
  `os` varchar(50) DEFAULT NULL COMMENT '운영체제',
  `ip` varchar(200) DEFAULT NULL COMMENT 'ip목록',
  `actual_client_ip` varchar(100) DEFAULT NULL COMMENT '항목연결항목IP, 서버감지후의권장ip',
  `custom_ip` varchar(20) DEFAULT NULL COMMENT '사용자항목지정항목ip',
  `port` int(11) DEFAULT NULL COMMENT '단말항목',
  `status` varchar(20) DEFAULT NULL COMMENT '현재상태, 실행중busy, 빈항목free, 항목offline, 단일기기중standalone',
  `remark` varchar(100) DEFAULT NULL COMMENT '단말설명',
  `user_id` varchar(100) DEFAULT NULL COMMENT '항목후로그인의사용자의id, 사용항목근거항목이름항목선택',
  `os_name` char(36) DEFAULT NULL COMMENT '정보항목: 항목항목준비사용자명',
  `os_pwd` varchar(200) DEFAULT NULL COMMENT '정보항목: 항목항목준비사용자비밀번호',
  `is_dispatch` smallint(6) DEFAULT NULL COMMENT '여부스케줄링항목방식',
  `monitor_url` varchar(100) DEFAULT NULL COMMENT '항목항목url',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '단말항목기록생성 시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) NOT NULL DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `custom_port` int(11) DEFAULT NULL COMMENT '항목지정항목단말항목',
  PRIMARY KEY (`id`),
  KEY `cloud_terminal_mac_tenant_index` (`tenant_id`),
  KEY `cloud_terminal_tenant_id_IDX` (`tenant_id`,`dept_id_path`),
  KEY `cloud_terminal_user_id_IDX` (`os_name`)
) ENGINE=InnoDB AUTO_INCREMENT=557 DEFAULT CHARSET=utf8mb4 COMMENT='단말테이블';


-- rpa.terminal_group definition

CREATE TABLE `terminal_group` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `group_id` bigint(20) DEFAULT NULL COMMENT '분그룹이름',
  `terminal_id` bigint(20) DEFAULT NULL COMMENT '단말id',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_id` (`group_id`),
  KEY `idx_terminal_id` (`terminal_id`)
) ENGINE=InnoDB AUTO_INCREMENT=85 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='단말분그룹-분그룹및단말의항목테이블;N:N항목';


-- rpa.terminal_group_info definition

CREATE TABLE `terminal_group_info` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `group_name` varchar(100) DEFAULT NULL COMMENT '분그룹이름',
  `terminal_id` varchar(20) DEFAULT NULL COMMENT '단말id',
  `dept_id` char(36) DEFAULT NULL COMMENT '항목부서ID',
  `usage_type` varchar(10) DEFAULT NULL COMMENT '가능사용계정유형항목(all/dept/select): 모든사람: all, 항목부서모든사람: dept, 항목지정사람: select',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_terminal_id` (`terminal_id`),
  KEY `idx_dept_id_path` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='단말분그룹';


-- rpa.terminal_group_user definition

CREATE TABLE `terminal_group_user` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `group_id` varchar(20) DEFAULT NULL COMMENT '분그룹이름',
  `user_id` char(36) DEFAULT NULL COMMENT '사용자id',
  `creator_id` char(36) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` char(36) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  `user_name` varchar(100) DEFAULT NULL COMMENT '사용자항목이름',
  `user_phone` varchar(100) DEFAULT NULL COMMENT '사용자휴대폰 번호',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_id` (`group_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='단말분그룹-분그룹및사용자의항목테이블;N:N항목';


-- rpa.terminal_login_history definition

CREATE TABLE `terminal_login_history` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `terminal_id` varchar(20) DEFAULT NULL COMMENT '단말id',
  `account` varchar(100) DEFAULT NULL COMMENT '계정',
  `user_name` varchar(100) DEFAULT NULL COMMENT '사용자명',
  `login_time` timestamp NULL DEFAULT NULL COMMENT '로그인시간',
  `logout_time` timestamp NULL DEFAULT NULL COMMENT '로그아웃시간',
  `creator_id` bigint(20) DEFAULT NULL COMMENT '생성자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updater_id` bigint(20) DEFAULT NULL COMMENT '수정자id',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='단말로그인계정항목항목기록';


-- rpa.terminal_login_record definition

CREATE TABLE `terminal_login_record` (
  `id` char(36) COLLATE utf8mb4_bin NOT NULL,
  `login_user_id` char(36) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '로그인사용자id',
  `login_phone` varchar(40) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '로그인휴대폰 번호',
  `login_name` varchar(40) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '로그인이름',
  `login_time` timestamp NULL DEFAULT NULL COMMENT '로그인시간',
  `logout_time` timestamp NULL DEFAULT NULL COMMENT '로그아웃시간',
  `terminal_id` varchar(20) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '단말id',
  `dept_id` char(36) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '부서id',
  `dept_id_path` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '부서전체경로id',
  `ip` varchar(40) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '로그인IP',
  `user_agent` varchar(512) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'user-agent',
  `login_status` int(11) NOT NULL COMMENT '여부로그인성공{0:로그인실패, 1:로그인성공}',
  `remark` varchar(1000) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '항목설명',
  `creator_id` char(36) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '생성자id',
  `updater_id` char(36) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '수정자id',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `deleted` smallint(1) DEFAULT '0' COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='단말로그인계정항목항목기록';


-- rpa.trigger_task definition

CREATE TABLE `trigger_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_id` bigint(20) DEFAULT NULL COMMENT '트리거기기예약 작업id',
  `name` varchar(50) DEFAULT NULL COMMENT '트리거기기예약 작업이름',
  `task_json` mediumtext COMMENT '항목생성예약 작업의항목매개변수',
  `creator_id` char(36) DEFAULT NULL,
  `updater_id` char(36) DEFAULT NULL,
  `deleted` smallint(1) NOT NULL DEFAULT '0',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 시간',
  `task_type` varchar(20) DEFAULT NULL COMMENT '작업유형: 예약schedule, 메일mail, 파일file, 항목hotKey:',
  `enable` smallint(1) NOT NULL DEFAULT '0' COMMENT '여부사용',
  `exceptional` varchar(20) NOT NULL DEFAULT 'stop' COMMENT '항목오류예항목관리: 건너뛰기jump, 중지stop',
  `timeout` int(10) DEFAULT '9999' COMMENT '시간 초과시간',
  `tenant_id` char(36) DEFAULT NULL COMMENT '테넌트id',
  `queue_enable` smallint(6) DEFAULT '0' COMMENT '여부사용정렬팀 1:사용 0:아니오사용',
  `retry_num` int(11) DEFAULT NULL COMMENT '항목있음exceptional로retry시, 항목기록의재시도항목데이터',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=99 DEFAULT CHARSET=utf8 COMMENT='트리거기기예약 작업';


-- rpa.user_blacklist definition

CREATE TABLE `user_blacklist` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(50) NOT NULL COMMENT '사용자ID',
  `username` varchar(100) NOT NULL COMMENT '사용자명',
  `ban_reason` varchar(500) DEFAULT NULL COMMENT '항목원인',
  `ban_level` int(11) DEFAULT '1' COMMENT '항목대기단계(1,2,3...)',
  `ban_count` int(11) DEFAULT '1' COMMENT '항목항목데이터',
  `ban_duration` bigint(20) DEFAULT NULL COMMENT '항목시길이(초)',
  `start_time` datetime NOT NULL COMMENT '항목시작 시간',
  `end_time` datetime NOT NULL COMMENT '항목종료 시간',
  `status` tinyint(4) DEFAULT '1' COMMENT '상태(1:항목중, 0:완료해제항목)',
  `operator` varchar(50) DEFAULT NULL COMMENT '항목사람',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_end_time_status` (`end_time`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- rpa.user_entitlement definition

CREATE TABLE `user_entitlement` (
  `id` varchar(64) NOT NULL COMMENT '기본 키ID',
  `user_id` varchar(64) NOT NULL COMMENT '사용자ID',
  `tenant_id` varchar(64) NOT NULL COMMENT '테넌트ID',
  `module_designer` tinyint(1) DEFAULT '0' COMMENT '항목계획기기권한(0-없음권한, 1-있음권한)',
  `module_executor` tinyint(1) DEFAULT '0' COMMENT '실행기기권한(0-없음권한, 1-있음권한)',
  `module_console` tinyint(1) DEFAULT '0' COMMENT '항목제어항목권한(0-없음권한, 1-있음권한)',
  `module_market` tinyint(1) DEFAULT '1' COMMENT '팀마켓권한(0-없음권한, 1-있음권한, 항목1)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `is_delete` tinyint(1) DEFAULT '0' COMMENT '삭제 여부(0-아니오, 1-예)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tenant` (`user_id`,`tenant_id`,`is_delete`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_is_delete` (`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사용자권한항목테이블';

-- rpa.astron_agent_auth definition

CREATE TABLE `astron_agent_auth` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(50) DEFAULT NULL,
  `astron_user_name` varchar(50) DEFAULT NULL,
  `name` varchar(50) DEFAULT NULL,
  `app_id` varchar(50) DEFAULT NULL,
  `api_key` varchar(100) DEFAULT NULL,
  `api_secret` varchar(100) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_astron_agent_auth_id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4;

-- rpa.openai_workflows definition

CREATE TABLE `openai_workflows` (
  `project_id` varchar(100) NOT NULL COMMENT '항목목록ID(기본 키)',
  `name` varchar(100) NOT NULL COMMENT '워크플로이름',
  `description` varchar(500) DEFAULT NULL COMMENT '워크플로설명',
  `version` int(11) NOT NULL DEFAULT '1' COMMENT '워크플로버전항목',
  `status` int(11) NOT NULL DEFAULT '1' COMMENT '워크플로상태(1=항목, 0=사용 안 함)',
  `user_id` varchar(50) NOT NULL COMMENT '사용자ID',
  `example_project_id` varchar(100) DEFAULT NULL COMMENT '예시사용자계정아래의project_id, 사용항목실행시항목',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
  `english_name` varchar(100) DEFAULT NULL COMMENT '항목후의영어이름',
  `parameters` text COMMENT '저장항목JSON문자열형식의매개변수',
  PRIMARY KEY (`project_id`),
  KEY `idx_name` (`name`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- rpa.openai_executions definition

CREATE TABLE `openai_executions` (
  `id` varchar(36) NOT NULL COMMENT '실행항목기록ID(UUID)',
  `project_id` varchar(100) NOT NULL COMMENT '항목목록ID(닫기항목워크플로)',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '실행상태(PENDING/RUNNING/COMPLETED/FAILED/CANCELLED)',
  `parameters` text COMMENT '실행매개변수(JSON형식)',
  `result` text COMMENT '실행 결과(JSON형식)',
  `error` text COMMENT '오류정보',
  `user_id` varchar(50) NOT NULL COMMENT '사용자ID',
  `exec_position` varchar(50) NOT NULL DEFAULT 'EXECUTOR' COMMENT '실행위치항목',
  `recording_config` text COMMENT '기록제어매칭항목',
  `version` int(11) DEFAULT NULL COMMENT '워크플로버전항목',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '시작 시간',
  `end_time` datetime DEFAULT NULL COMMENT '종료 시간',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`),
  CONSTRAINT `openai_executions_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `openai_workflows` (`project_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- rpa.openapi_users definition

CREATE TABLE `openapi_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(50) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `default_api_key` varchar(100) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  UNIQUE KEY `phone` (`phone`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=1151 DEFAULT CHARSET=utf8mb4;


-- rpa.point_allocations definition

CREATE TABLE `point_allocations` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(50) NOT NULL,
  `initial_amount` int(11) NOT NULL COMMENT '기존항목분매칭데이터량',
  `remaining_amount` int(11) NOT NULL COMMENT '현재항목데이터량',
  `allocation_type` varchar(100) NOT NULL COMMENT '항목분항목',
  `priority` int(11) NOT NULL DEFAULT '0' COMMENT '항목단계, 데이터값항목높음항목단계항목높음',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` datetime NOT NULL COMMENT '항목분경과항목시간',
  `description` varchar(255) DEFAULT NULL COMMENT '설명',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_expires_at` (`expires_at`),
  KEY `idx_user_expiry` (`user_id`,`expires_at`)
) ENGINE=InnoDB AUTO_INCREMENT=83 DEFAULT CHARSET=utf8mb4;


-- rpa.point_consumptions definition

CREATE TABLE `point_consumptions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `transaction_id` bigint(20) NOT NULL COMMENT '닫기항목의항목ID',
  `allocation_id` bigint(20) NOT NULL COMMENT '닫기항목의분매칭ID',
  `amount` int(11) NOT NULL COMMENT '에서항목분매칭중사용의항목분데이터량',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2809 DEFAULT CHARSET=utf8mb4;


-- rpa.point_transactions definition

CREATE TABLE `point_transactions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(100) NOT NULL,
  `amount` int(11) NOT NULL COMMENT '항목항목금액(정상데이터또는항목데이터)',
  `transaction_type` varchar(50) NOT NULL COMMENT '항목유형',
  `related_entity_type` varchar(50) DEFAULT NULL COMMENT '닫기항목항목유형',
  `related_entity_id` bigint(20) DEFAULT NULL COMMENT '닫기항목항목ID',
  `description` varchar(255) DEFAULT NULL COMMENT '설명',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2891 DEFAULT CHARSET=utf8mb4;