package com.iflytek.rpa.base.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author mjren
 * @date 2025-02-19 10:50
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class CommonAdvancedParameter {
    @NotBlank(message = "CommonAdvancedParameter.types 비워 둘 수 없습니다")
    private String types;

    @Valid
    @NotNull(message = "CommonAdvancedParameter.formType 비워 둘 수 없습니다")
    private FormType formType;

    @NotBlank(message = "CommonAdvancedParameter.key 비워 둘 수 없습니다")
    private String key;

    @NotBlank(message = "CommonAdvancedParameter.title 비워 둘 수 없습니다")
    private String title;

    @NotBlank(message = "CommonAdvancedParameter.name 비워 둘 수 없습니다")
    private String name;

    private String need_parse;

    private List<Dynamic> dynamics;

    @JsonProperty("default") // 를 JSON 중의 "default" 까지 defaultValue
    private Object defaultValue;

    @Valid
    private List<Option> options;

    @Data
    public static class FormType {
        @NotBlank(message = "FormType.type 비워 둘 수 없습니다")
        private String type;

        private Object params;
    }

    @Data
    public static class Option {
        @NotBlank(message = "Option.label 비워 둘 수 없습니다")
        private String label;

        private Object value;
    }

    @Data
    public static class Dynamic {
        private String key;
        private String expression;
    }

    /*    @Data
    public static class Conditional{
        private String operators;
        private OperandItem[] Operands;
    }*/

    @Data
    public static class OperandItem {
        private String left;
        private String right;
        private Object operator;
    }
}