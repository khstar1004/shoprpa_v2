package com.iflytek.rpa.auth.auditRecord.entity.enums;

import static com.iflytek.rpa.auth.auditRecord.entity.enums.EventMoudleEnum.*;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.core.enums.IEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum EventTypeEnum implements IEnum<Integer> {

    /**
     * 생성역할
     */
    ADD_ROLE(1001, "생성역할", ROLE),

    /**
     * 이름 변경역할
     */
    RENAME_ROLE(1002, "이름 변경역할", ROLE),

    /**
     * 삭제역할
     */
    DELETE_ROLE(1003, "삭제역할", ROLE),

    /**
     * 추가구성원
     */
    ADD_ROLE_USER(1004, "추가구성원", ROLE),
    /**
     * 제거구성원
     */
    REMOVE_ROLE_USER(1005, "제거구성원", ROLE),
    /**
     * 권한
     */
    EDIT_ROLE_FUNCTION(1006, "권한", ROLE),
    /**
     * 삭제봇
     */
    REMOVE_ROBOT(1101, "삭제봇", ROBOT),
    /**
     * 변환봇
     */
    TRANSFER_ROBOT(1102, "변환봇", ROBOT),
    /**
     * 권한
     */
    REMOVE_PROJECT(1201, "삭제", PROJECT),
    /**
     * 권한
     */
    TRANSFER_PROJECT(1202, "변환", PROJECT),

    /**
     * 새생성작업
     */
    CREATE_TASK(1301, "새생성작업", TASK),
    /**
     * 작업
     */
    UPDATE_TASK(1302, "작업", TASK),

    /**
     * 삭제작업
     */
    REMOVE_TASK(1303, "삭제작업", TASK),

    /**
     * 새생성준비
     */
    CREATE_TERMINAL(1401, "새생성준비", TERMINAL),
    /**
     * 작업
     */
    REMOVE_TERMINAL(1402, "삭제준비", TERMINAL),

    /**
     * 새생성준비그룹
     */
    CREATE_TERMINAL_GROUP(1403, "새생성준비그룹", TERMINAL),
    /**
     * 삭제준비그룹
     */
    REMOVE_TERMINAL_GROUP(1404, "삭제준비그룹", TERMINAL),
    ;

    EventTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    EventTypeEnum(int code, String name, EventMoudleEnum eventMoudleEnum) {
        this.code = code;
        this.name = name;
        this.eventMoudleEnum = eventMoudleEnum;
    }

    @EnumValue
    private final int code;

    private final String name;

    private EventMoudleEnum eventMoudleEnum;

    @JsonCreator
    public static EventTypeEnum getEnum(String name) {
        switch (name) {
            case "생성역할":
                return EventTypeEnum.ADD_ROLE;
            case "이름 변경역할":
                return EventTypeEnum.RENAME_ROLE;
            case "삭제역할":
                return EventTypeEnum.DELETE_ROLE;
            case "추가구성원":
                return EventTypeEnum.ADD_ROLE_USER;
            case "제거구성원":
                return EventTypeEnum.REMOVE_ROLE_USER;
            case "권한":
                return EventTypeEnum.EDIT_ROLE_FUNCTION;
            case "삭제봇":
                return EventTypeEnum.REMOVE_ROBOT;
            case "변환봇":
                return EventTypeEnum.TRANSFER_ROBOT;
            case "삭제":
                return EventTypeEnum.REMOVE_PROJECT;
            case "변환":
                return EventTypeEnum.TRANSFER_PROJECT;
            case "새생성작업":
                return EventTypeEnum.CREATE_TASK;
            case "작업":
                return EventTypeEnum.UPDATE_TASK;
            case "삭제작업":
                return EventTypeEnum.REMOVE_TASK;
            case "새생성준비":
                return EventTypeEnum.CREATE_TERMINAL;
            case "삭제준비":
                return EventTypeEnum.REMOVE_TERMINAL;
            case "새생성준비그룹":
                return EventTypeEnum.CREATE_TERMINAL_GROUP;
            case "삭제준비그룹":
                return EventTypeEnum.REMOVE_TERMINAL_GROUP;
            default:
                return null;
        }
    }

    public static Map<Integer, String> eventTypeMap = new HashMap<>();
    public static List<Map<String, String>> eventTypeList = new ArrayList<>();

    static {
        initEventTypeMap();
        initEventTypeList();
    }

    public static void initEventTypeMap() {
        for (EventTypeEnum eventTypeEnum : EventTypeEnum.values()) {
            eventTypeMap.put(eventTypeEnum.getCode(), eventTypeEnum.getName());
        }
    }

    public static void initEventTypeList() {
        for (EventTypeEnum eventTypeEnum : EventTypeEnum.values()) {
            eventTypeList.add(new HashMap<String, String>() {
                {
                    put("typeCode", String.valueOf(eventTypeEnum.getCode()));
                    put("typeName", eventTypeEnum.getName());
                }
            });
        }
    }

    @Override
    public Integer getValue() {
        return code;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public EventMoudleEnum getEventMoudleEnum() {
        return eventMoudleEnum;
    }
}