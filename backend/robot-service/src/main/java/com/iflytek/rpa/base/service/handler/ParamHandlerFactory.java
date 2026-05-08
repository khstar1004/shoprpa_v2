package com.iflytek.rpa.base.service.handler;

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
public class ParamHandlerFactory {
    private final Map<String, ParamModeHandler> handlerMap = new ConcurrentHashMap<>();

    @Autowired
    public ParamHandlerFactory(List<ParamModeHandler> handlers) {
        // 시생성방식까지관리기기의
        for (ParamModeHandler handler : handlers) {
            for (String mode : getSupportedModes(handler)) {
                handlerMap.put(mode, handler);
            }
        }
    }

    private List<String> getSupportedModes(ParamModeHandler handler) {
        // 근거서비스반환해당관리기기지원의모든방식
        if (handler instanceof EditModeHandler) {
            return Arrays.asList(EDIT_PAGE, PROJECT_LIST);
        }
        if (handler instanceof ExecutorModeHandler) {
            return Collections.singletonList(EXECUTOR);
        }
        if (handler instanceof TriggerModeHandler) {
            return Collections.singletonList(CRONTAB);
        }
        if (handler instanceof DispatchModeHandler) {
            return Collections.singletonList(DISPATCH);
        }
        return Collections.emptyList();
    }

    public ParamModeHandler getHandler(String mode) {
        ParamModeHandler handler = handlerMap.get(mode);
        if (handler == null) {
            throw new ServiceException(ErrorCodeEnum.E_PARAM.getCode(), "지원하지 않는 매개변수 방식입니다: " + mode);
        }
        return handler;
    }
}
