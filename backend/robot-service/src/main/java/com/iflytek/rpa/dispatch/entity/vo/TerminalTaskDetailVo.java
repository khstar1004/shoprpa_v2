package com.iflytek.rpa.dispatch.entity.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 단말작업VO
 *
 * @author jqfang
 * @since 2025-08-15
 */
@Data
@Builder
public class TerminalTaskDetailVo {

    /**
     * 단말ID
     */
    private String terminalId;

    /**
     * 작업정보목록
     */
    @Builder.Default
    private List<DispatchTaskInfo> dispatchTaskInfos = new ArrayList<>();

    @Builder.Default
    private List<DispatchTaskInfo> retryTaskInfos = new ArrayList<>();

    @Builder.Default
    private List<DispatchTaskInfo> stopTaskInfos = new ArrayList<>();

    @Data
    public static class DispatchTaskInfo {
        /**
         * 작업ID
         */
        private String taskId;

        /**
         * 작업이름
         */
        private String taskName;

        /**
         * 작업유형(/계획계획/예약)
         */
        private String taskType;

        /**
         * 작업실행의스케줄링정보
         */
        private String cronJson;

        /**
         * 작업상태
         */
        private String taskStatus;

        /**
         * 오류 관리
         */
        private String exceptional;

        /**
         * 시간 초과시간
         */
        private Integer timeout;
        /**
         * 시간 초과시간여부열기시작
         */
        private Integer timeoutEnable;

        /**
         * 재시도 데이터
         */
        private Integer retryNum;
        /**
         * 큐여부열기시작
         */
        private Integer queueEnable;

        /**
         * 화면기록제어여부열기시작
         */
        private Integer screenRecordEnable;

        /**
         * 여부열기시작
         */
        private Integer virtualDesktopEnable;

        /**
         * 봇정보목록
         */
        private List<DispatchRobotInfo> dispatchRobotInfos = new ArrayList<>();
    }

    @Data
    public static class DispatchRobotInfo {
        /**
         * 작업ID
         */
        private String taskId;

        /**
         * 봇ID
         */
        private String robotId;

        /**
         * 봇이름
         */
        private String robotName;

        /**
         * 봇버전
         */
        private String robotVersion;

        /**
         * 여부에서버전
         */
        private Integer online;

        /**
         * 매개변수JSON문자열
         */
        private String paramJson;

        /**
         * 정렬
         */
        private Integer sort;

        //        /**
        //         * 프로세스정보목록
        //         */
        //        private List<DispatchProcessInfo> dispatchProcessInfos;
        //
        //        /**
        //         * 코드모듈목록
        //         */
        //        private List<DispatchModuleInfo> modules;
        //
        //        /**
        //         * 전체영역매개변수목록
        //         */
        //        private List<DispatchGlobalParam> dispatchGlobalParams;
        //
        //        /**
        //         * 패키지목록
        //         */
        //        private List<DispatchRequirement> dispatchRequirements;
    }

    @Data
    public static class DispatchProcessInfo {
        /**
         * 프로세스ID
         */
        private String processId;

        /**
         * 프로세스정보
         */
        private String processContent;

        /**
         * 프로세스이름
         */
        private String processName;

        /**
         * 매개변수목록
         */
        private List<DispatchParam> dispatchParams = new ArrayList<>();
    }

    @Data
    public static class DispatchModuleInfo {
        /**
         * 코드모듈ID
         */
        private String moduleId;

        /**
         * 모듈내용
         */
        private String moduleContent;

        /**
         * 모듈이름
         */
        private String moduleName;
    }

    @Data
    public static class DispatchGlobalParam {
        /**
         * 매개변수이름
         */
        private String varName;

        /**
         * 매개변수유형
         */
        private String varType;

        /**
         * 매개변수값
         */
        private String varValue;

        /**
         * 매개변수설명
         */
        private String varDescribe;
    }

    @Data
    public static class DispatchRequirement {
        /**
         * 패키지이름
         */
        private String packageName;

        /**
         * 버전
         */
        private String packageVersion;

        /**
         * 이미지주소
         */
        private String mirror;
    }

    @Data
    public static class DispatchParam {
        /**
         * 매개변수ID
         */
        private String id;

        /**
         * 입력/출력
         */
        private String varDirection;

        /**
         * 매개변수이름
         */
        private String varName;

        /**
         * 매개변수유형
         */
        private String varType;

        /**
         * 매개변수값
         */
        private String varValue;

        /**
         * 매개변수설명
         */
        private String varDescribe;

        /**
         * 프로세스ID
         */
        private String processId;
    }
}