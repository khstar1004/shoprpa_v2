package com.iflytek.rpa.auth.sp.casdoor.mapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @desc: Casdoor Organization 및통신사용 Tenant 유형의기기, 에서casdoor profile아래
 * 비고: Casdoor의Organization통신사용의Tenant
 * @author: Auto Generated
 * @create: 2025/12/11
 */
@Component
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = true)
public class CasdoorTenantMapper {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat(DATE_TIME_FORMAT);
    private static final SimpleDateFormat ISO_DATE_TIME_FORMATTER = new SimpleDateFormat(ISO_DATE_TIME_FORMAT);

    /**
     * 를 Casdoor Organization 변환로통신사용 Tenant
     *
     * @param casdoorOrg Casdoor 조직객체
     * @return 통신사용테넌트객체
     */
    public com.iflytek.rpa.auth.core.entity.Tenant toCommonTenant(org.casbin.casdoor.entity.Organization casdoorOrg) {
        if (casdoorOrg == null) {
            return null;
        }

        com.iflytek.rpa.auth.core.entity.Tenant tenant = new com.iflytek.rpa.auth.core.entity.Tenant();

        // 시사용name로id, 후가능필요근거서비스조정
        tenant.setId(casdoorOrg.name);

        // 본필드
        tenant.setName(
                casdoorOrg.displayName != null && !casdoorOrg.displayName.isEmpty()
                        ? casdoorOrg.displayName
                        : casdoorOrg.name); // 이름까지테넌트이름
        tenant.setTenantCode(casdoorOrg.name); // Casdoor의name로테넌트코드

        // 삭제필드0
        tenant.setIsDelete(0);

        // 날짜필드변환
        tenant.setCreateTime(parseDateTime(casdoorOrg.createdTime));
        // updateTime에서Casdoor Organization중있음필드, 로null또는및createTime
        tenant.setUpdateTime(parseDateTime(casdoorOrg.createdTime));

        // 값
        if (tenant.getStatus() == null) {
            tenant.setStatus(1); // 사용상태
        }
        if (tenant.getIsDefaultTenant() == null) {
            tenant.setIsDefaultTenant(false); // 아니요예테넌트
        }

        // 시가능으로저장까지remark중, 또는필요근거서비스
        if (casdoorOrg.owner != null && !casdoorOrg.owner.isEmpty()) {
            // 가능으로를owner정보저장까지remark중
            tenant.setRemark("owner:" + casdoorOrg.owner);
        }

        // creator필드시빈, 필요근거서비스
        // tenantType필드시빈, 필요근거서비스
        // websiteUrl, tags, languages대기필드가능으로저장까지remark중
        if (casdoorOrg.websiteUrl != null && !casdoorOrg.websiteUrl.isEmpty()) {
            String remark = tenant.getRemark();
            if (remark != null && !remark.isEmpty()) {
                tenant.setRemark(remark + "|websiteUrl:" + casdoorOrg.websiteUrl);
            } else {
                tenant.setRemark("websiteUrl:" + casdoorOrg.websiteUrl);
            }
        }

        // 결과가tags아니요비어 있습니다, 가능으로저장까지remark중
        if (casdoorOrg.tags != null && casdoorOrg.tags.length > 0) {
            String tagsStr = String.join(",", casdoorOrg.tags);
            String remark = tenant.getRemark();
            if (remark != null && !remark.isEmpty()) {
                tenant.setRemark(remark + "|tags:" + tagsStr);
            } else {
                tenant.setRemark("tags:" + tagsStr);
            }
        }

        return tenant;
    }

    /**
     * 를통신사용 Tenant 변환로 Casdoor Organization
     *
     * @param tenant 통신사용테넌트객체
     * @return Casdoor 조직객체
     */
    public org.casbin.casdoor.entity.Organization toCasdoorOrganization(
            com.iflytek.rpa.auth.core.entity.Tenant tenant) {
        if (tenant == null) {
            return null;
        }

        org.casbin.casdoor.entity.Organization casdoorOrg = new org.casbin.casdoor.entity.Organization();

        // 결과가tenantCode비어 있습니다, 가능필요사용id또는필드, 필요근거서비스조정
        if (tenant.getTenantCode() != null && !tenant.getTenantCode().isEmpty()) {
            casdoorOrg.name = tenant.getTenantCode();
        } else {
            casdoorOrg.name = tenant.getId() != null ? tenant.getId() : "";
        }

        // 이름, 결과가name비어 있습니다이면사용tenantCode
        casdoorOrg.displayName = tenant.getName() != null && !tenant.getName().isEmpty()
                ? tenant.getName()
                : (tenant.getTenantCode() != null ? tenant.getTenantCode() : "");

        // 삭제필드false
        casdoorOrg.enableSoftDeletion = false;

        // 날짜필드변환: Date -> String
        casdoorOrg.createdTime = formatDateTime(tenant.getCreateTime());
        // Casdoor Organization있음updatedTime필드, 사용createdTime

        // 시빈, 필요근거서비스
        casdoorOrg.owner = "";

        // 으로아래필드에서Casdoor Organization중필요값
        casdoorOrg.websiteUrl = "";
        casdoorOrg.favicon = "";
        casdoorOrg.passwordType = "";
        casdoorOrg.passwordSalt = "";
        casdoorOrg.passwordOptions = new String[0];
        casdoorOrg.countryCodes = new String[0];
        casdoorOrg.defaultAvatar = "";
        casdoorOrg.defaultApplication = "";
        casdoorOrg.tags = new String[0];
        casdoorOrg.languages = new String[0];
        casdoorOrg.themeData = null;
        casdoorOrg.masterPassword = "";
        casdoorOrg.initScore = 0;
        casdoorOrg.isProfilePublic = false;
        casdoorOrg.mfaItems = null;
        casdoorOrg.accountItems = null;

        // 시도에서remark중가져오기owner, websiteUrl및tags
        if (tenant.getRemark() != null && !tenant.getRemark().isEmpty()) {
            String remark = tenant.getRemark();
            if (remark.contains("owner:")) {
                String ownerPart = remark.substring(remark.indexOf("owner:") + 6);
                if (ownerPart.contains("|")) {
                    ownerPart = ownerPart.substring(0, ownerPart.indexOf("|"));
                }
                if (!ownerPart.isEmpty()) {
                    casdoorOrg.owner = ownerPart;
                }
            }
            if (remark.contains("websiteUrl:")) {
                String urlPart = remark.substring(remark.indexOf("websiteUrl:") + 11);
                if (urlPart.contains("|")) {
                    urlPart = urlPart.substring(0, urlPart.indexOf("|"));
                }
                if (!urlPart.isEmpty()) {
                    casdoorOrg.websiteUrl = urlPart;
                }
            }
            if (remark.contains("tags:")) {
                String tagsPart = remark.substring(remark.indexOf("tags:") + 5);
                if (tagsPart.contains("|")) {
                    tagsPart = tagsPart.substring(0, tagsPart.indexOf("|"));
                }
                if (!tagsPart.isEmpty()) {
                    casdoorOrg.tags = tagsPart.split(",");
                }
            }
        }

        return casdoorOrg;
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