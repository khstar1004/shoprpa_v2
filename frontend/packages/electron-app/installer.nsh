!macro customInstall
    ; 시도입력 HKCR (필요관리자 권한, 모든사용자)
    ClearErrors
    WriteRegStr HKCR "shoprpa" "" "URL:shoprpa Protocol"
    WriteRegStr HKCR "shoprpa" "URL Protocol" ""
    WriteRegStr HKCR "shoprpa" "FriendlyTypeName" "shoprpa"
    WriteRegStr HKCR "shoprpa\Application" "ApplicationName" "shoprpa"
    WriteRegStr HKCR "shoprpa\shell\open\command" "" '"$INSTDIR\${APP_EXECUTABLE_FILENAME}" -- "%1"'
    
    ; 예결과 HKCR 입력실패(관리관리원), 이면입력 HKCU (현재사용자)
    ${If} ${Errors}
        ClearErrors
        WriteRegStr HKCU "Software\Classes\shoprpa" "" "URL:shoprpa Protocol"
        WriteRegStr HKCU "Software\Classes\shoprpa" "URL Protocol" ""
        WriteRegStr HKCU "Software\Classes\shoprpa" "FriendlyTypeName" "shoprpa"
        WriteRegStr HKCU "Software\Classes\shoprpa\Application" "ApplicationName" "shoprpa"
        WriteRegStr HKCU "Software\Classes\shoprpa\shell\open\command" "" '"$INSTDIR\${APP_EXECUTABLE_FILENAME}" -- "%1"'
    ${EndIf}
!macroend

!macro customUnInstall
    ; 전강함제어닫기클라이언트
    nsExec::Exec 'taskkill /F /IM "${APP_EXECUTABLE_FILENAME}"'
    Pop $R0 ; Pop the exit code to keep the stack clean

    ; 시도에서 HKCR 삭제
    ClearErrors
    DeleteRegKey HKCR "shoprpa"
    
    ; 시에서 HKCU 삭제(예결과저장에서)
    ClearErrors
    DeleteRegKey HKCU "Software\Classes\shoprpa"
!macroend
