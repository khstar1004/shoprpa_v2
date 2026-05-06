package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.DataAuthorityWithDimDictDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * DataAuthorityWithDimDictDto기기
 * 사용를UAP클라이언트의DataAuthorityWithDimDictDto변환로core패키지아래의DataAuthorityWithDimDictDto
 *
 * @author xqcao2
 */
@Component
public class DataAuthorityWithDimDictDtoMapper {

    /**
     * 를UAP클라이언트의DataAuthorityWithDimDictDto변환로DataAuthorityWithDimDictDto
     *
     * @param uapDataAuthorityWithDimDictDto UAP클라이언트의DataAuthorityWithDimDictDto
     * @return core패키지아래의DataAuthorityWithDimDictDto
     */
    public DataAuthorityWithDimDictDto fromUapDataAuthorityWithDimDictDto(
            com.iflytek.sec.uap.client.core.dto.dataauthority.DataAuthorityWithDimDictDto
                    uapDataAuthorityWithDimDictDto) {
        if (uapDataAuthorityWithDimDictDto == null) {
            return null;
        }

        DataAuthorityWithDimDictDto dataAuthorityWithDimDictDto = new DataAuthorityWithDimDictDto();
        // 사용BeanUtils복사본속성
        BeanUtils.copyProperties(uapDataAuthorityWithDimDictDto, dataAuthorityWithDimDictDto);

        // 변환dimList
        if (uapDataAuthorityWithDimDictDto.getDimList() != null
                && !uapDataAuthorityWithDimDictDto.getDimList().isEmpty()) {
            List<DataAuthorityWithDimDictDto.Dim> dimList = new ArrayList<>();
            for (com.iflytek.sec.uap.client.core.dto.dataauthority.DataAuthorityWithDimDictDto.Dim uapDim :
                    uapDataAuthorityWithDimDictDto.getDimList()) {
                DataAuthorityWithDimDictDto.Dim dim = fromUapDim(uapDim);
                if (dim != null) {
                    dimList.add(dim);
                }
            }
            dataAuthorityWithDimDictDto.setDimList(dimList);
        } else {
            dataAuthorityWithDimDictDto.setDimList(new ArrayList<>());
        }

        return dataAuthorityWithDimDictDto;
    }

    /**
     * 를UAP의Dim변환로core의Dim
     */
    private DataAuthorityWithDimDictDto.Dim fromUapDim(
            com.iflytek.sec.uap.client.core.dto.dataauthority.DataAuthorityWithDimDictDto.Dim uapDim) {
        if (uapDim == null) {
            return null;
        }

        DataAuthorityWithDimDictDto.Dim dim = new DataAuthorityWithDimDictDto.Dim();
        dim.setDimId(uapDim.getDimId());
        dim.setDimName(uapDim.getDimName());

        // 변환dimDictList
        if (uapDim.getDimDictList() != null && !uapDim.getDimDictList().isEmpty()) {
            List<DataAuthorityWithDimDictDto.DimDict> dimDictList = new ArrayList<>();
            for (com.iflytek.sec.uap.client.core.dto.dataauthority.DataAuthorityWithDimDictDto.DimDict uapDimDict :
                    uapDim.getDimDictList()) {
                DataAuthorityWithDimDictDto.DimDict dimDict = fromUapDimDict(uapDimDict);
                if (dimDict != null) {
                    dimDictList.add(dimDict);
                }
            }
            dim.setDimDictList(dimDictList);
        } else {
            dim.setDimDictList(new ArrayList<>());
        }

        return dim;
    }

    /**
     * 를UAP의DimDict변환로core의DimDict
     */
    private DataAuthorityWithDimDictDto.DimDict fromUapDimDict(
            com.iflytek.sec.uap.client.core.dto.dataauthority.DataAuthorityWithDimDictDto.DimDict uapDimDict) {
        if (uapDimDict == null) {
            return null;
        }

        DataAuthorityWithDimDictDto.DimDict dimDict = new DataAuthorityWithDimDictDto.DimDict();
        dimDict.setDictId(uapDimDict.getDictId());
        dimDict.setDictName(uapDimDict.getDictName());
        dimDict.setDictValue(uapDimDict.getDictValue());

        return dimDict;
    }

    /**
     * 량를UAP클라이언트의DataAuthorityWithDimDictDto목록변환로DataAuthorityWithDimDictDto목록
     *
     * @param uapDataAuthorityWithDimDictDtos UAP클라이언트의DataAuthorityWithDimDictDto목록
     * @return core패키지아래의DataAuthorityWithDimDictDto목록
     */
    public List<DataAuthorityWithDimDictDto> fromUapDataAuthorityWithDimDictDtos(
            List<com.iflytek.sec.uap.client.core.dto.dataauthority.DataAuthorityWithDimDictDto>
                    uapDataAuthorityWithDimDictDtos) {
        if (uapDataAuthorityWithDimDictDtos == null || uapDataAuthorityWithDimDictDtos.isEmpty()) {
            return Collections.emptyList();
        }

        return uapDataAuthorityWithDimDictDtos.stream()
                .map(this::fromUapDataAuthorityWithDimDictDto)
                .filter(dataAuthorityWithDimDictDto -> dataAuthorityWithDimDictDto != null)
                .collect(Collectors.toList());
    }
}