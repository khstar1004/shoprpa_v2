package com.iflytek.rpa.utils.exception;

/**
 * 목록이름:  com.iflytek.fuxi.common.exception
 * 유형 이름 명칭:  NoDataException
 * 유형 설명 서술:  EXCEL 내보내기데이터시
 * 생성 시간:  2020/4/1 5:27 아래
 * 생성 생성 사람:  shzhang7
 **/
public class NoDataException extends Exception {

    public NoDataException() {
        super();
    }

    public NoDataException(String message) {
        super(message);
    }

    public NoDataException(Exception e) {
        super(e);
    }
}