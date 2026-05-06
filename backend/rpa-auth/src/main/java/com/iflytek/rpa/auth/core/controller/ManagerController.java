package com.iflytek.rpa.auth.core.controller;

import com.iflytek.rpa.auth.core.entity.UpdateUserPasswordDto;
import com.iflytek.rpa.auth.idp.iflytekIdentity.IflytekAuthenticationServiceImpl;
import com.iflytek.rpa.auth.idp.iflytekIdentity.task.UserSyncTask;
import com.iflytek.rpa.auth.sp.uap.utils.UapManagementClientUtil;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.sec.uap.client.core.client.ManagementClient;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자작업제어기기
 */
@Slf4j
@RestController
@RequestMapping("/manager")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "saas", matchIfMissing = true)
public class ManagerController {

    private final UserSyncTask userSyncTask;
    private final IflytekAuthenticationServiceImpl authenticationService;

    /**
     * 실행사용자작업
     *
     * @param force 여부강함제어실행(), 로false
     * @param loginNames 가능선택, 지정필요의사용자로그인이름목록, 비어 있습니다이면모든기호합치기파일의사용자
     * @return 결과
     */
    @PostMapping("/user-sync/execute")
    public AppResponse<UserSyncTask.SyncResult> executeSync(
            @RequestParam(value = "force", defaultValue = "false") boolean force,
            @RequestParam(value = "loginNames", required = false) List<String> loginNames) {
        try {
            log.info("까지사용자작업요청 , force={}, loginNames={}", force, loginNames);
            UserSyncTask.SyncResult result = userSyncTask.executeSync(force, loginNames);
            return AppResponse.success(result);
        } catch (Exception e) {
            log.error("실행사용자작업예외", e);
            // 반환오류결과
            UserSyncTask.SyncResult errorResult = new UserSyncTask.SyncResult();
            errorResult.setMessage("실행작업실패: " + e.getMessage());
            return AppResponse.success(errorResult);
        }
    }

    /**
     * 관리관리단말업데이트사용자비밀번호
     *
     * @param requestDto 패키지로그인이름, 비밀번호및새비밀번호
     * @return 결과
     */
    @PostMapping("/user/password/update")
    public AppResponse<String> updateUserPassword(@RequestBody @Valid UpdateUserPasswordDto requestDto) {
        try {
            authenticationService.updateUserPassword(
                    requestDto.getLoginName(), requestDto.getOldPassword(), requestDto.getNewPassword());
            return AppResponse.success("업데이트비밀번호성공");
        } catch (Exception e) {
            log.error("관리관리원업데이트사용자비밀번호실패, loginName={}", requestDto.getLoginName(), e);
            return AppResponse.error("업데이트비밀번호실패: " + e.getMessage());
        }
    }

    /**
     * 조회작업상태
     *
     * @return 작업상태
     */
    @GetMapping("/status")
    public AppResponse<String> getStatus() {
        try {
            // 조회여부있음작업정상에서실행
            Object lock = com.iflytek.rpa.auth.utils.RedisUtils.get("auth:user_sync_task:lock");
            if (lock != null) {
                return AppResponse.success("작업정상에서실행중");
            } else {
                return AppResponse.success("작업빈중");
            }
        } catch (Exception e) {
            log.error("조회작업상태예외", e);
            return AppResponse.error("조회작업상태실패: " + e.getMessage());
        }
    }

    /**
     * 지정테넌트사용자까지개사람빈
     *
     * @param request HTTP요청 
     * @param tenantId 테넌트ID()
     * @param loginNames 가능선택, 지정필요의계정목록, 비어 있습니다이면해당테넌트아래모든사용자
     * @return 결과
     */
    @PostMapping("/tenant/migrate")
    public AppResponse<String> migrateTenantUsers(
            HttpServletRequest request,
            @RequestParam(value = "tenantId") String tenantId,
            @RequestParam(value = "loginNames", required = false) List<String> loginNames) {
        try {
            if (StringUtils.isBlank(tenantId)) {
                return AppResponse.error("테넌트 ID는 비워 둘 수 없습니다");
            }
            ManagementClient managementClient = UapManagementClientUtil.getManagementClient(request);
            UserSyncTask.MigrateResult result = userSyncTask.migrateTenantUsers(managementClient, tenantId, loginNames);
            if (StringUtils.isNotBlank(result.getMessage())
                    && result.getMessage().contains("실패")) {
                return AppResponse.error(result.getMessage());
            }
            return AppResponse.success(result.getMessage());
        } catch (Exception e) {
            log.error("테넌트사용자실패", e);
            return AppResponse.error("실패: " + e.getMessage());
        }
    }
}