# ShopRPA Build And Release Guide

This guide describes the currently verified way to build, verify, and hand over
ShopRPA on Windows.

## Supported Output

The primary verified desktop deliverable is the portable Windows package:

```powershell
D:\astron-rpa\frontend\packages\electron-app\dist\win-portable\ShopRPA.cmd
```

Run the app from `ShopRPA.cmd`. The portable package contains:

- `ShopRPA.cmd`: portable desktop launcher
- `resources/app.asar`: packaged Electron app
- `resources/python_core.7z`: bundled Python runtime and RPA engine
- `resources/conf.yaml`: runtime backend configuration
- `runtime/node_modules/electron/dist/electron.exe`: bundled Electron runtime
- `README-portable.txt`: end-user launch note

The same verified payload can also be wrapped as a PowerShell install zip:

```powershell
D:\astron-rpa\frontend\packages\electron-app\dist\installers\ShopRPA-1.1.8-portable-installer.zip
```

That package installs to `%LOCALAPPDATA%\Programs\ShopRPA` by default and creates
Desktop/Start Menu shortcuts. It is not an NSIS installer, but it gives a
validated install flow on hosts where NSIS packaging is blocked.

The NSIS installer is supported only on a normal Windows host where `node.exe`
is allowed to spawn child processes such as `app-builder.exe`.

## Environment

Required for the verified portable build:

| Tool | Required State |
| --- | --- |
| Windows | Windows 10/11 |
| Node.js | 22 or newer |
| pnpm | 9 or newer through Corepack |
| Bundled resources | `resources/python_core.7z`, `resources/7zr.exe`, `resources/conf.yaml` |

Required only for backend/source-level checks not covered by the portable build:

| Tool | Purpose |
| --- | --- |
| Java/JDK 21+ | Java backend service build and runtime checks; `resource-service` requires Java 21 |
| Maven | Java backend module tests and packaging |
| Docker Desktop | Docker compose backend deployment checks |
| Interactive desktop session | Screen capture, browser extension, CUA, and GUI automation validation |

The Docker backend uses `OPENAPI_WORKFLOWS_UPSERT_URL` for robot-service to
sync workflow create/delete events into openapi-service. The compose default is
`http://openapi-service:8020/workflows/upsert`; keep it on the Docker service
name unless the backend is split across hosts.

Run Java backend Maven tests on a JDK/Maven-capable validation host:

```powershell
corepack pnpm run test:java-backends
```

`ROBOT_SERVICE_BASE_URL` is the reverse internal call path used by
openapi-service for user registration and workflow copy operations. The compose
default is `http://robot-service:8040`.

## Fresh Verification

From the repository root:

```powershell
corepack pnpm run verify:release
```

This runs the frontend release checks, builds the Chromium browser-extension zip,
verifies the browser-extension static package, runs the portable host smoke,
executes the bundled engine runtime tests, builds the PowerShell installer zip,
and verifies that installer payload.

This is the portable release gate, not the full commercial acceptance gate. To
fail on any remaining backend host, browser runtime, or interactive GUI blocker,
run the full gate on a capable Windows validation host:

```powershell
corepack pnpm run verify:release:full
```

To add a host renderer screenshot smoke on a machine that allows
Electron/Chromium child processes, run:

```powershell
corepack pnpm run smoke:portable-renderer
```

The full runner continues through the remaining checks when possible, writes
step `status`, `summary`, exit code, and output tails to
`build\release-full-verification.json`, writes the human-readable summary
`build\release-full-verification.md`, and exits non-zero if any acceptance step
fails. It also refreshes `build\release-audit.md` after the step report has been
written.

To list the full gate without executing it, or to refresh only the audit from an
existing full report, use:

```powershell
corepack pnpm run verify:release:full:list
corepack pnpm run verify:release:full:refresh
```

For a fast readiness summary that separates the verified portable package from
host-dependent blockers, run:

```powershell
corepack pnpm run doctor:release
```

The doctor also probes gateway-backed backend routes through `remote_addr`.
`backend service route smoke` covers auth, robot, Casdoor, OpenAPI, and AI
service health routes. If the root gateway health passes but this route smoke
reports 401 `Missing SESSION` or 404 for `/api/rpa-openapi/health` or
`/api/rpa-ai-service/health`, the source route exists but the running gateway or
Python service containers are stale or not using the current bind-mounted
source. On a Docker-capable host, rebuild and recreate the Python services plus
nginx before rerunning the doctor:

```powershell
cd docker
docker compose up -d --build ai-service openapi-service openresty-nginx
```

To write a handoff audit report under `build/release-audit.md`:

```powershell
corepack pnpm run audit:release
```

To make the audit fail when any full-stack blocker remains:

```powershell
corepack pnpm run audit:release:strict
```

