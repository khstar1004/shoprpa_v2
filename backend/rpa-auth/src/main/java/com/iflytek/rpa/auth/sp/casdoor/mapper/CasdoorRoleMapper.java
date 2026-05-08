package com.iflytek.rpa.auth.sp.casdoor.mapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @desc: Casdoor Role 및통신사용 Role 유형의기기, 에서casdoor profile아래
 * @author: Auto Generated
 * @create: 2025/12/11
 */
@Component
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = true)
public class CasdoorRoleMapper {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat(DATE_TIME_FORMAT);
    private static final SimpleDateFormat ISO_DATE_TIME_FORMATTER = new SimpleDateFormat(ISO_DATE_TIME_FORMAT);

    /**
     * 를 Casdoor Role 변환로통신사용 Role
     *
     * @param casdoorRole Casdoor 역할객체
     * @return 통신사용역할객체
     */
    public com.iflytek.rpa.auth.core.entity.Role toCommonRole(org.casbin.casdoor.entity.Role casdoorRole) {
        if (casdoorRole == null) {
            return null;
        }

        com.iflytek.rpa.auth.core.entity.Role role = new com.iflytek.rpa.auth.core.entity.Role();

        // 시사용name로id, 후가능필요근거서비스조정
        role.setId(casdoorRole.name);

        // 본필드
        // 이름까지역할이름, 결과가비어 있습니다이면사용name
        role.setName(
                casdoorRole.displayName != null && !casdoorRole.displayName.isEmpty()
                        ? casdoorRole.displayName
                        : casdoorRole.name);
        role.setCode(casdoorRole.name); // Casdoor의name로역할코드

        // 상태필드변환: isEnabled (true -> status=1사용, false -> status=0중지사용)
        role.setStatus(casdoorRole.isEnabled ? 1 : 0);

        // owner가능테이블사용/테넌트식별자, 필요서비스
        role.setAppId("");

        // 비고필드
        role.setRemark(casdoorRole.description);

        // 날짜필드변환
        role.setCreateTime(parseDateTime(casdoorRole.createdTime));
        // updateTime에서Casdoor중있음필드, 로null또는및createTime
        role.setUpdateTime(parseDateTime(casdoorRole.createdTime));

        // 값
        if (role.getSort() == null) {
            role.setSort(1); // 정렬로1
        }
        if (role.getIsMustBind() == null) {
            role.setIsMustBind(1); // 강함지정
        }

        // 으로아래필드에서Casdoor중있음, 로null또는값
        // higherRole, higherName, firstLevelId, appName 필요근거서비스

        return role;
    }

    /**
     * 를통신사용 Role 변환로 Casdoor Role
     *
     * @param role 통신사용역할객체
     * @return Casdoor 역할객체
     */
    public org.casbin.casdoor.entity.Role toCasdoorRole(com.iflytek.rpa.auth.core.entity.Role role) {
        if (role == null) {
            return null;
        }

        org.casbin.casdoor.entity.Role casdoorRole = new org.casbin.casdoor.entity.Role();

        // 결과가code비어 있습니다, 가능필요사용id또는필드, 필요근거서비스조정
        if (role.getCode() != null && !role.getCode().isEmpty()) {
            casdoorRole.name = role.getCode();
        } else {
            casdoorRole.name = role.getId() != null ? role.getId() : "";
        }

        // 이름, 결과가name비어 있습니다이면사용code
        casdoorRole.displayName = role.getName() != null && !role.getName().isEmpty()
                ? role.getName()
                : (role.getCode() != null ? role.getCode() : "");

        // 상태필드변환: status (1사용 -> isEnabled=true, 0중지사용 -> isEnabled=false)
        casdoorRole.isEnabled = role.getStatus() != null && role.getStatus() == 1;

        // owner에서Casdoor예역할의조직, 통신사용있음조직정보, 원인빈
        casdoorRole.owner = "";

        // 비고필드
        casdoorRole.description = role.getRemark() != null ? role.getRemark() : "";

        // 날짜필드변환: Date -> String
        casdoorRole.createdTime = formatDateTime(role.getCreateTime());
        // Casdoor Role있음updatedTime필드, 사용createdTime

        // 으로아래필드에서Casdoor Role중필요값
        casdoorRole.users = new String[0];
        casdoorRole.roles = new String[0];

        return casdoorRole;
    }

    /**
     * 파싱날짜시간문자열로Date객체
     *
     * @param dateTimeStr 날짜시간문자열
     * @return Date객체, 파싱실패반환null
     */
    private Date parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            // 시도ISO형식 (yyyy-MM-dd'T'HH:mm:ss)
            if (dateTimeStr.contains("T")) {
                String isoStr = dateTimeStr;
                // 제거시정보(결과가있음)
                if (isoStr.contains("+") || isoStr.endsWith("Z")) {
                    isoStr = isoStr.replaceAll("[+Z].*", "");
                }
                // 시도파싱ISO형식
                if (isoStr.length() >= 19) {
                    isoStr = isoStr.substring(0, 19);
                    synchronized (ISO_DATE_TIME_FORMATTER) {
                        return ISO_DATE_TIME_FORMATTER.parse(isoStr);
                    }
                }
            }
            // 시도날짜시간형식 (yyyy-MM-dd HH:mm:ss)
            if (dateTimeStr.length() >= DATE_TIME_FORMAT.length()) {
                String dtStr = dateTimeStr.substring(0, DATE_TIME_FORMAT.length());
                synchronized (DATE_TIME_FORMATTER) {
                    return DATE_TIME_FORMATTER.parse(dtStr);
                }
            }
        } catch (ParseException e) {
            // 파싱실패, 반환null
        }
        return null;
    }

    /**
     * 형식Date객체로날짜시간문자열
     *
     * @param date Date객체
     * @return 날짜시간문자열, date로null반환빈문자열
     */
    private String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        synchronized (DATE_TIME_FORMATTER) {
            return DATE_TIME_FORMATTER.format(date);
        }
    }
}