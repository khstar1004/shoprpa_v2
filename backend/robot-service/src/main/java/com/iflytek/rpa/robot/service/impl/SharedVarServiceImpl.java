package com.iflytek.rpa.robot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.robot.dao.SharedSubVarDao;
import com.iflytek.rpa.robot.dao.SharedVarDao;
import com.iflytek.rpa.robot.dao.SharedVarKeyTenantDao;
import com.iflytek.rpa.robot.dao.SharedVarUserDao;
import com.iflytek.rpa.robot.entity.SharedVar;
import com.iflytek.rpa.robot.entity.SharedVarKeyTenant;
import com.iflytek.rpa.robot.entity.dto.SharedVarBatchDto;
import com.iflytek.rpa.robot.entity.enums.SharedVarTypeEnum;
import com.iflytek.rpa.robot.entity.vo.ClientSharedSubVarVo;
import com.iflytek.rpa.robot.entity.vo.ClientSharedVarVo;
import com.iflytek.rpa.robot.entity.vo.SharedSubVarVo;
import com.iflytek.rpa.robot.entity.vo.SharedVarKeyVo;
import com.iflytek.rpa.robot.service.SharedVarService;
import com.iflytek.rpa.terminal.entity.enums.UsageTypeEnum;
import com.iflytek.rpa.utils.EncryptionUtil;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 공유 변수서비스유형
 *
 * @author jqfang3
 * @since 2025-07-21
 */
@Slf4j
@Service
public class SharedVarServiceImpl extends ServiceImpl<SharedVarDao, SharedVar> implements SharedVarService {
    @Resource
    private SharedVarDao sharedVarDao;

    @Resource
    private SharedSubVarDao sharedSubVarDao;

    @Resource
    private SharedVarUserDao sharedVarUserDao;

