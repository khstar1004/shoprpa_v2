package com.iflytek.rpa.base.entity.dto;

import com.iflytek.rpa.base.entity.CProcess;
import lombok.Data;

@Data
public class CProcessDto extends CProcess {

    private String robotId;

    private String processId;

    private String processJson;

    /**
     * 지정에서개실행
     */
    private String mode;
}