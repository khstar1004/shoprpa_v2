package com.iflytek.rpa.auth.sp.casdoor.mapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @desc: Casdoor User 및통신사용 User 유형의기기, 에서casdoor profile아래
 * @author: Auto Generated
 * @create: 2025/12/11
 */
@Component
@ConditionalOnProperty(name = "rpa.auth.deployment-mode", havingValue = "casdoor", matchIfMissing = true)
public class CasdoorUserMapper {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat(DATE_TIME_FORMAT);
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(DATE_FORMAT);
    private static final SimpleDateFormat ISO_DATE_TIME_FORMATTER = new SimpleDateFormat(ISO_DATE_TIME_FORMAT);

    /**
     * 를 Casdoor User 변환로통신사용 User
     *
     * @param casdoorUser Casdoor 사용자객체
     * @return 통신사용사용자객체
     */
    public com.iflytek.rpa.auth.core.entity.User toCommonUser(org.casbin.casdoor.entity.User casdoorUser) {
        if (casdoorUser == null) {
            return null;
        }

        com.iflytek.rpa.auth.core.entity.User user = new com.iflytek.rpa.auth.core.entity.User();

        // 본필드
        user.setId(casdoorUser.id);
        user.setLoginName(casdoorUser.name); // Casdoor의name예로그인이름
        user.setName(casdoorUser.displayName); // 이름까지사용자이름
        user.setEmail(casdoorUser.email);
        user.setPhone(casdoorUser.phone != null ? casdoorUser.phone : "");

        // 이미지: 사용permanentAvatar, 사용avatar
        if (casdoorUser.permanentAvatar != null && !casdoorUser.permanentAvatar.isEmpty()) {
            user.setProfile(casdoorUser.permanentAvatar);
        } else if (casdoorUser.avatar != null && !casdoorUser.avatar.isEmpty()) {
            user.setProfile(casdoorUser.avatar);
        }

        // 날짜필드변환
        user.setBirthday(parseDate(casdoorUser.birthday));
        user.setCreateTime(parseDateTime(casdoorUser.createdTime));
        user.setUpdateTime(parseDateTime(casdoorUser.updatedTime));

        // 인증
        user.setIdNumber(casdoorUser.idCard);

        // 삭제필드변환: boolean -> Integer (true -> 1, false -> 0)
        user.setIsDelete(casdoorUser.isDeleted ? 1 : 0);

        // 상태필드변환: isForbidden (true -> status=0중지사용, false -> status=1사용)
        user.setStatus(casdoorUser.isForbidden ? 0 : 1);

        // 주소필드변환: String[] -> String
        if (casdoorUser.address != null && casdoorUser.address.length > 0) {
            user.setAddress(String.join(", ", casdoorUser.address));
        } else if (casdoorUser.location != null && !casdoorUser.location.isEmpty()) {
            user.setAddress(casdoorUser.location);
        }

        // todo 기기닫기필드 의기기의예casdoor의group name, 필요조정api조회
        user.setOrgId("group");
        user.setOrgCode("group");

        // 비고필드
        if (casdoorUser.bio != null && !casdoorUser.bio.isEmpty()) {
            user.setRemark(casdoorUser.bio);
        }

        // 테넌트필드: extInfo
        user.setExtInfo(casdoorUser.owner);

        // 정보: 에서properties중가져오기
        if (casdoorUser.properties != null && !casdoorUser.properties.isEmpty()) {
            // 가능으로를properties순서열로JSON문자열저장까지thirdExtInfo
            // 관리, 가져오기모듈분닫기 정보
            String thirdExtInfo = casdoorUser.properties.toString();
            user.setThirdExtInfo(thirdExtInfo);
        }

        // 값
        if (user.getUserSource() == null) {
            user.setUserSource(1); // 사용자로1
        }

        return user;
    }

