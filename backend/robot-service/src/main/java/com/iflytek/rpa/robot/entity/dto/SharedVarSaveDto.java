package com.iflytek.rpa.robot.entity.dto;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 공유 변수저장DTO
 *
 * @author jqfang3
 * @since 2025-07-21
 */
@Data
public class SharedVarSaveDto {

    /**
     * 변수이름
     */
    @NotBlank(message = "변수이름비워 둘 수 없습니다")
    private String sharedVarName;

    /**
     * 모듈ID
     */
    @NotBlank(message = "모듈비워 둘 수 없습니다")
    private String deptId;

    /**
     * 변수유형: text/password/array/group
     */
    @NotBlank(message = "변수유형비워 둘 수 없습니다")
    private String varType;

    /**
     * 사용상태: 1사용, 0사용 안 함
     */
    @NotNull(message = "사용상태비워 둘 수 없습니다")
    private Integer status;

    /**
     * 변수설명
     */
    private String remark;

    /**
     * 가능사용계정유형(all/dept/select)
     */
    @NotBlank(message = "가능사용계정비워 둘 수 없습니다")
    private String usageType;

    /**
     * 완료선택의사용자목록
     */
    @Valid
    private List<SelectedUser> selectedUserList;

    /**
     * 변수그룹목록, 아니요예변수그룹유형, 일개요소의목록
     */
    @Valid
    private List<VarGroupItem> varList;

    /**
     * 선택중의사용자 정보
     */
    @Data
    public static class SelectedUser {
        @NotBlank(message = "userId비워 둘 수 없습니다")
        private String userId; // 사용자ID

        @NotBlank(message = "userName비워 둘 수 없습니다")
        private String userName; // 사용자명

        @NotBlank(message = "userPhone비워 둘 수 없습니다")
        private String userPhone; // 사용자휴대폰 번호
    }

    /**
     * 변수그룹
     */
    @Data
    public static class VarGroupItem {
        @NotBlank(message = "변수이름비워 둘 수 없습니다")
        private String varName; // 변수이름

        @NotBlank(message = "변수유형비워 둘 수 없습니다")
        private String varType; // 변수유형 text/password/array

        private String varValue; // 변수값
        private Integer encrypt; // 여부암호화: 1-암호화, 0-아니요암호화
    }
}