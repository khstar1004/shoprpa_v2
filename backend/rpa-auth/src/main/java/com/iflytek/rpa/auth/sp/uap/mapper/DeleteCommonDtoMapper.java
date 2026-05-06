package com.iflytek.rpa.auth.sp.uap.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * DeleteCommonDto기기
 * 사용를core패키지아래의DeleteCommonDto변환로UAP클라이언트패키지아래의DeleteCommonDto
 *
 * @author xqcao2
 */
@Component
public class DeleteCommonDtoMapper {

    /**
     * 를core패키지아래의DeleteCommonDto변환로UAP클라이언트의DeleteCommonDto
     *
     * @param source core패키지아래의DeleteCommonDto
     * @return UAP클라이언트의DeleteCommonDto
     */
    public com.iflytek.sec.uap.client.core.dto.DeleteCommonDto toUapDeleteCommonDto(
            com.iflytek.rpa.auth.core.entity.DeleteCommonDto source) {
        if (source == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.DeleteCommonDto target =
                new com.iflytek.sec.uap.client.core.dto.DeleteCommonDto();

        // 사용BeanUtils복사속성(필드전체일)
        BeanUtils.copyProperties(source, target);

        return target;
    }

    /**
     * 를UAP클라이언트의DeleteCommonDto변환로DeleteCommonDto
     *
     * @param source UAP클라이언트의DeleteCommonDto
     * @return core패키지아래의DeleteCommonDto
     */
    public com.iflytek.rpa.auth.core.entity.DeleteCommonDto toCoreDeleteCommonDto(
            com.iflytek.sec.uap.client.core.dto.DeleteCommonDto source) {
        if (source == null) {
            return null;
        }

        com.iflytek.rpa.auth.core.entity.DeleteCommonDto target =
                new com.iflytek.rpa.auth.core.entity.DeleteCommonDto();

        // 사용BeanUtils복사속성(필드전체일)
        BeanUtils.copyProperties(source, target);

        return target;
    }

    /**
     * 량를core패키지아래의DeleteCommonDto목록변환로UAP클라이언트의DeleteCommonDto목록
     *
     * @param sourceList core패키지아래의DeleteCommonDto목록
     * @return UAP클라이언트의DeleteCommonDto목록
     */
    public List<com.iflytek.sec.uap.client.core.dto.DeleteCommonDto> toUapDeleteCommonDtos(
            List<com.iflytek.rpa.auth.core.entity.DeleteCommonDto> sourceList) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        return sourceList.stream().map(this::toUapDeleteCommonDto).collect(Collectors.toList());
    }

    /**
     * 량를UAP클라이언트의DeleteCommonDto목록변환로DeleteCommonDto목록
     *
     * @param sourceList UAP클라이언트의DeleteCommonDto목록
     * @return core패키지아래의DeleteCommonDto목록
     */
    public List<com.iflytek.rpa.auth.core.entity.DeleteCommonDto> toCoreDeleteCommonDtos(
            List<com.iflytek.sec.uap.client.core.dto.DeleteCommonDto> sourceList) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        return sourceList.stream().map(this::toCoreDeleteCommonDto).collect(Collectors.toList());
    }
}