    /**
     * 를통신사용 User 변환로 Casdoor User
     *
     * @param user 통신사용사용자객체
     * @return Casdoor 사용자객체
     */
    public org.casbin.casdoor.entity.User toCasdoorUser(com.iflytek.rpa.auth.core.entity.User user) {
        if (user == null) {
            return null;
        }

        org.casbin.casdoor.entity.User casdoorUser = new org.casbin.casdoor.entity.User();

        // 본필드
        casdoorUser.id = user.getId();
        casdoorUser.name = user.getLoginName(); // 로그인이름까지Casdoor의name
        casdoorUser.displayName = user.getName(); // 사용자이름까지이름
        casdoorUser.email = user.getEmail();
        casdoorUser.phone = user.getPhone() != null ? user.getPhone() : "";

        // 이미지
        if (user.getProfile() != null && !user.getProfile().isEmpty()) {
            casdoorUser.avatar = user.getProfile();
            casdoorUser.permanentAvatar = user.getProfile();
        }

        // 날짜필드변환: Date -> String
        casdoorUser.birthday = formatDate(user.getBirthday());
        casdoorUser.createdTime = formatDateTime(user.getCreateTime());
        casdoorUser.updatedTime = formatDateTime(user.getUpdateTime());

        // 인증
        casdoorUser.idCard = user.getIdNumber();

        // 삭제필드변환: Integer -> boolean (1 -> true, 0 -> false)
        casdoorUser.isDeleted = user.getIsDelete() != null && user.getIsDelete() == 1;

        // 상태필드변환: status (0중지사용 -> isForbidden=true, 1사용 -> isForbidden=false)
        casdoorUser.isForbidden = user.getStatus() != null && user.getStatus() == 0;

        // 주소필드변환: String -> String[]
        if (user.getAddress() != null && !user.getAddress().isEmpty()) {
            // 결과가패키지, 분;아니요이면로단일개요소
            if (user.getAddress().contains(",")) {
                casdoorUser.address = Arrays.stream(user.getAddress().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
            } else {
                casdoorUser.address = new String[] {user.getAddress()};
            }
            casdoorUser.location = user.getAddress();
        }

        // 테넌트
        casdoorUser.owner = user.getExtInfo();

        // todo 기기(그룹)
        casdoorUser.region = user.getOrgId();
        casdoorUser.region = user.getOrgCode();

        // 비고필드
        if (user.getRemark() != null && !user.getRemark().isEmpty()) {
            casdoorUser.bio = user.getRemark();
        }

        // 정보: 저장까지properties중
        // 관리, 결과가필요변경복사의, 가능으로일
        if (user.getExtInfo() != null && !user.getExtInfo().isEmpty()) {
            // properties예Map유형, 가능으로근거필요행파싱
            // 시빈, 필요시
        }

        // 값
        casdoorUser.password = "";
        casdoorUser.passwordSalt = "";
        casdoorUser.firstName = "";
        casdoorUser.lastName = "";
        casdoorUser.affiliation = "";
        casdoorUser.title = "";
        casdoorUser.idCardType = "";
        casdoorUser.homepage = "";
        casdoorUser.tag = "";
        casdoorUser.language = "";
        casdoorUser.gender = "";
        casdoorUser.education = "";
        casdoorUser.score = 0;
        casdoorUser.karma = 0;
        casdoorUser.ranking = 0;
        casdoorUser.isDefaultAvatar = false;
        casdoorUser.isOnline = false;
        casdoorUser.isAdmin = false;
        casdoorUser.isGlobalAdmin = false;
        casdoorUser.hash = "";
        casdoorUser.preHash = "";
        casdoorUser.createdIp = "";
        casdoorUser.lastSigninTime = "";
        casdoorUser.lastSigninIp = "";

        // 삼방법로그인닫기필드빈
        casdoorUser.github = "";
        casdoorUser.google = "";
        casdoorUser.qq = "";
        casdoorUser.wechat = "";
        casdoorUser.facebook = "";
        casdoorUser.dingtalk = "";
        casdoorUser.weibo = "";
        casdoorUser.gitee = "";
        casdoorUser.linkedin = "";
        casdoorUser.wecom = "";
        casdoorUser.lark = "";
        casdoorUser.gitlab = "";
        casdoorUser.adfs = "";
        casdoorUser.baidu = "";
        casdoorUser.alipay = "";
        casdoorUser.casdoor = "";
        casdoorUser.infoflow = "";
        casdoorUser.apple = "";
        casdoorUser.azuread = "";
        casdoorUser.slack = "";
        casdoorUser.steam = "";
        casdoorUser.bilibili = "";
        casdoorUser.okta = "";
        casdoorUser.douyin = "";
        casdoorUser.custom = "";
        casdoorUser.ldap = "";
        // 필드
        casdoorUser.type = "normal-user";
        return casdoorUser;
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
            // 시도날짜형식 (yyyy-MM-dd)
            if (dateTimeStr.length() >= DATE_FORMAT.length()) {
                String dStr = dateTimeStr.substring(0, DATE_FORMAT.length());
                synchronized (DATE_FORMATTER) {
                    return DATE_FORMATTER.parse(dStr);
                }
            }
        } catch (ParseException e) {
            // 파싱실패, 반환null
        }
        return null;
    }

    /**
     * 파싱날짜문자열로Date객체
     *
     * @param dateStr 날짜문자열
     * @return Date객체, 파싱실패반환null
     */
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            synchronized (DATE_FORMATTER) {
                return DATE_FORMATTER.parse(dateStr);
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

    /**
     * 형식Date객체로날짜문자열
     *
     * @param date Date객체
     * @return 날짜문자열, date로null반환빈문자열
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        synchronized (DATE_FORMATTER) {
            return DATE_FORMATTER.format(date);
        }
    }
}