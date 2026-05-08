package com.iflytek.rpa.robot.service.handler;

import static com.iflytek.rpa.robot.constants.RobotConstant.CRONTAB;
import static com.iflytek.rpa.robot.constants.RobotConstant.CREATE;
import static com.iflytek.rpa.robot.constants.RobotConstant.DEPLOY;
import static com.iflytek.rpa.robot.constants.RobotConstant.MARKET;

import com.iflytek.rpa.robot.dao.RobotExecuteDao;
import com.iflytek.rpa.robot.dao.RobotVersionDao;
import com.iflytek.rpa.robot.entity.RobotExecute;
import com.iflytek.rpa.robot.entity.RobotVersion;
import com.iflytek.rpa.robot.entity.dto.RobotIconDto;
import com.iflytek.rpa.robot.entity.vo.RobotIconVo;
import com.iflytek.rpa.utils.StringUtils;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IconTriggerModeHandler implements RobotIconModeHandler {
    private final RobotExecuteDao robotExecuteDao;
    private final RobotVersionDao robotVersionDao;

    @Override
    public boolean supports(String mode) {
        return CRONTAB.equals(mode);
    }

    @Override
    public AppResponse<RobotIconVo> handle(RobotIconDto dto) throws Exception {
        RobotExecute executeInfo = robotExecuteDao.getRobotExecuteByRobotId(dto.getRobotId());
        if (executeInfo == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL.getCode(), "예약 작업에 연결된 로봇 정보를 찾을 수 없습니다");
        }

        Integer robotVersion = dto.getRobotVersion();
        if (robotVersion != null) {
            executeInfo.setAppVersion(robotVersion);
            executeInfo.setRobotVersion(robotVersion);
        }

        if (MARKET.equals(executeInfo.getDataSource())) {
            RobotIconVo vo = robotVersionDao.getMarketInfo(executeInfo);
            return AppResponse.success(resolveIconVo(executeInfo, vo));
        }
        if (DEPLOY.equals(executeInfo.getDataSource())) {
            RobotIconVo vo = robotVersionDao.getDeployInfo(executeInfo);
            return AppResponse.success(resolveIconVo(executeInfo, vo));
        }
        if (CREATE.equals(executeInfo.getDataSource())) {
            Integer versionNum = robotVersion != null ? robotVersion : robotVersionDao.getRobotVersion(dto.getRobotId());
            RobotVersion version = versionNum == null ? null : robotVersionDao.getVersion(dto.getRobotId(), versionNum);
            String icon = version == null || StringUtils.isEmpty(version.getIcon()) ? "" : version.getIcon();
            return AppResponse.success(new RobotIconVo(executeInfo.getName(), icon));
        }

        throw new ServiceException(ErrorCodeEnum.E_PARAM.getCode(), "지원하지 않는 데이터 출처입니다");
    }

    private RobotIconVo resolveIconVo(RobotExecute executeInfo, RobotIconVo vo) {
        return vo != null ? vo : new RobotIconVo(executeInfo.getName(), "");
    }
}
