package com.iflytek.rpa.dispatch.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 스케줄링관리관리예약 작업분조회DTO
 *
 * @author jqfang
 * @since 2025-08-15
 */
@Data
public class DispatchTaskPageListVo {

    /**
     * 스케줄링방식예약 작업id
     */
    private String dispatchTaskId;
    /**
     * 작업이름
     */
    private String name;
    /**
     * 실행파일: 트리거manual, 예약schedule, 예약트리거trigger
     */
    private String type;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 작업상태: 사용중 active, 닫기 stop, 완료경과 expired
     */
    private String status;

    /**
     * 생성스케줄링예약 작업의매개변수;예약schedule저장계획계획실행의JSON
     */
    private String cronJson;

    /**
     * 실행단말/분그룹
     */
    private List<TerminalOrGroup> terminalOrGroupList = new ArrayList<>();

    private List<DispatchRobot> dispatchRobotList = new ArrayList<>();

    @Data
    public static class TerminalOrGroup {
        /**
         * 분그룹-id; 단말-terminalId
         */
        private String id;
        /**
         * 단말이름/분그룹이름
         */
        private String name;
        /**
         * 단말: terminal, 분그룹: group
         */
        private String type;
        /**
         * 순서
         */
        private Integer sort;
        /**
         * 단말상태
         */
        private String status;
        /**
         * 단말수
         */
        private Integer num;
    }

    @Data
    public static class DispatchRobot {
        private String name;
        /**
         * 봇id
         */
        private String robotId;
        /**
         * 여부사용버전
         */
        private Boolean online;
        /**
         * 봇버전
         */
        private Integer version;
        /**
         * 순서
         */
        private Integer sort;
        /**
         * 여부있음구성 매개변수
         */
        private Boolean haveParam;
        /**
         * 구성 매개변수
         */
        private String paramJson;
    }

    /**
     * 오류예관리: 건너뛰기jump, 중지stop, 재시도후건너뛰기retry_jump, 재시도후중지retry_stop
     */
    private String exceptional;
    /**
     * 재시도 데이터
     */
    private Integer retryNum;

    /**
     * 여부사용시간 초과시간 1:사용 0:아니요사용
     */
    private Boolean timeoutEnable;
    /**
     * 시간 초과시간
     */
    private Integer timeout;

    /**
     * 여부사용정렬팀 1:사용 0:아니요사용
     */
    private Boolean queueEnable;

    /**
     * 여부열기시작기록 1:사용 0:아니요사용
     */
    private Boolean screenRecordEnable;

    /**
     * 여부열기시작 1:사용 0:아니요사용
     */
    private Boolean virtualDesktopEnable;
}