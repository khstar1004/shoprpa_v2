package com.iflytek.rpa.auth.idp.iflytekIdentity.task;

import cn.hutool.core.collection.CollectionUtil;
import com.iflytek.rpa.auth.idp.iflytekIdentity.IflytekAuthenticationServiceImpl;
import com.iflytek.rpa.auth.idp.iflytekIdentity.dto.IflytekSyncUserInfoAccount;
import com.iflytek.rpa.auth.idp.iflytekIdentity.dto.IflytekSyncUserInfoUserInfo;
import com.iflytek.rpa.auth.sp.uap.dao.RoleDao;
import com.iflytek.rpa.auth.sp.uap.dao.TenantDao;
import com.iflytek.rpa.auth.sp.uap.dao.UserDao;
import com.iflytek.rpa.auth.sp.uap.entity.SyncUserInfo;
import com.iflytek.rpa.auth.sp.uap.service.impl.UserServiceImpl;
import com.iflytek.rpa.auth.utils.RedisUtils;
import com.iflytek.sec.uap.client.api.ClientManagementAPI;
import com.iflytek.sec.uap.client.core.client.ManagementClient;
import com.iflytek.sec.uap.client.core.dto.PageDto;
import com.iflytek.sec.uap.client.core.dto.tenant.ListTenantDto;
import com.iflytek.sec.uap.client.core.dto.tenant.UapTenant;
import com.iflytek.sec.uap.client.core.dto.user.BindRoleDto;
import com.iflytek.sec.uap.client.core.dto.user.ListUserDto;
import com.iflytek.sec.uap.client.core.dto.user.UapUser;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * 사용자작업서비스
 * 를있음데이터베이스중의사람요소까지ShopRPA 계정
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "saas", matchIfMissing = true)
@RequiredArgsConstructor
public class UserSyncTask {

    private final UserDao userDao;
    private final IflytekAuthenticationServiceImpl authenticationService;
    private final RoleDao roleDao;
    private final TenantDao tenantDao;
    private final UserServiceImpl userService;

    @Value("${uap.database.name:uap_db}")
    private String databaseName;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private Environment environment;

    // 작업실행의Redis key(사용중지발송실행)
    private static final String SYNC_TASK_LOCK_KEY = "auth:user_sync_task:lock";
    // 의경과시간(초), 로1시간
    private static final int LOCK_EXPIRE_TIME = 3600;

    /**
     * 실패정보
     */
    @Data
    public static class SyncFailureInfo {
        private String loginName;
        private String phone;
        private String reason;

        public SyncFailureInfo(String loginName, String phone, String reason) {
            this.loginName = loginName;
            this.phone = phone;
            this.reason = reason;
        }
    }

    /**
     * 결과시스템계획
     */
    @Data
    public static class SyncResult {
        private int totalCount;
        private int successCount;
        private int failCount;
        private int skipCount;
        private String message;
        private List<SyncFailureInfo> failureList = new ArrayList<>();
    }

