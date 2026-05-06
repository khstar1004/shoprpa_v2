package com.iflytek.rpa.market.entity.dto;

import com.iflytek.rpa.market.entity.AppMarketResource;
import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MarketResourceDto extends AppMarketResource {

    //    @NotNull(message = "식별자비워 둘 수 없습니다")
    private Integer editFlag;

    @NotBlank(message = "행분유형비워 둘 수 없습니다")
    private String category;

    @NotBlank(message = "봇id비워 둘 수 없습니다")
    private String robotId;

    //    @NotNull(message = "버전비워 둘 수 없습니다")
    private Integer version;

    private List<String> obtainDirection;

    private List<String> marketIdList;

    private List<AppMarketResource> appInsertInfoList;

    private List<AppMarketResource> appUpdateInfoList;
}