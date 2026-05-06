package com.iflytek.rpa.component.constants;

/**
 * 봇컴포넌트닫기일반량
 *
 * @author makejava
 * @since 2024-12-19
 */
public class ComponentRobotBlockConstant {

    /**
     * 테이블이름
     */
    public static final String TABLE_NAME = "component_robot_block";

    /**
     * 삭제상태 - 삭제되지 않음
     */
    public static final Integer DELETED_NO = 0;

    /**
     * 삭제상태 - 삭제됨
     */
    public static final Integer DELETED_YES = 1;

    /**
     * 봇버전
     */
    public static final Integer DEFAULT_ROBOT_VERSION = 1;

    /**
     * 량대수제한제어
     */
    public static final Integer MAX_BATCH_SIZE = 100;

    /**
     * 오류메시지
     */
    public static class ErrorMessage {
        /**
         * 봇ID비워 둘 수 없습니다
         */
        public static final String ROBOT_ID_EMPTY = "봇ID비워 둘 수 없습니다";

        /**
         * 컴포넌트ID비워 둘 수 없습니다
         */
        public static final String COMPONENT_ID_EMPTY = "컴포넌트ID비워 둘 수 없습니다";

        /**
         * 봇버전비워 둘 수 없습니다
         */
        public static final String ROBOT_VERSION_EMPTY = "봇버전비워 둘 수 없습니다";

        /**
         * 량수초과출력제한제어
         */
        public static final String BATCH_SIZE_EXCEEDED = "량수할 수 없음초과경과" + MAX_BATCH_SIZE + "개";

        /**
         * 기록완료저장에서
         */
        public static final String BLOCK_ALREADY_EXISTS = "해당컴포넌트완료해당봇";

        /**
         * 기록을 찾을 수 없습니다
         */
        public static final String BLOCK_NOT_EXISTS = "찾을 수 없는 기록";
    }

    /**
     * 성공메시지
     */
    public static class SuccessMessage {
        /**
         * 추가완료
         */
        public static final String ADD_SUCCESS = "추가완료";

        /**
         * 제거완료
         */
        public static final String REMOVE_SUCCESS = "제거완료";

        /**
         * 량추가완료
         */
        public static final String BATCH_ADD_SUCCESS = "량추가완료";

        /**
         * 량제거완료
         */
        public static final String BATCH_REMOVE_SUCCESS = "량제거완료";
    }
}