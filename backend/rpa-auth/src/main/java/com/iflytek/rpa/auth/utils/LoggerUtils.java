package com.iflytek.rpa.auth.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUtils {

    /**
     * 오류 인쇄예외정보
     *
     * @param logger  logger
     * @param message 오류정보
     * @param ex      예외
     */
    public static void error(Logger logger, String message, Exception ex) {
        logger.error(message);
        if (ex != null) {
            logger.error(getExceptionInfo(ex));
        }
    }

    public static void error(String message, Exception ex) {
        error(getLoggerBySelf(), message, ex);
    }

    public static void error(Logger logger, String message) {
        error(logger, message, null);
    }

    public static void error(String message) {
        error(getLoggerBySelf(), message);
    }

    /**
     * 경고 인쇄예외정보
     *
     * @param logger  logger
     * @param message 오류정보
     * @param ex      예외
     */
    public static void warn(Logger logger, String message, Exception ex) {
        logger.warn(message);
        if (ex != null) {
            logger.warn(getExceptionInfo(ex));
        }
    }

    public static void warn(String message, Exception ex) {
        warn(getLoggerBySelf(), message, ex);
    }

    public static void warn(Logger logger, String message) {
        warn(logger, message, null);
    }

    public static void warn(String message) {
        warn(getLoggerBySelf(), message);
    }

    /**
     * 알림 인쇄예외정보
     *
     * @param logger  logger
     * @param message 오류정보
     * @param ex      예외
     */
    public static void info(Logger logger, String message, Exception ex) {
        logger.info(message);
        if (ex != null) {
            logger.info(getExceptionInfo(ex));
        }
    }

    public static void info(String message, Exception ex) {
        info(getLoggerBySelf(), message, ex);
    }

    public static void info(Logger logger, String message) {
        info(logger, message, null);
    }

    public static void info(String message) {
        info(getLoggerBySelf(), message);
    }

    /**
     * 디버그 인쇄예외정보
     *
     * @param logger  logger
     * @param message 오류정보
     * @param ex      예외
     */
    public static void debug(Logger logger, String message, Exception ex) {
        logger.debug(message);
        if (ex != null) {
            logger.debug(getExceptionInfo(ex));
        }
    }

    public static void debug(String message, Exception ex) {
        debug(getLoggerBySelf(), message, ex);
    }

    public static void debug(Logger logger, String message) {
        debug(logger, message, null);
    }

    public static void debug(String message) {
        debug(getLoggerBySelf(), message);
    }

    /**
     * 가져오기예외정보
     *
     * @param ex 예외
     * @return String
     */
    public static String getExceptionInfo(Exception ex) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        ex.printStackTrace(printStream);
        String rs = out.toString();
        try {
            printStream.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rs;
    }

    /**
     * 가져오기현재데이터의호출유형
     * 에서본유형중사용
     *
     * @return Logger
     */
    public static Logger getLoggerBySelf() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        if (stackTraceElements.length >= 4) {
            return LoggerFactory.getLogger(stackTraceElements[3].getClassName());
        } else {
            return LoggerFactory.getLogger(LoggerUtils.class);
        }
    }

    /**
     * 가져오기현재데이터의호출유형
     * 에서외부모듈유형중사용
     *
     * @return Logger
     */
    public static Logger getLogger() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        if (stackTraceElements.length >= 3) {
            return LoggerFactory.getLogger(stackTraceElements[2].getClassName());
        } else {
            return LoggerFactory.getLogger(LoggerUtils.class);
        }
    }
}