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

게이트웨이와 주요 백엔드 라우트까지 한 번에 확인하려면 저장소 루트에서 다음을 실행합니다.

```powershell
corepack pnpm run doctor:release
```

`backend service route smoke`에서 `/api/rpa-openapi/health` 또는 `/api/rpa-ai-service/health`가 401 `Missing SESSION` 또는 404로 나오면 현재 소스가 아니라 낡은 게이트웨이/nginx 또는 Python 서비스 컨테이너가 떠 있을 가능성이 큽니다. Docker 권한이 있는 환경에서 다음처럼 Python 서비스와 nginx를 다시 빌드/기동한 뒤 재확인합니다.

```powershell
cd docker
docker compose up -d --build ai-service openapi-service openresty-nginx
```

기본 서버 설정은 `docker/.env`에서 관리합니다. 서버 주소, 포트, 인증 서비스 주소를 운영 환경에 맞게 조정한 뒤 실행합니다.
`OPENAPI_WORKFLOWS_UPSERT_URL`은 robot-service가 OpenAPI 서비스에 워크플로우를 동기화할 때 쓰는 내부 주소이며, Docker 기본값은 `http://openapi-service:8020/workflows/upsert`입니다.
`ROBOT_SERVICE_BASE_URL`은 openapi-service가 사용자 등록과 워크플로우 복사를 위해 robot-service를 호출할 때 쓰며, Docker 기본값은 `http://robot-service:8040`입니다.

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

빌드 과정에서는 엔진 리소스 준비, 프론트엔드 빌드, 데스크톱 앱 패키징이 함께 수행됩니다. 기본 데스크톱 산출물은 검증 가능한 휴대용 패키지로 생성되며, 실행 파일은 `frontend/packages/electron-app/dist/win-portable/ShopRPA.cmd`입니다.

Python 백엔드 서비스를 로컬에서 직접 실행하거나 `doctor:release`의 Python 런타임 경고를 해소하려면 먼저 두 서비스의 `.venv`를 동기화합니다.

```powershell
corepack pnpm run setup:python-backends
```

현재 머신에서 잠금 파일 기준으로 어떤 패키지가 설치될지 먼저 확인하려면 다음 명령을 사용합니다.

```powershell
corepack pnpm run setup:python-backends:check
```

네트워크를 쓰지 않고 로컬 `uv` 캐시만으로 가능한지 확인하려면 다음 명령을 사용합니다.

```powershell
corepack pnpm run setup:python-backends:offline-check
```

백엔드 wheelhouse 준비용 잠금 requirements를 내보내려면 다음 명령을 사용합니다.

```powershell
corepack pnpm run export:python-backend-reqs
```

이 명령은 `build\python-backend-requirements` 아래에 서비스별 requirements와 `python-backend-wheelhouse-manifest.md/json`도 함께 생성합니다.

폐쇄망에서는 잠긴 Python 3.13 Windows 런타임 휠을 모은 wheelhouse를 넘길 수 있습니다.

```powershell
corepack pnpm run setup:python-backends -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends -Offline
```

온라인 준비 머신에서는 requirements 내보내기와 wheel 다운로드를 한 번에 실행할 수 있습니다. 다운로드가 끝나면 같은 wheelhouse로 오프라인 dry-run 검증도 자동 실행됩니다.

```powershell
corepack pnpm run export:python-backend-reqs -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends -DownloadWheelhouse
```

사내 PyPI 미러를 써야 하면 실행 전에 `$env:SHOPRPA_PIP_INDEX_URL = "https://your-pypi-mirror/simple"`을 지정하거나 스크립트에 `-IndexUrl`을 넘깁니다.

wheelhouse가 충분한지만 먼저 확인하려면 `-CheckOnly`를 추가합니다.

```powershell
corepack pnpm run setup:python-backends -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends -Offline -CheckOnly
```

