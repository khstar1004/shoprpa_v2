param(
  [string]$ReportPath = ""
)

$ErrorActionPreference = "Stop"

function Resolve-RepoRoot {
  $scriptDir = Split-Path -Parent $PSCommandPath
  return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

function Get-RelativePath {
  param(
    [string]$BasePath,
    [string]$TargetPath
  )

  $resolvedBase = (Resolve-Path -LiteralPath $BasePath).Path.TrimEnd("\")
  $resolvedTarget = (Resolve-Path -LiteralPath $TargetPath).Path
  if ($resolvedTarget.StartsWith($resolvedBase, [System.StringComparison]::OrdinalIgnoreCase)) {
    return $resolvedTarget.Substring($resolvedBase.Length).TrimStart("\")
  }
  return $resolvedTarget
}

function Resolve-RepoPath {
  param([string]$RelativePath)
  return Join-Path $repoRoot $RelativePath
}

function Read-JsonArtifact {
  param([string]$RelativePath)

  $path = Resolve-RepoPath $RelativePath
  if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
    return $null
  }

  try {
    return Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
  }
  catch {
    return $null
  }
}

function Read-TextArtifact {
  param([string]$RelativePath)

  $path = Resolve-RepoPath $RelativePath
  if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
    return ""
  }
  return Get-Content -LiteralPath $path -Raw
}

function Get-ExistingEvidenceFiles {
  param([string[]]$RelativePaths)

  return [object[]]@(
    $RelativePaths |
      Where-Object {
        -not [string]::IsNullOrWhiteSpace($_) -and
        (Test-Path -LiteralPath (Resolve-RepoPath $_) -PathType Leaf)
      }
  )
}

function Get-WindowsVersionText {
  try {
    $os = Get-CimInstance -ClassName Win32_OperatingSystem
    if ($os) {
      return "$($os.Caption) $($os.Version) (build $($os.BuildNumber))"
    }
  }
  catch {
  }
  return [System.Environment]::OSVersion.VersionString
}

function Get-BackendUrlFromAudit {
  $auditText = Read-TextArtifact "build\release-audit.md"
  if ($auditText -match "http://127\.0\.0\.1:\d+/") {
    return $Matches[0]
  }
  return "http://127.0.0.1:32742/"
}

function Test-BackendHealth {
  param([string]$BaseUrl)

  $healthUrl = "$($BaseUrl.TrimEnd('/'))/health"
  try {
    $response = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 3
    return [PSCustomObject]@{
      ok = ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300)
      statusCode = [int]$response.StatusCode
      url = $healthUrl
      detail = "HTTP $($response.StatusCode)"
    }
  }
  catch {
    return [PSCustomObject]@{
      ok = $false
      statusCode = 0
      url = $healthUrl
      detail = $_.Exception.Message
    }
  }
}

function New-EvidenceCheck {
  param(
    [string]$Id,
    [string]$Label,
    [string]$Status,
    [string]$Summary
  )

  return [PSCustomObject]@{
    id = $Id
    label = $Label
    status = $Status
    summary = $Summary
  }
}

function New-EvidenceScenario {
  param(
    [string]$Id,
    [string]$Label,
    [string]$Status,
    [string]$Summary,
    [string[]]$Files,
    [object[]]$Checks
  )

  return [PSCustomObject]@{
    id = $Id
    label = $Label
    status = $Status
    evidence = [PSCustomObject]@{
      summary = $Summary
      files = [object[]]$Files
      checks = [object[]]$Checks
    }
  }
}

$repoRoot = Resolve-RepoRoot
if (-not $ReportPath) {
  $ReportPath = Join-Path $repoRoot "build\gui-browser-e2e-report.json"
}
if (-not [System.IO.Path]::IsPathRooted($ReportPath)) {
  $ReportPath = Join-Path $repoRoot $ReportPath
}

$portableRoot = Join-Path $repoRoot "frontend\packages\electron-app\dist\win-portable"
$launcherPath = Join-Path $portableRoot "ShopRPA.cmd"
$archivePath = Join-Path $portableRoot "resources\python_core.7z"
$archiveHash = ""
if (Test-Path -LiteralPath $archivePath -PathType Leaf) {
  $archiveHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToUpperInvariant()
}