The interactive GUI and browser-extension acceptance run must produce a
validated report before the audit can be complete:

```powershell
corepack pnpm run template:gui-e2e
# Complete build\gui-browser-e2e-report.json during an interactive desktop run.
corepack pnpm run verify:gui-e2e
```

That verifier checks the current portable package hash, required scenario IDs,
PASS status, environment fields (`windowsVersion`, `backendMode`, `backendUrl`,
and `browser`), plus a written summary and at least one non-empty evidence file
for each GUI/browser scenario. Every scenario-level `evidence.checks` item must
also be marked `PASS` with a concrete summary. The `visual-polish` scenario also
requires a screenshot evidence file.

For a static browser-extension package verification that does not launch a
browser, run:

```powershell
corepack pnpm run verify:browser-extension-static
```

That verifier checks `dist\manifest.json`, the packaged zip manifest, the service
worker, content script, CSS, icons, required permissions, host permissions,
extension-page CSP, and ShopRPA artifact naming. It writes
`build\browser-extension-static\browser-extension-static-report.json`.

For content-script browser unit tests, run:

```powershell
corepack pnpm run test:browser-extension-content
```

This writes
`build\browser-extension-content-tests\browser-extension-content-tests-report.json`
and requires a desktop host that allows Chromium process launch.

If Chromium or Playwright launch is blocked by endpoint policy, run the jsdom
auxiliary content-script check to cover pure DOM logic without a browser:

```powershell
corepack pnpm run test:browser-extension-content:jsdom
```

This does not replace the browser-backed acceptance check above.

For a narrower automated browser-extension runtime smoke, run:

```powershell
corepack pnpm run smoke:browser-extension
corepack pnpm run verify:browser-extension-smoke
```

This launches a Chromium-compatible browser with the built extension, exercises
content-script messaging, element input/click, and table extraction, then writes
`build\browser-extension-smoke\browser-extension-smoke-report.json`. It requires
a desktop host that allows browser CDP debugging or Playwright browser launch.
If that policy is blocked, the report is written with `ok=false` and the release
doctor keeps it as a WARN.
`connectOverCDP` timeouts or `spawn EPERM` failures mean the endpoint policy must
allow Node, Electron, Chromium child processes, and local `127.0.0.1`
CDP/WebSocket automation before this smoke can pass.
If new browser process launch is blocked but an already approved browser CDP
endpoint is available, start that browser manually with
`--remote-debugging-port=9222 --load-extension=<repo>\frontend\packages\browser-plugin\dist`,
set `SHOPRPA_BROWSER_CDP_URL=http://127.0.0.1:9222`, and rerun
`corepack pnpm run smoke:browser-extension`.

Before treating a full-stack release gate as actionable, verify the host has the
tools and permissions that the portable-only verifier does not require:

```powershell
java -version
mvn -version
# If the repository carries a Maven wrapper, .\mvnw.cmd -version is also accepted.
# Maven is auto-detected from PATH, MAVEN_HOME, M2_HOME, common local paths, or repo mvnw.cmd.
docker version
whoami /groups | Select-String -Pattern "docker-users"
Get-Service -Name com.docker.service
```

`docker version` must show a server version. If it reports named-pipe permission
denied, add the Windows user to `docker-users`, ensure Docker Desktop is running,
and restart the terminal session. If `com.docker.service` is stopped, start Docker
Desktop or the service before rerunning `doctor:release`.

For an operator-friendly diagnostic that groups the same host blockers and can
repair the Docker/Python paths when the host is ready, run:

```powershell
corepack pnpm run repair:release-host
corepack pnpm run repair:release-host:strict
corepack pnpm run repair:release-host:apply
```

On a Docker-capable validation machine with a populated Python wheelhouse, apply
the repair flow before rerunning the release doctor:
The diagnostic also writes `build\release-host-repair-report.json` and the
human-readable `build\release-host-repair-report.md`.

```powershell
corepack pnpm run repair:release-host:apply -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends -Offline
```

For these root PowerShell scripts, pass additional arguments after `--`.
The root scripts use `scripts/run-powershell.cjs` to strip that token before
calling PowerShell, so `pnpm run ... -- -Wheelhouse ...` is the supported form.
If an operations machine needs to repair only the Docker/Python path before
Maven is installed, add `-SkipMaven`. Final release acceptance still requires
Maven to be present and `audit:release:strict` to pass without warnings.

Use strict mode when the machine is expected to support installer, Java/Maven,
and Docker verification as well:

```powershell
corepack pnpm run doctor:release:strict
```

The release verifier expands to these gates:

```powershell
corepack pnpm --dir frontend run lint
corepack pnpm --filter shoprpa run typecheck
corepack pnpm --dir frontend run test:run
corepack pnpm --filter @rpa/web-app run build
corepack pnpm --filter @rpa/extension run build:bridge-inject
corepack pnpm --dir frontend run build:desktop
corepack pnpm run verify:portable:host
corepack pnpm run test:engine-runtime
corepack pnpm run build:portable-installer:fast
corepack pnpm run verify:portable-installer
```

