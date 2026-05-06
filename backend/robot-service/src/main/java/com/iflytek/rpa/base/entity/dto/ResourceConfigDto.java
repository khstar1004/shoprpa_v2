package com.iflytek.rpa.base.entity.dto;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 매칭DTO
 * 사용JSON순서열및반대순서열
 * 저장시저장type, base, final필드
 * 사용시에서버전매칭중urls, parent대기정보
 */
@Data
public class ResourceConfigDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 유형: QUOTA(매칭금액)또는 SWITCH(열기닫기)
     */
    @JSONField(name = "type")
    private String type;

    /**
     * 값(버전값)
     */
    @JSONField(name = "base")
    private Integer base;

    /**
     * 종료값(직선연결저장, 아니요계획값)
     */
    @JSONField(name = "final")
    private Integer finalValue;

    /**
     * URL경로방식목록(에서버전매칭중가져오기, 찾을 수 없습니다데이터베이스extraConfigJson중, 필요저장에서Redis저장중)
     */
    @JSONField(serialize = true, deserialize = true)
    private List<String> urls;

    /**
     * 단계코드(에서버전매칭중가져오기, 찾을 수 없습니다데이터베이스extraConfigJson중, 필요저장에서Redis저장중)
     */
    @JSONField(serialize = true, deserialize = true)
    private String parent;
}