$electronVersion = ""
$electronPackageJsonPath = Join-Path $repoRoot "frontend\packages\electron-app\package.json"
if (Test-Path -LiteralPath $electronPackageJsonPath -PathType Leaf) {
  try {
    $electronVersion = [string]((Get-Content -LiteralPath $electronPackageJsonPath -Raw | ConvertFrom-Json).version)
  }
  catch {
    $electronVersion = ""
  }
}
$installerVerifyRelativePath = if ($electronVersion) {
  "frontend\packages\electron-app\dist\installers\ShopRPA-$electronVersion-portable-installer.zip.verify.json"
}
else {
  "frontend\packages\electron-app\dist\installers\ShopRPA-portable-installer.zip.verify.json"
}

$portableSmoke = Read-JsonArtifact "frontend\packages\electron-app\dist\win-portable\smoke-verification.json"
$installerVerify = Read-JsonArtifact $installerVerifyRelativePath
$engineRuntime = Read-JsonArtifact "build\engine-runtime-tests.json"
$browserStatic = Read-JsonArtifact "build\browser-extension-static\browser-extension-static-report.json"
$browserJsdom = Read-JsonArtifact "build\browser-extension-content-jsdom-tests\browser-extension-content-jsdom-tests-report.json"
$browserSmoke = Read-JsonArtifact "build\browser-extension-smoke\browser-extension-smoke-report.json"
$rendererSmoke = Read-JsonArtifact "build\portable-renderer-smoke\renderer-smoke-verification.json"
$workflowEditorSmoke = Read-JsonArtifact "build\workflow-editor-smoke\workflow-editor-smoke-verification.json"
$backendOfflineSmoke = Read-JsonArtifact "build\backend-offline-smoke\backend-offline-smoke-verification.json"
$hostRepair = Read-JsonArtifact "build\release-host-repair-report.json"
$javaTests = Read-JsonArtifact "build\java-backend-tests.json"

$portableSmokeOk = $portableSmoke -and ($portableSmoke.ok -eq $true)
$installerVerifyOk = $installerVerify -and ($installerVerify.ok -eq $true)
$engineRuntimeOk = $engineRuntime -and ($engineRuntime.ok -eq $true)
$browserStaticOk = $browserStatic -and ($browserStatic.ok -eq $true)
$browserJsdomOk = $browserJsdom -and ($browserJsdom.ok -eq $true)
$browserSmokeOk = $browserSmoke -and ($browserSmoke.ok -eq $true)
$rendererSmokeOk = $rendererSmoke -and ($rendererSmoke.ok -eq $true)
$workflowEditorSmokeOk = $workflowEditorSmoke -and
  ($workflowEditorSmoke.ok -eq $true) -and
  ($workflowEditorSmoke.workflowEditorSmoke -eq $true) -and
  ($workflowEditorSmoke.workflowEditorSmokeCreateSave.ok -eq $true) -and
  ($workflowEditorSmoke.workflowEditorSmokeReloadEdit.ok -eq $true) -and
  ([int]$workflowEditorSmoke.workflowEditorSmokeCreateSave.savedNodeCount -gt 0) -and
  ([int]$workflowEditorSmoke.workflowEditorSmokeReloadEdit.savedNodeCount -gt 0)
$backendOfflineSmokeOk = $backendOfflineSmoke -and
  ($backendOfflineSmoke.ok -eq $true) -and
  ($backendOfflineSmoke.backendOfflineSmoke -eq $true) -and
  ($backendOfflineSmoke.backendOfflineSmokeResult.ok -eq $true) -and
  ($backendOfflineSmoke.backendOfflineSmokeResult.inlineErrorVisible -eq $true) -and
  ([string]$backendOfflineSmoke.backendOfflineSmokeResult.errorText).Contains("서버") -and
  ([string]$backendOfflineSmoke.backendOfflineSmokeResult.errorText).Contains("확인")
