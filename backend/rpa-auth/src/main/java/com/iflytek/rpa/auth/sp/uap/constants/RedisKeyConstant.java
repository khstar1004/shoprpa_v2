package com.iflytek.rpa.auth.sp.uap.constants;

public class RedisKeyConstant {
    public static final String REDIS_KEY_DEPT_CHILD_NODES_PREFIX = "dept:childrenNodes:";

    public static final String REDIS_KEY_DEPT_PERSON_CHILD_NODES_PREFIX = "dept:person:childrenNodes:";

    public static final String REDIS_KEY_DEPT_ALL_USER_PREFIX = "dept:user:user_name:";

    public static final String REDIS_KEY_DEPT_PREFIX = "dept:";

    public static final String REDIS_KEY_DEPT_USER_PREFIX = "dept:user:";

    public static final String REDIS_KEY_TENANT_USER_PREFIX = "tenant:user:";

    /**
     * 사용자session전
     * Key형식: user:session:{userId}
     * Value: sessionId
     */
    public static final String REDIS_KEY_USER_SESSION_PREFIX = "user:session:";

    /**
     * 테넌트빈있음상태전
     * Key형식: tenant:has_space:{tenantId}
     * Value: "true" 또는 "false"
     */
    public static final String REDIS_KEY_TENANT_HAS_SPACE_PREFIX = "tenant:has_space:";

    /**
     * 테넌트까지정보전
     * Key형식: tenant:expiration:{tenantId}
     * Value: TenantExpiration객체의JSON문자열
     */
    public static final String REDIS_KEY_TENANT_EXPIRATION_PREFIX = "tenant:expiration:";
}