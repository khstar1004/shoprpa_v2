package com.iflytek.rpa.robot.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

@Data
public class ExecuteListVo {
    String robotName; // 봇이름

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date updateTime; // 수정 시간

    Integer version; // 버전
    String sourceName; // 이름 : 마켓이름(팀마켓이름또는방법마켓), 본
    String robotId; // 봇
    Integer appVersion; // 결과가예에서마켓가져오기, appVersion
    String appId; // 결과가예에서마켓가져오기, appId
    Integer updateStatus; // 여부있음업데이트 식별자위치 1 예 0 없음
    Integer usePermission; // 사용권한 0 없음 1 있음
    Date expiryDate; // 색상비밀단계식별자의중지시간
    String expiryDateStr; // 색상비밀단계식별자의중지시간안내
}