package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.ListUserDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * ListUserDto기기
 * 사용를core패키지아래의ListUserDto변환로UAP클라이언트의ListUserDto
 *
 * @author xqcao2
 */
@Component
public class ListUserDtoMapper {

    /**
     * 를core패키지아래의ListUserDto변환로UAP클라이언트의ListUserDto
     *
     * @param listUserDto core패키지아래의ListUserDto
     * @return UAP클라이언트의ListUserDto
     */
    public com.iflytek.sec.uap.client.core.dto.user.ListUserDto toUapListUserDto(ListUserDto listUserDto) {
        if (listUserDto == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.user.ListUserDto uapListUserDto =
                new com.iflytek.sec.uap.client.core.dto.user.ListUserDto();
        // 사용BeanUtils복사속성(패키지PageQueryDto의pageNum및pageSize)
        BeanUtils.copyProperties(listUserDto, uapListUserDto);

        return uapListUserDto;
    }

    /**
     * 를UAP클라이언트의ListUserDto변환로core패키지아래의ListUserDto
     *
     * @param uapListUserDto UAP클라이언트의ListUserDto
     * @return core패키지아래의ListUserDto
     */
    public ListUserDto fromUapListUserDto(com.iflytek.sec.uap.client.core.dto.user.ListUserDto uapListUserDto) {
        if (uapListUserDto == null) {
            return null;
        }

        ListUserDto listUserDto = new ListUserDto();
        // 사용BeanUtils복사속성(패키지PageQueryDto의pageNum및pageSize)
        BeanUtils.copyProperties(uapListUserDto, listUserDto);

        return listUserDto;
    }
}