    /**
     * 실행사용자작업
     *
     * @param force 여부강함제어실행()
     * @param loginNames 가능선택, 지정필요의사용자로그인이름목록, 비어 있습니다이면모든기호합치기파일의사용자
     * @return 결과
     */
    public SyncResult executeSync(boolean force, List<String> loginNames) {
        SyncResult result = new SyncResult();

        // 조회여부있음작업정상에서실행(중지발송)
        if (!force) {
            Object lock = RedisUtils.get(SYNC_TASK_LOCK_KEY);
            if (lock != null) {
                result.setMessage("작업정상에서실행중, 요청 후시도");
                log.warn("사용자작업정상에서실행중, 건너뛰기본요청 ");
                return result;
            }
        }

        // 분방식
        try {
            RedisUtils.set(SYNC_TASK_LOCK_KEY, "1", LOCK_EXPIRE_TIME);

            log.info("==================== 사용자 동기화 작업 시작 ====================");
            if (CollectionUtil.isNotEmpty(loginNames)) {
                log.info("지정된 사용자 목록 수: {}", loginNames.size());
            }

            // 동기화가 필요한 사용자를 조회합니다.
            List<SyncUserInfo> usersToSync = userDao.queryUsersToSync(databaseName, loginNames);
            result.setTotalCount(usersToSync.size());
            log.info("동기화 대상 사용자 수: {}", usersToSync.size());

            if (usersToSync.isEmpty()) {
                result.setMessage("동기화할 사용자가 없습니다");
                log.info("동기화할 사용자가 없습니다");
                return result;
            }

            int successCount = 0;
            int failCount = 0;
            int skipCount = 0;
            int totalCount = usersToSync.size();
            int processedCount = 0;

            for (SyncUserInfo user : usersToSync) {
                processedCount++;
                // 10명마다 또는 완료 시 진행 상황을 출력합니다.
                if (processedCount % 10 == 0 || processedCount == totalCount) {
                    log.info(
                            "사용자정도: {}/{} ({}%), 성공: {}, 실패: {}, 건너뛰기: {}",
                            processedCount,
                            totalCount,
                            String.format("%.1f", (processedCount * 100.0 / totalCount)),
                            successCount,
                            failCount,
                            skipCount);
                }

                try {
                    // 완료일의userid: RPA + 시간(까지초)+ 기기데이터확인일
                    String userid = generateUserId();

                    // 생성로그인계정정보
                    IflytekSyncUserInfoAccount account = new IflytekSyncUserInfoAccount(user.getPhone(), "86", 1);
                    List<IflytekSyncUserInfoAccount> loginAccounts = Arrays.asList(account);

                    // 생성사용자정보
                    IflytekSyncUserInfoUserInfo userInfo = new IflytekSyncUserInfoUserInfo(
                            user.getName() != null ? user.getName() : "",
                            "", // headpic비어 있습니다
                            "", // sign비어 있습니다
                            "0", // sex지정로0
                            user.getAddress() != null ? user.getAddress() : "",
                            null // extras비어 있습니다
                            );

                    // 호출연결
                    authenticationService.syncUserInfo(userid, "", loginAccounts, userInfo);

                    // 완료후, 업데이트third_ext_info필드
                    userDao.updateThirdExtInfo(user.getLoginName(), userid, databaseName);
                    // 를 ext_info 로완료
                    userDao.updateExtInfo(user.getPhone(), "1", databaseName);

                    successCount++;
                    log.info("사용자완료, 로그인이름: {}, 휴대폰 번호: {}, userid: {}", user.getLoginName(), user.getPhone(), userid);

                    // 요청 경과, 추가짧음지연
                    Thread.sleep(100);

                } catch (Exception e) {
                    String errorMessage = e.getMessage() != null
                            ? e.getMessage()
                            : e.getClass().getSimpleName();

                    // 기록실패계정정보
                    result.getFailureList()
                            .add(new SyncFailureInfo(user.getLoginName(), user.getPhone(), errorMessage));
                    // 결과가예사용자완료저장에서또는로그인방식재복사, 건너뛰기해당사용자
                    if (e.getMessage() != null
                            && (e.getMessage().contains("사용자완료저장에서")
                                    || e.getMessage().contains("로그인방식재복사"))) {
                        skipCount++;
                        log.warn("사용자건너뛰기, 로그인이름: {}, 휴대폰 번호: {}, 원인: {}", user.getLoginName(), user.getPhone(), errorMessage);
                    } else {
                        failCount++;

                        log.error("사용자실패, 로그인이름: {}, 휴대폰 번호: {}, 원인: {}", user.getLoginName(), user.getPhone(), errorMessage, e);
                    }
                }
            }

            result.setSuccessCount(successCount);
            result.setFailCount(failCount);
            result.setSkipCount(skipCount);

            // 생성반환메시지
            String messageBuilder = String.format(
                    "완료: 성공 %d 개, 실패 %d 개, 건너뛰기 %d 개, 계획 %d 개", successCount, failCount, skipCount, usersToSync.size());

            result.setMessage(messageBuilder);

            log.info("==================== 사용자작업실행완료 ====================");
            log.info(result.getMessage());

        } catch (Exception e) {
            log.error("사용자작업실행예외", e);
            result.setMessage("작업실행예외: " + e.getMessage());
        } finally {
            // 
            RedisUtils.del(SYNC_TASK_LOCK_KEY);
        }

        return result;
    }

