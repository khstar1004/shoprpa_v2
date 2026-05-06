package com.iflytek.rpa.base.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iflytek.rpa.base.entity.CParam;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author tzzhang
 * @date 2025/3/25 17:07
 */
@Data
public class CParamListDto {
    @JsonProperty("paramList")
    private List<CParam> paramList = new ArrayList<>();

    @NotBlank
    private String robotId;

    //    /**
    //     * 실행위치, , EDIT_PAGE,PROJECT_LIST계획기기목록 ,EXECUTOR실행기기봇목록 ,CRONTAB트리거기기(본예약 작업)
    //     */
    //    @NotBlank(message = "실행위치비워 둘 수 없습니다")
    //    @Pattern(regexp = "EDIT_PAGE|PROJECT_LIST|EXECUTOR|CRONTAB", message =
    // "매개변수값예EDIT_PAGE|PROJECT_LIST|EXECUTOR|CRONTAB")
    //    private String mode = EDIT_PAGE;

}