package com.iflytek.rpa.base.entity.dto;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 새기존가능조회DTO
 */
@Data
public class CAtomMetaNewListDto {

    @NotEmpty(message = "keys비워 둘 수 없습니다")
    private List<String> keys;
}