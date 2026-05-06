package com.iflytek.rpa.notify.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

@Data
public class NotifyVo {
    Long id;
    String messageInfo;
    String messageType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    // 결과, 미완료1,  완료2, 완료추가입력3, 완료4
    private Integer operateResult;

    String appName;

    String marketId;
}