package com.iflytek.rpa.component.service.impl;

import com.iflytek.rpa.base.annotation.RobotVersionAnnotation;
import com.iflytek.rpa.base.entity.dto.BaseDto;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.component.dao.ComponentRobotBlockDao;
import com.iflytek.rpa.component.dao.ComponentRobotUseDao;
import com.iflytek.rpa.component.entity.ComponentRobotBlock;
import com.iflytek.rpa.component.entity.ComponentRobotUse;
import com.iflytek.rpa.component.entity.dto.AddRobotBlockDto;
import com.iflytek.rpa.component.entity.dto.GetRobotBlockDto;
import com.iflytek.rpa.component.service.ComponentRobotBlockService;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 봇컴포넌트테이블(ComponentRobotBlock)테이블서비스유형
 *
 * @author makejava
 * @since 2024-12-19
 */
@Service("componentRobotBlockService")
public class ComponentRobotBlockServiceImpl implements ComponentRobotBlockService {

    @Autowired
    private ComponentRobotBlockDao componentRobotBlockDao;

    @Autowired
    private ComponentRobotUseDao componentRobotUseDao;

    @Autowired
    private ComponentRobotBlockServiceImpl self;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    /**
     * 추가봇컴포넌트의기록
     *
     * @param addRobotBlockDto 추가기록요청 매개변수
     * @return 결과
     * @throws Exception 예외정보
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<Boolean> addRobotBlock(AddRobotBlockDto addRobotBlockDto) throws Exception {
        // 가져오기현재테넌트ID
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();

        Integer robotVersion = getRobotVersion(
                addRobotBlockDto.getRobotId(),
                addRobotBlockDto.getMode(),
                addRobotBlockDto.getRobotVersion(),
                new BaseDto());

        // 조회componentRobotUse테이블중여부저장에서해당기록
        ComponentRobotUse existingUse = componentRobotUseDao.getByRobotIdVersionAndComponentId(
                addRobotBlockDto.getRobotId(), robotVersion, addRobotBlockDto.getComponentId(), userId);
        if (existingUse != null) throw new ServiceException(ErrorCodeEnum.E_SQL_REPEAT.getCode(), "해당봇완료사용컴포넌트, 불가");

        // 조회여부완료저장된 기록
        Long existingCount = componentRobotBlockDao.checkBlockExists(
                addRobotBlockDto.getRobotId(), robotVersion, addRobotBlockDto.getComponentId(), userId);
        if (existingCount > 0) throw new ServiceException(ErrorCodeEnum.E_SQL_REPEAT.getCode(), "해당봇완료컴포넌트, 필요하지 않습니다재복사추가");

        // 생성새의기록
        ComponentRobotBlock block = new ComponentRobotBlock();
        block.setRobotId(addRobotBlockDto.getRobotId());
        block.setRobotVersion(robotVersion);
        block.setComponentId(addRobotBlockDto.getComponentId());
        block.setCreatorId(userId);
        block.setCreateTime(new Date());
        block.setUpdaterId(userId);
        block.setUpdateTime(new Date());
        block.setDeleted(0);
        block.setTenantId(tenantId);

        // 저장까지데이터베이스
        int result = componentRobotBlockDao.insert(block);

        if (result > 0) {
            return AppResponse.success(true);
        } else {
            throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "추가실패");
        }
    }

    /**
     * 삭제봇컴포넌트의기록
     *
     * @param addRobotBlockDto 삭제기록요청 매개변수
     * @return 결과
     * @throws Exception 예외정보
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<Boolean> deleteRobotBlock(AddRobotBlockDto addRobotBlockDto) throws Exception {
        // 가져오기현재테넌트ID및사용자ID
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();

        Integer robotVersion = getRobotVersion(
                addRobotBlockDto.getRobotId(),
                addRobotBlockDto.getMode(),
                addRobotBlockDto.getRobotVersion(),
                new BaseDto());

        // 조회여부완료저장된 기록
        Long existingCount = componentRobotBlockDao.checkBlockExists(
                addRobotBlockDto.getRobotId(), robotVersion, addRobotBlockDto.getComponentId(), userId);
        if (existingCount == 0) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "해당 봇에 컴포넌트가 없어 삭제할 필요가 없습니다");
        }

        // 삭제기록
        int result = componentRobotBlockDao.deleteBlockByRobotAndComponent(
                addRobotBlockDto.getRobotId(), robotVersion, addRobotBlockDto.getComponentId(), userId, tenantId);

        if (result > 0) {
            return AppResponse.success(true);
        } else {
            throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "삭제실패");
        }
    }

    /**
     * 가져오기봇의컴포넌트ID목록
     *
     * @param queryDto
     * @return 의컴포넌트ID목록
     * @throws Exception 예외정보
     */
    @Override
    public AppResponse<List<String>> getBlockedComponentIds(GetRobotBlockDto queryDto) throws Exception {
        // 가져오기현재테넌트ID
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();

        Integer robotVersion =
                getRobotVersion(queryDto.getRobotId(), queryDto.getMode(), queryDto.getRobotVersion(), new BaseDto());

        // 조회의컴포넌트ID목록
        List<String> blockedComponentIds =
                componentRobotBlockDao.getBlockedComponentIds(queryDto.getRobotId(), robotVersion, tenantId);

        return AppResponse.success(blockedComponentIds);
    }

    public Integer getRobotVersion(String robotId, String mode, Integer version, BaseDto baseDto) {
        baseDto.setMode(mode);
        baseDto.setRobotVersion(version);
        baseDto.setRobotId(robotId);

        // 해제관리아니요제목, 할 수 없음사용유형호출의방법법, 가능사용비고입력의방식
        self.getVersion(baseDto);

        return baseDto.getRobotVersion();
    }

    @RobotVersionAnnotation
    public void getVersion(BaseDto baseDto) {}
}