루트 pnpm 스크립트로 `-Wheelhouse` 같은 추가 인자를 넘길 때는 `--` 뒤에 붙입니다. 예: `corepack pnpm run setup:python-backends:offline-check -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends`.

`-Wheelhouse`와 `-CheckOnly`를 함께 사용하면 `build\python-backend-requirements`에 내보낸 requirements 기준으로 정확한 `name==version` wheel이 있는지 먼저 확인한 뒤 `uv --dry-run`을 실행합니다. 결과는 다음 파일에도 남습니다.

```text
build\python-backend-requirements\python-backend-wheelhouse-preflight.md
build\python-backend-requirements\python-backend-wheelhouse-preflight.json
build\python-backend-requirements\python-backend-wheelhouse-missing.requirements.txt
build\python-backend-requirements\ai-service.missing-wheelhouse.requirements.txt
build\python-backend-requirements\openapi-service.missing-wheelhouse.requirements.txt
```

`*.missing-wheelhouse.requirements.txt` 파일은 현재 wheelhouse에 없는 정확한 잠금 버전만 모읍니다. 온라인 Windows/Python 3.13 준비 머신에서 이 파일들을 `pip download -r ... -d <wheelhouse>` 또는 사내 미러 다운로드 입력으로 사용한 뒤, 같은 `-Wheelhouse` 경로로 다시 `-Offline -CheckOnly`를 실행합니다.

부족한 wheel만 다시 받을 때는 다음 명령을 사용합니다. 다운로드가 끝나면 같은 wheelhouse로 오프라인 dry-run 검증까지 이어서 실행됩니다. 사설 미러나 보안 정책 때문에 일부 패키지만 막히는 환경에서는 `-ContinueOnError`를 추가해 전체 누락 목록과 항목별 실패 사유를 리포트로 남깁니다. 막힌 환경에서 목록만 빠르게 수집하려면 `-PipRetries 0`도 함께 사용합니다.

```powershell
corepack pnpm run download:python-backend-wheelhouse-missing -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends -ContinueOnError -PipRetries 0
```

현재 필요한 잠금 wheel 목록만 네트워크 없이 확인하려면 다음 명령을 사용합니다.

```powershell
corepack pnpm run download:python-backend-wheelhouse-missing -- -ListOnly
```

전체 스택 릴리스 검증은 포터블 빌드와 별개로 Java, Maven, Docker 권한, 브라우저 자동화 가능 세션이 필요합니다. 현재 머신이 준비됐는지는 다음 명령으로 먼저 확인합니다.

```powershell
java -version
mvn -version
# Maven wrapper를 넣어둔 환경이면 .\mvnw.cmd -version으로 대체 가능
docker version
whoami /groups | Select-String -Pattern "docker-users"
Get-Service -Name com.docker.service
```

Java 백엔드 중 `backend\resource-service`는 POM 기준 Java 21을 요구하므로 최종 상용 검증 머신은 JDK 21 이상을 사용해야 합니다.
Maven은 PATH 외에도 `MAVEN_HOME`, `M2_HOME`, 일반 설치 경로, repo `mvnw.cmd`, VS Code Java 확장의 내장 m2e Maven에서 자동 탐색합니다. Maven을 ZIP으로 배치한 운영 머신에서는 `MAVEN_HOME` 또는 `M2_HOME`을 Maven 루트로 지정한 뒤 새 터미널에서 검증합니다.

사내 Maven 미러나 반입한 로컬 Maven 저장소를 써야 하는 환경에서는 다음처럼 저장소와 settings 파일을 지정합니다. `settings.xml`에는 사내 mirror 또는 repository 인증 정보를 넣고, 로컬 저장소에는 `spring-boot-starter-parent`, `spring-boot-dependencies` 등 POM 잠금 버전에 필요한 artifact가 있어야 합니다.

```powershell
$env:SHOPRPA_MAVEN_LOCAL_REPOSITORY = "D:\maven-cache\shoprpa-m2"
$env:SHOPRPA_MAVEN_SETTINGS = "D:\maven-cache\settings.xml"
corepack pnpm run test:java-backends
```