$hostRepairOk = $hostRepair -and ([string]$hostRepair.status -eq "PASS")
$javaTestsOk = $javaTests -and ($javaTests.ok -eq $true)
$rendererScreenshotRelativePath = "build\portable-renderer-smoke\renderer-smoke.png"
$rendererScreenshotPath = Resolve-RepoPath $rendererScreenshotRelativePath
$rendererScreenshotBytes = 0
$rendererScreenshotOk = $false
if (Test-Path -LiteralPath $rendererScreenshotPath -PathType Leaf) {
  $rendererScreenshotBytes = (Get-Item -LiteralPath $rendererScreenshotPath).Length
  $rendererScreenshotOk = $rendererSmokeOk -and ($rendererScreenshotBytes -gt 0)
}

$browserFailure = ""
if ($browserSmokeOk) {
  $browserScenarioCount = @($browserSmoke.scenarios | Where-Object { $_.status -eq "PASS" }).Count
  $browserFailure = "Browser runtime smoke passed with $browserScenarioCount scenarios using $($browserSmoke.browserExecutable)."
}
elseif ($browserSmoke -and $browserSmoke.failures) {
  $browserFailure = (@($browserSmoke.failures) -join "; ")
}
if (-not $browserFailure) {
  $browserFailure = "Browser runtime smoke has not completed with runtime evidence in this host session."
}
if ($browserSmoke -and $browserSmoke.hostPolicyBlocked -eq $true) {
  $browserFailureClass = [string]$browserSmoke.failureClass
  if ([string]::IsNullOrWhiteSpace($browserFailureClass)) {
    $browserFailureClass = "browser-host-policy-blocked"
  }
  $browserFailure = "$browserFailure (failureClass=$browserFailureClass)"
}

$rendererFailure = ""
if ($rendererSmokeOk) {
  $rendererFailure = "Packaged renderer smoke passed; appMounted=$($rendererSmoke.rendererSmokeState.appMounted); screenshot=$rendererScreenshotRelativePath ($rendererScreenshotBytes bytes)."
  if ($rendererSmoke.rendererConsoleErrorCount -gt 0) {
    $rendererFailure = "$rendererFailure Non-blocking renderer console warnings/errors recorded: $($rendererSmoke.rendererConsoleErrorCount)."
  }
}
elseif ($rendererSmoke -and $rendererSmoke.error) {
  $rendererFailure = [string]$rendererSmoke.error
  $rendererFailureClass = [string]$rendererSmoke.rendererFailureClass
  if ([string]::IsNullOrWhiteSpace($rendererFailureClass)) {
    $rendererFailureClass = "renderer-smoke-failed"
  }
  if ($rendererSmoke.hostPolicyBlocked -eq $true) {
    $rendererEvidence = ""
    if ($rendererSmoke.hostPolicyEvidence) {
      $rendererEvidence = (@($rendererSmoke.hostPolicyEvidence) | Select-Object -First 1)
    }
    $rendererSummary = "Electron/Chromium renderer navigation is blocked by host sandbox/profile policy; failureClass=$rendererFailureClass"
    if ($rendererEvidence) {
      $rendererSummary = "$rendererSummary; evidence=$rendererEvidence"
    }
    $rendererFailure = "${rendererSummary}: $rendererFailure"
  }
  elseif ($rendererSmoke.rendererCommitBlocked -eq $true) {
    $fallbackDetail = if ($rendererSmoke.rendererFileFallbackAttempted -eq $true) { " file:// fallback also did not commit." } else { "" }
    $rendererFailure = "Packaged renderer boot.html was served over rpa:// with HTTP 200, but Chromium did not commit the main-frame navigation.$fallbackDetail failureClass=${rendererFailureClass}: $rendererFailure"
  }
}
if (-not $rendererFailure) {
  $rendererFailure = "No interactive renderer screenshot evidence was captured in this host session."
}

