package com.iflytek.rpa.auth.sp.casdoor.mapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @desc: Casdoor Group 및통신사용 Org 유형의기기, 에서casdoor profile아래
 * 비고: Casdoor의Organization통신사용의Tenant, Casdoor의Group통신사용의Org
 * @author: Auto Generated
 * @create: 2025/12/11
 */
@Component
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = true)
public class CasdoorOrganizationMapper {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat(DATE_TIME_FORMAT);
    private static final SimpleDateFormat ISO_DATE_TIME_FORMATTER = new SimpleDateFormat(ISO_DATE_TIME_FORMAT);

    /**
     * 를 Casdoor Group 변환로통신사용 Org
     *
     * @param casdoorGroup Casdoor 그룹객체
     * @return 통신사용조직객체
     */
    public com.iflytek.rpa.auth.core.entity.Org toCommonOrg(org.casbin.casdoor.entity.Group casdoorGroup) {
        if (casdoorGroup == null) {
            return null;
        }

        com.iflytek.rpa.auth.core.entity.Org org = new com.iflytek.rpa.auth.core.entity.Org();

        // TODO: id필드필요근거서비스완료, Casdoor의name가능로일식별자
        // 시사용name로id, 후가능필요근거서비스조정
        org.setId(casdoorGroup.name);

        // 본필드
        org.setName(
                casdoorGroup.displayName != null && !casdoorGroup.displayName.isEmpty()
                        ? casdoorGroup.displayName
                        : casdoorGroup.name); // 이름까지기기이름
        org.setCode(casdoorGroup.name); // Casdoor의name로기기코드

        // 기기유형
        org.setOrgType(casdoorGroup.type);

        // 위단계기기
        org.setHigherOrg(casdoorGroup.parentId);

        // 기기명칭: 사용title
        org.setShortName(casdoorGroup.title);

        // 상태필드변환: isEnabled (true -> status=1사용, false -> status=0중지사용)
        org.setStatus(casdoorGroup.isEnabled ? 1 : 0);

        // 날짜필드변환
        org.setCreateTime(parseDateTime(casdoorGroup.createdTime));
        org.setUpdateTime(parseDateTime(casdoorGroup.updatedTime));

        // TODO: isTopGroup필드필요근거서비스관리, 가능사용level또는firstLevelId
        // 결과가예단계그룹, 가능필요level=1또는firstLevelId
        if (casdoorGroup.isTopGroup) {
            org.setLevel(1);
            // TODO: firstLevelId가능필요로현재id, 필요서비스
            org.setFirstLevelId(casdoorGroup.name);
        }

        // 값
        if (org.getSort() == null) {
            org.setSort(1); // 정렬로1
        }

        // 삭제필드: Group있음필드, 로0(삭제되지 않음)
        org.setIsDelete(0);

        // 정보: 를manager, contactEmail, users, key대기정보저장까지extInfo
        StringBuilder extInfoBuilder = new StringBuilder();
        if (casdoorGroup.manager != null && !casdoorGroup.manager.isEmpty()) {
            extInfoBuilder.append("manager:").append(casdoorGroup.manager);
        }
        if (casdoorGroup.contactEmail != null && !casdoorGroup.contactEmail.isEmpty()) {
            if (extInfoBuilder.length() > 0) {
                extInfoBuilder.append("|");
            }
            extInfoBuilder.append("contactEmail:").append(casdoorGroup.contactEmail);
        }
        if (casdoorGroup.users != null && !casdoorGroup.users.isEmpty()) {
            if (extInfoBuilder.length() > 0) {
                extInfoBuilder.append("|");
            }
            extInfoBuilder.append("users:").append(String.join(",", casdoorGroup.users));
        }
        if (casdoorGroup.key != null && !casdoorGroup.key.isEmpty()) {
            if (extInfoBuilder.length() > 0) {
                extInfoBuilder.append("|");
            }
            extInfoBuilder.append("key:").append(casdoorGroup.key);
        }
        if (extInfoBuilder.length() > 0) {
            org.setExtInfo(extInfoBuilder.toString());
        }

        // TODO: owner필드필요근거서비스지정, 가능테이블테넌트식별자
        // 시가능으로저장까지thirdExtInfo중
        if (casdoorGroup.owner != null && !casdoorGroup.owner.isEmpty()) {
            org.setThirdExtInfo("owner:" + casdoorGroup.owner);
        }

        // TODO: children필드필요근거서비스관리, 가능필요관리조직
        // 시아니요관리, 필요서비스

        // 으로아래필드에서Casdoor Group중있음, 로null또는값
        // province, provinceCode, city, cityCode, district, districtCode
        // orgTypeName, orgTypeCode
        // higherName, levelCode
        // remark 가능으로시도에서필드, 결과가있음이면빈

        return org;
    }

