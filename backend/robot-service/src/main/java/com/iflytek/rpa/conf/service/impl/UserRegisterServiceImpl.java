package com.iflytek.rpa.conf.service.impl;

import com.iflytek.rpa.conf.dao.UapUserDao;
import com.iflytek.rpa.conf.entity.vo.UserRegisterVo;
import com.iflytek.rpa.conf.service.UserRegisterService;
import com.iflytek.rpa.utils.response.AppResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자회원가입서비스유형
 */
@Service
public class UserRegisterServiceImpl implements UserRegisterService {

    @Autowired
    private UapUserDao uapUserDao;

    //    @Value("${uap.database.name:uap_db}")
    //    private String uapDatabaseName;
    //
    //    @Value("${uap.default-tenant-id:}")
    //    private String defaultTenantId;
    //
    //    @Value("${uap.default-user-secret:}")
    //    private String defaultUserSecret;
    //
    //    @Value("${package.download:}")
    //    private String packageDownloadUrl;
    //
    //    @Value("${uap.default-role-id:}")
    //    private String defaultRoleId;

    /**
     * 비밀번호매칭필드이름
     */
    private static final String DEFAULT_PASSWORD_FIELD_NAME = "user.default.pwd";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<UserRegisterVo> register(String phone) {
        return AppResponse.success(new UserRegisterVo());
        //        try {
        //            // 조회계정여부완료저장에서
        //            String existingUserId = uapUserDao.getUserIdByLoginNameOrPhone(uapDatabaseName, phone, phone);
        //            if (existingUserId != null) {
        //                // 결과가계정완료저장에서, 반환해당사용자의userId
        //                UserRegisterVo registerVo = new UserRegisterVo();
        //                registerVo.setAccount(phone);
        //                registerVo.setUserId(existingUserId);
        //                AppResponse<UserRegisterVo> response = AppResponse.error(ErrorCodeEnum.E_COMMON,
        // "회원가입실패: 계정완료저장에서");
        //                response.setData(registerVo);
        //                return response;
        //            }
        //
        //            // 에서데이터베이스가져오기 비밀번호
        //            String defaultPassword = uapUserDao.getConfigValue(uapDatabaseName, DEFAULT_PASSWORD_FIELD_NAME);
        //            if (defaultPassword == null || defaultPassword.trim().isEmpty()) {
        //                AppResponse<UserRegisterVo> response = AppResponse.error(ErrorCodeEnum.E_COMMON,
        // "회원가입실패: 찾을 수 없는 비밀번호매칭");
        //                return response;
        //            }
        //
        //            // 완료UUID로사용자ID및테넌트사용자닫기시스템ID
        //            String userId = UUID.randomUUID().toString();
        //            String tenantUserId = UUID.randomUUID().toString();
        //            String roleUserId = UUID.randomUUID().toString();
        //
        //            // 삽입사용자테이블(사용암호화후의비밀번호)
        //            int userResult = uapUserDao.insertUser(uapDatabaseName, userId, phone, defaultUserSecret, phone);
        //            if (userResult <= 0) {
        //                AppResponse<UserRegisterVo> response =
        // AppResponse.error(ErrorCodeEnum.E_COMMON,"회원가입실패: 삽입사용자 정보실패");
        //                return response;
        //            }
        //
        //            // 삽입테넌트사용자닫기시스템테이블
        //            int tenantUserResult = uapUserDao.insertTenantUser(uapDatabaseName, tenantUserId, defaultTenantId,
        // userId);
        //            if (tenantUserResult <= 0) {
        //                AppResponse<UserRegisterVo> response =
        // AppResponse.error(ErrorCodeEnum.E_COMMON,"회원가입실패: 삽입테넌트사용자닫기시스템실패");
        //                return response;
        //            }
        //
        //            int roleUserResult = uapUserDao.insertRoleUser(uapDatabaseName, roleUserId, defaultRoleId,
        // defaultTenantId, userId);
        //            if (roleUserResult <= 0) {
        //                AppResponse<UserRegisterVo> response =
        // AppResponse.error(ErrorCodeEnum.E_COMMON,"회원가입실패: 삽입역할사용자닫기시스템실패");
        //                return response;
        //            }
        //
        //            // 반환계정및비밀번호(문서)
        //            UserRegisterVo registerVo = new UserRegisterVo();
        //            registerVo.setAccount(phone);
        //            registerVo.setPassword(defaultPassword);
        //            registerVo.setUserId(userId);
        //            registerVo.setUrl(packageDownloadUrl);
        //
        //            return AppResponse.success(registerVo);
        //        } catch (Exception e) {
        //            AppResponse<UserRegisterVo> response = AppResponse.error(ErrorCodeEnum.E_COMMON,"회원가입실패: " +
        // e.getMessage());
        //            return response;
        //        }
    }
}