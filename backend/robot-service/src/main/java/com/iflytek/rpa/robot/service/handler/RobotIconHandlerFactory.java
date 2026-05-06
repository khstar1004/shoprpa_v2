package com.iflytek.rpa.robot.service.handler;

import static com.iflytek.rpa.robot.constants.RobotConstant.*;

import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mjren
 * @date 2025-04-17 15:13
 * @copyright Copyright (c) 2025 mjren
 */
@Component
public class RobotIconHandlerFactory {
    private final Map<String, RobotIconModeHandler> handlerMap = new ConcurrentHashMap<>();

    @Autowired
    public RobotIconHandlerFactory(List<RobotIconModeHandler> handlers) {
        // 시생성방식까지관리기기의
        for (RobotIconModeHandler handler : handlers) {
            for (String mode : getSupportedModes(handler)) {
                handlerMap.put(mode, handler);
            }
        }
    }

    private List<String> getSupportedModes(RobotIconModeHandler handler) {
        // 근거서비스반환해당관리기기지원의모든방식
        if (handler instanceof IconEditModeHandler) {
            return Arrays.asList(EDIT_PAGE, PROJECT_LIST);
        }
        if (handler instanceof IconExecutorModeHandler) {
            return Collections.singletonList(EXECUTOR);
        }
        if (handler instanceof IconTriggerModeHandler) {
            return Collections.singletonList(CRONTAB);
        }
        if (handler instanceof IconDispatchModeHandler) {
            return Collections.singletonList(DISPATCH);
        }
        return Collections.emptyList();
    }

    public RobotIconModeHandler getHandler(String mode) {
        RobotIconModeHandler handler = handlerMap.get(mode);
        if (handler == null) {
            throw new ServiceException(ErrorCodeEnum.E_PARAM.getCode(), "지원하지 않음의매개변수 방식: " + mode);
        }
        return handler;
    }
}