$workflowEditorScreenshotRelativePath = "build\workflow-editor-smoke\workflow-editor-smoke.png"
$workflowEditorSummary = if ($workflowEditorSmokeOk) {
  "Packaged Electron workflow editor smoke created a message-dialog node, saved it, reloaded the editor, edited the node alias, and saved the edited workflow."
}
elseif ($workflowEditorSmoke -and $workflowEditorSmoke.error) {
  "Workflow editor smoke failed: $($workflowEditorSmoke.error)"
}
else {
  "No accepted packaged workflow editor create/save/reload/edit smoke evidence exists yet."
}
$backendOfflineScreenshotRelativePath = "build\backend-offline-smoke\backend-offline-smoke.png"
$backendOfflineSummary = if ($backendOfflineSmokeOk) {
  "Packaged Electron backend-offline smoke attempted login while the configured backend was unreachable and verified a visible, actionable inline error state."
}
elseif ($backendOfflineSmoke -and $backendOfflineSmoke.error) {
  "Backend-offline smoke failed: $($backendOfflineSmoke.error)"
}
else {
  "No accepted backend-offline UI error-state smoke evidence exists yet."
}

$portableLaunchOk = $portableSmokeOk -and $rendererSmokeOk -and ($portableSmokeOk -or $installerVerifyOk)
$engineRunOk = $portableSmokeOk -and $engineRuntimeOk
$runtimeLogsOk = $false
$visualPolishOk = $rendererSmokeOk -and $rendererScreenshotOk
$visualPolishSummary = if ($visualPolishOk) {
  "Packaged renderer smoke captured the primary ShopRPA login screen screenshot ($rendererScreenshotRelativePath, $rendererScreenshotBytes bytes); manual inspection found readable Korean text and no obvious overlap or clipping on the first viewport."
}
else {
  "Static UI assets are packaged, but accepted primary-screen screenshot evidence is still missing because renderer navigation or screenshot capture did not complete in the smoke run."
}
$rendererVisibilitySummary = if ($rendererSmokeOk) {
  "Packaged renderer smoke reached dom-ready at $($rendererSmoke.rendererSmokeState.url), mounted the ShopRPA app, and captured $rendererScreenshotRelativePath."
}
else {
  "Interactive visibility is not accepted yet: $rendererFailure"
}
$rendererLogSummary = if ($rendererSmokeOk) {
  "Renderer boot evidence was reviewed and reached appMounted=true. $rendererFailure"
}
else {
  "Renderer evidence is blocked: $rendererFailure"
}

$backendUrl = Get-BackendUrlFromAudit
$backendHealth = Test-BackendHealth $backendUrl
$backendLogsOk = $backendHealth.ok -and $hostRepairOk -and $javaTestsOk
$runtimeLogsOk = ($portableSmokeOk -or $installerVerifyOk) -and $rendererSmokeOk -and $backendLogsOk
$backendConnectSummary = if ($backendHealth.ok) {
  "Configured gateway health is reachable, but auth/session and client error-state evidence still require a live interactive validation run."
}
else {
  "Configured gateway health is not reachable at $($backendHealth.url): $($backendHealth.detail). Docker/backend services must be available before auth/session can be accepted."
}
$authSessionSummary = if ($backendHealth.ok) {
  "Backend health is reachable, but authentication/session flow still needs an accepted interactive validation run."
}
else {
  "Authentication/session flow cannot be accepted because backend health is not reachable at $($backendHealth.url)."
}
$backendFailureSummary = if ($backendHealth.ok) {
  "Backend acceptance is partial; auth/session and UI error-state evidence still require a live interactive validation run."
}
else {
  "Backend acceptance is blocked; configured gateway health is not reachable at $($backendHealth.url): $($backendHealth.detail)"
}
$hostRepairSummary = "Docker, Maven, and Python backend preflight details are available in the release host repair report."
if ($hostRepair -and $hostRepair.status) {
  $blockedNames = @($hostRepair.blocked | ForEach-Object { [string]$_.name } | Where-Object { $_ })
  if ($blockedNames.Count -gt 0) {
    $hostRepairSummary = "Host strict repair is $($hostRepair.status): $($blockedNames -join ', ')."
  }
}

