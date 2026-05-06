package com.iflytek.rpa.dispatch.entity.vo;

import java.util.List;
import lombok.Data;

@Data
public class DispatchTaskExecuteRecordListVo {

    /**
     * 작업실행ID
     */
    private String dispatchTaskExecuteId;

    /**
     * 작업ID
     */
    private String dispatchTaskId;

    /**
     * 단말정보
     */
    private TerminalInfo terminalInfo;

    /**
     * 작업이름
     */
    private String taskName;

    /**
     * 작업실행
     */
    private Integer count;

    /**
     * 작업유형 : manual, 예약: schedule, 메일: mail, 파일: file, : hotKey
     */
    private String dispatchTaskType;

    /**
     * 작업시작 시간
     */
    private String taskStartTime;

    /**
     * 작업종료 시간
     */
    private String taskEndTime;

    /**
     * 작업실행시
     */
    private Long taskExecuteTime;

    /**
     * 작업상태: 
     * 성공: success, 시작 실패: start_error, 실행실패: exe_error, 가져오기 : cancel, 실행중: executing
     */
    private String taskExecuteStatus;

    /**
     * 봇실행기록목록
     */
    private List<DispatchTaskRobotExecuteRecordVo> robotExecuteRecordList;

    /**
     * 단말정보내부모듈유형
     */
    @Data
    public static class TerminalInfo {
        /**
         * 단말ID
         */
        private String terminalId;

        /**
         * 사용자명
         */
        private String userName;

        /**
         * 단말운영체제
         */
        private String os;

        /**
         * 단말준비사용자명
         */
        private String osName;

        /**
         * 단말운영체제비밀번호
         */
        private String osPwd;

        /**
         * 단말이름
         */
        private String terminalName;

        /**
         * 단말단말
         */
        private Integer port;

        /**
         * 단말지정단말
         */
        private Integer customPort;

        /**
         * 단말IP
         */
        private String ip;

        /**
         * 단말지정IP
         */
        private String customIp;

        /**
         * 단말클라이언트IP
         */
        private String actualClientIp;
    }
}