    @Resource
    private SharedVarKeyTenantDao sharedVarKeyTenantDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    /**
     * 가져오기공유 변수테넌트키
     *
     * @return 키 key
     * @throws NoLoginException
     */
    @Override
    public AppResponse<SharedVarKeyVo> getSharedVarKey() throws NoLoginException {
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        if (tenantId == null) {
            throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "적음테넌트 정보");
        }
        SharedVarKeyTenant keyTenant = sharedVarKeyTenantDao.selectByTenantId(tenantId);
        if (keyTenant == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "테넌트키찾을 수 없습니다");
        }
        SharedVarKeyVo result = new SharedVarKeyVo();
        result.setKey(keyTenant.getKey());
        return AppResponse.success(result);
    }

    @Override
    public AppResponse<List<ClientSharedVarVo>> getClientSharedVars() throws NoLoginException {
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        if (tenantId == null) {
            throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "적음테넌트 정보");
        }
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppResponse<String> deptIdRes = rpaAuthFeign.getDeptIdByUserId(userId, tenantId);

        if (!deptIdRes.ok()) return AppResponse.success(new ArrayList<>());
        String deptId = deptIdRes.getData();
        SharedVarKeyTenant keyTenant = sharedVarKeyTenantDao.selectByTenantId(tenantId);
        if (keyTenant == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "테넌트키찾을 수 없습니다");
        }
        String aesKey = keyTenant.getKey();

        // 3. 조회삼유형의공유 변수
        List<String> selectVarIds = sharedVarUserDao.getAvailableSharedVarIds(userId);
        List<SharedVar> availableVars = sharedVarDao.getAvailableSharedVars(tenantId, deptId, selectVarIds);
        if (availableVars.isEmpty()) {
            return AppResponse.success(new ArrayList<>());
        }

        // 4. 설치결과
        List<ClientSharedVarVo> result = packageResultVo(availableVars, aesKey);

        return AppResponse.success(result);
    }

    private List<ClientSharedVarVo> packageResultVo(List<SharedVar> availableVars, String aesKey) {
        List<Long> sharedVarIds = availableVars.stream().map(SharedVar::getId).collect(Collectors.toList());
        List<SharedSubVarVo> subVarList = baseMapper.getSubVarListBySharedVarIds(sharedVarIds);
        Map<Long, List<SharedSubVarVo>> sharedVarId2SubVarMap =
                subVarList.stream().collect(Collectors.groupingBy(SharedSubVarVo::getSharedVarId));

        List<ClientSharedVarVo> result = new ArrayList<>();
        for (SharedVar sharedVar : availableVars) {
            ClientSharedVarVo clientVar = new ClientSharedVarVo();
            clientVar.setId(sharedVar.getId());
            clientVar.setSharedVarName(sharedVar.getSharedVarName());
            clientVar.setSharedVarType(sharedVar.getSharedVarType());

            // 변수목록
            List<SharedSubVarVo> subVars = sharedVarId2SubVarMap.get(sharedVar.getId());
            if (subVars != null && !subVars.isEmpty()) {
                List<ClientSharedSubVarVo> clientSubVars = new ArrayList<>();

                for (SharedSubVarVo subVar : subVars) {
                    ClientSharedSubVarVo clientSubVar = new ClientSharedSubVarVo();
                    clientSubVar.setVarName(subVar.getVarName());
                    clientSubVar.setVarType(subVar.getVarType());
                    clientSubVar.setEncrypt(subVar.getEncrypt());
                    // 암호화데이터
                    packageEncryptValue(aesKey, subVar, clientSubVar);
                    clientSubVars.add(clientSubVar);
                }
                clientVar.setSubVarList(clientSubVars);

                // 변수그룹유형, 변수의값및암호화상태
                if (!SharedVarTypeEnum.GROUP.getCode().equals(sharedVar.getSharedVarType())) {
                    ClientSharedSubVarVo firstSubVar = clientSubVars.get(0);
                    clientVar.setSharedVarValue(firstSubVar.getVarValue());
                    clientVar.setEncrypt(firstSubVar.getEncrypt());
                }
            }

            result.add(clientVar);
        }
        return result;
    }

    private static void packageEncryptValue(String aesKey, SharedSubVarVo subVar, ClientSharedSubVarVo clientSubVar) {
        // 관리암호화
        String varValue = subVar.getVarValue();
        if (subVar.getEncrypt() != null && subVar.getEncrypt() == 1 && varValue != null) {
            try {
                varValue = EncryptionUtil.encrypt(varValue, aesKey);
            } catch (Exception e) {
                log.error("암호화변수값실패: {}", e.getMessage());
                throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "변수암호화실패");
            }
        }
        clientSubVar.setVarValue(varValue);
    }

    /**
     * 완료지정길이정도의기기키
     *
     * @param length 키길이정도
     * @return 기기키
     */
    private String generateRandomKey(int length) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < length; i++) {
            key.append(chars.charAt(random.nextInt(chars.length())));
        }
        return key.toString();
    }

    @Override
    public AppResponse<List<ClientSharedVarVo>> getBatchSharedVar(SharedVarBatchDto updateDto) throws NoLoginException {
        List<Long> ids = updateDto.getIds();
        if (ids.isEmpty()) {
            return AppResponse.success(new ArrayList<>());
        }
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        if (tenantId == null) {
            throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "적음테넌트 정보");
        }
        SharedVarKeyTenant keyTenant = sharedVarKeyTenantDao.selectByTenantId(tenantId);
        if (keyTenant == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "테넌트키찾을 수 없습니다");
        }
        String aesKey = keyTenant.getKey();
        List<SharedVar> availableVars = sharedVarDao.getAvailableByIds(ids);
        if (availableVars.isEmpty()) {
            return AppResponse.success(new ArrayList<>());
        }

        checkUsePermission(availableVars);

        // 4. 설치결과
        List<ClientSharedVarVo> result = packageResultVo(availableVars, aesKey);

        return AppResponse.success(result);
    }

    private void checkUsePermission(List<SharedVar> availableVars) throws NoLoginException {
        Iterator<SharedVar> iterator = availableVars.iterator();
        while (iterator.hasNext()) {
            SharedVar availableVar = iterator.next();
            if (availableVar.getUsageType().equals(UsageTypeEnum.ALL.getCode())) {
                continue;
            }
            AppResponse<User> response = rpaAuthFeign.getLoginUser();
            if (response == null || response.getData() == null) {
                throw new ServiceException("사용자 정보 조회 실패");
            }
            User uapUser = response.getData();
            if (availableVar.getUsageType().equals(UsageTypeEnum.DEPT.getCode())) {
                String orgId = uapUser.getOrgId();
                if (!availableVar.getDeptId().equals(orgId)) {
                    iterator.remove();
                    continue;
                }
            }
            if (availableVar.getUsageType().equals(UsageTypeEnum.SELECT.getCode())) {
                String userId = uapUser.getId();
                List<String> availableSharedVarIds = sharedVarUserDao.getAvailableSharedVarIds(userId);
                if (!availableSharedVarIds.contains(String.valueOf(availableVar.getId()))) {
                    iterator.remove();
                }
            }
        }
    }
}