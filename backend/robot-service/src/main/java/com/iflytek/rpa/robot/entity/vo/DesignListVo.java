package com.iflytek.rpa.robot.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

@Data
public class DesignListVo {
    String robotName; // 봇이름

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date updateTime; // 수정 시간

    String publishStatus; // 게시상태 editing published shared locked
    /**
     * 위신청상태 pending:신청중 approved:신청완료통신경과 none:없음신청기록 null:위검토미완료열기시작
     */
    String applicationStatus;

    Integer version; // 사용버전
    Integer latestVersion; // 새버전
    String iconUrl; // 아이콘이름문자
    String robotId; // 봇id
    Integer editEnable; // 여부허용 1가능, 0할 수 없음
}