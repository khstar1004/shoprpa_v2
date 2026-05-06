package com.iflytek.rpa.base.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 클라이언트, 원정보(CElement)유형
 *
 * @author mjren
 * @since 2024-10-14 17:21:34
 */
@Data
public class CElement implements Serializable {
    private static final long serialVersionUID = -53914169890628551L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 분그룹id
     */
    @JSONField(name = "group_id")
    private String groupId;

    /**
     * 요소 선택유형: sigle통신선택, batch데이터가져오기
     */
    @JSONField(name = "common_sub_type")
    private String commonSubType;

    /**
     * 요소id
     */
    @JSONField(name = "element_id")
    private String elementId;
    /**
     * 원이름
     */
    @JSONField(name = "element_name")
    private String elementName;
    /**
     * 아이콘
     */
    private String icon;
    /**
     * 이미지다운로드id
     */
    @JSONField(name = "image_id")
    private String imageId;
    /**
     * 원내용
     */
    @JSONField(name = "element_data")
    private String elementData;

    private Integer deleted;

    @JSONField(name = "creator_id")
    private String creatorId;

    private Date createTime;

    @JSONField(name = "updater_id")
    private String updaterId;

    private Date updateTime;
    /**
     * 요소의단계이미지id
     */
    @JSONField(name = "parent_image_id")
    private String parentImageId;

    @JSONField(name = "robot_id")
    private String robotId;

    @JSONField(name = "robot_version")
    private Integer robotVersion;
}