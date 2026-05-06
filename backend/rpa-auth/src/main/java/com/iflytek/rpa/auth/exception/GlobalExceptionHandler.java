package com.iflytek.rpa.auth.exception;

import com.iflytek.rpa.auth.blacklist.exception.ShouldBeBlackException;
import com.iflytek.rpa.auth.blacklist.exception.UserBlockedException;
import com.iflytek.rpa.auth.blacklist.service.BlackListService;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 시스템일가져오기 제어미완료관리의예외, 확인종료로 AppResponse 결과.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.iflytek.rpa.auth")
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final BlackListService blackListService;

    /**
     * 관리입력이름단일예외
     * 가져오기후를사용자추가까지이름단일
     */
    @ExceptionHandler(ShouldBeBlackException.class)
    public AppResponse<String> handleShouldBeBlackException(ShouldBeBlackException e) {
        log.warn(
                "트리거이면, userId: {}, username: {}, reason: {}, type: {}",
                e.getUserId(),
                e.getUsername(),
                e.getReason(),
                e.getBlackType());

        try {
            // 추가까지이름단일
            blackListService.add(e.getUserId(), e.getUsername(), e.getReason(), "SYSTEM");
            log.info("사용자완료추가까지이름단일, userId: {}", e.getUserId());
        } catch (Exception ex) {
            log.error("추가이름단일실패", ex);
        }

        return AppResponse.error(ErrorCodeEnum.E_NO_POWER, "의계정완료: " + e.getReason());
    }

    /**
     * 관리사용자예외
     */
    @ExceptionHandler(UserBlockedException.class)
    public AppResponse<String> handleUserBlockedException(UserBlockedException e) {
        log.warn("사용자, userId: {}, username: {}, reason: {}", e.getUserId(), e.getUsername(), e.getReason());
        return AppResponse.error(ErrorCodeEnum.E_NO_POWER, e.getMessage());
    }

    @ExceptionHandler(ServiceException.class)
    public AppResponse<String> handleServiceException(ServiceException e) {
        log.warn("서비스예외: {}", e.getMessage());
        return AppResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public AppResponse<String> handleMethodArgumentNotValidException(Exception e) {
        FieldError fieldError = null;
        if (e instanceof MethodArgumentNotValidException) {
            fieldError =
                    ((MethodArgumentNotValidException) e).getBindingResult().getFieldError();
        } else if (e instanceof BindException) {
            fieldError = ((BindException) e).getBindingResult().getFieldError();
        }
        String message = fieldError != null ? fieldError.getDefaultMessage() : "매개변수검증실패";
        log.warn("매개변수검증예외: {}", message);
        return AppResponse.error(ErrorCodeEnum.E_PARAM.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public AppResponse<String> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("매개변수 예외: {}", message);
        return AppResponse.error(ErrorCodeEnum.E_PARAM.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public AppResponse<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("요청 파싱예외: {}", e.getMessage());
        return AppResponse.error(ErrorCodeEnum.E_PARAM.getCode(), "요청 파싱실패");
    }

    @ExceptionHandler(Exception.class)
    public AppResponse<String> handleException(Exception e) {
        log.error("시스템예외", e);
        String message = e.getMessage() == null ? "서비스 예외, 요청 후재시도" : e.getMessage();
        return AppResponse.error(ErrorCodeEnum.E_SERVICE.getCode(), message);
    }
}