환경 변수를 남기지 않고 한 번만 실행하려면 스크립트 인자로도 같은 값을 넘길 수 있습니다.

```powershell
corepack pnpm run test:java-backends -- -MavenLocalRepository D:\maven-cache\shoprpa-m2 -MavenSettings D:\maven-cache\settings.xml
```

`docker version`에서 named-pipe permission denied가 나오면 현재 Windows 사용자를 `docker-users`에 넣고 Docker Desktop을 실행한 뒤 터미널을 새로 열어야 합니다.

Java 백엔드 모듈이 실제로 컴파일/테스트되는지는 Maven이 준비된 머신에서 다음 명령으로 확인합니다.

```powershell
corepack pnpm run test:java-backends
```

현재 호스트에서 남은 full-stack 차단 요인을 한 번에 진단하려면 다음 명령을 사용합니다.

```powershell
corepack pnpm run repair:release-host
corepack pnpm run repair:release-host:strict
corepack pnpm run repair:release-host:apply
```

Docker 권한과 Python wheelhouse가 준비된 운영 검증 머신에서는 `-Apply`로 Python 백엔드 의존성 확인과 `ai-service`, `openapi-service`, `openresty-nginx` 재빌드/재기동을 이어서 실행할 수 있습니다.
진단 결과는 `build\release-host-repair-report.json`과 사람이 바로 읽는 `build\release-host-repair-report.md`에도 남습니다.

```powershell
corepack pnpm run repair:release-host:apply -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends -Offline
```

이 Windows PowerShell 기반 루트 스크립트에 추가 인자를 넘길 때는 `pnpm run ... -- -Wheelhouse ...`처럼 `--`를 넣습니다. 루트 스크립트는 `scripts/run-powershell.cjs` 래퍼를 통해 이 토큰을 제거한 뒤 PowerShell에 안전하게 전달합니다.
Maven 설치 전 Docker/Python 경로만 먼저 복구해야 하는 운영 머신에서는 `-SkipMaven`을 추가할 수 있습니다. 단, 최종 릴리스 검증에서는 Maven 경고가 남으면 `audit:release:strict`가 계속 실패합니다.

포터블 산출물의 파일, 해시, Python 런타임 압축, 실행 스모크를 PowerShell에서 직접 검증하려면 다음 명령을 사용합니다.

```powershell
corepack pnpm run verify:portable:host
```

`build.bat`으로 프론트엔드 패키징까지 수행하는 경우에도 마지막에 같은 host 검증이 실행되어 archive 무결성, browser bridge inject 포함 여부, packaged smoke가 확인됩니다.

릴리스 인수인계 감사 리포트를 생성하려면 다음 명령을 사용합니다.

```powershell
corepack pnpm run audit:release
```

남은 차단점이 있을 때 실패해야 하는 CI/최종 게이트에서는 `corepack pnpm run audit:release:strict`를 사용합니다. 전체 상용 인수용으로는 포터블 검증, 브라우저 런타임 검증, GUI E2E 리포트 검증, 호스트 strict 점검, strict 감사를 한 번에 묶은 다음 명령을 사용합니다.

```powershell
corepack pnpm run verify:release:full
```

이 명령은 앞단 검증이 실패해도 가능한 후속 점검과 strict 감사를 이어서 실행하고 `build\release-full-verification.json`에 단계별 `status`, `summary`, exit code, 출력 요약을 남깁니다. 사람이 바로 읽는 요약은 `build\release-full-verification.md`에 함께 생성되며, `build\release-audit.md`도 최종 결과로 갱신됩니다.

전체 검증 단계를 빠르게 확인하거나, 이미 생성된 전체 검증 리포트의 audit만 새로 고칠 때는 다음 명령을 사용합니다.

```powershell
corepack pnpm run verify:release:full:list
corepack pnpm run verify:release:full:refresh
```

