package com.iflytek.rpa.base.entity.dto;

import static com.iflytek.rpa.robot.constants.RobotConstant.EDIT_PAGE;

import com.iflytek.rpa.base.entity.CElement;
import lombok.Data;

@Data
public class ServerBaseDto {

    /**
     * 지정에서개실행; EDIT_PAGE ;목록  PROJECT_LIST; 실행기기실행 EXECUTOR; 예약 작업시작 CRONTAB;
     */
    private String mode = EDIT_PAGE;

    private String elementType;

    private String robotId;

    private Integer robotVersion;

    private String groupId;

    private String groupName;

    private String elementName;

    private String elementId;

    private String creatorId;

    // =============================================================================

    private CElement element;

    private String processName;

    //    private String groupId;
    //    /**
    //     * 요소id
    //     */
    //    private String elementId;
    //    /**
    //     * 원이름
    //     */
    //    private String elementName;
    //    /**
    //     * 아이콘
    //     */
    //    private String icon;
    //    /**
    //     * 이미지id
    //     */
    //    private String imageId;
    //    /**
    //     * 요소의단계이미지id
    //     */
    //    private String parentImageId;
    //    /**
    //     * 원내용
    //     */
    //    private String elementData;

}