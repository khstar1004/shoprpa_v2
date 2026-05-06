<div align="center">
  <img src="./frontend/public/icons/128x128.png" width="96" height="96" alt="ShopRPA icon" />

  <h1>ShopRPA</h1>

  <p><strong>반복 업무를 워크플로우로 만들고 실행하는 데스크톱 RPA 자동화 프로그램</strong></p>

  <p>
    <strong>Desktop Automation</strong> ·
    <strong>Web Automation</strong> ·
    <strong>Workflow Engine</strong> ·
    <strong>Team Operation</strong>
  </p>
</div>

<p align="center">
  <img src="./frontend/packages/web-app/src/assets/brand/shoprpa-hero-bg.png" alt="ShopRPA preview" width="100%" />
</p>

## 소개

ShopRPA는 화면 조작, 브라우저 작업, 파일 처리, 엑셀/문서 작업, API 연동 같은 반복 업무를 자동화하는 RPA 프로그램입니다. 사용자는 데스크톱 클라이언트에서 워크플로우를 만들고, 내장 엔진을 통해 실제 업무 화면을 자동으로 조작할 수 있습니다.

이 저장소에는 클라이언트, 실행 엔진, 백엔드 서비스, Docker 서버 구성이 함께 들어 있습니다.

## 핵심 기능

| 영역 | 기능 |
| --- | --- |
| 데스크톱 자동화 | Windows 프로그램 클릭, 입력, 단축키, 화면 요소 조작 |
| 웹 자동화 | Chrome, Edge 기반 웹 업무 자동화 |
| 데이터 처리 | 엑셀, PDF, Word, 파일, 폴더 작업 |
| 인식/판단 | 이미지 인식, 화면 요소 탐지, AI 서비스 연동 |
| 실행 관리 | 예약 실행, 원격 실행, 실행 로그 확인 |
| 협업 운영 | 팀 단위 로봇 공유, 서버 기반 관리 |

## 구성

```text
ShopRPA
├─ frontend/   데스크톱 앱과 사용자 화면
├─ engine/     Python 기반 RPA 실행 엔진
├─ backend/    인증, 워크플로우, 리소스, 실행 관리 서비스
├─ docker/     서버 실행용 Docker Compose 구성
├─ resources/  클라이언트 실행 리소스와 기본 설정
└─ docs/       부가 문서와 이미지 자료
```

## 동작 구조

| 구성 요소 | 역할 |
| --- | --- |
| 서버 | 사용자, 인증, 워크플로우, 파일 리소스, 실행 기록 관리 |
| 클라이언트 | 워크플로우 작성, 실행, 디버깅, 결과 확인 |
| 엔진 | 실제 화면 조작과 자동화 컴포넌트 실행 |

## 서버 실행

서버는 `docker/` 폴더의 Docker Compose 구성으로 실행합니다.

```powershell
cd docker
docker compose up -d
```

상태 확인:

```powershell
docker compose ps
```

기본 서버 설정은 `docker/.env`에서 관리합니다. 서버 주소, 포트, 인증 서비스 주소를 운영 환경에 맞게 조정한 뒤 실행합니다.

## 클라이언트 설정

클라이언트는 `resources/conf.yaml`의 서버 주소로 백엔드에 연결합니다.

```yaml
remote_addr: http://YOUR_SERVER_ADDRESS:32742/
skip_engine_start: false
```

| 항목 | 설명 |
| --- | --- |
| `remote_addr` | 연결할 ShopRPA 서버 주소 |
| `skip_engine_start` | 클라이언트 실행 시 내장 엔진 실행 여부 |

설치된 프로그램에서 사용할 때도 설치 경로의 `resources/conf.yaml`을 확인하면 됩니다.

## 빌드

소스에서 설치 파일을 만들 때는 루트의 `build.bat`을 사용합니다.

```powershell
.\build.bat
```

Python 경로를 명시해야 하는 경우:

```powershell
.\build.bat --python-exe "C:\Program Files\Python313\python.exe"
```

빌드 과정에서는 엔진 리소스 준비, 프론트엔드 빌드, 데스크톱 앱 패키징이 함께 수행됩니다.

## 로그

| 위치 | 내용 |
| --- | --- |
| `logs/` | 로컬 실행 로그 |
| `.run-logs/` | 개발/실행 보조 로그 |
| `docker compose logs` | 서버 컨테이너 로그 |

Docker 서버 로그:

```powershell
cd docker
docker compose logs -f
```

## 운영 흐름

1. 서버를 실행합니다.
2. 클라이언트의 `resources/conf.yaml`에서 서버 주소를 맞춥니다.
3. 클라이언트를 실행합니다.
4. 워크플로우를 만들거나 불러옵니다.
5. 실행 후 로그와 결과를 확인합니다.

## 참고

| 파일 | 내용 |
| --- | --- |
| `BUILD_GUIDE.md` | 상세 빌드 정보 |
| `FAQ.md` | 자주 발생하는 문제 |
| `backend/openapi-service/api.yaml` | OpenAPI 정의 |
| `docker/` | 서버 실행 구성 |
| `resources/conf.yaml` | 클라이언트 연결 설정 |