실제 GUI 조작과 브라우저 확장 흐름은 대화형 Windows 데스크톱에서 검증한 뒤 별도 리포트로 남깁니다.

```powershell
corepack pnpm run template:gui-e2e
# build\gui-browser-e2e-report.json을 실제 검증 결과로 채운 뒤
corepack pnpm run verify:gui-e2e
```

이 리포트는 포터블 패키지 해시, 필수 시나리오 PASS 여부, 실행 환경(`windowsVersion`, `backendMode`, `backendUrl`, `browser`), 각 시나리오의 요약과 비어 있지 않은 증거 파일을 검증하므로 단순 체크리스트보다 엄격합니다. 각 시나리오의 `evidence.checks`에 있는 세부 확인 항목도 모두 `PASS`와 구체적인 요약을 가져야 합니다. 시각 품질 항목(`visual-polish`)은 실제 스크린샷 파일도 필요합니다.

브라우저 실행 권한 없이 확장 산출물만 정적으로 확인하려면 다음 명령을 사용합니다. 이 검증은 `dist\manifest.json`, 배포 zip 내부 manifest, 서비스워커, content script, CSS, 아이콘, 핵심 권한, CSP, ShopRPA 산출물명을 확인하고 `build\browser-extension-static\browser-extension-static-report.json`에 결과를 남깁니다.

```powershell
corepack pnpm run verify:browser-extension-static
```

content script 브라우저 단위 테스트는 다음 명령으로 실행합니다. 이 테스트는 Chromium 실행 권한이 필요하며, 결과는 `build\browser-extension-content-tests\browser-extension-content-tests-report.json`에 남습니다.

```powershell
corepack pnpm run test:browser-extension-content
```

Chromium/Playwright 실행이 호스트 정책으로 막힌 환경에서 content script의 순수 DOM 로직만 빠르게 확인하려면 다음 보조 테스트를 실행합니다. 이 경로는 jsdom 기반이라 실제 브라우저 확장 승인 검증을 대체하지 않습니다.

```powershell
corepack pnpm run test:browser-extension-content:jsdom
```

브라우저 확장 런타임까지 자동 스모크하려면 다음 명령을 사용합니다. 이 검증은 Chromium 호환 브라우저의 CDP 포트가 허용된 데스크톱 세션이 필요하며, 결과는 `build\browser-extension-smoke\browser-extension-smoke-report.json`에 남습니다.

```powershell
corepack pnpm run smoke:browser-extension
corepack pnpm run verify:browser-extension-smoke
```

`connectOverCDP` timeout이나 `spawn EPERM`이 나오면 확장 패키지 자체보다 호스트 보안 정책이 브라우저 디버깅 포트, Playwright launch, 또는 Node/Chromium child process 실행을 막는 상태입니다. 이 경우 엔드포인트 정책에서 Node, Electron, Chromium 실행과 `127.0.0.1` CDP/WebSocket 자동화를 허용한 뒤 다시 실행합니다.

새 브라우저 프로세스 실행만 막히고 이미 허용된 브라우저 CDP는 사용할 수 있는 환경에서는 브라우저를 수동으로 `--remote-debugging-port=9222 --load-extension=<repo>\frontend\packages\browser-plugin\dist` 옵션으로 띄운 뒤 다음처럼 기존 CDP에 붙여 smoke를 실행할 수 있습니다.

```powershell
$env:SHOPRPA_BROWSER_CDP_URL = "http://127.0.0.1:9222"
corepack pnpm run smoke:browser-extension
```

검증된 포터블 폴더를 설치 가능한 zip 패키지로 묶으려면 다음 명령을 사용합니다. 이 패키지는 NSIS가 아니라 PowerShell 설치 스크립트와 포터블 payload를 포함하며, 기본 설치 위치는 `%LOCALAPPDATA%\Programs\ShopRPA`입니다.

```powershell
corepack pnpm run build:portable-installer
corepack pnpm run verify:portable-installer
```

