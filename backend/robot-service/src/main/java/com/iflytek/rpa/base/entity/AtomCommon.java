package com.iflytek.rpa.base.entity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author mjren
 * @date 2025-02-19 10:26
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class AtomCommon {

    /**
     * 단계순서닫기시스템
     */
    @Valid
    @NotNull(message = "atomicTree 비워 둘 수 없습니다")
    private List<AtomicTree> atomicTree;

    /**
     * 의즐겨찾기대기단계순서닫기시스템
     */
    @Valid
    private List<AtomicTree> atomicTreeExtend;

    /**
     * 높이단계매개변수
     */
    @Valid
    @NotNull(message = "commonAdvancedParameter 비워 둘 수 없습니다")
    private List<CommonAdvancedParameter> commonAdvancedParameter;

    /**
     * 변수유형
     */
    @Valid
    @NotNull(message = "types 비워 둘 수 없습니다")
    private Map<String, TypeInfo> types;

    @Data
    public static class TypeInfo {
        @NotBlank(message = "types.key 비워 둘 수 없습니다")
        private String key;

        private String src;

        @NotBlank(message = "types.desc 비워 둘 수 없습니다")
        private String desc;

        @NotBlank(message = "types.version 비워 둘 수 없습니다")
        private String version;

        private String channel;

        private String template;

        @Valid
        private List<FuncItem> funcList;
    }

    @Data
    public static class FuncItem {
        @NotBlank(message = "funcList.key 비워 둘 수 없습니다")
        private String key;

        @NotBlank(message = "funcList.funcDesc 비워 둘 수 없습니다")
        private String funcDesc;

        @NotBlank(message = "funcList.resType 비워 둘 수 없습니다")
        private String resType;

        @NotBlank(message = "funcList.resDesc 비워 둘 수 없습니다")
        private String resDesc;

        @NotBlank(message = "funcList.useSrc 비워 둘 수 없습니다")
        private String useSrc;
    }

    public static List<String> getPropertyNames() {
        List<String> propertyNames = new ArrayList<>();
        Field[] fields = AtomCommon.class.getDeclaredFields();

        for (Field field : fields) {
            propertyNames.add(field.getName());
        }

        return propertyNames;
    }
}