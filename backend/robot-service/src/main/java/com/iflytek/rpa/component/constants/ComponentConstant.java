package com.iflytek.rpa.component.constants;

/**
 * 컴포넌트일반량
 *
 * @author makejava
 * @since 2024-12-19
 */
public class ComponentConstant {

    /**
     * 상태: 대기가져오기
     */
    public static final String RESOURCE_STATUS_TO_OBTAIN = "toObtain";

    /**
     * 상태: 완료가져오기
     */
    public static final String RESOURCE_STATUS_OBTAINED = "obtained";

    /**
     * 상태: 대기업데이트
     */
    public static final String RESOURCE_STATUS_TO_UPDATE = "toUpdate";

    /**
     * 데이터: 생성
     */
    public static final String DATA_SOURCE_CREATE = "create";

    /**
     * 데이터: 마켓가져오기
     */
    public static final String DATA_SOURCE_MARKET = "market";

    /**
     * 변환상태: 중
     */
    public static final String TRANSFORM_STATUS_EDITING = "editing";

    /**
     * 변환상태: 완료발송버전
     */
    public static final String TRANSFORM_STATUS_PUBLISHED = "published";

    /**
     * 변환상태: 완료위
     */
    public static final String TRANSFORM_STATUS_SHARED = "shared";

    /**
     * 변환상태: 지정
     */
    public static final String TRANSFORM_STATUS_LOCKED = "locked";

    /**
     * 여부: 아니요
     */
    public static final Integer IS_SHOWN_NO = 0;

    /**
     * 여부: 
     */
    public static final Integer IS_SHOWN_YES = 1;

    /**
     * 삭제 여부: 삭제되지 않음
     */
    public static final Integer DELETED_NO = 0;

    /**
     * 삭제 여부: 삭제됨
     */
    public static final Integer DELETED_YES = 1;
}