루트의 `corepack pnpm run verify:release`는 포터블 빌드, 프론트엔드 타입체크와 단위 테스트, 브라우저 확장 Chromium zip 생성과 정적 검증, host 격리 smoke, 포터블 Python 엔진 runtime 테스트, 설치 zip 생성, 설치 zip smoke 검증까지 함께 실행합니다. 이 명령은 포터블 릴리스 게이트이며, Docker/Maven/Python 백엔드/실제 GUI 및 브라우저 런타임까지 포함한 최종 상용 인수 판정은 `corepack pnpm run verify:release:full` 또는 `corepack pnpm run audit:release:strict`로 확인합니다.

Electron renderer가 실제로 화면까지 그리는지 스크린샷으로 추가 확인하려면 호스트 보안 정책이 Electron/Chromium child process와 로컬 렌더러 실행을 허용하는 환경에서 다음 선택 검증을 실행합니다.

```powershell
corepack pnpm run smoke:portable-renderer
```

빌드 스크립트는 일반적인 사용자 설치 경로의 Python 3.13, `resources/7zr.exe`, `python -m uv`를 자동으로 탐지합니다. 전체 빌드에서는 엔진 압축 전에 브라우저 bridge-inject JS를 다시 빌드해 `engine\servers\astronverse-browser-bridge\src\astronverse\browser_bridge\inject`에 동기화합니다. 폐쇄망이나 오프라인 환경에서 엔진 소스 빌드가 외부 Python 패키지 다운로드 때문에 실패하더라도 기존 `resources/python_core.7z` 번들이 있으면 이를 재사용해 실행 가능한 데스크톱 패키지를 계속 만들 수 있습니다. 엔진 소스 빌드를 반드시 새로 성공시켜야 하는 검증 환경에서는 다음처럼 실행합니다.

사내 PyPI 미러를 써야 하는 환경에서는 빌드 전에 `$env:SHOPRPA_PIP_INDEX_URL = "https://your-pypi-mirror/simple"`처럼 지정합니다. 지정하지 않으면 빌드 스크립트는 특정 외부 미러를 강제하지 않습니다.

```powershell
$env:SHOPRPA_STRICT_ENGINE_BUILD = "1"
$env:UV_LOCK_TIMEOUT = "300"
.\build.bat
```

폐쇄망에서 `hatchling` 같은 Python 빌드 의존성을 새로 받을 수 없지만 기존 `resources/python_core.7z` 기반으로 엔진 소스 수정만 반영해야 할 때는 다음처럼 오프라인 overlay 모드를 사용합니다.

```powershell
$env:SHOPRPA_OFFLINE_ENGINE_OVERLAY = "1"
.\build.bat
```

운영 배포에서는 Electron의 웹 보안과 인증서 검증을 기본값으로 유지합니다. 폐쇄망 테스트에서 임시로 자체 서명 인증서나 교차 출처 문제를 우회해야 할 때만 실행 직전에 `$env:SHOPRPA_ALLOW_INSECURE_ELECTRON = "1"`을 지정합니다.

NSIS 설치형 패키지를 별도로 만들 때는 프론트엔드 폴더에서 다음 명령을 사용합니다.

```powershell
cd frontend
corepack pnpm run verify:release
corepack pnpm run doctor:installer
corepack pnpm run build:installer
```

`doctor:installer`는 `electron-builder`가 사용하는 `app-builder.exe`를 Node child process로 실행할 수 있는지 먼저 확인합니다. 보안 정책, 샌드박스, AppLocker, 백신 정책 등으로 `spawn EPERM`이 발생하는 환경에서는 NSIS 설치 파일 생성을 진행할 수 없습니다. 이 경우에도 `pnpm build:desktop`으로 만드는 포터블 패키지는 실행 가능한 배포 산출물이며, 루트의 `build:portable-installer`로 PowerShell 기반 설치 zip을 만들 수 있습니다.

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