The release doctor also compiles and imports the Python backend entrypoints
from `backend/ai-service/.venv` and `backend/openapi-service/.venv` when those
virtual environments are present. A missing dependency there is reported as a
WARN in normal mode and fails strict mode. It also warns when `uv.lock` does
not contain runtime dependencies declared in `pyproject.toml`; regenerate the
lockfile with `uv lock` or `uv sync` on a machine with `uv` installed.

To prepare both Python backend virtual environments with locked runtime
dependencies:

```powershell
corepack pnpm run setup:python-backends
```

For a dry-run dependency check that does not modify the virtual environments:

```powershell
corepack pnpm run setup:python-backends:check
```

For a cache-only dry run that does not touch the network:

```powershell
corepack pnpm run setup:python-backends:offline-check
```

To export locked runtime requirements for wheelhouse preparation:

```powershell
corepack pnpm run export:python-backend-reqs
```

The export also writes `python-backend-wheelhouse-manifest.md/json` under
`build\python-backend-requirements`, including service package counts, local
wheel artifacts, and the online/offline wheelhouse commands.

In a closed network, this command only works when the locked wheels are already
available in the local `uv` cache. To force cache-only behavior:

```powershell
corepack pnpm run setup:python-backends -- -Offline
```

For a closed-network handoff with a prepared wheel folder, point the script at
that folder. The folder must contain the locked runtime wheels for Python 3.13
on Windows, including transitive packages such as `PyYAML`, `cryptography`,
`SQLAlchemy`, `aiomysql`, and `pydantic-settings`.

```powershell
corepack pnpm run setup:python-backends -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends -Offline
```

On an online Windows/Python 3.13 preparation host, the export script can also
populate the wheel folder directly. After download, it validates the same
wheelhouse with the offline dry-run preflight:

```powershell
corepack pnpm run export:python-backend-reqs -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends -DownloadWheelhouse
```

If the preparation host must use a private PyPI mirror, set
`SHOPRPA_PIP_INDEX_URL` or pass `-IndexUrl` to the export script before
downloading the wheelhouse.

To validate the wheelhouse without modifying `.venv`, add `-CheckOnly`:

```powershell
corepack pnpm run setup:python-backends -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends -Offline -CheckOnly
```

When passing extra arguments through a root pnpm script, insert `--`.
Use `corepack pnpm run setup:python-backends:offline-check -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends`.

When a wheelhouse is supplied in offline/check-only mode, the setup script first
compares the folder against the exact locked `name==version` pins exported under
`build\python-backend-requirements`, then runs the `uv` dry run. The preflight
also writes these handoff reports:

```text
build\python-backend-requirements\python-backend-wheelhouse-preflight.md
build\python-backend-requirements\python-backend-wheelhouse-preflight.json
build\python-backend-requirements\python-backend-wheelhouse-missing.requirements.txt
build\python-backend-requirements\ai-service.missing-wheelhouse.requirements.txt
build\python-backend-requirements\openapi-service.missing-wheelhouse.requirements.txt
```

The `*.missing-wheelhouse.requirements.txt` files contain only the exact locked
pins missing from the supplied wheelhouse. On an online Windows/Python 3.13
preparation host, use them as `pip download -r ... -d <wheelhouse>` inputs or
feed them to the private mirror process, then rerun the offline preflight against
the same wheelhouse.

To download only the currently missing pins and then validate the same
wheelhouse, use `-ContinueOnError` when a private mirror or endpoint policy may
block only part of the list. The command still exits non-zero if any pin fails,
but the report records every attempted package. Use `-PipRetries 0` for a fast
blocked-host inventory, or omit it on a healthy online prep host:

```powershell
corepack pnpm run download:python-backend-wheelhouse-missing -- -Wheelhouse D:\wheelhouse\shoprpa-python-backends -ContinueOnError -PipRetries 0
```

If those exports are missing, run `corepack pnpm run export:python-backend-reqs`
first.

If an engine source build is required and `uv` reports a package cache lock
timeout, retry with a longer lock wait:

```powershell
$env:SHOPRPA_STRICT_ENGINE_BUILD = "1"
$env:UV_LOCK_TIMEOUT = "300"
.\build.bat
```

For closed-network rebuilds where Python build dependencies such as `hatchling`
cannot be downloaded, use the existing `build\python_core` runtime and overlay
the current engine sources before archiving:

```powershell
$env:SHOPRPA_OFFLINE_ENGINE_OVERLAY = "1"
.\build.bat
```

The portable build command creates and verifies:

