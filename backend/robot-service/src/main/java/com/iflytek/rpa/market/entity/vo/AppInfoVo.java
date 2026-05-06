package com.iflytek.rpa.market.entity.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AppInfoVo {
    String appName;
    Long downloadNum;
    Long checkNum;
    String appIntro; // 사용
    Integer allowOperate; // 버튼 0 아니요허용 ; 1 허용
    Integer obtainStatus; // 0: 가져오기 1: 다시 가져오기
    Integer updateStatus; // 0: 아니요안내업데이트 1: 안내업데이트
    String appId;
    String marketId;
    String resourceUuid; // 의resourceId, 결과가있음가져오기 개필드비어 있습니다
    String iconUrl; // 사용아이콘
    String securityLevel; // 비밀단계식별자
    Date expiryDate; // 색상비밀단계식별자의중지시간
    String expiryDateStr; // 색상비밀단계식별자의중지시간안내
    private Boolean editFlag;
}