package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.sec.uap.client.core.dto.role.GetRoleDto;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * @desc: TODO
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/11/25 16:32
 */
@Component
public class GetRoleDtoMapper {

    /**
     * 를GetRoleDto변환로UAP클라이언트의GetRoleDto
     */
    public GetRoleDto toUapGetRoleDto(com.iflytek.rpa.auth.core.entity.GetRoleDto source) {
        if (source == null) {
            return null;
        }

        GetRoleDto target = new GetRoleDto();

        // 사용BeanUtils복사속성(필드전체일)
        BeanUtils.copyProperties(source, target);

        return target;
    }

    /**
     * 를UAP클라이언트의GetRoleDto변환로GetRoleDto
     */
    public com.iflytek.rpa.auth.core.entity.GetRoleDto toCoreGetRoleDto(GetRoleDto source) {
        if (source == null) {
            return null;
        }

        com.iflytek.rpa.auth.core.entity.GetRoleDto target = new com.iflytek.rpa.auth.core.entity.GetRoleDto();

        // 사용BeanUtils복사속성(필드전체일)
        BeanUtils.copyProperties(source, target);

        return target;
    }

    /**
     * 량를GetRoleDto목록변환로UAP클라이언트의GetRoleDto목록
     */
    public List<GetRoleDto> toUapGetRoleDtos(List<com.iflytek.rpa.auth.core.entity.GetRoleDto> sourceList) {
        if (sourceList == null) {
            return null;
        }

        return sourceList.stream().map(this::toUapGetRoleDto).collect(Collectors.toList());
    }

    /**
     * 량를UAP클라이언트의GetRoleDto목록변환로GetRoleDto목록
     */
    public List<com.iflytek.rpa.auth.core.entity.GetRoleDto> toCoreGetRoleDtos(List<GetRoleDto> sourceList) {
        if (sourceList == null) {
            return null;
        }

        return sourceList.stream().map(this::toCoreGetRoleDto).collect(Collectors.toList());
    }
}