package com.iflytek.rpa.component.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

/**
 * 컴포넌트테이블(Component)유형
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Component implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 기본 키id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 봇일id, 가져오기의사용id
     */
    private String componentId;

    /**
     * 현재이름문자, 사용목록 
     */
    @NotBlank(message = "컴포넌트이름비워 둘 수 없습니다")
    @Length(max = 100, message = "컴포넌트이름할 수 없음초과경과100개문자기호")
    private String name;

    /**
     * 생성자id
     */
    private String creatorId;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 수정자id
     */
    private String updaterId;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 여부에서사용자목록  0: 아니요, 1: 
     */
    private Integer isShown;

    /**
     * 삭제 여부 0: 삭제되지 않음, 1: 삭제됨
     */
    private Integer deleted;

    /**
     * 테넌트id
     */
    private String tenantId;

    /**
     * appmarketResource중의사용id
     */
    private String appId;

    /**
     * 가져오기의사용: 앱 마켓버전
     */
    private Integer appVersion;

    /**
     * 가져오기의사용: 마켓id
     */
    private String marketId;

    /**
     * 상태: toObtain, obtained, toUpdate
     */
    private String resourceStatus;

    /**
     * : create 생성 ; market 마켓가져오기
     */
    private String dataSource;

    /**
     * editing 중, published 완료발송버전, shared 완료위, locked지정(불가)
     */
    private String transformStatus;
}