package com.iflytek.rpa.robot.entity.dto;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 공유 변수업데이트DTO
 *
 * @author jqfang3
 * @since 2025-07-21
 */
@Data
public class SharedVarBatchDto {

    /**
     * 공유 변수ID List
     */
    @NotNull(message = "공유 변수ID-List비워 둘 수 없습니다")
    private List<Long> ids;
}