package com.iflytek.rpa.auth.core.entity;

import java.io.Serializable;
import java.util.List;

/**
 * @author byzhou2
 * @version 1.0 역할데이터권한데이터권한정보 객체
 * @description
 * @create 2017/11/2 15:10
 */
public class DataAuthorityWithDimDictDto implements Serializable {

    private static final long serialVersionUID = -7748820351070302015L;
    /**
     * 데이터권한id
     */
    private String dataAuthId;

    /**
     * 데이터권한이름
     */
    private String dataAuthName;

    /**
     * 정렬
     */
    private Integer sort;

    /**
     * 여부선택중
     */
    private boolean checked;

    /**
     * 닫기 정도합치기
     */
    private List<Dim> dimList;

    public DataAuthorityWithDimDictDto() {}

    public static class Dim implements Serializable {
        private static final long serialVersionUID = -1548982536071323814L;
        // 정도id
        private String dimId;
        // 정도이름
        private String dimName;

        private List<DimDict> dimDictList;

        public Dim() {}

        public Dim(String dimId, String dimName, List<DimDict> list) {
            this.dimId = dimId;
            this.dimName = dimName;
            this.dimDictList = list;
        }

        public String getDimId() {
            return dimId;
        }

        public void setDimId(String dimId) {
            this.dimId = dimId == null ? null : dimId.trim();
        }

        public String getDimName() {
            return dimName;
        }

        public void setDimName(String dimName) {
            this.dimName = dimName;
        }

        public List<DimDict> getDimDictList() {
            return dimDictList;
        }

        public void setDimDictList(List<DimDict> dimDictList) {
            this.dimDictList = dimDictList;
        }
    }

    public static class DimDict implements Serializable {
        private static final long serialVersionUID = -5991082020733531941L;
        // 정도딕셔너리id,시스템내부모듈정도,값로시스템내부데이터id
        private String dictId;
        // 정도딕셔너리이름
        private String dictName;
        // 정도딕셔너리값,지정정도,값로서비스방법데이터id
        // 지정정도,가져오기value값
        private String dictValue;

        public DimDict() {}

        public DimDict(String dictId, String dictName, String dictValue) {
            this.dictId = dictId;
            this.dictName = dictName;
            this.dictValue = dictValue;
        }

        public String getDictId() {
            return dictId;
        }

        public void setDictId(String dictId) {
            this.dictId = dictId == null ? null : dictId.trim();
        }

        public String getDictName() {
            return dictName;
        }

        public void setDictName(String dictName) {
            this.dictName = dictName;
        }

        public String getDictValue() {
            return dictValue;
        }

        public void setDictValue(String dictValue) {
            this.dictValue = dictValue;
        }
    }

    public String getDataAuthId() {
        return dataAuthId;
    }

    public void setDataAuthId(String dataAuthId) {
        this.dataAuthId = dataAuthId == null ? null : dataAuthId.trim();
    }

    public String getDataAuthName() {
        return dataAuthName;
    }

    public void setDataAuthName(String dataAuthName) {
        this.dataAuthName = dataAuthName;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public List<Dim> getDimList() {
        return dimList;
    }

    public void setDimList(List<Dim> dimList) {
        this.dimList = dimList;
    }
}