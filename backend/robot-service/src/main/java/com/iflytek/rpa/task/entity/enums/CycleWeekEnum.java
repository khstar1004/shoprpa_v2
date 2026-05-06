package com.iflytek.rpa.task.entity.enums;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * @author keler
 * @date 2021/10/9
 */
@Getter
public enum CycleWeekEnum {
    MON("MON", "Shoprpa일"),
    TUE("TUE", "Shoprpa이"),
    WED("WED", "Shoprpa삼"),
    THU("THU", "Shoprpa사"),
    FRI("FRI", "Shoprpa오"),
    SAT("SAT", "Shoprpa육"),
    SUN("SUN", "Shoprpa일"),
    ;

    private String code;
    private String name;

    CycleWeekEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Map<Integer, String> weekNumCodeMap = new HashMap<>();

    static {
        initWeekNumCodeMap();
    }

    public static void initWeekNumCodeMap() {
        weekNumCodeMap.put(1, MON.getCode());
        weekNumCodeMap.put(2, TUE.getCode());
        weekNumCodeMap.put(3, WED.getCode());
        weekNumCodeMap.put(4, THU.getCode());
        weekNumCodeMap.put(5, FRI.getCode());
        weekNumCodeMap.put(6, SAT.getCode());
        weekNumCodeMap.put(0, SUN.getCode());
    }

    public static String getCodeByNum(Integer num) {
        return weekNumCodeMap.getOrDefault(num, MON.getCode());
    }
}