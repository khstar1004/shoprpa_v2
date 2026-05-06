package com.iflytek.rpa.base.entity.vo;

import com.iflytek.rpa.base.entity.dto.CSmartComponentDto;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SmartComponentVo {
    /**
     * 가능컴포넌트Id
     */
    private String smartId;

    /**
     * 봇Id
     */
    private String robotId;

    /**
     * 컴포넌트버전내용
     */
    private CSmartComponentDto.SmartDetail detail;

    /**
     * 컴포넌트유형 web_auto | data_process
     */
    private String smartType;
}