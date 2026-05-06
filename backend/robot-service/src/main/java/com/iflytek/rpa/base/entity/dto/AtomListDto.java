package com.iflytek.rpa.base.entity.dto;

import java.util.List;
import lombok.Data;

@Data
public class AtomListDto {
    // 목록
    private List<Atom> atomList;

    @Data
    public static class Atom {
        private String key;
        private String version;
    }
}