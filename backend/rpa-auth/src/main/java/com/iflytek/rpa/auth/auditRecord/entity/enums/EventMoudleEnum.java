package com.iflytek.rpa.auth.auditRecord.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.core.enums.IEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum EventMoudleEnum implements IEnum<Integer> {

    /**
     * 관리자 권한
     */
    ROLE(10, "관리자 권한"),
    ROBOT(11, "봇관리관리"),
    PROJECT(12, "관리관리"),
    TASK(13, "작업관리관리"),
    TERMINAL(14, "단말관리관리"),
    ;

    EventMoudleEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    @EnumValue
    private final int code;

    private final String name;

    public static Map<Integer, String> eventModuleMap = new HashMap<>();
    public static List<Map<String, String>> eventModuleList = new ArrayList<>();

    static {
        initEventModuleMap();
        initEventModuleList();
    }

    public static void initEventModuleMap() {
        for (EventMoudleEnum eventMoudleEnum : EventMoudleEnum.values()) {
            eventModuleMap.put(eventMoudleEnum.getCode(), eventMoudleEnum.getName());
        }
    }

    public static void initEventModuleList() {
        for (EventMoudleEnum eventMoudleEnum : EventMoudleEnum.values()) {
            eventModuleList.add(new HashMap<String, String>() {
                {
                    put("typeCode", String.valueOf(eventMoudleEnum.getCode()));
                    put("typeName", eventMoudleEnum.getName());
                }
            });
        }
    }

    @JsonCreator
    public static EventMoudleEnum getEnum(String name) {
        switch (name) {
            case "관리자 권한":
                return EventMoudleEnum.ROLE;
            case "봇관리관리":
                return EventMoudleEnum.ROBOT;
            case "관리관리":
                return EventMoudleEnum.PROJECT;
            case "작업관리관리":
                return EventMoudleEnum.TASK;
            case "단말관리관리":
                return EventMoudleEnum.TERMINAL;
            default:
                return null;
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
}