package com.iflytek.rpa.component.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 컴포넌트이미지객체
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
public class ComponentVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 봇일id, 가져오기의사용id
     */
    private String componentId;

    /**
     * 현재이름문자, 사용목록 
     */
    private String name;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * editing 중, published 완료발송버전, shared 완료위, locked지정(불가)
     */
    private String transformStatus;

    /**
     * 컴포넌트의새버전, 결과가있음componentVersion, 비어 있습니다가능
     */
    private Integer version;
}