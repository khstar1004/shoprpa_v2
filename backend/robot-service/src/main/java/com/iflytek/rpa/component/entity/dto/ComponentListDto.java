package com.iflytek.rpa.component.entity.dto;

import javax.validation.constraints.Min;
import lombok.Data;

/**
 * 컴포넌트목록조회DTO
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
public class ComponentListDto {

    String sortType = "desc"; // asc desc

    /**
     * 코드
     */
    @Min(value = 1, message = "코드대0")
    private Integer pageNum = 1;

    /**
     * 매크기
     */
    @Min(value = 1, message = "매크기대0")
    private Integer pageSize = 10;

    /**
     * 컴포넌트이름(조회)
     */
    private String name;

    /**
     * 데이터 create:생성의 ; market:마켓
     */
    private String dataSource = "create";
}