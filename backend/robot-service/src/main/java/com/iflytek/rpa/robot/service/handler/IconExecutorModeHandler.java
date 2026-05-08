package com.iflytek.rpa.robot.service.handler;

import static com.iflytek.rpa.robot.constants.RobotConstant.EXECUTOR;

import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.robot.dao.RobotVersionDao;
import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.robot.entity.RobotVersion;
import com.iflytek.rpa.robot.entity.dto.RobotIconDto;
import com.iflytek.rpa.robot.entity.vo.RobotIconVo;
import com.iflytek.rpa.utils.StringUtils;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IconExecutorModeHandler implements RobotIconModeHandler {
    @Autowired
    private RobotExecuteDao robotExecuteDao;

    @Autowired
    private RobotVersionDao robotVersionDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public boolean supports(String mode) {
        return EXECUTOR.equals(mode);
    }

    @Override
    public AppResponse<RobotIconVo> handle(RobotIconDto dto) throws Exception {
        RobotExecute robotExecute = getRobotExecute(dto.getRobotId());
        return handleDataSource(robotExecute, dto.getRobotVersion());
    }

    private RobotExecute getRobotExecute(String robotId) throws NoLoginException {
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

        RobotExecute executeInfo = robotExecuteDao.getRobotInfoByRobotId(robotId, userId, tenantId);
        if (executeInfo == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL.getCode(), "실행할 로봇 정보를 찾을 수 없습니다");
        }
        return executeInfo;
    }

    private AppResponse<RobotIconVo> handleDataSource(RobotExecute robotExecute, Integer robotVersion) {
        if (robotVersion != null) {
            robotExecute.setAppVersion(robotVersion);
            robotExecute.setRobotVersion(robotVersion);
        }
        if ("market".equals(robotExecute.getDataSource())) {
            return handleMarketSource(robotExecute, robotVersion);
        } else if ("create".equals(robotExecute.getDataSource())) {
            return handleCreateSource(robotExecute, robotVersion);
        } else if ("deploy".equals(robotExecute.getDataSource())) {
            return handleDeploySource(robotExecute, robotVersion);
        }

        throw new ServiceException(ErrorCodeEnum.E_PARAM.getCode(), "지원하지 않는 데이터 출처입니다");
    }

    private AppResponse<RobotIconVo> handleMarketSource(RobotExecute executeInfo, Integer robotVersion) {
        RobotIconVo vo = robotVersionDao.getMarketInfo(executeInfo);
        return AppResponse.success(resolveIconVo(executeInfo, vo));
    }

    private AppResponse<RobotIconVo> handleCreateSource(RobotExecute robotExecute, Integer robotVersion) {
        String robotId = robotExecute.getRobotId();
        Integer versionNum = robotVersion != null ? robotVersion : robotVersionDao.getRobotVersion(robotId);
        RobotVersion version = versionNum == null ? null : robotVersionDao.getVersion(robotId, versionNum);
        String icon = version == null || StringUtils.isEmpty(version.getIcon()) ? "" : version.getIcon();
        String name = robotExecute.getName();
        return AppResponse.success(new RobotIconVo(name, icon));
    }

    private AppResponse<RobotIconVo> handleDeploySource(RobotExecute robotExecute, Integer robotVersion) {
        RobotIconVo vo = robotVersionDao.getDeployInfo(robotExecute);
        return AppResponse.success(resolveIconVo(robotExecute, vo));
    }

    private RobotIconVo resolveIconVo(RobotExecute robotExecute, RobotIconVo vo) {
        return vo != null ? vo : new RobotIconVo(robotExecute.getName(), "");
    }
}
