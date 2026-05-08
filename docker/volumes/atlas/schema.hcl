table "agent_table" {
  schema  = schema.rpa
  comment = "RPA Agent매칭항목테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "항목증가기본 키"
    auto_increment = true
  }
  column "agent_id" {
    null    = false
    type    = varchar(100)
    comment = "RPA Agent ID"
  }
  column "content" {
    null    = true
    type    = mediumtext
    comment = "Agent매칭항목정보(초과길이텍스트)"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제식별자: 0-삭제되지 않음, 1-삭제됨"
  }
  column "creator_id" {
    null    = true
    type    = varchar(36)
    comment = "생성사람ID"
  }
  column "create_time" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간, 삽입시항목항목성공"
  }
  column "updater_id" {
    null    = true
    type    = varchar(36)
    comment = "업데이트사람ID"
  }
  column "update_time" {
    null      = false
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간, 업데이트시항목업데이트"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  index "uk_agent_id" {
    unique  = true
    columns = [column.agent_id]
    comment = "AgentId전체영역항목일"
  }
}
table "alarm_rule" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "enable" {
    null    = true
    type    = tinyint
    comment = "여부사용"
  }
  column "name" {
    null    = true
    type    = varchar(255)
    comment = "항목이면이름"
  }
  column "condition" {
    null    = true
    type    = varchar(100)
    comment = "항목파일JSON문자열: {\"hours\":23,\"minutes\":59,\"count\":10}"
  }
  column "duration" {
    null    = true
    type    = char(17)
    comment = "HH:MM:SS-HH:MM:SS  시간항목(열기항목-결과항목)"
  }
  column "role_id" {
    null    = true
    type    = char(36)
    comment = "항목항목역할id"
  }
  column "process_id_list" {
    null    = true
    type    = mediumtext
    comment = "processId"
  }
  column "event_module_code" {
    null    = true
    type    = int
    comment = "항목파일모듈항목코드"
  }
  column "event_module_name" {
    null    = true
    type    = varchar(255)
    comment = "항목파일모듈"
  }
  column "event_type_code" {
    null    = true
    type    = int
    comment = "항목파일항목코드"
  }
  column "event_type_name" {
    null    = true
    type    = varchar(255)
    comment = "항목파일유형"
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = smallint
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
}
table "alarm_rule_user" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "alarm_rule_id" {
    null    = true
    type    = bigint
    comment = "alarm_rule테이블id"
  }
  column "phone" {
    null    = true
    type    = varchar(200)
    comment = "항목"
  }
  column "name" {
    null    = true
    type    = varchar(100)
    comment = "사용자항목이름"
  }
  column "deleted" {
    null    = true
    type    = smallint
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
}
table "app_application" {
  schema  = schema.rpa
  comment = "위항목/사용검토테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "robot_id" {
    null    = false
    type    = varchar(100)
    comment = "봇ID"
  }
  column "robot_version" {
    null    = false
    type    = int
    comment = "봇버전ID"
  }
  column "status" {
    null    = false
    type    = varchar(20)
    comment = "상태: 대기검토pending, 완료통신경과approved, 미완료통신경과rejected, 완료항목판매canceled, 항목nullify"
  }
  column "application_type" {
    null    = false
    type    = varchar(20)
    comment = "신청유형: release(위항목)/use(사용)"
  }
  column "security_level" {
    null    = true
    type    = varchar(10)
    comment = "검토항목의비밀단계red,green,yellow"
  }
  column "allowed_dept" {
    null    = true
    type    = varchar(5000)
    comment = "허용사용의부서ID목록"
  }
  column "expire_time" {
    null    = true
    type    = timestamp
    comment = "사용항목제한(항목중지날짜)"
  }
  column "audit_opinion" {
    null    = true
    type    = varchar(500)
    comment = "검토항목"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "신청사람ID"
  }
  column "create_time" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자또는검토항목id"
  }
  column "update_time" {
    null      = false
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "client_deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "클라이언트의신청항목기록-삭제 여부"
  }
  column "cloud_deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "항목중항목의신청항목기록-삭제 여부"
  }
  column "default_pass" {
    null    = true
    type    = bool
    comment = "선택항목색상비밀단계시, 후항목업데이트발송버전여부항목통신경과"
  }
  column "market_info" {
    null    = true
    type    = varchar(500)
    comment = "팀마켓id대기정보, 사용항목일항목발송항목위항목신청, 검토통신경과후항목공유까지해당마켓"
  }
  column "publish_info" {
    null = true
    type = varchar(500)
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_app_robot" {
    columns = [column.robot_id]
  }
}
table "app_application_tenant" {
  schema  = schema.rpa
  comment = "테넌트여부열기시작검토매칭항목테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "tenant_id" {
    null = false
    type = varchar(36)
  }
  column "audit_enable" {
    null    = true
    type    = smallint
    comment = "여부열기시작검토, 1열기시작, 0아니오열기시작"
  }
  column "audit_enable_time" {
    null = true
    type = timestamp
  }
  column "audit_enable_operator" {
    null = true
    type = char(36)
  }
  column "audit_enable_reason" {
    null = true
    type = varchar(100)
  }
  primary_key {
    columns = [column.tenant_id]
  }
}
table "app_market" {
  schema  = schema.rpa
  comment = "팀마켓-팀테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  column "market_id" {
    null    = true
    type    = varchar(20)
    comment = "팀마켓id"
  }
  column "market_name" {
    null    = true
    type    = varchar(60)
    comment = "마켓이름"
  }
  column "market_describe" {
    null    = true
    type    = varchar(800)
    comment = "마켓설명"
  }
  column "market_type" {
    null    = true
    type    = varchar(10)
    comment = "마켓유형: team,official"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "app_market_creator_id_IDX" {
    columns = [column.creator_id]
  }
  index "app_market_market_id_IDX" {
    columns = [column.market_id]
  }
  index "app_market_tenant_id_IDX" {
    columns = [column.tenant_id]
  }
}
table "app_market_classification" {
  schema = schema.rpa
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "name" {
    null    = true
    type    = varchar(64)
    comment = "분유형이름"
  }
  column "source" {
    null    = true
    type    = bool
    comment = "항목: 0-시스템시스템항목, 1-항목지정항목"
  }
  column "sort" {
    null    = true
    type    = int
    comment = "정렬"
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "creator_id" {
    null = true
    type = char(36)
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updater_id" {
    null = true
    type = char(36)
  }
  column "update_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
  }
  primary_key {
    columns = [column.id]
  }
  index "name_IDX" {
    columns = [column.name]
  }
}
table "app_market_classification_map" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "english" {
    null = false
    type = varchar(255)
  }
  column "name" {
    null = false
    type = varchar(255)
  }
}
table "app_market_dict" {
  schema = schema.rpa
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "business_code" {
    null    = true
    type    = varchar(100)
    comment = "항목서비스항목코드: 1, 행항목유형, 2, 역할공가능marketRoleFunc"
  }
  column "name" {
    null    = true
    type    = varchar(64)
    comment = "행항목이름, 역할공가능이름"
  }
  column "dict_code" {
    null    = true
    type    = varchar(64)
    comment = "행항목코드, 공가능항목코드"
  }
  column "dict_value" {
    null    = true
    type    = varchar(100)
    comment = "T있음권한, F없음권한"
  }
  column "user_type" {
    null    = true
    type    = varchar(100)
    comment = "owner,admin,acquirer,author"
  }
  column "description" {
    null    = true
    type    = varchar(256)
    comment = "설명"
  }
  column "seq" {
    null    = true
    type    = int
    comment = "정렬"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    default = "73"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    default = "73"
  }
  column "update_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
  }
  primary_key {
    columns = [column.id]
  }
  index "app_market_dict_dict_code_IDX" {
    columns = [column.dict_code]
  }
}
table "app_market_invite" {
  schema  = schema.rpa
  comment = "팀마켓-초대항목연결테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키id"
    auto_increment = true
  }
  column "invite_key" {
    null    = true
    type    = varchar(20)
    comment = "초대항목연결key"
  }
  column "inviter_id" {
    null    = true
    type    = varchar(50)
    comment = "초대사람id"
  }
  column "market_id" {
    null    = true
    type    = varchar(50)
    comment = "마켓id"
  }
  column "current_join_count" {
    null    = true
    type    = int
    comment = "현재완료추가입력사람데이터"
  }
  column "max_join_count" {
    null    = true
    type    = int
    comment = "항목대추가입력사람데이터"
  }
  column "expire_time" {
    null    = true
    type    = timestamp
    comment = "실패항목시간"
  }
  column "expire_type" {
    null    = true
    type    = varchar(50)
    comment = "실패항목유형"
  }
  column "creator_id" {
    null    = true
    type    = varchar(50)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = varchar(50)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = int
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "uk_invite_key" {
    unique  = true
    columns = [column.invite_key]
  }
}
table "app_market_resource" {
  schema  = schema.rpa
  comment = "팀마켓-항목항목테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "market_id" {
    null    = true
    type    = varchar(20)
    comment = "팀마켓id"
  }
  column "app_id" {
    null    = true
    type    = varchar(50)
    comment = "항목사용id, 항목id, 컴포넌트id"
  }
  column "download_num" {
    null    = true
    type    = bigint
    default = 0
    comment = "다운로드항목데이터"
  }
  column "check_num" {
    null    = true
    type    = bigint
    default = 0
    comment = "조회항목데이터"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "게시사람"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "게시시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  column "robot_id" {
    null    = true
    type    = varchar(100)
    comment = "봇id"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "app_name" {
    null    = true
    type    = varchar(64)
    comment = "항목이름"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  primary_key {
    columns = [column.id]
  }
  index "app_market_resource_app_id_IDX" {
    columns = [column.app_id]
  }
  index "app_market_resource_creator_id_IDX" {
    columns = [column.creator_id]
  }
  index "app_market_resource_market_id_IDX" {
    columns = [column.market_id]
  }
  index "app_market_resource_tenant_id_IDX" {
    columns = [column.tenant_id]
  }
}
table "app_market_user" {
  schema  = schema.rpa
  comment = "팀마켓-사람원테이블, n:n의닫기시스템"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "market_id" {
    null    = true
    type    = varchar(20)
    comment = "마켓id"
  }
  column "user_type" {
    null    = true
    type    = varchar(10)
    comment = "구성원유형: owner,admin,acquirer,author"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "구성원id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "추가입력시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "app_market_user_creator_id_IDX" {
    columns = [column.creator_id]
  }
  index "app_market_user_market_id_IDX" {
    columns = [column.market_id]
  }
  index "app_market_user_tenant_id_IDX" {
    columns = [column.tenant_id]
  }
}
table "app_market_version" {
  schema  = schema.rpa
  comment = "팀마켓-항목사용버전테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "market_id" {
    null    = true
    type    = varchar(100)
    comment = "마켓id"
  }
  column "app_id" {
    null = true
    type = varchar(50)
  }
  column "app_version" {
    null    = true
    type    = int
    comment = "항목사용버전, 항목봇버전"
  }
  column "edit_flag" {
    null    = true
    type    = bool
    default = 1
    comment = "항목생성의공유까지마켓, 여부지원항목/열기항목코드;0지원하지 않음, 1지원"
  }
  column "category" {
    null    = true
    type    = varchar(100)
    comment = "공유까지마켓의봇행항목: 항목서비스, 항목, 상업항목대기"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "게시사람"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "게시시간"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "category_id" {
    null    = true
    type    = bigint
    comment = "분유형id"
  }
  primary_key {
    columns = [column.id]
  }
  index "app_market_version_app_id_IDX" {
    columns = [column.app_id]
  }
  index "app_market_version_market_id_IDX" {
    columns = [column.market_id]
  }
  index "idx_app_id_version_deleted" {
    columns = [column.app_id, column.app_version, column.deleted]
  }
  index "idx_market_app_version" {
    columns = [column.market_id, column.app_id, column.app_version]
  }
}
table "atom_like" {
  schema  = schema.rpa
  comment = "기존항목가능항목즐겨찾기"
  column "id" {
    null           = false
    type           = int
    auto_increment = true
  }
  column "like_id" {
    null = false
    type = varchar(20)
  }
  column "atom_key" {
    null    = false
    type    = varchar(100)
    comment = "기존항목가능항목의key, 전체영역항목일"
  }
  column "creator_id" {
    null = false
    type = char(36)
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "is_deleted" {
    null    = false
    type    = bool
    default = 0
  }
  column "updater_id" {
    null = true
    type = char(36)
  }
  column "create_time" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "update_time" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
}
table "atom_meta_duplicate_log" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null    = false
    type    = bigint
    default = 0
  }
  column "atom_key" {
    null = true
    type = varchar(100)
  }
  column "version" {
    null    = true
    type    = varchar(20)
    comment = "기존항목가능항목버전"
  }
  column "request_body" {
    null    = true
    type    = mediumtext
    comment = "항목요청항목"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부"
  }
  column "creator_id" {
    null    = true
    type    = bigint
    default = 73
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updater_id" {
    null    = true
    type    = bigint
    default = 73
  }
  column "update_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
}
table "audit_checkpoint" {
  schema  = schema.rpa
  comment = "항목관리관리시스템계획항목테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "audit_object_type" {
    null    = true
    type    = varchar(36)
    comment = "robot, dept"
  }
  column "last_processed_id" {
    null = true
    type = varchar(36)
  }
  column "audit_status" {
    null    = true
    type    = varchar(20)
    comment = "시스템계획항목정도: counting, completed, pending,to_count"
  }
  column "count_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제식별자"
  }
  primary_key {
    columns = [column.id]
  }
}
table "audit_record" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "event_module_code" {
    null = true
    type = int
  }
  column "event_module_name" {
    null    = true
    type    = varchar(255)
    comment = "항목파일모듈"
  }
  column "event_type_code" {
    null = true
    type = int
  }
  column "event_type_name" {
    null    = true
    type    = varchar(255)
    comment = "항목파일유형"
  }
  column "event_detail" {
    null    = true
    type    = varchar(255)
    comment = "항목파일항목"
  }
  column "creator_id" {
    null = true
    type = char(36)
  }
  column "creator_name" {
    null = true
    type = varchar(255)
  }
  column "create_time" {
    null      = true
    type      = timestamp
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  column "process_id_list" {
    null = true
    type = mediumtext
  }
  column "role_id_list" {
    null = true
    type = mediumtext
  }
  primary_key {
    columns = [column.id]
  }
}
table "client_update_version" {
  schema  = schema.rpa
  comment = "클라이언트버전항목조회테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "version" {
    null    = false
    type    = char(15)
    comment = "버전"
  }
  column "version_num" {
    null    = false
    type    = mediumint
    comment = "버전숫자"
  }
  column "download_url" {
    null    = false
    type    = varchar(255)
    comment = "다운로드항목연결"
  }
  column "update_info" {
    null    = true
    type    = mediumtext
    comment = "업데이트내용"
  }
  column "create_time" {
    null    = true
    type    = datetime
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null      = true
    type      = datetime
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "os" {
    null    = true
    type    = varchar(255)
    comment = "시스템시스템"
  }
  column "arch" {
    null    = true
    type    = varchar(255)
    comment = "아키텍처"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_version" {
    columns = [column.version]
  }
  index "idx_version_num" {
    columns = [column.version_num]
  }
}
table "cloud_terminal" {
  schema  = schema.rpa
  comment = "단말테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "dept_id_path" {
    null    = true
    type    = varchar(100)
    comment = "부서전체경로id"
  }
  column "name" {
    null    = true
    type    = varchar(100)
    comment = "단말이름"
  }
  column "terminal_mac" {
    null    = true
    type    = varchar(100)
    comment = "항목준비항목, 단말항목일식별자"
  }
  column "terminal_ip" {
    null    = true
    type    = varchar(100)
    comment = "ip"
  }
  column "terminal_status" {
    null    = true
    type    = varchar(100)
    comment = "현재상태, 항목busy, 빈항목free, 항목offline"
  }
  column "terminal_des" {
    null    = true
    type    = varchar(100)
    comment = "단말설명"
  }
  column "user_id" {
    null    = true
    type    = char(36)
    comment = "항목항목사용자id"
  }
  column "dept_name" {
    null    = true
    type    = varchar(100)
    comment = "부서이름"
  }
  column "account_last" {
    null    = true
    type    = varchar(100)
    comment = "항목항목계정"
  }
  column "user_name_last" {
    null    = true
    type    = varchar(100)
    comment = "항목항목사용자명"
  }
  column "time_last" {
    null    = true
    type    = timestamp
    comment = "항목항목시간"
  }
  column "execute_time_total" {
    null    = true
    type    = bigint
    default = 0
    comment = "단일개단말항목계획실행시길이, 사용항목단말목록항목, 업데이트봇실행항목기록테이블시항목업데이트해당테이블"
  }
  column "execute_num" {
    null    = true
    type    = bigint
    default = 0
    comment = "단일개단말항목계획실행항목데이터, 업데이트봇실행항목기록테이블시항목업데이트해당테이블"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "단말항목기록생성 시간"
  }
  column "terminal_type" {
    null = true
    type = varchar(50)
  }
  primary_key {
    columns = [column.id]
  }
  index "cloud_terminal_mac_tenant_index" {
    columns = [column.terminal_mac, column.tenant_id]
  }
  index "cloud_terminal_tenant_id_IDX" {
    columns = [column.tenant_id, column.dept_id_path]
  }
  index "cloud_terminal_terminal_mac_IDX" {
    columns = [column.terminal_mac]
  }
  index "cloud_terminal_user_id_IDX" {
    columns = [column.user_id]
  }
}
table "component" {
  schema  = schema.rpa
  comment = "컴포넌트테이블"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키id"
    auto_increment = true
  }
  column "component_id" {
    null    = false
    type    = varchar(100)
    comment = "봇항목일id, 가져오기의항목사용id"
  }
  column "name" {
    null    = false
    type    = varchar(100)
    comment = "현재이름문자, 사용항목목록항목"
  }
  column "creator_id" {
    null    = false
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = false
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "is_shown" {
    null    = false
    type    = bool
    default = 1
    comment = "여부에서사용자목록항목항목 0: 아니오항목, 1: 항목"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  column "app_id" {
    null    = true
    type    = varchar(50)
    comment = "appmarketResource중의항목사용id"
    charset = "utf8mb4"
    collate = "utf8mb4_general_ci"
  }
  column "app_version" {
    null    = true
    type    = int
    comment = "가져오기의항목사용: 앱 마켓버전"
  }
  column "market_id" {
    null    = true
    type    = varchar(20)
    comment = "가져오기의항목사용: 마켓id"
    charset = "utf8mb4"
    collate = "utf8mb4_general_ci"
  }
  column "resource_status" {
    null    = true
    type    = varchar(20)
    comment = "항목상태: toObtain, obtained, toUpdate"
  }
  column "data_source" {
    null    = true
    type    = varchar(20)
    comment = "항목: create 항목생성 ; market 마켓가져오기 "
  }
  column "transform_status" {
    null    = true
    type    = varchar(20)
    comment = "editing 항목중, published 완료발송버전, shared 완료위항목, locked항목지정(없음법항목)"
  }
  primary_key {
    columns = [column.id]
  }
}
table "component_robot_block" {
  schema  = schema.rpa
  comment = "봇항목컴포넌트항목테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키id"
    auto_increment = true
  }
  column "robot_id" {
    null    = false
    type    = varchar(100)
    comment = "봇id"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "robot_version" {
    null    = false
    type    = int
    comment = "봇버전항목"
  }
  column "component_id" {
    null    = false
    type    = varchar(100)
    comment = "컴포넌트id"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  primary_key {
    columns = [column.id]
  }
}
table "component_robot_use" {
  schema  = schema.rpa
  comment = "봇항목컴포넌트항목사용테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키id"
    auto_increment = true
  }
  column "robot_id" {
    null    = false
    type    = varchar(100)
    comment = "봇id"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "robot_version" {
    null    = false
    type    = int
    comment = "봇버전항목"
  }
  column "component_id" {
    null    = false
    type    = varchar(100)
    comment = "컴포넌트id"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "component_version" {
    null    = false
    type    = int
    comment = "컴포넌트버전항목"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  primary_key {
    columns = [column.id]
  }
}
table "component_version" {
  schema  = schema.rpa
  comment = "컴포넌트버전테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키id"
    auto_increment = true
  }
  column "component_id" {
    null    = false
    type    = varchar(100)
    comment = "봇id"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "version" {
    null    = false
    type    = int
    comment = "버전항목"
  }
  column "introduction" {
    null    = true
    type    = longtext
    comment = "항목"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "update_log" {
    null    = true
    type    = longtext
    comment = "변경 로그"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  column "param" {
    null    = true
    type    = text
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "param_detail" {
    null    = true
    type    = text
    comment = "발송버전시항목의테이블단일매개변수정보"
    collate = "utf8mb4_unicode_ci"
  }
  column "icon" {
    null    = false
    type    = varchar(30)
    comment = "아이콘"
  }
  primary_key {
    columns = [column.id]
  }
}
table "consult_form" {
  schema = schema.rpa
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "form_type" {
    null    = true
    type    = tinyint
    comment = "1=항목버전 2=항목버전 항목 3~99"
  }
  column "company_name" {
    null = false
    type = varchar(128)
  }
  column "contact_name" {
    null = false
    type = varchar(64)
  }
  column "mobile" {
    null = false
    type = varchar(20)
  }
  column "email" {
    null    = true
    type    = varchar(128)
    comment = "항목항목"
  }
  column "team_size" {
    null    = true
    type    = varchar(32)
    comment = "사람데이터항목, 딕셔너리값"
  }
  column "status" {
    null    = false
    type    = tinyint
    default = 0
    comment = "0=대기항목관리 1=완료항목관리 2=완료항목"
  }
  column "remark" {
    null    = true
    type    = varchar(512)
    comment = "항목서비스비고"
  }
  column "created_at" {
    null    = false
    type    = datetime
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updated_at" {
    null      = false
    type      = datetime
    default   = sql("CURRENT_TIMESTAMP")
    on_update = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_created" {
    columns = [column.created_at]
  }
  index "idx_type_status" {
    columns = [column.form_type, column.status]
  }
}
table "contact" {
  schema  = schema.rpa
  comment = "항목정보테이블"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키ID"
    auto_increment = true
  }
  column "name" {
    null    = false
    type    = varchar(100)
    comment = "항목이름"
  }
  column "phone" {
    null    = false
    type    = varchar(11)
    comment = "휴대폰 번호"
  }
  column "company_name" {
    null    = false
    type    = varchar(200)
    comment = "항목이름"
  }
  column "company_size" {
    null    = false
    type    = varchar(50)
    comment = "팀항목 매개항목CompanySizeEnum"
  }
  column "email" {
    null    = true
    type    = varchar(100)
    comment = "메일함"
  }
  column "demand_desc" {
    null    = true
    type    = text
    comment = "필요항목설명"
  }
  column "contact_kind" {
    null    = false
    type    = varchar(50)
    comment = "항목문의유형 매개항목ContactKindEnum"
  }
  column "agreement" {
    null    = true
    type    = bool
    default = 1
    comment = "여부항목항목 0-아니오항목 1-항목"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성사람ID"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "업데이트사람ID"
  }
  column "create_time" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "수정 시간"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0-삭제되지 않음 1-삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_company_name" {
    columns = [column.company_name]
  }
  index "idx_create_time" {
    columns = [column.create_time]
  }
  index "idx_deleted" {
    columns = [column.deleted]
  }
  index "idx_phone" {
    columns = [column.phone]
  }
}
table "c_atom_meta_new" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "atom_key" {
    null = true
    type = varchar(100)
  }
  column "atom_content" {
    null    = true
    type    = mediumtext
    comment = "기존항목가능항목모든매칭항목정보, json"
  }
  column "sort" {
    null    = true
    type    = int
    comment = "기존항목가능항목항목순서"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "update_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_atom_key" {
    unique  = true
    columns = [column.atom_key]
    comment = "atom_key검색항목"
  }
}
table "c_element" {
  schema  = schema.rpa
  comment = "클라이언트, 원항목정보"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "element_id" {
    null    = true
    type    = varchar(100)
    comment = "원항목id"
  }
  column "element_name" {
    null    = true
    type    = varchar(100)
    comment = "원항목이름"
  }
  column "icon" {
    null    = true
    type    = varchar(100)
    comment = "아이콘"
  }
  column "image_id" {
    null    = true
    type    = varchar(100)
    comment = "이미지다운로드주소"
  }
  column "parent_image_id" {
    null    = true
    type    = varchar(100)
    comment = "원항목의항목단계이미지다운로드주소"
  }
  column "element_data" {
    null    = true
    type    = mediumtext
    comment = "원항목내용"
  }
  column "deleted" {
    null    = true
    type    = smallint
    default = 0
  }
  column "creator_id" {
    null = true
    type = char(36)
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updater_id" {
    null = true
    type = char(36)
  }
  column "update_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "robot_id" {
    null = true
    type = varchar(100)
  }
  column "robot_version" {
    null = true
    type = int
  }
  column "group_id" {
    null = true
    type = varchar(30)
  }
  column "common_sub_type" {
    null    = true
    type    = varchar(50)
    comment = "cv이미지, sigle항목통신선택, batch데이터근거항목가져오기"
  }
  column "group_name" {
    null = true
    type = varchar(100)
  }
  column "element_type" {
    null = true
    type = varchar(20)
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_element_id" {
    columns = [column.element_id]
  }
  index "idx_element_name" {
    columns = [column.element_name]
  }
  index "idx_element_robot_version" {
    columns = [column.element_id, column.robot_id, column.robot_version]
  }
  index "idx_group_id" {
    columns = [column.group_id]
  }
  index "idx_robot_info" {
    columns = [column.robot_id, column.robot_version]
  }
}
table "c_global_var" {
  schema  = schema.rpa
  comment = "클라이언트-전역 변수"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "project_id" {
    null = true
    type = varchar(100)
  }
  column "global_id" {
    null = true
    type = varchar(100)
  }
  column "var_name" {
    null = true
    type = varchar(100)
  }
  column "var_type" {
    null = true
    type = varchar(100)
  }
  column "var_value" {
    null = true
    type = varchar(100)
  }
  column "var_describe" {
    null = true
    type = varchar(100)
  }
  column "deleted" {
    null = true
    type = smallint
  }
  column "creator_id" {
    null = true
    type = char(36)
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updater_id" {
    null = true
    type = char(36)
  }
  column "update_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "robot_id" {
    null = true
    type = varchar(100)
  }
  column "robot_version" {
    null = true
    type = int
  }
  primary_key {
    columns = [column.id]
  }
}
table "c_group" {
  schema  = schema.rpa
  comment = "원항목또는이미지의분그룹"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "group_id" {
    null = true
    type = varchar(100)
  }
  column "group_name" {
    null = true
    type = varchar(100)
  }
  column "creator_id" {
    null = true
    type = char(36)
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updater_id" {
    null = true
    type = char(36)
  }
  column "update_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = smallint
    default = 0
  }
  column "robot_id" {
    null = true
    type = varchar(100)
  }
  column "robot_version" {
    null = true
    type = int
  }
  column "element_type" {
    null    = true
    type    = varchar(20)
    comment = "cv: cv선택; common:항목통신요소 선택"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_element_type" {
    columns = [column.element_type]
  }
  index "idx_group_id" {
    columns = [column.group_id]
  }
  index "idx_robot_info" {
    columns = [column.robot_id, column.robot_version]
  }
}
table "c_module" {
  schema  = schema.rpa
  comment = "클라이언트-python모듈데이터근거"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "module_id" {
    null    = true
    type    = varchar(100)
    comment = "프로세스id"
  }
  column "module_content" {
    null    = true
    type    = mediumtext
    comment = "전체량python항목코드데이터근거"
  }
  column "module_name" {
    null    = true
    type    = varchar(100)
    comment = "python파일이름"
  }
  column "deleted" {
    null    = true
    type    = smallint
    default = 0
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    default = "73"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    default = "73"
  }
  column "update_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "robot_id" {
    null = true
    type = varchar(100)
  }
  column "robot_version" {
    null = true
    type = int
  }
  column "breakpoint" {
    null    = true
    type    = mediumtext
    comment = "항목정보"
  }
  primary_key {
    columns = [column.id]
  }
  index "c_module_module_id_IDX" {
    columns = [column.module_id]
  }
}
table "c_param" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null    = false
    type    = varchar(20)
    comment = "매개변수id"
  }
  column "var_direction" {
    null    = true
    type    = int
    comment = "입력/출력"
  }
  column "var_name" {
    null    = true
    type    = varchar(100)
    comment = "매개변수이름"
  }
  column "var_type" {
    null    = true
    type    = varchar(100)
    comment = "매개변수유형"
  }
  column "var_value" {
    null    = true
    type    = varchar(1000)
    comment = "매개변수내용"
  }
  column "var_describe" {
    null    = true
    type    = varchar(100)
    comment = "매개변수설명"
  }
  column "process_id" {
    null    = true
    type    = varchar(100)
    comment = "프로세스id"
  }
  column "creator_id" {
    null = true
    type = char(36)
  }
  column "updater_id" {
    null = true
    type = char(36)
  }
  column "create_time" {
    null = true
    type = timestamp
  }
  column "update_time" {
    null = true
    type = timestamp
  }
  column "deleted" {
    null = true
    type = int
  }
  column "robot_id" {
    null = true
    type = varchar(100)
  }
  column "robot_version" {
    null = true
    type = int
  }
  column "module_id" {
    null    = true
    type    = varchar(100)
    comment = "python모듈id"
  }
  index "c_param_id_IDX" {
    columns = [column.id]
  }
}
table "c_process" {
  schema  = schema.rpa
  comment = "클라이언트-프로세스데이터근거"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "project_id" {
    null    = true
    type    = varchar(100)
    comment = "항목id"
  }
  column "process_id" {
    null    = true
    type    = varchar(100)
    comment = "프로세스id"
  }
  column "process_content" {
    null    = true
    type    = mediumtext
    comment = "전체량프로세스데이터근거"
  }
  column "process_name" {
    null    = true
    type    = varchar(100)
    comment = "프로세스이름"
  }
  column "deleted" {
    null    = true
    type    = smallint
    default = 0
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    default = "73"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    default = "73"
  }
  column "update_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "robot_id" {
    null = true
    type = varchar(100)
  }
  column "robot_version" {
    null = true
    type = int
  }
  primary_key {
    columns = [column.id]
  }
}
table "c_project" {
  schema  = schema.rpa
  comment = "항목테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "project_id" {
    null = true
    type = varchar(100)
  }
  column "project_name" {
    null    = true
    type    = varchar(200)
    comment = "항목목록이름"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = datetime
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null    = true
    type    = datetime
    comment = "생성 시간"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "항목삭제 0: 삭제되지 않음 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
}
table "c_require" {
  schema  = schema.rpa
  comment = "python항목관리관리"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "project_id" {
    null = true
    type = varchar(100)
  }
  column "package_name" {
    null    = true
    type    = varchar(100)
    comment = "항목목록이름"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "package_version" {
    null = true
    type = varchar(20)
  }
  column "mirror" {
    null = true
    type = varchar(100)
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = datetime
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null    = true
    type    = datetime
    comment = "생성 시간"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "항목삭제 0: 삭제되지 않음 1: 삭제됨"
  }
  column "robot_id" {
    null = true
    type = varchar(100)
  }
  column "robot_version" {
    null = true
    type = int
  }
  primary_key {
    columns = [column.id]
  }
}
table "c_smart_version" {
  schema  = schema.rpa
  comment = "항목가능컴포넌트버전테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "항목증가기본 키"
    auto_increment = true
  }
  column "smart_id" {
    null    = false
    type    = varchar(100)
    comment = "항목가능컴포넌트Id"
  }
  column "smart_type" {
    null    = true
    type    = varchar(100)
    comment = "항목가능컴포넌트의유형"
  }
  column "content" {
    null    = true
    type    = mediumtext
    comment = "컴포넌트내용(초과길이텍스트)"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제식별자: 0-삭제되지 않음, 1-삭제됨"
  }
  column "robot_id" {
    null    = true
    type    = varchar(100)
    comment = "봇Id"
  }
  column "robot_version" {
    null    = true
    type    = int
    comment = "봇버전항목"
  }
  column "creator_id" {
    null    = true
    type    = varchar(36)
    comment = "생성사람ID"
  }
  column "create_time" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간, 삽입시항목항목성공"
  }
  column "updater_id" {
    null    = true
    type    = varchar(36)
    comment = "업데이트사람ID"
  }
  column "update_time" {
    null      = false
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간, 업데이트시항목업데이트"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_smart_id_robot_id" {
    columns = [column.smart_id, column.robot_id]
  }
}
table "dispatch_day_task_info" {
  schema  = schema.rpa
  comment = "스케줄링항목방식:단말매일업로드의작업항목정보"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "terminal_id" {
    null    = true
    type    = varchar(20)
    comment = "단말id"
  }
  column "task_id" {
    null    = true
    type    = varchar(30)
    comment = "작업ID"
  }
  column "task_name" {
    null    = true
    type    = varchar(30)
    comment = "작업이름"
  }
  column "robot_id" {
    null    = true
    type    = varchar(30)
    comment = "봇ID"
  }
  column "robot_name" {
    null    = true
    type    = varchar(30)
    comment = "봇이름"
  }
  column "status" {
    null    = true
    type    = varchar(10)
    comment = "현재상태 대기실행 todo /완료실행 done /에서실행 doing"
  }
  column "execute_time" {
    null    = true
    type    = varchar(10)
    comment = "작업실행시간"
  }
  column "sort" {
    null    = true
    type    = int
    comment = "정렬, 항목소항목전"
  }
  column "tenant_id" {
    null = true
    type = varchar(36)
  }
  column "creator_id" {
    null    = true
    type    = varchar(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = varchar(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_robot_id" {
    columns = [column.robot_id]
  }
  index "idx_task_id" {
    columns = [column.task_id]
  }
}
table "dispatch_task" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "dispatch_task_id" {
    null           = false
    type           = bigint
    comment        = "스케줄링항목방식예약 작업id"
    auto_increment = true
  }
  column "status" {
    null    = false
    type    = varchar(10)
    default = "0"
    comment = "작업상태: 사용중active, 닫기stop, 완료경과항목expired"
  }
  column "name" {
    null    = true
    type    = varchar(50)
    comment = "스케줄링항목방식예약 작업이름"
  }
  column "cron_json" {
    null    = true
    type    = mediumtext
    comment = "항목생성스케줄링예약 작업의항목매개변수;예약schedule저장계획계획실행의항목JSON"
  }
  column "type" {
    null    = true
    type    = varchar(10)
    comment = "트리거항목파일: 항목트리거manual, 예약schedule, 예약트리거trigger"
  }
  column "exceptional" {
    null    = false
    type    = varchar(20)
    default = "stop"
    comment = "항목오류예항목관리: 건너뛰기jump, 중지stop, 재시도후건너뛰기retry_jump, 재시도후중지retry_stop"
  }
  column "retry_num" {
    null    = true
    type    = int
    comment = "항목있음exceptional로retry시, 항목기록의재시도항목데이터"
  }
  column "timeout_enable" {
    null    = true
    type    = smallint
    comment = "여부사용시간 초과시간 1:사용 0:아니오사용"
  }
  column "timeout" {
    null    = true
    type    = int
    default = 9999
    comment = "시간 초과시간"
  }
  column "queue_enable" {
    null    = true
    type    = smallint
    default = 0
    comment = "여부사용정렬팀 1:사용 0:아니오사용"
  }
  column "screen_record_enable" {
    null    = true
    type    = smallint
    default = 0
    comment = "여부열기시작기록항목 1:사용 0:아니오사용"
  }
  column "virtual_desktop_enable" {
    null    = true
    type    = smallint
    default = 0
    comment = "여부열기시작항목항목 1:사용 0:아니오사용"
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = smallint
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.dispatch_task_id]
  }
}
table "dispatch_task_execute_record" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "dispatch_task_id" {
    null    = true
    type    = bigint
    comment = "스케줄링항목방식예약 작업id"
  }
  column "dispatch_task_execute_id" {
    null    = true
    type    = bigint
    comment = "스케줄링항목방식예약 작업실행id"
  }
  column "count" {
    null    = true
    type    = int
    comment = "실행항목, 1, 2, 3...."
  }
  column "dispatch_task_type" {
    null    = true
    type    = varchar(20)
    comment = "트리거항목파일: 항목트리거manual, 예약schedule, 예약트리거trigger"
  }
  column "result" {
    null    = true
    type    = varchar(20)
    comment = "실행 결과항목:성공success, 실패error, 실행중executing, 중중지cancel, 아래발송실패dispatch_error, 실행실패exe_error"
  }
  column "start_time" {
    null    = true
    type    = datetime
    comment = "실행시작 시간"
  }
  column "end_time" {
    null    = true
    type    = datetime
    comment = "실행종료 시간"
  }
  column "execute_time" {
    null    = true
    type    = bigint
    comment = "실행항목시 단일위치초"
  }
  column "terminal_id" {
    null    = true
    type    = char(36)
    comment = "단말항목일식별자, 예항목준비mac주소"
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "task_detail_json" {
    null    = true
    type    = mediumtext
    comment = "작업항목"
  }
  column "deleted" {
    null    = true
    type    = smallint
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_dispatch_task_teminal_task_id" {
    columns = [column.dispatch_task_id]
  }
}
table "dispatch_task_robot" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "dispatch_task_id" {
    null    = true
    type    = bigint
    comment = "스케줄링항목방식예약 작업id"
  }
  column "robot_id" {
    null    = true
    type    = varchar(30)
    comment = "봇ID"
  }
  column "online" {
    null    = true
    type    = tinyint
    comment = "여부사용버전:  0:미완료사용,1:완료사용"
  }
  column "version" {
    null    = true
    type    = int
    comment = "봇버전"
  }
  column "param_json" {
    null    = true
    type    = mediumtext
    comment = "봇구성 매개변수"
  }
  column "sort" {
    null    = true
    type    = int
    comment = "정렬, 항목소항목전"
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = smallint
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_dispatch_task_teminal_task_id" {
    columns = [column.dispatch_task_id]
  }
}
table "dispatch_task_robot_execute_record" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키id"
    auto_increment = true
  }
  column "execute_id" {
    null    = true
    type    = bigint
    comment = "봇실행id"
  }
  column "dispatch_task_execute_id" {
    null    = true
    type    = bigint
    comment = "스케줄링항목방식예약 작업실행id"
  }
  column "robot_id" {
    null    = true
    type    = varchar(100)
    comment = "봇id"
  }
  column "robot_version" {
    null    = true
    type    = int
    comment = "봇버전항목"
  }
  column "start_time" {
    null    = true
    type    = timestamp
    comment = "시작 시간"
  }
  column "end_time" {
    null    = true
    type    = timestamp
    comment = "종료 시간"
  }
  column "execute_time" {
    null    = true
    type    = bigint
    comment = "실행항목시 단일위치초"
  }
  column "result" {
    null    = true
    type    = varchar(20)
    comment = "실행 결과항목:: robotFail:실패,  robotSuccess:성공, robotCancel:가져오기항목(중중지), robotExecute:정상에서실행"
  }
  column "param_json" {
    null    = true
    type    = mediumtext
    comment = "봇실행매개변수"
  }
  column "error_reason" {
    null    = true
    type    = varchar(255)
    comment = "오류원인"
  }
  column "execute_log" {
    null    = true
    type    = longtext
    comment = "로그내용"
  }
  column "video_local_path" {
    null    = true
    type    = varchar(200)
    comment = "항목항목기록의본항목저장항목경로"
  }
  column "dept_id_path" {
    null    = true
    type    = varchar(100)
    comment = "부서전체경로항목코드"
  }
  column "terminal_id" {
    null    = true
    type    = char(36)
    comment = "단말항목일식별자, 예항목준비mac주소"
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = smallint
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "data_table_path" {
    null    = true
    type    = varchar(255)
    comment = "데이터근거항목가져오기저장항목위치항목"
  }
  primary_key {
    columns = [column.id]
  }
}
table "dispatch_task_terminal" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "dispatch_task_id" {
    null    = true
    type    = bigint
    comment = "스케줄링항목방식예약 작업id"
  }
  column "terminal_or_group" {
    null    = true
    type    = varchar(10)
    comment = "트리거항목파일: 단말teminal, 단말분그룹group"
  }
  column "execute_method" {
    null    = true
    type    = varchar(10)
    comment = "실행방법방식: 항목기기일항목random_one, 전체실행all"
  }
  column "value" {
    null    = true
    type    = mediumtext
    comment = "항목값: 저장항목 list<id> ; 항목중단말항목: terminal_id(테이블terminal) 분그룹항목: id (terminal_group_name)"
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = smallint
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_dispatch_task_teminal_task_id" {
    columns = [column.dispatch_task_id]
  }
}
table "feedback_report" {
  schema  = schema.rpa
  comment = "반대항목항목테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키ID"
    auto_increment = true
  }
  column "report_no" {
    null    = false
    type    = varchar(32)
    comment = "항목일번호"
  }
  column "username" {
    null    = false
    type    = varchar(100)
    comment = "사용자로그인이름"
  }
  column "categories" {
    null    = false
    type    = text
    comment = "항목제목분유형목록(JSON형식)"
  }
  column "description" {
    null    = false
    type    = text
    comment = "항목제목설명"
  }
  column "image_ids" {
    null    = true
    type    = varchar(500)
    comment = "이미지파일ID목록(항목분항목)"
  }
  column "create_time" {
    null    = false
    type    = datetime
    comment = "생성 시간"
  }
  column "update_time" {
    null    = true
    type    = datetime
    comment = "수정 시간"
  }
  column "deleted" {
    null    = true
    type    = tinyint
    default = 0
    comment = "항목삭제항목로그 0:삭제되지 않음 1:삭제됨"
  }
  column "processed" {
    null    = true
    type    = tinyint
    default = 0
    comment = "여부완료항목관리 0:미완료항목관리 1:완료항목관리"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_create_time" {
    columns = [column.create_time]
  }
  index "idx_processed" {
    columns = [column.processed]
  }
  index "idx_username" {
    columns = [column.username]
  }
  index "uk_report_no" {
    unique  = true
    columns = [column.report_no]
  }
}
table "file" {
  schema  = schema.rpa
  comment = "파일테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = int
    comment        = "기본 키ID"
    auto_increment = true
  }
  column "file_id" {
    null    = true
    type    = varchar(50)
    comment = "파일항목의uuid"
  }
  column "path" {
    null    = true
    type    = varchar(100)
    comment = "파일에서s3위항목의경로"
  }
  column "create_time" {
    null    = true
    type    = datetime
    comment = "생성 시간"
  }
  column "update_time" {
    null    = true
    type    = datetime
    comment = "수정 시간"
  }
  column "deleted" {
    null    = true
    type    = int
    comment = "항목삭제항목로그위치"
    default = 0
  }
  column "file_name" {
    null    = true
    type    = varchar(1000)
    comment = "파일항목이름"
  }
  primary_key {
    columns = [column.id]
  }
}
table "his_base" {
  schema  = schema.rpa
  comment = "전체봇및전체단말항목테이블"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "dept_id_path" {
    null    = true
    type    = varchar(100)
    comment = "부서전체경로항목코드"
  }
  column "execute_success" {
    null    = true
    type    = bigint
    comment = "항목계획실행성공항목데이터"
  }
  column "execute_fail" {
    null    = true
    type    = bigint
    comment = "항목계획실행실패항목데이터"
  }
  column "execute_abort" {
    null    = true
    type    = bigint
    comment = "항목계획실행중중지항목데이터"
  }
  column "robot_num" {
    null    = true
    type    = bigint
    comment = "항목계획봇항목데이터"
  }
  column "execute_total" {
    null    = true
    type    = bigint
    comment = "봇항목계획실행항목데이터"
  }
  column "execute_time_total" {
    null    = true
    type    = bigint
    comment = "전체봇또는전체단말항목계획실행시길이, 단일위치초, 항목계획항목성공의"
  }
  column "execute_success_rate" {
    null     = true
    type     = decimal(5,2)
    unsigned = false
    comment  = "항목계획실행성공항목"
  }
  column "user_num" {
    null    = true
    type    = bigint
    comment = "항목계획사용자데이터량"
  }
  column "count_time" {
    null    = true
    type    = timestamp
    comment = "시스템계획시간"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = false
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "terminal" {
    null    = true
    type    = bigint
    comment = "단말데이터량"
  }
  column "labor_save" {
    null    = true
    type    = bigint
    comment = "항목의사람항목"
  }
  primary_key {
    columns = [column.id]
  }
}
table "his_data_enum" {
  schema  = schema.rpa
  comment = "항목관리관리데이터근거항목항목조각매칭항목데이터근거항목"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "parent_code" {
    null = true
    type = varchar(100)
  }
  column "icon" {
    null = true
    type = varchar(100)
  }
  column "field" {
    null = true
    type = varchar(100)
  }
  column "text" {
    null = true
    type = varchar(100)
  }
  column "num" {
    null = true
    type = varchar(100)
  }
  column "unit" {
    null = true
    type = varchar(100)
  }
  column "percent" {
    null = true
    type = varchar(100)
  }
  column "tip" {
    null = true
    type = varchar(100)
  }
  column "order" {
    null = true
    type = bigint
  }
  primary_key {
    columns = [column.id]
  }
}
table "his_robot" {
  schema  = schema.rpa
  comment = "단일개봇항목테이블,항목일데이터근거"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "execute_num_total" {
    null    = true
    type    = bigint
    comment = "항목일실행항목데이터"
  }
  column "execute_success" {
    null    = true
    type    = bigint
    comment = "매일성공항목데이터"
  }
  column "execute_fail" {
    null    = true
    type    = bigint
    comment = "매일실패항목데이터"
  }
  column "execute_abort" {
    null    = true
    type    = bigint
    comment = "매일중중지항목데이터"
  }
  column "execute_success_rate" {
    null     = true
    type     = decimal(5,2)
    unsigned = false
    comment  = "매일성공항목"
  }
  column "execute_time" {
    null    = true
    type    = bigint
    comment = "매일실행시길이, 단일위치초"
  }
  column "count_time" {
    null    = true
    type    = timestamp
    comment = "시스템계획시간"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "robot_id" {
    null = true
    type = varchar(100)
  }
  column "user_id" {
    null    = true
    type    = char(36)
    comment = "사용자id"
  }
  column "dept_id_path" {
    null    = true
    type    = varchar(100)
    comment = "부서전체경로id"
  }
  primary_key {
    columns = [column.id]
  }
}
table "his_terminal" {
  schema  = schema.rpa
  comment = "단일개단말항목테이블"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "dept_id_path" {
    null    = true
    type    = varchar(36)
    comment = "부서전체경로id"
  }
  column "terminal_id" {
    null    = true
    type    = varchar(100)
    comment = "항목준비mac"
  }
  column "execute_time" {
    null    = true
    type    = bigint
    comment = "매일실행시길이"
  }
  column "execute_num" {
    null    = true
    type    = bigint
    comment = "단말매일실행항목데이터"
  }
  column "count_time" {
    null    = true
    type    = timestamp
    comment = "시스템계획시간"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "his_terminal_count_time_IDX" {
    columns = [column.count_time]
  }
  index "his_terminal_tenant_id_IDX" {
    columns = [column.tenant_id, column.dept_id_path]
  }
  index "his_terminal_terminal_id_IDX" {
    columns = [column.terminal_id]
  }
}
table "install_package" {
  schema  = schema.rpa
  comment = "설치 패키지테이블"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키ID"
    auto_increment = true
  }
  column "name" {
    null    = false
    type    = varchar(255)
    comment = "항목이름"
  }
  column "download_path" {
    null    = false
    type    = varchar(500)
    comment = "다운로드항목연결"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성사람ID"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "업데이트사람ID"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "수정 시간"
  }
  column "deleted" {
    null    = true
    type    = tinyint
    default = 0
    comment = "삭제 여부 0-삭제되지 않음 1-삭제됨"
  }
  column "is_online" {
    null    = true
    type    = tinyint
    default = 0
    comment = "여부위항목 0-아니오위항목 1-위항목"
  }
  primary_key {
    columns = [column.id]
  }
}
table "notify_send" {
  schema  = schema.rpa
  comment = "메시지알림-메시지테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "user_id" {
    null    = true
    type    = varchar(50)
    comment = "수신항목"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "message_info" {
    null    = true
    type    = varchar(100)
    comment = "메시지항목id"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "message_type" {
    null    = true
    type    = varchar(20)
    comment = "메시지유형: 항목사람메시지teamMarketInvite, 업데이트메시지teamMarketUpdate"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "operate_result" {
    null    = true
    type    = smallint
    comment = "항목결과: 미완료항목1,  완료항목2, 완료추가입력3, 완료항목4"
  }
  column "market_id" {
    null    = true
    type    = varchar(500)
    comment = "마켓id"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "수정 시간"
  }
  column "deleted" {
    null    = true
    type    = smallint
    default = 0
    comment = "삭제식별자"
  }
  column "user_type" {
    null    = true
    type    = varchar(10)
    comment = "구성원유형: owner,admin,consumer"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "app_name" {
    null = true
    type = varchar(200)
  }
  primary_key {
    columns = [column.id]
  }
}
table "openapi_auth" {
  schema  = schema.rpa
  comment = "openapi항목권한항목저장"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "name" {
    null = true
    type = varchar(50)
  }
  column "user_id" {
    null    = true
    type    = char(36)
    comment = "사용자id"
  }
  column "api_key" {
    null = true
    type = varchar(100)
  }
  column "prefix" {
    null = true
    type = varchar(10)
  }
  column "created_at" {
    null = true
    type = datetime
  }
  column "updated_at" {
    null = true
    type = datetime
  }
  column "is_active" {
    null = true
    type = bool
  }
  primary_key {
    columns = [column.id]
  }
  index "UNIQUE" {
    unique  = true
    columns = [column.api_key]
  }
}
table "pypi_packages" {
  schema = schema.rpa
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "package_name" {
    null = false
    type = varchar(255)
  }
  column "oss_path" {
    null = false
    type = varchar(255)
  }
  column "visibility" {
    null    = true
    type    = bool
    default = 1
    comment = "visibility 1: 항목공유가능항목패키지 2: 개사람항목있음패키지 3: 항목정도패키지, 부서분사람가능항목"
  }
  column "user_id" {
    null    = true
    type    = char(36)
    default = "0"
    comment = "게시사용자id"
  }
  column "create_time" {
    null    = true
    type    = datetime
    default = sql("CURRENT_TIMESTAMP")
  }
  column "update_time" {
    null      = true
    type      = datetime
    default   = sql("CURRENT_TIMESTAMP")
    on_update = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  index "unique_key" {
    unique  = true
    columns = [column.package_name, column.visibility, column.user_id]
  }
}
table "renewal_form" {
  schema  = schema.rpa
  comment = "항목테이블단일테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "form_type" {
    null    = false
    type    = tinyint
    comment = "1=항목버전 2=항목버전 항목 3~99"
  }
  column "company_name" {
    null    = false
    type    = varchar(128)
    comment = "항목이름"
  }
  column "mobile" {
    null    = false
    type    = varchar(20)
    comment = "항목사람휴대폰 번호"
  }
  column "renewal_duration" {
    null    = false
    type    = varchar(32)
    comment = "항목시길이"
  }
  column "status" {
    null    = false
    type    = tinyint
    default = 0
    comment = "0=대기항목관리 1=완료항목관리 2=완료항목"
  }
  column "remark" {
    null    = true
    type    = varchar(512)
    comment = "항목서비스비고"
  }
  column "created_at" {
    null    = false
    type    = datetime
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updated_at" {
    null      = false
    type      = datetime
    default   = sql("CURRENT_TIMESTAMP")
    on_update = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_created" {
    columns = [column.created_at]
  }
  index "idx_type_status" {
    columns = [column.form_type, column.status]
  }
}
table "robot_design" {
  schema  = schema.rpa
  comment = "항목단말봇테이블"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키id"
    auto_increment = true
  }
  column "robot_id" {
    null    = true
    type    = varchar(100)
    comment = "봇항목일id, 가져오기의항목사용id"
  }
  column "name" {
    null    = true
    type    = varchar(100)
    comment = "현재이름문자, 사용항목목록항목"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  column "app_id" {
    null    = true
    type    = varchar(50)
    comment = "appmarketResource중의항목사용id"
    charset = "utf8mb4"
    collate = "utf8mb4_general_ci"
  }
  column "app_version" {
    null    = true
    type    = int
    comment = "가져오기의항목사용: 앱 마켓버전"
  }
  column "market_id" {
    null    = true
    type    = varchar(20)
    comment = "가져오기의항목사용: 마켓id"
    charset = "utf8mb4"
    collate = "utf8mb4_general_ci"
  }
  column "resource_status" {
    null    = true
    type    = varchar(20)
    comment = "항목상태: toObtain, obtained, toUpdate"
  }
  column "data_source" {
    null    = true
    type    = varchar(20)
    comment = "항목: create 항목생성 ; market 마켓가져오기 "
  }
  column "transform_status" {
    null    = true
    type    = varchar(20)
    comment = "editing 항목중, published 완료발송버전, shared 완료위항목, locked항목지정(없음법항목)"
  }
  column "edit_enable" {
    null    = true
    type    = varchar(100)
    comment = "항목"
  }
  primary_key {
    columns = [column.id]
  }
}
table "robot_execute" {
  schema  = schema.rpa
  comment = "항목단말봇테이블"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키id"
    auto_increment = true
  }
  column "robot_id" {
    null    = true
    type    = varchar(100)
    comment = "봇항목일id, 가져오기의항목사용id"
  }
  column "name" {
    null    = true
    type    = varchar(100)
    comment = "현재이름문자, 사용항목목록항목"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  column "app_id" {
    null    = true
    type    = varchar(50)
    comment = "appmarketResource중의항목사용id"
    charset = "utf8mb4"
    collate = "utf8mb4_general_ci"
  }
  column "app_version" {
    null    = true
    type    = int
    comment = "가져오기의항목사용: 앱 마켓버전"
  }
  column "market_id" {
    null    = true
    type    = varchar(20)
    comment = "가져오기의항목사용: 마켓id"
    charset = "utf8mb4"
    collate = "utf8mb4_general_ci"
  }
  column "resource_status" {
    null    = true
    type    = varchar(20)
    comment = "항목상태: toObtain, obtained, toUpdate"
  }
  column "data_source" {
    null    = true
    type    = varchar(20)
    comment = "항목: create 항목생성 ; market 마켓가져오기 "
  }
  column "param_detail" {
    null    = true
    type    = text
    comment = "실행전사용자항목지정항목의테이블단일매개변수"
    charset = "utf8mb4"
    collate = "utf8mb4_unicode_ci"
  }
  column "dept_id_path" {
    null    = true
    type    = varchar(200)
    comment = "부서전체경로"
  }
  column "type" {
    null    = true
    type    = varchar(10)
    comment = "항목새버전봇의유형, web, other"
  }
  column "latest_release_time" {
    null    = true
    type    = timestamp
    comment = "항목새버전발송버전시간"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_robot_id" {
    columns = [column.robot_id]
  }
}
table "robot_execute_record" {
  schema  = schema.rpa
  comment = "항목단말봇실행항목기록테이블"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키id"
    auto_increment = true
  }
  column "execute_id" {
    null    = true
    type    = varchar(30)
    comment = "실행id"
  }
  column "robot_id" {
    null    = true
    type    = varchar(100)
    comment = "봇id"
  }
  column "robot_version" {
    null    = true
    type    = int
    comment = "봇버전항목"
  }
  column "start_time" {
    null    = true
    type    = timestamp
    comment = "시작 시간"
  }
  column "end_time" {
    null    = true
    type    = timestamp
    comment = "종료 시간"
  }
  column "execute_time" {
    null    = true
    type    = bigint
    comment = "실행항목시 단일위치초"
  }
  column "mode" {
    null    = true
    type    = varchar(60)
    comment = "항목목록항목PROJECT_LIST ; 항목항목항목EDIT_PAGE; 예약 작업시작CRONTAB ; 실행기기실행 EXECUTOR"
  }
  column "task_execute_id" {
    null    = true
    type    = varchar(30)
    comment = "예약 작업실행id, 항목schedule_task_execute의execute_id"
  }
  column "result" {
    null    = true
    type    = varchar(20)
    comment = "실행 결과: robotFail:실패,  robotSuccess:성공, robotCancel:가져오기항목(중중지), robotExecute:정상에서실행"
  }
  column "error_reason" {
    null    = true
    type    = varchar(255)
    comment = "오류원인"
  }
  column "execute_log" {
    null    = true
    type    = longtext
    comment = "로그내용"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  column "video_local_path" {
    null    = true
    type    = varchar(200)
    comment = "항목항목기록의본항목저장항목경로"
  }
  column "dept_id_path" {
    null    = true
    type    = varchar(100)
    comment = "부서전체경로항목코드"
  }
  column "terminal_id" {
    null    = true
    type    = char(36)
    comment = "단말항목일식별자, 예항목준비mac주소"
    charset = "utf8mb4"
    collate = "utf8mb4_general_ci"
  }
  column "data_table_path" {
    null    = true
    type    = varchar(255)
    comment = "데이터근거항목가져오기저장항목위치항목"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_rer_task_execute_id" {
    columns = [column.task_execute_id, column.deleted]
  }
  index "idx_robot_id" {
    columns = [column.robot_id]
  }
  index "robot_execute_record_execute_id_IDX" {
    columns = [column.execute_id, column.creator_id, column.tenant_id]
  }
}
table "robot_version" {
  schema  = schema.rpa
  comment = "항목단말봇버전테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키id"
    auto_increment = true
  }
  column "robot_id" {
    null    = true
    type    = varchar(100)
    comment = "봇id"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "version" {
    null    = true
    type    = int
    comment = "버전항목"
  }
  column "introduction" {
    null    = true
    type    = longtext
    comment = "항목"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "update_log" {
    null    = true
    type    = longtext
    comment = "변경 로그"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "use_description" {
    null    = true
    type    = longtext
    comment = "사용설명"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "online" {
    null    = true
    type    = smallint
    default = 0
    comment = "여부사용 0:미완료사용,1:완료사용"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  column "param" {
    null    = true
    type    = text
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "param_detail" {
    null    = true
    type    = text
    comment = "발송버전시항목의테이블단일매개변수정보"
    collate = "utf8mb4_unicode_ci"
  }
  column "video_id" {
    null    = true
    type    = varchar(100)
    comment = "항목주소id"
  }
  column "appendix_id" {
    null    = true
    type    = varchar(100)
    comment = "항목파일주소id"
  }
  column "icon" {
    null    = true
    type    = varchar(100)
    comment = "아이콘"
  }
  primary_key {
    columns = [column.id]
  }
}
table "sample_templates" {
  schema  = schema.rpa
  comment = "시스템시스템항목지정항목의항목라이브러리, 사용항목비고입력사용자항목항목데이터근거.지원 robot, project, task 대기다중항목유형."
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    comment        = "기본 키"
    auto_increment = true
  }
  column "sample_id" {
    null    = true
    type    = varchar(100)
    comment = "항목id"
  }
  column "name" {
    null    = false
    type    = varchar(50)
    comment = "항목버전이름"
  }
  column "type" {
    null    = false
    type    = varchar(20)
    comment = "항목유형: robot_design, robot_execute, schedule_task 대기"
  }
  column "version" {
    null    = false
    type    = varchar(20)
    default = "1.0.0"
    comment = "항목항목항목버전항목(예 1.2.0)"
  }
  column "data" {
    null    = false
    type    = mediumtext
    comment = "항목매칭항목데이터근거(JSON 형식), 데이터근거라이브러리일행의데이터근거"
  }
  column "description" {
    null    = true
    type    = text
    comment = "항목설명"
  }
  column "is_active" {
    null    = false
    type    = tinyint
    default = 1
    comment = "여부사용(false 이면새사용자아니오비고입력)"
  }
  column "is_deleted" {
    null    = false
    type    = tinyint
    default = 0
    comment = "항목삭제항목(항목물품관리삭제)"
  }
  column "created_time" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updated_time" {
    null      = false
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    on_update = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
}
table "sample_users" {
  schema  = schema.rpa
  comment = "항목기록사용자에서시스템시스템항목중비고입력의항목데이터근거, 예항목항목의항목중항목."
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    comment        = "기본 키항목증가ID"
    auto_increment = true
  }
  column "creator_id" {
    null    = false
    type    = char(36)
    comment = "사용자항목일식별자(예 UUID)"
  }
  column "tenant_id" {
    null = true
    type = varchar(36)
  }
  column "sample_id" {
    null    = false
    type    = varchar(100)
    comment = "닫기항목 sample_templates.sample_id"
  }
  column "name" {
    null    = false
    type    = varchar(100)
    comment = "사용자항목까지의이름(항목항목항목 name, 가능항목지정항목)"
  }
  column "data" {
    null    = false
    type    = mediumtext
    comment = "에서항목중비고입력의매칭항목데이터근거(JSON 문자열, 항목 Java 순서열항목)"
  }
  column "source" {
    null    = false
    type    = enum("system","user")
    default = "system"
    comment = "항목: system(시스템시스템항목비고입력)또는 user(사용자항목생성/수정)"
  }
  column "version_injected" {
    null    = false
    type    = varchar(20)
    comment = "비고입력시항목사용항목의버전항목, 사용항목후항목업그레이드항목"
  }
  column "created_time" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updated_time" {
    null      = false
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "항목후수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
}
table "schedule_task" {
  schema  = schema.rpa
  comment = "스케줄링작업"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "task_id" {
    null    = true
    type    = varchar(100)
    comment = "예약 작업id"
  }
  column "name" {
    null    = true
    type    = varchar(64)
    comment = "작업이름"
  }
  column "description" {
    null    = true
    type    = varchar(255)
    comment = "설명"
  }
  column "exception_handle_way" {
    null    = true
    type    = varchar(64)
    comment = "예외항목관리방법방식: stop중지  skip건너뛰기"
  }
  column "run_mode" {
    null    = true
    type    = varchar(64)
    comment = "실행항목방식, 항목cycle, 예약fixed,항목지정항목custom"
  }
  column "cycle_frequency" {
    null    = true
    type    = varchar(10)
    comment = "항목항목,단일위치초, -1로항목있음일항목, 3600, , , custom"
  }
  column "cycle_num" {
    null    = true
    type    = varchar(64)
    comment = "항목지정항목항목, 항목유형, 매1시간, 매3시간, , 항목지정항목"
  }
  column "cycle_unit" {
    null    = true
    type    = varchar(20)
    comment = "항목지정항목항목, 항목단일위치: minutes, hour"
  }
  column "status" {
    null    = true
    type    = varchar(64)
    comment = "상태: doing실행중 close완료결과항목 ready대기실행"
  }
  column "enable" {
    null    = true
    type    = bool
    comment = "시작/사용 안 함"
  }
  column "schedule_type" {
    null    = true
    type    = varchar(64)
    comment = "예약방법방식,day,month,week"
  }
  column "schedule_rule" {
    null    = true
    type    = varchar(255)
    comment = "예약매칭항목(매칭항목객체)"
  }
  column "start_at" {
    null    = true
    type    = datetime
    comment = "시작 시간"
  }
  column "end_at" {
    null    = true
    type    = datetime
    comment = "종료 시간"
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  column "enable_queue_execution" {
    null    = true
    type    = bool
    comment = "여부정렬팀실행"
  }
  column "cron_expression" {
    null    = true
    type    = varchar(50)
    comment = "cron테이블항목방식"
  }
  column "last_time" {
    null    = true
    type    = timestamp
    comment = "위항목가져오기시의nextTime"
  }
  column "next_time" {
    null    = true
    type    = timestamp
    comment = "아래항목실행시간"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성사람ID"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null = true
    type = smallint
  }
  column "pull_time" {
    null    = true
    type    = timestamp
    comment = "위항목가져오기시간"
  }
  column "log_enable" {
    null    = true
    type    = varchar(5)
    default = "F"
    comment = "여부열기시작로그항목기록"
    charset = "utf8mb4"
    collate = "utf8mb4_general_ci"
  }
  primary_key {
    columns = [column.id]
  }
}
table "schedule_task_execute" {
  schema  = schema.rpa
  comment = "예약 작업실행항목기록"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "task_id" {
    null    = true
    type    = varchar(20)
    comment = "작업ID"
  }
  column "task_execute_id" {
    null    = true
    type    = varchar(20)
    comment = "예약 작업실행id"
  }
  column "count" {
    null    = true
    type    = int
    comment = "실행항목, 1, 2, 3...."
  }
  column "result" {
    null    = true
    type    = varchar(20)
    comment = "작업상태항목    성공  \"success\"     # 시작 실패     \"start_error\"     # 실행실패      \"exe_error\"     # 가져오기항목     CANCEL = \"cancel\"     # 실행중   \"executing\""
  }
  column "start_time" {
    null    = true
    type    = datetime
    comment = "실행시작 시간"
  }
  column "end_time" {
    null    = true
    type    = datetime
    comment = "실행종료 시간"
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_ste_query" {
    columns = [column.tenant_id, column.creator_id, column.start_time, column.deleted]
  }
  index "idx_ste_status" {
    columns = [column.tenant_id, column.creator_id, column.result, column.start_time, column.deleted]
  }
}
table "schedule_task_pull_log" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null = true
    type = bigint
  }
  column "task_id" {
    null    = true
    type    = varchar(100)
    comment = "예약 작업id"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "pull_time" {
    null    = true
    type    = timestamp
    comment = "위항목가져오기시간"
  }
  column "last_time" {
    null    = true
    type    = timestamp
    comment = "위항목가져오기시의nextTime"
  }
  column "next_time" {
    null    = true
    type    = timestamp
    comment = "아래항목실행시간"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성사람ID"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
}
table "schedule_task_robot" {
  schema  = schema.rpa
  comment = "예약 작업봇목록"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "task_id" {
    null    = true
    type    = varchar(30)
    comment = "작업ID"
  }
  column "robot_id" {
    null    = true
    type    = varchar(30)
    comment = "봇ID"
  }
  column "sort" {
    null    = true
    type    = int
    comment = "정렬, 항목소항목전"
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "param_json" {
    null    = true
    type    = mediumtext
    comment = "예약 작업항목닫기매개변수"
  }
  primary_key {
    columns = [column.id]
  }
}
table "shared_file" {
  schema  = schema.rpa
  comment = "공유항목파일테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "id"
    auto_increment = true
  }
  column "file_id" {
    null    = true
    type    = bigint
    comment = "파일항목의uuid"
  }
  column "path" {
    null    = true
    type    = varchar(500)
    comment = "파일에서s3위항목의경로"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "수정 시간"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "file_name" {
    null    = true
    type    = varchar(1000)
    comment = "파일항목이름"
  }
  column "tags" {
    null    = true
    type    = varchar(512)
    comment = "파일태그이름항목합치기"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자ID"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "file_type" {
    null    = true
    type    = tinyint
    comment = "파일유형: 0-위치항목유형 1-텍스트 2-WORD 3-PDF"
  }
  column "file_index_status" {
    null    = true
    type    = tinyint
    comment = "파일항목량항목상태:1-항목항목 2-항목성공 3-실패"
  }
  column "dept_id" {
    null    = true
    type    = varchar(100)
    comment = "부서id"
  }
  primary_key {
    columns = [column.id]
  }
}
table "shared_file_tag" {
  schema  = schema.rpa
  comment = "공유항목파일태그테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "tag_id" {
    null           = false
    type           = bigint
    unsigned       = true
    comment        = "태그id"
    auto_increment = true
  }
  column "tag_name" {
    null    = true
    type    = varchar(255)
    comment = "태그항목이름"
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "수정 시간"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자ID"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자ID"
  }
  primary_key {
    columns = [column.tag_id]
  }
}
table "shared_sub_var" {
  schema  = schema.rpa
  comment = "공유 변수-항목변수"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    comment        = "항목변수id"
    auto_increment = true
  }
  column "shared_var_id" {
    null     = false
    type     = bigint
    unsigned = true
    comment  = "공유 변수id"
  }
  column "var_name" {
    null    = true
    type    = varchar(255)
    comment = "항목변수이름"
  }
  column "var_type" {
    null    = true
    type    = varchar(20)
    comment = "변수유형: text/password/array"
  }
  column "var_value" {
    null    = true
    type    = varchar(750)
    comment = "변수항목값, 암호화이면로비밀문서, 아니오이면로항목문서"
  }
  column "encrypt" {
    null    = true
    type    = bool
    comment = "여부암호화:1-암호화"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_shared_var_id" {
    columns = [column.shared_var_id]
  }
}
table "shared_var" {
  schema  = schema.rpa
  comment = "공유 변수정보"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "shared_var_name" {
    null    = true
    type    = varchar(255)
    comment = "공유 변수이름"
  }
  column "status" {
    null    = true
    type    = bool
    comment = "사용상태: 1사용"
  }
  column "remark" {
    null    = true
    type    = varchar(255)
    comment = "변수설명"
  }
  column "dept_id" {
    null    = true
    type    = char(36)
    comment = "항목부서ID"
  }
  column "usage_type" {
    null    = true
    type    = varchar(10)
    comment = "가능사용계정유형항목(all/dept/select): 모든사람: all, 항목부서모든사람: dept, 항목지정사람: select"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "shared_var_type" {
    null    = true
    type    = varchar(20)
    comment = "공유 변수유형: text/password/array/group"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_dept_id_path" {
    columns = [column.dept_id]
  }
}
table "shared_var_key_tenant" {
  schema  = schema.rpa
  comment = "공유 변수테넌트키테이블"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "tenant_id" {
    null = false
    type = varchar(36)
  }
  column "key" {
    null    = true
    type    = varchar(500)
    comment = "공유 변수테넌트키"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_tenant_id" {
    columns = [column.tenant_id]
  }
}
table "shared_var_user" {
  schema  = schema.rpa
  comment = "공유 변수및사용자의항목테이블;N:N항목"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "shared_var_id" {
    null     = false
    type     = bigint
    unsigned = true
    comment  = "공유 변수id"
  }
  column "user_id" {
    null    = true
    type    = char(36)
    comment = "사용자id"
  }
  column "user_name" {
    null    = true
    type    = varchar(100)
    comment = "사용자항목이름"
  }
  column "user_phone" {
    null    = true
    type    = varchar(100)
    comment = "사용자휴대폰 번호"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_shared_var_id" {
    columns = [column.shared_var_id]
  }
  index "idx_user_id" {
    columns = [column.user_id]
  }
}
table "sms_record" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = int
    comment        = "ID"
    auto_increment = true
  }
  column "receiver" {
    null    = true
    type    = varchar(30)
    comment = "짧음정보수신항목"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "send_type" {
    null    = true
    type    = varchar(30)
    comment = "짧음정보유형"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "send_result" {
    null    = true
    type    = varchar(20)
    comment = "전송결과"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "fail_reason" {
    null    = true
    type    = varchar(3000)
    comment = "실패원인"
    charset = "utf8"
    collate = "utf8_general_ci"
  }
  column "create_by" {
    null    = true
    type    = int
    comment = "생성자"
  }
  column "create_time" {
    null    = true
    type    = datetime
    comment = "생성 시간"
  }
  column "update_by" {
    null    = true
    type    = int
    comment = "수정자"
  }
  column "update_time" {
    null    = true
    type    = datetime
    comment = "수정 시간"
  }
  column "deleted" {
    null    = true
    type    = int
    comment = "여부삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
}
table "sys_product_version" {
  schema  = schema.rpa
  comment = "제품품목버전테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키ID"
    auto_increment = true
  }
  column "version_code" {
    null    = false
    type    = varchar(50)
    comment = "버전항목코드(예: personal, professional, enterprise)"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제식별자: 0-삭제되지 않음, 1-삭제됨"
  }
  column "create_time" {
    null    = true
    type    = datetime
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  primary_key {
    columns = [column.id]
  }
  index "uk_version_code" {
    unique  = true
    columns = [column.version_code]
  }
}
table "sys_tenant_config" {
  schema  = schema.rpa
  comment = "테넌트매칭항목테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키ID"
    auto_increment = true
  }
  column "tenant_id" {
    null    = false
    type    = varchar(64)
    comment = "테넌트ID"
  }
  column "version_id" {
    null    = false
    type    = bigint
    comment = "버전ID, 닫기항목sys_product_version.id"
  }
  column "extra_config_json" {
    null    = false
    type    = text
    comment = "매칭항목빠름항목(JSON형식, 항목패키지항목type, base, final필드)"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제식별자: 0-삭제되지 않음, 1-삭제됨"
  }
  column "create_time" {
    null    = true
    type    = datetime
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null      = true
    type      = datetime
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  index "uk_tenant_id" {
    unique  = true
    columns = [column.tenant_id]
  }
}
table "sys_version_default_config" {
  schema  = schema.rpa
  comment = "버전항목매칭항목테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키ID"
    auto_increment = true
  }
  column "version_id" {
    null    = false
    type    = bigint
    comment = "버전ID, 닫기항목sys_product_version.id"
  }
  column "resource_code" {
    null    = false
    type    = varchar(100)
    comment = "항목항목코드(예: designer_count, component_count대기)"
  }
  column "resource_type" {
    null    = false
    type    = bool
    comment = "항목유형: 1-Quota(매칭금액), 2-Switch(열기닫기)"
  }
  column "parent_code" {
    null    = true
    type    = varchar(100)
    comment = "항목단계항목항목코드(사용항목단계닫기시스템)"
  }
  column "default_value" {
    null    = false
    type    = int
    comment = "항목값(항목Quota예데이터량, 항목Switch예0또는1)"
  }
  column "url_patterns" {
    null    = true
    type    = text
    comment = "URL경로항목방식(JSON배열형식, 예: [\"/api/v1/design/**\"])"
  }
  column "description" {
    null    = true
    type    = varchar(500)
    comment = "항목설명"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제식별자: 0-삭제되지 않음, 1-삭제됨"
  }
  column "create_time" {
    null    = true
    type    = datetime
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null      = true
    type      = datetime
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_resource_code" {
    columns = [column.resource_code]
  }
  index "idx_version_id" {
    columns = [column.version_id]
  }
}
table "task_mail" {
  schema  = schema.rpa
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "user_id" {
    null = true
    type = char(36)
  }
  column "tenant_id" {
    null = true
    type = char(36)
  }
  column "resource_id" {
    null = true
    type = varchar(255)
  }
  column "email_service" {
    null    = true
    type    = varchar(50)
    comment = "메일함서비스서비스기기, 163Email, 126Email, qqEmail, customEmail"
  }
  column "email_protocol" {
    null    = true
    type    = varchar(50)
    comment = "사용항목, POP3,IMAP"
  }
  column "email_service_address" {
    null    = true
    type    = varchar(255)
    comment = "메일함서비스서비스기기주소"
  }
  column "port" {
    null    = true
    type    = varchar(50)
    comment = "메일함서비스서비스기기단말항목"
  }
  column "enable_ssl" {
    null    = true
    type    = bool
    comment = "여부사용SSL"
  }
  column "email_account" {
    null    = true
    type    = varchar(255)
    comment = "메일함계정"
  }
  column "authorization_code" {
    null    = true
    type    = varchar(255)
    comment = "메일함권한 부여코드"
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부"
  }
  primary_key {
    columns = [column.id]
  }
}
table "terminal" {
  schema  = schema.rpa
  comment = "단말테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null           = false
    type           = bigint
    comment        = "기본 키id, 사용항목데이터근거예약시스템계획의항목정도관리관리"
    auto_increment = true
  }
  column "terminal_id" {
    null    = false
    type    = char(36)
    comment = "단말항목일식별자, 예항목준비mac주소"
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "dept_id" {
    null    = true
    type    = varchar(100)
    comment = "부서id"
  }
  column "dept_id_path" {
    null    = true
    type    = varchar(100)
    comment = "부서전체경로id"
  }
  column "name" {
    null    = true
    type    = varchar(200)
    comment = "단말이름"
  }
  column "account" {
    null    = true
    type    = varchar(100)
    comment = "항목준비계정"
  }
  column "os" {
    null    = true
    type    = varchar(50)
    comment = "운영체제"
  }
  column "ip" {
    null    = true
    type    = varchar(200)
    comment = "ip목록"
  }
  column "actual_client_ip" {
    null    = true
    type    = varchar(100)
    comment = "항목연결항목IP, 서버감지후의권장ip"
  }
  column "custom_ip" {
    null    = true
    type    = varchar(20)
    comment = "사용자항목지정항목ip"
  }
  column "port" {
    null    = true
    type    = int
    comment = "단말항목"
  }
  column "status" {
    null    = true
    type    = varchar(20)
    comment = "현재상태, 실행중busy, 빈항목free, 항목offline, 단일기기중standalone"
  }
  column "remark" {
    null    = true
    type    = varchar(100)
    comment = "단말설명"
  }
  column "user_id" {
    null    = true
    type    = varchar(100)
    comment = "항목후로그인의사용자의id, 사용항목근거항목이름항목선택"
  }
  column "os_name" {
    null    = true
    type    = char(36)
    comment = "정보항목: 항목항목준비사용자명"
  }
  column "os_pwd" {
    null    = true
    type    = varchar(200)
    comment = "정보항목: 항목항목준비사용자비밀번호"
  }
  column "is_dispatch" {
    null    = true
    type    = smallint
    comment = "여부스케줄링항목방식"
  }
  column "monitor_url" {
    null    = true
    type    = varchar(100)
    comment = "항목항목url"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "단말항목기록생성 시간"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = false
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "custom_port" {
    null    = true
    type    = int
    comment = "항목지정항목단말항목"
  }
  primary_key {
    columns = [column.id]
  }
  index "cloud_terminal_mac_tenant_index" {
    columns = [column.tenant_id]
  }
  index "cloud_terminal_tenant_id_IDX" {
    columns = [column.tenant_id, column.dept_id_path]
  }
  index "cloud_terminal_user_id_IDX" {
    columns = [column.os_name]
  }
}
table "terminal_group" {
  schema  = schema.rpa
  comment = "단말분그룹-분그룹및단말의항목테이블;N:N항목"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "group_id" {
    null    = true
    type    = bigint
    comment = "분그룹이름"
  }
  column "terminal_id" {
    null    = true
    type    = bigint
    comment = "단말id"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_group_id" {
    columns = [column.group_id]
  }
  index "idx_terminal_id" {
    columns = [column.terminal_id]
  }
}
table "terminal_group_info" {
  schema  = schema.rpa
  comment = "단말분그룹"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "group_name" {
    null    = true
    type    = varchar(100)
    comment = "분그룹이름"
  }
  column "terminal_id" {
    null    = true
    type    = varchar(20)
    comment = "단말id"
  }
  column "dept_id" {
    null    = true
    type    = char(36)
    comment = "항목부서ID"
  }
  column "usage_type" {
    null    = true
    type    = varchar(10)
    comment = "가능사용계정유형항목(all/dept/select): 모든사람: all, 항목부서모든사람: dept, 항목지정사람: select"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_dept_id_path" {
    columns = [column.dept_id]
  }
  index "idx_terminal_id" {
    columns = [column.terminal_id]
  }
}
table "terminal_group_user" {
  schema  = schema.rpa
  comment = "단말분그룹-분그룹및사용자의항목테이블;N:N항목"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "group_id" {
    null    = true
    type    = varchar(20)
    comment = "분그룹이름"
  }
  column "user_id" {
    null    = true
    type    = char(36)
    comment = "사용자id"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  column "user_name" {
    null    = true
    type    = varchar(100)
    comment = "사용자항목이름"
  }
  column "user_phone" {
    null    = true
    type    = varchar(100)
    comment = "사용자휴대폰 번호"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_group_id" {
    columns = [column.group_id]
  }
  index "idx_user_id" {
    columns = [column.user_id]
  }
}
table "terminal_login_history" {
  schema  = schema.rpa
  comment = "단말로그인계정항목항목기록"
  column "id" {
    null           = false
    type           = bigint
    unsigned       = true
    auto_increment = true
  }
  column "terminal_id" {
    null    = true
    type    = varchar(20)
    comment = "단말id"
  }
  column "account" {
    null    = true
    type    = varchar(100)
    comment = "계정"
  }
  column "user_name" {
    null    = true
    type    = varchar(100)
    comment = "사용자명"
  }
  column "login_time" {
    null    = true
    type    = timestamp
    comment = "로그인시간"
  }
  column "logout_time" {
    null    = true
    type    = timestamp
    comment = "로그아웃시간"
  }
  column "creator_id" {
    null    = true
    type    = bigint
    comment = "생성자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updater_id" {
    null    = true
    type    = bigint
    comment = "수정자id"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
}
table "terminal_login_record" {
  schema  = schema.rpa
  comment = "단말로그인계정항목항목기록"
  charset = "utf8mb4"
  collate = "utf8mb4_bin"
  column "id" {
    null = false
    type = char(36)
  }
  column "login_user_id" {
    null    = true
    type    = char(36)
    comment = "로그인사용자id"
  }
  column "login_phone" {
    null    = true
    type    = varchar(40)
    comment = "로그인휴대폰 번호"
  }
  column "login_name" {
    null    = true
    type    = varchar(40)
    comment = "로그인이름"
  }
  column "login_time" {
    null    = true
    type    = timestamp
    comment = "로그인시간"
  }
  column "logout_time" {
    null    = true
    type    = timestamp
    comment = "로그아웃시간"
  }
  column "terminal_id" {
    null    = true
    type    = varchar(20)
    comment = "단말id"
  }
  column "dept_id" {
    null    = true
    type    = char(36)
    comment = "부서id"
  }
  column "dept_id_path" {
    null    = true
    type    = varchar(100)
    comment = "부서전체경로id"
  }
  column "ip" {
    null    = true
    type    = varchar(40)
    comment = "로그인IP"
  }
  column "user_agent" {
    null    = true
    type    = varchar(512)
    comment = "user-agent"
  }
  column "login_status" {
    null    = false
    type    = int
    comment = "여부로그인성공{0:로그인실패, 1:로그인성공}"
  }
  column "remark" {
    null    = true
    type    = varchar(1000)
    comment = "항목설명"
  }
  column "creator_id" {
    null    = true
    type    = char(36)
    comment = "생성자id"
  }
  column "updater_id" {
    null    = true
    type    = char(36)
    comment = "수정자id"
  }
  column "create_time" {
    null    = true
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null      = true
    type      = timestamp
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "deleted" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부 0: 삭제되지 않음, 1: 삭제됨"
  }
  primary_key {
    columns = [column.id]
  }
}
table "trigger_task" {
  schema  = schema.rpa
  comment = "트리거기기예약 작업"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "task_id" {
    null    = true
    type    = bigint
    comment = "트리거기기예약 작업id"
  }
  column "name" {
    null    = true
    type    = varchar(50)
    comment = "트리거기기예약 작업이름"
  }
  column "task_json" {
    null    = true
    type    = mediumtext
    comment = "항목생성예약 작업의항목매개변수"
  }
  column "creator_id" {
    null = true
    type = char(36)
  }
  column "updater_id" {
    null = true
    type = char(36)
  }
  column "deleted" {
    null    = false
    type    = bool
    default = 0
  }
  column "create_time" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
    comment = "수정 시간"
  }
  column "task_type" {
    null    = true
    type    = varchar(20)
    comment = "작업유형: 예약schedule, 메일mail, 파일file, 항목hotKey:"
  }
  column "enable" {
    null    = false
    type    = bool
    default = 0
    comment = "여부사용"
  }
  column "exceptional" {
    null    = false
    type    = varchar(20)
    default = "stop"
    comment = "항목오류예항목관리: 건너뛰기jump, 중지stop"
  }
  column "timeout" {
    null    = true
    type    = int
    default = 9999
    comment = "시간 초과시간"
  }
  column "tenant_id" {
    null    = true
    type    = char(36)
    comment = "테넌트id"
  }
  column "queue_enable" {
    null    = true
    type    = smallint
    default = 0
    comment = "여부사용정렬팀 1:사용 0:아니오사용"
  }
  column "retry_num" {
    null    = true
    type    = int
    comment = "항목있음exceptional로retry시, 항목기록의재시도항목데이터"
  }
  primary_key {
    columns = [column.id]
  }
}
table "t_tenant_expiration" {
  schema  = schema.rpa
  comment = "테넌트까지항목정보테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null    = false
    type    = varchar(64)
    comment = "기본 키ID"
  }
  column "tenant_id" {
    null    = false
    type    = varchar(64)
    comment = "테넌트ID"
  }
  column "expiration_date" {
    null    = true
    type    = varchar(64)
    comment = "까지항목시간(형식: YYYY-MM-DD, 항목항목항목버전로암호화데이터근거, 항목버전로항목문서)"
  }
  column "create_time" {
    null    = true
    type    = datetime
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null      = true
    type      = datetime
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "is_delete" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부(0-아니오, 1-예)"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_is_delete" {
    columns = [column.is_delete]
  }
  index "idx_tenant_id" {
    columns = [column.tenant_id]
  }
  index "uk_tenant_id" {
    unique  = true
    columns = [column.tenant_id]
  }
}
table "user_blacklist" {
  schema = schema.rpa
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "user_id" {
    null    = false
    type    = varchar(50)
    comment = "사용자ID"
  }
  column "username" {
    null    = false
    type    = varchar(100)
    comment = "사용자명"
  }
  column "ban_reason" {
    null    = true
    type    = varchar(500)
    comment = "항목원인"
  }
  column "ban_level" {
    null    = true
    type    = int
    default = 1
    comment = "항목대기단계(1,2,3...)"
  }
  column "ban_count" {
    null    = true
    type    = int
    default = 1
    comment = "항목항목데이터"
  }
  column "ban_duration" {
    null    = true
    type    = bigint
    comment = "항목시길이(초)"
  }
  column "start_time" {
    null    = false
    type    = datetime
    comment = "항목시작 시간"
  }
  column "end_time" {
    null    = false
    type    = datetime
    comment = "항목종료 시간"
  }
  column "status" {
    null    = true
    type    = tinyint
    default = 1
    comment = "상태(1:항목중, 0:완료해제항목)"
  }
  column "operator" {
    null    = true
    type    = varchar(50)
    comment = "항목사람"
  }
  column "create_time" {
    null    = true
    type    = datetime
    default = sql("CURRENT_TIMESTAMP")
  }
  column "update_time" {
    null      = true
    type      = datetime
    default   = sql("CURRENT_TIMESTAMP")
    on_update = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_end_time_status" {
    columns = [column.end_time, column.status]
  }
  index "idx_user_id" {
    columns = [column.user_id]
  }
}
table "user_entitlement" {
  schema  = schema.rpa
  comment = "사용자권한항목테이블"
  charset = "utf8mb4"
  collate = "utf8mb4_general_ci"
  column "id" {
    null    = false
    type    = varchar(64)
    comment = "기본 키ID"
  }
  column "user_id" {
    null    = false
    type    = varchar(64)
    comment = "사용자ID"
  }
  column "tenant_id" {
    null    = false
    type    = varchar(64)
    comment = "테넌트ID"
  }
  column "module_designer" {
    null    = true
    type    = bool
    default = 0
    comment = "항목계획기기권한(0-없음권한, 1-있음권한)"
  }
  column "module_executor" {
    null    = true
    type    = bool
    default = 0
    comment = "실행기기권한(0-없음권한, 1-있음권한)"
  }
  column "module_console" {
    null    = true
    type    = bool
    default = 0
    comment = "항목제어항목권한(0-없음권한, 1-있음권한)"
  }
  column "module_market" {
    null    = true
    type    = bool
    default = 1
    comment = "팀마켓권한(0-없음권한, 1-있음권한, 항목1)"
  }
  column "create_time" {
    null    = true
    type    = datetime
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "update_time" {
    null      = true
    type      = datetime
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "is_delete" {
    null    = true
    type    = bool
    default = 0
    comment = "삭제 여부(0-아니오, 1-예)"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_is_delete" {
    columns = [column.is_delete]
  }
  index "idx_tenant_id" {
    columns = [column.tenant_id]
  }
  index "idx_user_id" {
    columns = [column.user_id]
  }
  index "uk_user_tenant" {
    unique  = true
    columns = [column.user_id, column.tenant_id, column.is_delete]
  }
}
table "astron_agent_auth" {
  schema = schema.rpa
  comment = "ShopRPAAgent항목권한항목저장"
  charset = "utf8mb4"
  collate = "utf8mb4_0900_ai_ci"
  column "id" {
    null           = false
    type           = int
    auto_increment = true
  }

  column "user_id" {
    null = true
    type = varchar(50)
  }

  column "astron_user_name" {
    null = true
    type = varchar(50)
  }

  column "name" {
    null = true
    type = varchar(50)
  }

  column "app_id" {
    null = true
    type = varchar(50)
  }

  column "api_key" {
    null = true
    type = varchar(100)
  }

  column "api_secret" {
    null = true
    type = varchar(100)
  }

  column "created_at" {
    null = true
    type = datetime
  }

  column "updated_at" {
    null = true
    type = datetime
  }

  column "is_active" {
    null = true
    type = tinyint(1)
  }

  primary_key {
    columns = [column.id]
  }
}
table "openai_workflows" {
  schema  = schema.rpa
  collate = "utf8mb4_0900_ai_ci"
  column "project_id" {
    null    = false
    type    = varchar(100)
    comment = "항목목록ID(기본 키)"
  }
  column "name" {
    null    = false
    type    = varchar(100)
    comment = "워크플로이름"
  }
  column "description" {
    null    = true
    type    = varchar(500)
    comment = "워크플로설명"
  }
  column "version" {
    null    = false
    type    = int
    default = 1
    comment = "워크플로버전항목"
  }
  column "status" {
    null    = false
    type    = int
    default = 1
    comment = "워크플로상태(1=항목, 0=사용 안 함)"
  }
  column "user_id" {
    null    = false
    type    = varchar(50)
    comment = "사용자ID"
  }
  column "example_project_id" {
    null    = true
    type    = varchar(100)
    comment = "예시사용자계정아래의project_id, 사용항목실행시항목"
  }
  column "created_at" {
    null    = false
    type    = datetime
    default = sql("CURRENT_TIMESTAMP")
    comment = "생성 시간"
  }
  column "updated_at" {
    null      = false
    type      = datetime
    default   = sql("CURRENT_TIMESTAMP")
    comment   = "수정 시간"
    on_update = sql("CURRENT_TIMESTAMP")
  }
  column "english_name" {
    null    = true
    type    = varchar(100)
    comment = "항목후의영어이름"
  }
  column "parameters" {
    null    = true
    type    = text
    comment = "저장항목JSON문자열형식의매개변수"
  }
  primary_key {
    columns = [column.project_id]
  }
  index "idx_created_at" {
    columns = [column.created_at]
  }
  index "idx_name" {
    columns = [column.name]
  }
  index "idx_status" {
    columns = [column.status]
  }
  index "idx_user_id" {
    columns = [column.user_id]
  }
}
table "openai_executions" {
  schema  = schema.rpa
  collate = "utf8mb4_0900_ai_ci"
  column "id" {
    null    = false
    type    = varchar(36)
    comment = "실행항목기록ID(UUID)"
  }
  column "project_id" {
    null    = false
    type    = varchar(100)
    comment = "항목목록ID(닫기항목워크플로)"
  }
  column "status" {
    null    = false
    type    = varchar(20)
    default = "PENDING"
    comment = "실행상태(PENDING/RUNNING/COMPLETED/FAILED/CANCELLED)"
  }
  column "parameters" {
    null    = true
    type    = text
    comment = "실행매개변수(JSON형식)"
  }
  column "result" {
    null    = true
    type    = text
    comment = "실행 결과(JSON형식)"
  }
  column "error" {
    null    = true
    type    = text
    comment = "오류정보"
  }
  column "user_id" {
    null    = false
    type    = varchar(50)
    comment = "사용자ID"
  }
  column "exec_position" {
    null    = false
    type    = varchar(50)
    default = "EXECUTOR"
    comment = "실행위치항목"
  }
  column "recording_config" {
    null    = true
    type    = text
    comment = "기록제어매칭항목"
  }
  column "version" {
    null    = true
    type    = int
    comment = "워크플로버전항목"
  }
  column "start_time" {
    null    = false
    type    = datetime
    default = sql("CURRENT_TIMESTAMP")
    comment = "시작 시간"
  }
  column "end_time" {
    null    = true
    type    = datetime
    comment = "종료 시간"
  }
  primary_key {
    columns = [column.id]
  }
  foreign_key "openai_executions_ibfk_1" {
    columns     = [column.project_id]
    ref_columns = [table.openai_workflows.column.project_id]
    on_update   = NO_ACTION
    on_delete   = CASCADE
  }
  index "idx_project_id" {
    columns = [column.project_id]
  }
  index "idx_start_time" {
    columns = [column.start_time]
  }
  index "idx_status" {
    columns = [column.status]
  }
  index "idx_user_id" {
    columns = [column.user_id]
  }
}
table "openapi_users" {
  schema  = schema.rpa
  collate = "utf8mb4_0900_ai_ci"
  column "id" {
    null           = false
    type           = int
    auto_increment = true
  }
  column "user_id" {
    null = false
    type = varchar(50)
  }
  column "phone" {
    null = false
    type = varchar(20)
  }
  column "default_api_key" {
    null = true
    type = varchar(100)
  }
  column "created_at" {
    null    = false
    type    = datetime
    default = sql("CURRENT_TIMESTAMP")
  }
  column "updated_at" {
    null      = false
    type      = datetime
    default   = sql("CURRENT_TIMESTAMP")
    on_update = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_phone" {
    columns = [column.phone]
  }
  index "idx_user_id" {
    columns = [column.user_id]
  }
  index "phone" {
    unique  = true
    columns = [column.phone]
  }
  index "user_id" {
    unique  = true
    columns = [column.user_id]
  }
}
table "point_allocations" {
  schema  = schema.rpa
  collate = "utf8mb4_0900_ai_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "user_id" {
    null = false
    type = varchar(50)
  }
  column "initial_amount" {
    null    = false
    type    = int
    comment = "기존항목분매칭데이터량"
  }
  column "remaining_amount" {
    null    = false
    type    = int
    comment = "현재항목데이터량"
  }
  column "allocation_type" {
    null    = false
    type    = varchar(100)
    comment = "항목분항목"
  }
  column "priority" {
    null    = false
    type    = int
    default = 0
    comment = "항목단계, 데이터값항목높음항목단계항목높음"
  }
  column "created_at" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  column "expires_at" {
    null    = false
    type    = datetime
    comment = "항목분경과항목시간"
  }
  column "description" {
    null    = true
    type    = varchar(255)
    comment = "설명"
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_expires_at" {
    columns = [column.expires_at]
  }
  index "idx_user_expiry" {
    columns = [column.user_id, column.expires_at]
  }
  index "idx_user_id" {
    columns = [column.user_id]
  }
}
table "point_consumptions" {
  schema  = schema.rpa
  collate = "utf8mb4_0900_ai_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "transaction_id" {
    null    = false
    type    = bigint
    comment = "닫기항목의항목ID"
  }
  column "allocation_id" {
    null    = false
    type    = bigint
    comment = "닫기항목의분매칭ID"
  }
  column "amount" {
    null    = false
    type    = int
    comment = "에서항목분매칭중사용의항목분데이터량"
  }
  column "created_at" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
}
table "point_transactions" {
  schema  = schema.rpa
  collate = "utf8mb4_0900_ai_ci"
  column "id" {
    null           = false
    type           = bigint
    auto_increment = true
  }
  column "user_id" {
    null = false
    type = varchar(100)
  }
  column "amount" {
    null    = false
    type    = int
    comment = "항목항목금액(정상데이터또는항목데이터)"
  }
  column "transaction_type" {
    null    = false
    type    = varchar(50)
    comment = "항목유형"
  }
  column "related_entity_type" {
    null    = true
    type    = varchar(50)
    comment = "닫기항목항목유형"
  }
  column "related_entity_id" {
    null    = true
    type    = bigint
    comment = "닫기항목항목ID"
  }
  column "description" {
    null    = true
    type    = varchar(255)
    comment = "설명"
  }
  column "created_at" {
    null    = false
    type    = timestamp
    default = sql("CURRENT_TIMESTAMP")
  }
  primary_key {
    columns = [column.id]
  }
  index "idx_user_id" {
    columns = [column.user_id]
  }
}
schema "rpa" {
  charset = "utf8"
  collate = "utf8_general_ci"
}