package com.iflytek.rpa.resource.common.exp;

import com.iflytek.rpa.resource.common.response.AppResponse;
import com.iflytek.rpa.resource.common.response.ErrorCodeEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 전체영역예외관리기기
 * 시스템일관리사용프로그램중의예외, 반환의오류 
 *
 * @author system
 * @date 2024
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 관리서비스예외
     *
     * @param e 서비스예외
     * @return 오류 
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<AppResponse<String>> handleServiceException(ServiceException e) {
        log.warn("서비스예외: {}", e.getMessage(), e);
        AppResponse<String> response = AppResponse.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 관리매개변수검증예외 - @Valid 비고해제검증실패
     *
     * @param e 매개변수검증예외
     * @return 오류 
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AppResponse<String>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.warn("매개변수검증예외: {}", e.getMessage());

        StringBuilder errorMsg = new StringBuilder();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errorMsg.append(fieldError.getField())
                    .append(": ")
                    .append(fieldError.getDefaultMessage())
                    .append("; ");
        }

        AppResponse<String> response =
                AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "매개변수검증실패: " + errorMsg.toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 관리매개변수 지정예외 - @Validated 비고해제검증실패
     *
     * @param e 매개변수 지정예외
     * @return 오류 
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<AppResponse<String>> handleBindException(BindException e) {
        log.warn("매개변수 지정예외: {}", e.getMessage());

        StringBuilder errorMsg = new StringBuilder();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errorMsg.append(fieldError.getField())
                    .append(": ")
                    .append(fieldError.getDefaultMessage())
                    .append("; ");
        }

        AppResponse<String> response =
                AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "매개변수 지정실패: " + errorMsg.toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 관리반대예외 - @Validated 비고해제검증실패
     *
     * @param e 반대예외
     * @return 오류 
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<AppResponse<String>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("반대예외: {}", e.getMessage());

        StringBuilder errorMsg = new StringBuilder();
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            errorMsg.append(violation.getPropertyPath())
                    .append(": ")
                    .append(violation.getMessage())
                    .append("; ");
        }

        AppResponse<String> response =
                AppResponse.error(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "매개변수 반대: " + errorMsg.toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 관리적음요청 매개변수예외
     *
     * @param e 적음요청 매개변수예외
     * @return 오류 
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<AppResponse<String>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        log.warn("적음요청 매개변수예외: {}", e.getMessage());
        String message = String.format("적음필요의요청 매개변수: %s", e.getParameterName());
        AppResponse<String> response = AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE.getCode(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 관리메서드 매개변수 유형이 일치하지 않습니다
     *
     * @param e 메서드 매개변수 유형이 일치하지 않습니다
     * @return 오류 
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<AppResponse<String>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.warn("메서드 매개변수 유형이 일치하지 않습니다: {}", e.getMessage());
        String message = String.format(
                "매개변수 '%s' 의값 '%s' 불가변환로유형 '%s'",
                e.getName(), e.getValue(), e.getRequiredType().getSimpleName());
        AppResponse<String> response = AppResponse.error(ErrorCodeEnum.E_PARAM_PARSE.getCode(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 관리요청 방법법지원하지 않음예외
     *
     * @param e 요청 방법법지원하지 않음예외
     * @return 오류 
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<AppResponse<String>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.warn("요청 방법법지원하지 않음예외: {}", e.getMessage());
        String message =
                String.format("요청 방법법 '%s' 지원하지 않음, 지원의방법법: %s", e.getMethod(), String.join(", ", e.getSupportedMethods()));
        AppResponse<String> response = AppResponse.error(ErrorCodeEnum.E_SERVICE_NOT_SUPPORT.getCode(), message);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * 관리404예외 - 아니요까지관리기기
     *
     * @param e 처리되지 않은 예외
     * @return 오류 
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<AppResponse<String>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("처리되지 않은 예외: {}", e.getMessage());
        String message = String.format("요청 경로 '%s' 찾을 수 없습니다", e.getRequestURL());
        AppResponse<String> response = AppResponse.error(ErrorCodeEnum.E_SERVICE_NOT_SUPPORT.getCode(), message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 관리파일업로드크기초과제한예외
     *
     * @param e 파일업로드크기초과제한예외
     * @return 오류 
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<AppResponse<String>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("파일업로드크기초과제한예외: {}", e.getMessage());
        AppResponse<String> response = AppResponse.error(ErrorCodeEnum.E_PARAM.getCode(), "업로드파일크기초과출력제한제어");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 관리법매개변수예외
     *
     * @param e 법매개변수예외
     * @return 오류 
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AppResponse<String>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("법매개변수예외: {}", e.getMessage(), e);
        AppResponse<String> response = AppResponse.error(ErrorCodeEnum.E_PARAM.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 관리빈예외
     *
     * @param e 빈예외
     * @return 오류 
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<AppResponse<String>> handleNullPointerException(NullPointerException e) {
        log.error("빈예외: {}", e.getMessage(), e);
        AppResponse<String> response = AppResponse.error(ErrorCodeEnum.E_EXCEPTION.getCode(), "시스템내부 오류");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 관리런타임예외
     *
     * @param e 런타임예외
     * @return 오류 
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AppResponse<String>> handleRuntimeException(RuntimeException e) {
        log.error("런타임예외: {}", e.getMessage(), e);
        AppResponse<String> response = AppResponse.error(ErrorCodeEnum.E_EXCEPTION.getCode(), "시스템실행예외");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 관리모든예외
     *
     * @param e 예외
     * @return 오류 
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppResponse<String>> handleException(Exception e) {
        log.error("시스템예외: {}", e.getMessage(), e);
        AppResponse<String> response = AppResponse.error(ErrorCodeEnum.E_EXCEPTION.getCode(), "시스템내부 오류");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}