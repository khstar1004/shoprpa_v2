package com.iflytek.rpa.resource.file.entity.enums;

/**
 * 파일유형
 */
public enum FileType {
    /**
     * 지원하지 않는유형
     */
    OTHER(0, "지원하지 않는유형"),

    /**
     * 텍스트
     */
    TXT(1, "텍스트"),

    /**
     * WORD
     */
    DOC(2, "WORD"),

    /**
     * PDF
     */
    PDF(3, "PDF");

    private final Integer value;
    private final String comment;

    /**
     * 데이터
     *
     * @param value   값
     * @param comment 비고설명
     */
    FileType(Integer value, String comment) {
        this.value = value;
        this.comment = comment;
    }

    /**
     * 근거파일이름가져오기파일유형
     *
     * @param filename 파일이름
     * @return 파일유형
     */
    public static FileType getFileType(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return FileType.OTHER;
        }

        String filenameLower = filename.toLowerCase();
        if (filenameLower.endsWith(".docx") || filenameLower.endsWith(".doc")) {
            return FileType.DOC;
        } else if (filenameLower.endsWith(".pdf")) {
            return FileType.PDF;
        } else if (filenameLower.endsWith(".txt")) {
            return FileType.TXT;
        } else {
            return FileType.OTHER;
        }
    }

    /**
     * 가져오기 값
     *
     * @return 값
     */
    public Integer getValue() {
        return value;
    }

    /**
     * 가져오기비고설명
     *
     * @return 비고설명
     */
    public String getComment() {
        return comment;
    }
}