package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.UapExtendPropertyDto;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * UapExtendPropertyDto기기
 * 사용를core패키지아래의UapExtendPropertyDto및UAP클라이언트의UapExtendPropertyDto변환
 *
 * @author xqcao2
 */
@Component
public class UapExtendPropertyDtoMapper {

    /**
     * 를core패키지아래의UapExtendPropertyDto변환로UAP클라이언트의UapExtendPropertyDto
     *
     * @param uapExtendPropertyDto core패키지아래의UapExtendPropertyDto
     * @return UAP클라이언트의UapExtendPropertyDto
     */
    public com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto toUapExtendPropertyDto(
            UapExtendPropertyDto uapExtendPropertyDto) {
        if (uapExtendPropertyDto == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto uapExtendProperty =
                new com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(uapExtendPropertyDto, uapExtendProperty);
        return uapExtendProperty;
    }

    /**
     * 를UAP클라이언트의UapExtendPropertyDto변환로core패키지아래의UapExtendPropertyDto
     *
     * @param uapExtendPropertyDto UAP클라이언트의UapExtendPropertyDto
     * @return core패키지아래의UapExtendPropertyDto
     */
    public UapExtendPropertyDto fromUapExtendPropertyDto(
            com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto uapExtendPropertyDto) {
        if (uapExtendPropertyDto == null) {
            return null;
        }

        UapExtendPropertyDto uapExtendProperty = new UapExtendPropertyDto();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(uapExtendPropertyDto, uapExtendProperty);
        return uapExtendProperty;
    }

    /**
     * 량를core패키지아래의UapExtendPropertyDto목록변환로UAP클라이언트의UapExtendPropertyDto목록
     *
     * @param uapExtendPropertyDtoList core패키지아래의UapExtendPropertyDto목록
     * @return UAP클라이언트의UapExtendPropertyDto목록
     */
    public List<com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto> toUapExtendPropertyDtoList(
            List<UapExtendPropertyDto> uapExtendPropertyDtoList) {
        if (uapExtendPropertyDtoList == null || uapExtendPropertyDtoList.isEmpty()) {
            return Collections.emptyList();
        }

        return uapExtendPropertyDtoList.stream()
                .map(this::toUapExtendPropertyDto)
                .filter(uapExtendProperty -> uapExtendProperty != null)
                .collect(Collectors.toList());
    }

    /**
     * 량를UAP클라이언트의UapExtendPropertyDto목록변환로core패키지아래의UapExtendPropertyDto목록
     *
     * @param uapExtendPropertyDtoList UAP클라이언트의UapExtendPropertyDto목록
     * @return core패키지아래의UapExtendPropertyDto목록
     */
    public List<UapExtendPropertyDto> fromUapExtendPropertyDtoList(
            List<com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto> uapExtendPropertyDtoList) {
        if (uapExtendPropertyDtoList == null || uapExtendPropertyDtoList.isEmpty()) {
            return Collections.emptyList();
        }

        return uapExtendPropertyDtoList.stream()
                .map(this::fromUapExtendPropertyDto)
                .filter(uapExtendProperty -> uapExtendProperty != null)
                .collect(Collectors.toList());
    }
}