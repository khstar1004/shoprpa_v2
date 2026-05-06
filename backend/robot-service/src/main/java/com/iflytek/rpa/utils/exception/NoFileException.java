package com.iflytek.rpa.utils.exception;

/**
 * 목록이름:  com.iflytek.projectmanage.common.exception
 * 유형 이름 명칭:  NoFileException
 * 유형 설명 서술:  파일아니요까지예외
 * 생성 시간:  2020/4/26 4:00 아래
 * 생성 생성 사람:  keler
 **/
public class NoFileException extends Exception {

    public NoFileException() {
        super();
    }

    public NoFileException(String message) {
        super(message);
    }

    public NoFileException(Exception e) {
        super(e);
    }
}