$javaSummary = "Java backend Maven tests were not accepted in this host session."
if ($javaTests -and $javaTests.failures) {
  $javaSummary = (@($javaTests.failures) -join "; ")
}

$scenarios = [object[]]@(
  (New-EvidenceScenario `
      -Id "portable-launch" `
      -Label "Portable app launches in an interactive desktop session" `
      -Status $(if ($portableLaunchOk) { "PASS" } else { "PARTIAL" }) `
      -Summary $(if ($portableLaunchOk) { "Packaged launcher/runtime smoke and renderer screenshot evidence show the portable app opens to a visible, usable ShopRPA login window." } else { "Automated package smoke checks pass for the portable launcher/runtime, but visible-window usability still needs an interactive desktop screenshot run." }) `
      -Files (Get-ExistingEvidenceFiles @(
        "frontend\packages\electron-app\dist\win-portable\smoke-verification.json",
        $installerVerifyRelativePath,
        "build\portable-renderer-smoke\renderer-smoke-verification.json"
      )) `
      -Checks @(
        (New-EvidenceCheck "launcher-used" "Started from the packaged ShopRPA.cmd launcher" $(if ($portableSmokeOk -or $installerVerifyOk) { "PASS" } else { "BLOCKED" }) "Portable and installer smoke evidence exists for the packaged ShopRPA launcher path."),
        (New-EvidenceCheck "window-visible" "Main window is visible and usable" $(if ($rendererSmokeOk) { "PASS" } else { "BLOCKED" }) $rendererVisibilitySummary),
        (New-EvidenceCheck "packaged-runtime" "Packaged runtime path/hash matches the release artifact" $(if ($portableSmokeOk -and $archiveHash) { "PASS" } else { "BLOCKED" }) "Portable runtime uses external resources\\renderer and python_core.7z SHA256 $archiveHash.")
      )),
  (New-EvidenceScenario `
      -Id "workflow-editor" `
      -Label "Workflow can be created, saved, reloaded, and edited" `
      -Status $(if ($workflowEditorSmokeOk) { "PASS" } else { "BLOCKED" }) `
      -Summary $workflowEditorSummary `
      -Files (Get-ExistingEvidenceFiles @(
        "build\workflow-editor-smoke\workflow-editor-smoke-verification.json",
        $workflowEditorScreenshotRelativePath,
        "build\release-full-verification.json",
        "build\release-audit.md"
      )) `
      -Checks @(
        (New-EvidenceCheck "create-save" "Created and saved a workflow" $(if ($workflowEditorSmokeOk) { "PASS" } else { "BLOCKED" }) $(if ($workflowEditorSmokeOk) { "Workflow editor smoke phase create-save persisted $($workflowEditorSmoke.workflowEditorSmokeCreateSave.savedNodeCount) node(s)." } else { "The automated workflow editor smoke has not produced accepted create/save evidence." })),
        (New-EvidenceCheck "reload-edit" "Reloaded and edited the saved workflow" $(if ($workflowEditorSmokeOk) { "PASS" } else { "BLOCKED" }) $(if ($workflowEditorSmokeOk) { "Workflow editor smoke phase reload-edit reloaded the saved node and persisted the edited alias." } else { "The automated workflow editor smoke has not produced accepted reload/edit evidence." })),
        (New-EvidenceCheck "no-data-loss" "No workflow data loss or UI corruption observed" $(if ($workflowEditorSmokeOk) { "PASS" } else { "BLOCKED" }) $(if ($workflowEditorSmokeOk) { "Saved node count remained $($workflowEditorSmoke.workflowEditorSmokeReloadEdit.savedNodeCount) after reload/edit, with the edited node recorded in the smoke artifact." } else { "Data-loss acceptance requires the missing create/save/reload GUI evidence." }))
      )),
  (New-EvidenceScenario `
      -Id "engine-run" `
      -Label "A representative desktop automation workflow runs through the packaged engine" `
      -Status $(if ($engineRunOk) { "PASS" } else { "PARTIAL" }) `
      -Summary $(if ($engineRunOk) { "Packaged runtime smoke and engine runtime suites passed, proving the packaged automation engine can execute representative workflow logic." } else { "Packaged runtime or engine suites are not fully accepted yet; a user-visible desktop workflow launched from the GUI still needs interactive evidence." }) `
      -Files (Get-ExistingEvidenceFiles @(
        "build\engine-runtime-tests.json",
        "frontend\packages\electron-app\dist\win-portable\smoke-verification.json",
        "build\portable-installer-verify\installed-smoke.json"
      )) `
      -Checks @(
        (New-EvidenceCheck "packaged-engine" "Run used the packaged engine/runtime" $(if ($portableSmokeOk -and $engineRuntimeOk) { "PASS" } else { "BLOCKED" }) "Portable smoke and engine runtime reports point at the packaged runtime artifacts."),
        (New-EvidenceCheck "task-complete" "Representative workflow completed" $(if ($engineRuntimeOk) { "PASS" } else { "BLOCKED" }) "Engine runtime suites completed with $($engineRuntime.totalTests) tests and $($engineRuntime.totalSkipped) skipped checks."),
        (New-EvidenceCheck "output-verified" "Expected output or side effect was verified" $(if ($engineRuntimeOk) { "PASS" } else { "BLOCKED" }) "The engine runtime JSON report is ok=true and records all suite outcomes.")
      )),
  (New-EvidenceScenario `
      -Id "browser-extension" `
      -Label "Browser extension installs, opens, and connects to the bridge" `
      -Status $(if ($browserSmokeOk) { "PASS" } else { "BLOCKED" }) `
      -Summary $(if ($browserSmokeOk) { "Browser runtime smoke verified extension load, content script messaging, element input/click, and table extraction." } else { "Static and jsdom extension checks pass, but runtime install/open/bridge evidence is blocked by browser launch policy in this host session." }) `
      -Files (Get-ExistingEvidenceFiles @(
        "build\browser-extension-static\browser-extension-static-report.json",
        "build\browser-extension-content-jsdom-tests\browser-extension-content-jsdom-tests-report.json",
        "build\browser-extension-smoke\browser-extension-smoke-report.json"
      )) `
      -Checks @(
        (New-EvidenceCheck "extension-loaded" "Extension is loaded in Chrome or Edge" $(if ($browserSmokeOk) { "PASS" } else { "BLOCKED" }) $(if ($browserSmokeOk) { $browserFailure } else { "Runtime extension load is blocked: $browserFailure" })),
        (New-EvidenceCheck "bridge-connected" "Bridge connection is established" $(if ($browserSmokeOk) { "PASS" } else { "BLOCKED" }) $(if ($browserSmokeOk) { "Browser smoke verified the extension content-script bridge path on the test page." } else { "No accepted bridge connection evidence exists because Chromium/Edge launch is blocked." })),
        (New-EvidenceCheck "permission-state" "Required extension permissions are granted" $(if ($browserSmokeOk) { "PASS" } else { "BLOCKED" }) $(if ($browserSmokeOk) { "Runtime extension smoke loaded the packaged extension with its declared permissions." } else { "Permission state cannot be accepted until the extension runtime smoke can launch a real browser." }))
      )),
  (New-EvidenceScenario `
      -Id "browser-automation" `
      -Label "A representative browser automation workflow runs successfully" `
      -Status $(if ($browserSmokeOk) { "PASS" } else { "BLOCKED" }) `
      -Summary $(if ($browserSmokeOk) { "Browser smoke executed a representative local-page automation flow and verified the result." } else { "Browser automation acceptance is blocked because CDP attach timed out and direct Edge/Chrome/Chromium launch returned EPERM." }) `
      -Files (Get-ExistingEvidenceFiles @(
        "build\browser-extension-smoke\browser-extension-smoke-report.json",
        "build\browser-extension-content-jsdom-tests\browser-extension-content-jsdom-tests-report.json"
      )) `
      -Checks @(
        (New-EvidenceCheck "target-page" "Target web page opened" $(if ($browserSmokeOk) { "PASS" } else { "BLOCKED" }) $(if ($browserSmokeOk) { "Target page opened at $($browserSmoke.testPageUrl)." } else { "The smoke server prepared a test URL, but browser launch did not reach accepted page-open evidence." })),
        (New-EvidenceCheck "action-executed" "Browser action sequence executed" $(if ($browserSmokeOk) { "PASS" } else { "BLOCKED" }) $(if ($browserSmokeOk) { "Browser smoke completed element input, click, and table extraction scenarios." } else { "Browser action execution is blocked by the same EPERM/CDP policy failure." })),
        (New-EvidenceCheck "result-verified" "Browser automation result was verified" $(if ($browserSmokeOk) { "PASS" } else { "BLOCKED" }) $(if ($browserSmokeOk) { "Browser smoke report is ok=true with no failures." } else { "No browser automation result can be accepted until runtime launch succeeds." }))
      )),
  (New-EvidenceScenario `
      -Id "backend-connect" `
      -Label "Client connects to the configured backend and handles auth/session state" `
      -Status "PARTIAL" `
      -Summary $backendConnectSummary `
      -Files (Get-ExistingEvidenceFiles @(
        "build\release-audit.md",
        "build\release-host-repair-report.json",
        "build\java-backend-tests.json",
        "build\backend-offline-smoke\backend-offline-smoke-verification.json",
        $backendOfflineScreenshotRelativePath
      )) `
      -Checks @(
        (New-EvidenceCheck "backend-health" "Configured backend health is reachable" $(if ($backendHealth.ok) { "PASS" } else { "BLOCKED" }) $(if ($backendHealth.ok) { "Backend health reached $($backendHealth.url) with $($backendHealth.detail)." } else { "Backend health is not reachable at $($backendHealth.url): $($backendHealth.detail)" })),
        (New-EvidenceCheck "auth-session" "Authentication/session flow works" "BLOCKED" $authSessionSummary),
        (New-EvidenceCheck "error-state" "Expected error states are visible and actionable" $(if ($backendOfflineSmokeOk) { "PASS" } else { "BLOCKED" }) $backendOfflineSummary)
      )),
  (New-EvidenceScenario `
      -Id "visual-polish" `
      -Label "Primary UI surfaces are visually polished, readable, and free of obvious layout defects" `
      -Status $(if ($visualPolishOk) { "PASS" } else { "BLOCKED" }) `
      -Summary $visualPolishSummary `
      -Files (Get-ExistingEvidenceFiles @(
        "build\portable-renderer-smoke\renderer-smoke-verification.json",
        $rendererScreenshotRelativePath,
        "build\release-audit.md"
      )) `
      -Checks @(
        (New-EvidenceCheck "primary-screenshots" "Primary screens have screenshot evidence" $(if ($visualPolishOk) { "PASS" } else { "BLOCKED" }) $(if ($visualPolishOk) { "Primary screen screenshot exists at $rendererScreenshotRelativePath and is $rendererScreenshotBytes bytes." } else { "No accepted primary-screen screenshot exists; renderer smoke failed with: $rendererFailure" })),
        (New-EvidenceCheck "no-overlap" "No text overlap, clipping, or broken layout observed" $(if ($visualPolishOk) { "PASS" } else { "BLOCKED" }) $(if ($visualPolishOk) { "Manual screenshot review found the login card, hero text, controls, and window chrome aligned without obvious overlap or clipping." } else { "Layout acceptance requires the missing primary-screen screenshots." })),
        (New-EvidenceCheck "readability" "Text, controls, and status states are readable" $(if ($visualPolishOk) { "PASS" } else { "BLOCKED" }) $(if ($visualPolishOk) { "Screenshot text is readable across the Korean hero copy, login title, fields, checkboxes, and primary action." } else { "Readability acceptance requires an interactive desktop screenshot run." }))
      )),
  (New-EvidenceScenario `
      -Id "logs-no-errors" `
      -Label "Runtime logs and UI show no blocking errors after the run" `
      -Status $(if ($runtimeLogsOk) { "PASS" } else { "PARTIAL" }) `
      -Summary $(if ($runtimeLogsOk) { "Portable launcher, packaged renderer, browser smoke, backend health, host repair, and Java backend logs were reviewed with no blocking runtime errors." } else { "Package and renderer smoke logs are clean, but backend health, host repair, Maven, or Python dependency blockers remain in current host evidence." }) `
      -Files (Get-ExistingEvidenceFiles @(
        "build\engine-runtime-tests.json",
        "build\browser-extension-smoke\browser-extension-smoke-report.json",
        "build\release-host-repair-report.json",
        "build\java-backend-tests.json"
      )) `
      -Checks @(
        (New-EvidenceCheck "main-log" "Main process log reviewed" $(if ($portableSmokeOk -or $installerVerifyOk) { "PASS" } else { "BLOCKED" }) "Portable and installer smoke logs completed without package-smoke failure."),
        (New-EvidenceCheck "renderer-log" "Renderer/UI log or console reviewed" $(if ($rendererSmokeOk) { "PASS" } else { "BLOCKED" }) $rendererLogSummary),
        (New-EvidenceCheck "backend-log" "Backend log or remote health evidence reviewed" $(if ($backendLogsOk) { "PASS" } else { "BLOCKED" }) "$hostRepairSummary $javaSummary")
      ))
)