```powershell
frontend\packages\electron-app\dist\win-portable
```

For an additional archive check, run 7-Zip directly from PowerShell:

```powershell
& frontend\packages\electron-app\dist\win-portable\resources\7zr.exe t frontend\packages\electron-app\dist\win-portable\resources\python_core.7z
```

The preferred host-level portable verification command combines file checks,
archive hash verification, direct 7-Zip integrity testing, and packaged startup
smoke testing:

```powershell
corepack pnpm run verify:portable:host
```

`build.bat` runs this host-level verification automatically after frontend
packaging, so a successful full build has already covered the direct 7-Zip
archive check and packaged startup smoke test.

For a standalone packaged startup smoke test:

```powershell
$env:SHOPRPA_SMOKE_TEST='1'
& frontend\packages\electron-app\dist\win-portable\ShopRPA.cmd
```

Expected success line:

```text
packaged smoke test passed
```

## Installer Build

To build and verify the PowerShell-based install zip from the verified portable
payload:

```powershell
corepack pnpm run build:portable-installer
corepack pnpm run verify:portable-installer
```

The verifier extracts the zip, installs it under `build\portable-installer-verify`,
checks the copied files and hashes, and runs the installed package smoke test.

For the NSIS installer, run the preflight first:

```powershell
corepack pnpm --dir frontend run doctor:installer
```

If it passes:

```powershell
corepack pnpm --dir frontend run build:installer
```

If it fails with `spawn EPERM`, the host security policy blocks Node child
process execution. That is a host restriction, not a corrupt ShopRPA package.
Use the verified portable build on that host, or build the installer on a normal
Windows session. On the blocked host, use `build:portable-installer` for the
validated PowerShell install zip.

## Release Checklist

Before handing over a build, verify each item with real command output.

| Requirement | Evidence |
| --- | --- |
| Frontend lint is clean | `corepack pnpm --dir frontend run lint` exits 0 |
| Desktop TypeScript is clean | `corepack pnpm --filter shoprpa run typecheck` exits 0 |
| Browser plugin unit tests pass | `corepack pnpm --dir frontend run test:run` exits 0 |
| Browser bridge inject assets are current | `corepack pnpm --filter @rpa/extension run build:bridge-inject` syncs the generated JS into `engine\servers\astronverse-browser-bridge\src\astronverse\browser_bridge\inject` |
| Web renderer builds | `corepack pnpm --filter @rpa/web-app run build` exits 0 |
| Portable package builds | `corepack pnpm --dir frontend run build:desktop` exits 0 |
| Portable archive is readable | `corepack pnpm run verify:portable:host` reports `Everything is Ok` |
| Browser bridge inject ships in the Python archive | `corepack pnpm run verify:portable:host` verifies `browser_bridge\inject\backgroundInject.js` and `contentInject.js` inside `python_core.7z` |
| Portable app starts | `SHOPRPA_SMOKE_TEST=1` reports `packaged smoke test passed` |
| Bundled engine runtime passes local suites | `corepack pnpm run test:engine-runtime` writes `build\engine-runtime-tests.json` |
| PowerShell install zip is valid | `corepack pnpm run build:portable-installer` and `corepack pnpm run verify:portable-installer` exit 0 |
| Python system layer passes | bundled Python `unittest discover` exits 0 |
| Installer host is valid | `doctor:installer` exits 0, or the release notes state the host restriction |
| Full release readiness is summarized | `corepack pnpm run doctor:release` lists PASS/WARN/FAIL states |
| Docker internal hostnames match compose | `doctor:release` reports `docker env hostnames` and `docker example hostnames` as PASS |

## Known Verification Boundaries

The portable desktop package can be built and smoke-tested without Java, Maven,
or Docker. That does not prove Java backend services, Docker compose deployment,
or every interactive automation path.

Full commercial acceptance still requires these checks on a capable machine:

- Java and Maven available for Java backend builds/tests, either from PATH, `MAVEN_HOME`, `M2_HOME`, a common local install path, or a repository `mvnw.cmd`
- Docker Desktop engine accessible for compose deployment checks
- Real interactive Windows desktop session for screen capture and CUA checks
- Browser extension installation and browser automation smoke tests
- End-to-end workflow execution against the intended backend environment

If any of those are unavailable, record the exact blocker in the release note
instead of claiming full functional coverage.

## Useful Paths

| Purpose | Path |
| --- | --- |
| Portable launcher | `frontend\packages\electron-app\dist\win-portable\ShopRPA.cmd` |
| Portable resources | `frontend\packages\electron-app\dist\win-portable\resources` |
| Source Python archive | `resources\python_core.7z` |
| Electron app scripts | `frontend\packages\electron-app\scripts` |
| Web app source | `frontend\packages\web-app\src` |
| System component tests | `engine\components\astronverse-system\tests` |
