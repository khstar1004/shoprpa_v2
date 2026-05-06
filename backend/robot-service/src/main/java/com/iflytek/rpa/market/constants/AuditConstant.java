package com.iflytek.rpa.market.constants;

/**
 * @author mjren
 * @date 2025-07-02 11:19
 * @copyright Copyright (c) 2025 mjren
 */
public class AuditConstant {
    /**
     * 상태: 대기검토pending, 완료통신경과approved, 통과하지 못했습니다rejected, 완료판매canceled
     */
    public static final String AUDIT_STATUS_PENDING = "pending";

    public static final String AUDIT_STATUS_APPROVED = "approved";

    public static final String AUDIT_STATUS_REJECTED = "rejected";

    public static final String AUDIT_STATUS_CANCELED = "canceled";

    public static final String AUDIT_STATUS_NULLIFY = "nullify";

    /**
     * 검토열기닫기상태
     */
    public static final Short AUDIT_ENABLE_ON = 1; // 사용검토

    public static final Short AUDIT_ENABLE_OFF = 0; // 사용 안 함검토

    /**
     * 검토열기닫기상태문자열
     */
    public static final String AUDIT_ENABLE_STATUS_ON = "on"; // 사용

    public static final String AUDIT_ENABLE_STATUS_OFF = "off"; // 사용 안 함
}