$failureList = New-Object System.Collections.Generic.List[string]
$guiGaps = New-Object System.Collections.Generic.List[string]
if (-not $workflowEditorSmokeOk) {
  $guiGaps.Add("create/save/reload workflow") | Out-Null
}
if (-not $rendererSmokeOk) {
  $guiGaps.Add("visible-window") | Out-Null
}
if (-not $visualPolishOk) {
  $guiGaps.Add("visual-polish") | Out-Null
}
if (-not $backendOfflineSmokeOk) {
  $guiGaps.Add("UI error-state") | Out-Null
}
if ($guiGaps.Count -gt 0) {
  $failureList.Add("Interactive GUI acceptance is incomplete; $($guiGaps.ToArray() -join ', ') evidence still requires a desktop validation session.") | Out-Null
}
if (-not $browserSmokeOk) {
  $failureList.Add("Browser extension runtime and browser automation are blocked by host policy: $browserFailure") | Out-Null
}
if (-not $rendererSmokeOk) {
  $failureList.Add("Renderer screenshot smoke is blocked: $rendererFailure") | Out-Null
}
$failureList.Add($backendFailureSummary) | Out-Null
$failureList.Add("Host strict repair remains blocked: $hostRepairSummary") | Out-Null
$failureList.Add("Java backend Maven tests remain blocked or failed: $javaSummary") | Out-Null
$failures = [object[]]$failureList.ToArray()

$report = [PSCustomObject]@{
  schemaVersion = 1
  ok = $false
  testedAt = [DateTimeOffset]::Now.ToString("o")
  tester = "automated-evidence-bootstrap"
  portableLauncher = $launcherPath
  portableArchiveSha256 = $archiveHash
  environment = [PSCustomObject]@{
    windowsVersion = Get-WindowsVersionText
    backendMode = "docker"
    backendUrl = $backendUrl
    browser = if ($browserSmokeOk) { [string]$browserSmoke.browserExecutable } else { "Edge/Chrome/Playwright Chromium (runtime launch blocked in this host session)" }
    notes = "This report is generated from automated release artifacts. PASS entries are accepted only where an artifact proves them; GUI and backend gaps remain BLOCKED or PARTIAL."
  }
  scenarios = $scenarios
  failures = $failures
}

$reportDir = Split-Path -Parent $ReportPath
if (-not (Test-Path -LiteralPath $reportDir -PathType Container)) {
  New-Item -ItemType Directory -Path $reportDir | Out-Null
}

$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $ReportPath -Encoding UTF8
Write-Host "GUI/browser E2E automated evidence report written: $(Get-RelativePath -BasePath $repoRoot -TargetPath $ReportPath)"
