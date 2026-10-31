package com.aichat.exception;

import com.aichat.model.vo.ErrorVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** GlobalExceptionHandler 的应用组件。 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorVO> api(ApiException e) {
        if (e.getCause() != null) {
            LOGGER.error("Business request failed status={}", e.getStatus(), SafeLog.forLogging(e));
        }
        return error(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ErrorVO> validation(Exception e) {
        return error(HttpStatus.BAD_REQUEST.value(), "输入格式不正确，请检查必填项、字数和邮箱格式");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ErrorVO> conflict(Exception e) {
        return error(HttpStatus.CONFLICT.value(), "知识树已在其他页面修改，请刷新后重试");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorVO> duplicate(Exception e) {
        return error(HttpStatus.CONFLICT.value(), "数据已存在或发生冲突，请检查输入后重试");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorVO> unexpected(Exception e) {
        LOGGER.error("Unhandled API error", SafeLog.forLogging(e));
        return error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务暂时异常，请稍后重试");
    }

    private ResponseEntity<ErrorVO> error(int status, String message) {
        return ResponseEntity.status(status).body(new ErrorVO(message));
    }
}
