package com.iflytek.rpa.quota.service.impl;

import com.iflytek.rpa.market.dao.AppMarketUserDao;
import com.iflytek.rpa.quota.service.QuotaCountService;
import com.iflytek.rpa.robot.dao.RobotDesignDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 매칭금액수조회서비스유형
 * 의현재수조회(저장)
 */
@Slf4j
@Service
public class QuotaCountServiceImpl implements QuotaCountService {
    @Autowired
    private RobotDesignDao robotDesignDao;

    @Autowired
    private AppMarketUserDao appMarketUserDao;

    @Override
    public Integer getDesignerCount(String tenantId, String userId) {
        if (StringUtils.isBlank(tenantId) || StringUtils.isBlank(userId)) {
            return 0;
        }
        // 에서데이터베이스조회
        try {
            // 조회사용자에서현재테넌트아래생성의봇수(삭제되지 않음의)
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.iflytek.rpa.robot.entity.RobotDesign>
                    wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                    com.iflytek.rpa.robot.entity.RobotDesign>()
                            .eq(com.iflytek.rpa.robot.entity.RobotDesign::getTenantId, tenantId)
                            .eq(com.iflytek.rpa.robot.entity.RobotDesign::getCreatorId, userId)
                            .eq(com.iflytek.rpa.robot.entity.RobotDesign::getDeleted, 0);
            Integer count = robotDesignDao.selectCount(wrapper);
            Integer result = count != null ? count : 0;

            return result;
        } catch (Exception e) {
            log.error("조회계획기기수실패, tenantId: {}, userId: {}", tenantId, userId, e);
            return 0;
        }
    }

    @Override
    public Integer getMarketJoinCount(String tenantId, String userId) {
        if (StringUtils.isBlank(tenantId) || StringUtils.isBlank(userId)) {
            return 0;
        }
        // 에서데이터베이스조회
        try {
            // 조회사용자에서현재테넌트아래완료추가입력의마켓수(삭제되지 않음의)
            Integer count = appMarketUserDao.getMarketJoinCount(tenantId, userId);
            Integer result = count != null ? count : 0;
            return result;
        } catch (Exception e) {
            log.error("조회마켓추가입력수실패, tenantId: {}, userId: {}", tenantId, userId, e);
            return 0;
        }
    }
}