# Component 컴포넌트모듈

## 개요
Component모듈완료의컴포넌트관리관리공가능, 패키지컴포넌트의증가삭제수정조회, 상태관리관리, 권한제어대기.

## 공가능
- 컴포넌트의생성, 업데이트, 삭제, 조회
- 컴포넌트이름재복사조회
- 컴포넌트상태관리관리(중, 완료발송버전, 완료위, 지정)
- 컴포넌트관리관리(생성, 마켓가져오기)
- 분조회및파일선택
- 테넌트및사용자권한제어

## 파일결과
```
src/main/java/com/iflytek/rpa/component/
├── constants/
│   └── ComponentConstant.java          # 컴포넌트일반량지정
├── controller/
│   └── ComponentController.java        # 제어기기
├── dao/
│   └── ComponentDao.java               # 데이터방문연결
├── entity/
│   ├── Component.java                  # 컴포넌트유형
│   ├── dto/
│   │   └── ComponentQueryDto.java      # 조회DTO
│   ├── enums/
│   │   └── ComponentStatusEnum.java    # 상태
│   └── vo/
│       └── ComponentVo.java            # 이미지객체
├── service/
│   ├── ComponentService.java           # 서비스연결
│   └── impl/
│       └── ComponentServiceImpl.java   # 서비스
└── README.md                           # 설명문서
```

## 데이터베이스테이블결과
```sql
CREATE TABLE component (
    id               BIGINT AUTO_INCREMENT COMMENT '기본 키id' PRIMARY KEY,
    component_id     VARCHAR(100) NULL COMMENT '봇일id, 가져오기의사용id',
    name             VARCHAR(100) NULL COMMENT '현재이름문자, 사용목록 ',
    creator_id       BIGINT NULL COMMENT '생성자id',
    create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL COMMENT '생성 시간',
    updater_id       BIGINT NULL COMMENT '수정자id',
    update_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시간',
    is_shown         SMALLINT(1) DEFAULT 1 NULL COMMENT '여부에서사용자목록  0: 아니요, 1: ',
    deleted          SMALLINT(1) DEFAULT 0 NULL COMMENT '삭제 여부 0: 삭제되지 않음, 1: 삭제됨',
    tenant_id        BIGINT NULL,
    app_id           VARCHAR(50) CHARSET utf8mb4 NULL COMMENT 'appmarketResource중의사용id',
    app_version      INT NULL COMMENT '가져오기의사용: 앱 마켓버전',
    market_id        VARCHAR(20) CHARSET utf8mb4 NULL COMMENT '가져오기의사용: 마켓id',
    resource_status  VARCHAR(20) NULL COMMENT '상태: toObtain, obtained, toUpdate',
    data_source      VARCHAR(20) NULL COMMENT ': create 생성 ; market 마켓가져오기',
    transform_status VARCHAR(20) NULL COMMENT 'editing 중, published 완료발송버전, shared 완료위, locked지정(불가)'
) COMMENT '컴포넌트테이블' CHARSET = utf8;
```

## API연결

### 1. 생성컴포넌트
- **연결**: `POST /component/create`
- **매개변수**: Component객체
- **공가능**: 생성새의컴포넌트

### 2. 업데이트컴포넌트
- **연결**: `POST /component/update`
- **매개변수**: Component객체
- **공가능**: 업데이트있음컴포넌트정보

### 3. 삭제컴포넌트
- **연결**: `DELETE /component/delete/{componentId}`
- **매개변수**: componentId (경로매개변수)
- **공가능**: 삭제컴포넌트

### 4. 조회컴포넌트
- **연결**: `GET /component/detail/{componentId}`
- **매개변수**: componentId (경로매개변수)
- **공가능**: 근거ID조회컴포넌트

### 5. 분조회컴포넌트목록
- **연결**: `GET /component/list`
- **매개변수**: pageNum, pageSize, name (가능선택)
- **공가능**: 분조회컴포넌트목록, 지원이름조회

### 6. 이름 변경컴포넌트
- **연결**: `PUT /component/rename`
- **매개변수**: componentId, newName
- **공가능**: 이름 변경컴포넌트

### 7. 조회이름재복사
- **연결**: `GET /component/check-name`
- **매개변수**: name, componentId (가능선택)
- **공가능**: 조회컴포넌트이름여부재복사

### 8. 업데이트컴포넌트상태
- **연결**: `PUT /component/status`
- **매개변수**: componentId, transformStatus
- **공가능**: 업데이트컴포넌트의변환상태

### 9. 가져오기 의컴포넌트목록
- **연결**: `GET /component/my-list`
- **공가능**: 가져오기현재사용자생성의컴포넌트목록

## 상태설명

### 상태 (resourceStatus)
- `toObtain`: 대기가져오기
- `obtained`: 완료가져오기
- `toUpdate`: 대기업데이트

### 데이터 (dataSource)
- `create`: 생성
- `market`: 마켓가져오기

### 변환상태 (transformStatus)
- `editing`: 중
- `published`: 완료발송버전
- `shared`: 완료위
- `locked`: 지정

## 사용예시

### 생성컴포넌트
```java
Component component = new Component();
component.setName("시도컴포넌트");
component.setAppId("app123");
component.setMarketId("market456");

AppResponse<?> response = componentService.createComponent(component);
```

### 조회컴포넌트목록
```java
AppResponse<?> response = componentService.getComponentList(1, 10, "시도");
```

### 업데이트컴포넌트상태
```java
AppResponse<?> response = componentService.updateComponentStatus("comp123", "published");
```

## 비고
1. 모든필요사용자로그인및테넌트권한검증인증
2. 컴포넌트이름에서일테넌트아래할 수 없음재복사
3. 삭제사용삭제, 아니요물품관리삭제데이터
4. 컴포넌트상태변수변경필요의권한제어
5. 지원테넌트, 아니요테넌트의데이터

## 설명
- MyBatis-Plus: 데이터방문
- Spring Boot: 사용
- Lombok: 코드도구
- Validation: 매개변수검증인증 