    /**
     * 를통신사용 Org 변환로 Casdoor Group
     *
     * @param org 통신사용조직객체
     * @return Casdoor 그룹객체
     */
    public org.casbin.casdoor.entity.Group toCasdoorGroup(com.iflytek.rpa.auth.core.entity.Org org) {
        if (org == null) {
            return null;
        }

        org.casbin.casdoor.entity.Group casdoorGroup = new org.casbin.casdoor.entity.Group();

        // TODO: name필드필요, 사용code로name(기기코드로Casdoor의name)
        // 결과가code비어 있습니다, 가능필요사용id또는필드, 필요근거서비스조정
        if (org.getCode() != null && !org.getCode().isEmpty()) {
            casdoorGroup.name = org.getCode();
        } else {
            // TODO: 결과가code비어 있습니다, 사용id로name, 필요서비스
            casdoorGroup.name = org.getId() != null ? org.getId() : "";
        }

        // 이름, 결과가name비어 있습니다이면사용code
        casdoorGroup.displayName = org.getName() != null && !org.getName().isEmpty()
                ? org.getName()
                : (org.getCode() != null ? org.getCode() : "");

        // 기기유형
        casdoorGroup.type = org.getOrgType() != null ? org.getOrgType() : "";

        // 위단계기기
        casdoorGroup.parentId = org.getHigherOrg();

        // title필드: 사용shortName
        casdoorGroup.title = org.getShortName();

        // 상태필드변환: status (1사용 -> isEnabled=true, 0중지사용 -> isEnabled=false)
        casdoorGroup.isEnabled = org.getStatus() != null && org.getStatus() == 1;

        // 날짜필드변환: Date -> String
        casdoorGroup.createdTime = formatDateTime(org.getCreateTime());
        casdoorGroup.updatedTime = formatDateTime(org.getUpdateTime());

        // TODO: isTopGroup필드필요근거서비스, 가능근거level또는firstLevelId
        // 결과가level=1또는firstLevelId대기현재id, 가능예단계그룹
        casdoorGroup.isTopGroup = (org.getLevel() != null && org.getLevel() == 1)
                || (org.getFirstLevelId() != null && org.getFirstLevelId().equals(org.getId()));

        // TODO: owner필드필요서비스, 가능으로에서thirdExtInfo중파싱
        // 시빈, 필요근거서비스
        casdoorGroup.owner = "";

        // 으로아래필드에서Casdoor Group중필요값
        casdoorGroup.manager = ""; // TODO: 가능으로에서extInfo중파싱manager, 필요근거서비스
        casdoorGroup.contactEmail = ""; // TODO: 가능으로에서extInfo중파싱contactEmail, 필요근거서비스
        casdoorGroup.users = new ArrayList<>(); // TODO: 가능으로에서extInfo중파싱users, 필요근거서비스
        casdoorGroup.key = ""; // TODO: 가능으로에서extInfo중파싱key, 필요근거서비스
        casdoorGroup.children = new ArrayList<>(); // TODO: 필요근거서비스조직목록

        // 시도에서extInfo중가져오기manager, contactEmail, users, key대기정보
        if (org.getExtInfo() != null && !org.getExtInfo().isEmpty()) {
            String extInfo = org.getExtInfo();
            if (extInfo.contains("manager:")) {
                String managerPart = extInfo.substring(extInfo.indexOf("manager:") + 8);
                if (managerPart.contains("|")) {
                    managerPart = managerPart.substring(0, managerPart.indexOf("|"));
                }
                if (!managerPart.isEmpty()) {
                    casdoorGroup.manager = managerPart;
                }
            }
            if (extInfo.contains("contactEmail:")) {
                String emailPart = extInfo.substring(extInfo.indexOf("contactEmail:") + 13);
                if (emailPart.contains("|")) {
                    emailPart = emailPart.substring(0, emailPart.indexOf("|"));
                }
                if (!emailPart.isEmpty()) {
                    casdoorGroup.contactEmail = emailPart;
                }
            }
            if (extInfo.contains("users:")) {
                String usersPart = extInfo.substring(extInfo.indexOf("users:") + 6);
                if (usersPart.contains("|")) {
                    usersPart = usersPart.substring(0, usersPart.indexOf("|"));
                }
                if (!usersPart.isEmpty()) {
                    casdoorGroup.users = Arrays.asList(usersPart.split(","));
                }
            }
            if (extInfo.contains("key:")) {
                String keyPart = extInfo.substring(extInfo.indexOf("key:") + 4);
                if (keyPart.contains("|")) {
                    keyPart = keyPart.substring(0, keyPart.indexOf("|"));
                }
                if (!keyPart.isEmpty()) {
                    casdoorGroup.key = keyPart;
                }
            }
        }

        // 시도에서thirdExtInfo중파싱owner
        if (org.getThirdExtInfo() != null && org.getThirdExtInfo().contains("owner:")) {
            String ownerPart =
                    org.getThirdExtInfo().substring(org.getThirdExtInfo().indexOf("owner:") + 6);
            if (!ownerPart.isEmpty()) {
                casdoorGroup.owner = ownerPart;
            }
        }

        return casdoorGroup;
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