    /**
     * 완료일의userid: RPA + 시간(까지초)
     */
    private String generateUserId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String timestamp = sdf.format(new Date());
        // 추가기기데이터확인일
        String random = String.format("%03d", new Random().nextInt(1000));
        return "RPA" + timestamp + random;
    }

    /**
     * 결과
     */
    @Data
    public static class MigrateResult {
        private int successCount;
        private int failCount;
        private List<String> failedUsers = new ArrayList<>();
        private String message;
    }

    /**
     * 지정테넌트사용자까지개사람빈
     *
     * @param managementClient 관리관리클라이언트
     * @param tenantId 테넌트ID
     * @param loginNames 가능선택, 지정필요의계정목록, 비어 있습니다이면해당테넌트아래모든사용자
     * @return 결과
     */
    public MigrateResult migrateTenantUsers(
            ManagementClient managementClient, String tenantId, List<String> loginNames) {
        MigrateResult result = new MigrateResult();
        try {
            if (StringUtils.isBlank(tenantId)) {
                result.setMessage("테넌트 ID는 비워 둘 수 없습니다");
                return result;
            }

            log.info("열기 실행테넌트사용자, 테넌트ID: {}, 지정계정목록: {}", tenantId, loginNames);

            List<UapUser> users = fetchAllTenantUsers(tenantId);

            // 결과가지정완료계정목록, 이면필터링
            if (CollectionUtil.isNotEmpty(loginNames)) {
                users = users.stream()
                        .filter(user -> user != null && loginNames.contains(user.getLoginName()))
                        .collect(Collectors.toList());
            }

            if (CollectionUtil.isEmpty(users)) {
                result.setMessage("찾을 수 없는 필요의사용자");
                return result;
            }

            List<String> userIds = users.stream()
                    .map(UapUser::getId)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (CollectionUtil.isEmpty(userIds)) {
                result.setMessage("가져올 수 없는 있음의사용자ID");
                return result;
            }

            // 를사용자로사용자
            userDao.batchUpdateUserType(userIds, 3, databaseName);

            // 결과가name비어 있습니다, 이면업데이트로login_name
            userDao.batchUpdateNameFromLoginName(userIds, databaseName);

            // 가져오기회원가입역할ID
            String registerRoleId = roleDao.getRoleIdByName(databaseName, "회원가입역할");
            if (StringUtils.isBlank(registerRoleId)) {
                log.warn("찾을 수 없는 [회원가입역할], 사용역할ID: 1");
                registerRoleId = "1";
            }

            int successCount = 0;
            List<String> failedUsers = new ArrayList<>();
            int totalCount = users.size();
            int processedCount = 0;

            log.info("열기 사용자, 사용자데이터: {}", totalCount);

            for (UapUser user : users) {
                processedCount++;
                // 매관리10개사용자또는관리완료시인쇄정도
                if (processedCount % 10 == 0 || processedCount == totalCount) {
                    log.info(
                            "사용자정도: {}/{} ({}%), 성공: {}, 실패: {}",
                            processedCount,
                            totalCount,
                            String.format("%.1f", (processedCount * 100.0 / totalCount)),
                            successCount,
                            failedUsers.size());
                }

                if (user == null || StringUtils.isAnyBlank(user.getId(), user.getLoginName())) {
                    continue;
                }
                try {
                    unbindRegisterTenantByDb(tenantId, user.getId());
                    String personalTenantId = userService.createPersonalTenantAndBindRpa(
                            user.getId(), user.getLoginName(), managementClient);

                    // 확인 t_uap_tenant_role 테이블중있음기록
                    ensureTenantRoleRelation(personalTenantId, registerRoleId);

                    // 테넌트지정사용자, 역할
                    BindRoleDto bindRoleDto = new BindRoleDto();
                    bindRoleDto.setRoleIdList(Collections.singletonList(personalTenantId));
                    bindRoleDto.setUserId(user.getId());
                    ClientManagementAPI.bindUserRole(personalTenantId, bindRoleDto);

                    // 확인데이터베이스테이블중있음닫기 기록
                    ensureTenantAndUserRoleRelations(personalTenantId, user.getId(), registerRoleId);

                    // 새로고침서비스데이터: 업데이트닫기테이블의 tenant_id
                    refreshBusinessData(tenantId, personalTenantId, user.getId());
                    successCount++;
                } catch (Exception ex) {
                    log.error("사용자실패, userId={}, loginName={}", user.getId(), user.getLoginName(), ex);
                    failedUsers.add(user.getLoginName());
                }
            }

            result.setSuccessCount(successCount);
            result.setFailCount(failedUsers.size());
            result.setFailedUsers(failedUsers);

            if (CollectionUtil.isNotEmpty(failedUsers)) {
                result.setMessage(String.format(
                        "완료, 성공%d개, 실패%d개: %s", successCount, failedUsers.size(), String.join(",", failedUsers)));
            } else {
                result.setMessage(String.format("완료, 성공%d개사용자", successCount));
            }

            log.info("회원가입계정테넌트완료: {}", result.getMessage());
            return result;
        } catch (Exception e) {
            log.error("회원가입계정테넌트실패", e);
            result.setMessage("실패: " + e.getMessage());
            return result;
        }
    }

    private String findRegisterTenantId() {
        ListTenantDto listTenantDto = new ListTenantDto();
        listTenantDto.setName("회원가입테넌트");
        listTenantDto.setPageNum(1);
        listTenantDto.setPageSize(20);
        PageDto<UapTenant> tenantPage = ClientManagementAPI.queryTenantPageList(listTenantDto);
        if (tenantPage == null || CollectionUtil.isEmpty(tenantPage.getResult())) {
            log.warn("미완료에서UAP중까지이름로[회원가입테넌트]의테넌트");
            return null;
        }
        return tenantPage.getResult().stream()
                .filter(tenant -> tenant != null && "회원가입테넌트".equals(tenant.getName()))
                .map(UapTenant::getId)
                .findFirst()
                .orElse(null);
    }

    private List<UapUser> fetchAllTenantUsers(String tenantId) {
        List<UapUser> result = new ArrayList<>();
        int pageNum = 1;
        int pageSize = 100;
        long totalCount = Long.MAX_VALUE;
        while ((long) (pageNum - 1) * pageSize < totalCount) {
            ListUserDto listUserDto = new ListUserDto();
            listUserDto.setPageNum(pageNum);
            listUserDto.setPageSize(pageSize);
            PageDto<UapUser> userPage = ClientManagementAPI.queryUserPageList(tenantId, listUserDto);
            if (userPage == null || CollectionUtil.isEmpty(userPage.getResult())) {
                break;
            }
            result.addAll(userPage.getResult());
            totalCount = userPage.getTotalCount();
            pageNum++;
        }
        return result;
    }

    /**
     * 확인테넌트역할닫기 및사용자역할닫기 에서데이터베이스중저장에서
     * @param tenantId 테넌트ID
     * @param userId 사용자ID
     * @param roleId 역할ID
     */
    private void ensureTenantAndUserRoleRelations(String tenantId, String userId, String roleId) {

        // 확인 t_uap_user_role 테이블중있음기록
        ensureUserRoleRelation(tenantId, userId, roleId);
    }

    /**
     * 확인테넌트역할닫기 저장에서, 찾을 수 없습니다이면삽입
     * @param tenantId 테넌트ID
     * @param roleId 역할ID
     */
    private void ensureTenantRoleRelation(String tenantId, String roleId) {
        Integer existsCount = roleDao.checkTenantRoleExists(databaseName, tenantId, roleId);
        if (existsCount == null || existsCount == 0) {
            roleDao.insertTenantRole(databaseName, tenantId, roleId);
            log.info("완료삽입테넌트역할닫기 , 테넌트ID: {}, 역할ID: {}", tenantId, roleId);
        } else {
            log.debug("테넌트역할닫기 완료저장에서, 테넌트ID: {}, 역할ID: {}", tenantId, roleId);
        }
    }

    /**
     * 확인사용자역할닫기 저장에서
     * 데이터베이스있음 user_id 및 role_id 의일: 
     * - 결과가저장에서 user_id 및 role_id 의기록, 이면업데이트 tenant_id
     * - 결과가찾을 수 없습니다, 이면삽입새기록
     * @param tenantId 테넌트ID
     * @param userId 사용자ID
     * @param roleId 역할ID
     */
    private void ensureUserRoleRelation(String tenantId, String userId, String roleId) {
        // 조회여부저장에서 user_id 및 role_id 의기록(아니요 tenant_id)
        Integer existsCount = roleDao.checkUserRoleExistsByUserAndRole(databaseName, userId, roleId);
        if (existsCount != null && existsCount > 0) {
            // 결과가저장에서, 업데이트 tenant_id
            int updated = roleDao.updateUserRoleTenant(databaseName, userId, roleId, tenantId);
            if (updated > 0) {
                log.info("완료업데이트사용자역할닫기 의테넌트ID, 테넌트ID: {}, 사용자ID: {}, 역할ID: {}", tenantId, userId, roleId);
            } else {
                log.warn("업데이트사용자역할닫기 의테넌트ID실패, 테넌트ID: {}, 사용자ID: {}, 역할ID: {}", tenantId, userId, roleId);
            }
        } else {
            // 결과가찾을 수 없습니다, 삽입새기록
            roleDao.insertUserRole(databaseName, tenantId, userId, roleId);
            log.info("완료삽입사용자역할닫기 , 테넌트ID: {}, 사용자ID: {}, 역할ID: {}", tenantId, userId, roleId);
        }
    }

    private void unbindRegisterTenantByDb(String tenantId, String userId) {
        Integer affected = tenantDao.deleteTenantUser(databaseName, tenantId, userId);
        if (affected == null || affected == 0) {
            throw new RuntimeException("해제회원가입테넌트실패: 찾을 수 없는 테넌트사용자기록");
        }
    }

    /**
     * 새로고침서비스데이터: 업데이트닫기테이블의 tenant_id
     * @param oldTenantId 테넌트ID(회원가입테넌트)
     * @param newTenantId 새테넌트ID(개사람테넌트)
     * @param userId 사용자ID
     */
    public void refreshBusinessData(String oldTenantId, String newTenantId, String userId) {
        try {
            // 가져오기 서비스데이터베이스이름(사용매칭, 에서DataSource URL파싱, 후에서JDBC연결가져오기)
            String dbName = getBusinessDatabaseName();
            if (StringUtils.isBlank(dbName)) {
                log.warn("불가가져오기 서비스데이터베이스이름, 건너뛰기서비스데이터새로고침");
                return;
            }

            // 조회시패키지 tenant_id 및 creator_id 필드의테이블(정렬제거 t_uap 열기 의테이블)
            List<String> tables = tenantDao.getTablesWithTenantId(dbName);
            if (CollectionUtil.isEmpty(tables)) {
                log.info("찾을 수 없는 시패키지 tenant_id 및 creator_id 필드의서비스테이블");
                return;
            }

            log.info("열기 새로고침서비스데이터, 데이터베이스: {}, 테이블수: {}", dbName, tables.size());
            int totalUpdated = 0;

            // 관리 robot_execute_record 테이블: 조회id, 근거id업데이트(대수테이블의업데이트가능)
            String robotExecuteRecordTable = "robot_execute_record";
            boolean isRobotExecuteRecordProcessed = false;
            if (tables.contains(robotExecuteRecordTable)) {
                try {
                    // 조회기호합치기파일의기록ID
                    List<Long> recordIds = tenantDao.queryRobotExecuteRecordIds(dbName, oldTenantId, userId);
                    if (CollectionUtil.isNotEmpty(recordIds)) {
                        log.debug("테이블 {} 조회까지 {} 기호합치기파일의기록, 열기 량업데이트", robotExecuteRecordTable, recordIds.size());
                        // 근거ID목록 량업데이트
                        Integer updated =
                                tenantDao.updateRobotExecuteRecordTenantIdByIds(dbName, newTenantId, recordIds);
                        if (updated != null && updated > 0) {
                            totalUpdated += updated;
                            log.info("테이블 {} 업데이트완료 {} 기록(통신경과ID량업데이트)", robotExecuteRecordTable, updated);
                        }
                    }
                    isRobotExecuteRecordProcessed = true;
                } catch (Exception e) {
                    log.warn("업데이트테이블 {} 의 tenant_id 실패: {}", robotExecuteRecordTable, e.getMessage());
                    isRobotExecuteRecordProcessed = true;
                }
            }

            // 관리테이블
            for (String tableName : tables) {
                // 건너뛰기완료관리의 robot_execute_record 테이블
                if (robotExecuteRecordTable.equals(tableName) && isRobotExecuteRecordProcessed) {
                    continue;
                }
                try {
                    Integer updated =
                            tenantDao.updateTableTenantId(dbName, tableName, oldTenantId, newTenantId, userId);
                    if (updated != null && updated > 0) {
                        totalUpdated += updated;
                        log.info("테이블 {} 업데이트완료 {} 기록", tableName, updated);
                    }
                } catch (Exception e) {
                    // 테이블가능있음 creator_id 필드, 기록경고아니요중프로세스
                    log.warn("업데이트테이블 {} 의 tenant_id 실패: {}", tableName, e.getMessage());
                }
            }
            log.info("서비스데이터새로고침완료, 공유업데이트 {} 기록", totalUpdated);
        } catch (Exception e) {
            log.error("새로고침서비스데이터실패", e);
            // 서비스데이터새로고침실패아니요프로세스, 기록로그
        }
    }

    /**
     * 가져오기 서비스데이터베이스이름
     * 단계: 
     *         2. 에서 DataSource URL 중파싱
     *         3. 에서 JDBC 연결중가져오기현재데이터베이스이름
     * @return 서비스데이터베이스이름
     */
    private String getBusinessDatabaseName() {

        // 방식2: 에서 DataSource URL 중파싱데이터베이스이름
        String dbName = extractDatabaseNameFromUrl();
        if (StringUtils.isNotBlank(dbName)) {
            log.info("에서 DataSource URL 파싱출력서비스데이터베이스이름: {}", dbName);
            return dbName;
        }

        // 방식3: 에서 JDBC 연결중가져오기현재데이터베이스이름
        dbName = getDatabaseNameFromConnection();
        if (StringUtils.isNotBlank(dbName)) {
            log.info("에서 JDBC 연결가져오기 서비스데이터베이스이름: {}", dbName);
            return dbName;
        }

        return null;
    }

    /**
     * 에서 DataSource URL 중파싱데이터베이스이름
     * 지원형식: jdbc:mysql://host:port/database?params
     */
    private String extractDatabaseNameFromUrl() {
        try {
            String url = null;

            // 시도에서 Environment 중가져오기
            if (environment != null) {
                url = environment.getProperty("spring.datasource.url");
            }

            // 결과가 Environment 중있음, 시도에서 DataSource 가져오기
            if (StringUtils.isBlank(url) && dataSource != null) {
                //  DruidDataSource, 가능으로통신경과 getUrl() 방법법가져오기
                try {
                    java.lang.reflect.Method getUrlMethod =
                            dataSource.getClass().getMethod("getUrl");
                    url = (String) getUrlMethod.invoke(dataSource);
                } catch (Exception e) {
                    // 결과가아니요예 DruidDataSource 또는방법법찾을 수 없습니다, 
                }
            }

            if (StringUtils.isBlank(url)) {
                return null;
            }

            // 파싱 MySQL JDBC URL: jdbc:mysql://host:port/database?params
            // 또는: jdbc:mysql://host:port/database
            Pattern pattern = Pattern.compile("jdbc:mysql://[^/]+/([^?]+)");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.warn("에서 DataSource URL 파싱데이터베이스이름실패", e);
        }
        return null;
    }

    /**
     * 에서 JDBC 연결중가져오기현재데이터베이스이름
     */
    private String getDatabaseNameFromConnection() {
        if (dataSource == null) {
            return null;
        }

        try (Connection connection = dataSource.getConnection()) {
            // MySQL: SELECT DATABASE()
            try (java.sql.Statement stmt = connection.createStatement();
                    java.sql.ResultSet rs = stmt.executeQuery("SELECT DATABASE()")) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (Exception e) {
            log.warn("에서 JDBC 연결가져오기데이터베이스이름실패", e);
        }
        return null;
    }
}
