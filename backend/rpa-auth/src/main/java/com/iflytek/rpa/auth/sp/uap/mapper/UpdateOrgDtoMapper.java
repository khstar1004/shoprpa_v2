package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.UpdateOrgDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * UpdateOrgDto기기
 * 사용를core패키지아래의UpdateOrgDto및UAP클라이언트의UpdateOrgDto변환
 *
 * @author xqcao2
 */
@Component
public class UpdateOrgDtoMapper {

    /**
     * 를core패키지아래의UpdateOrgDto변환로UAP클라이언트의UpdateOrgDto
     *
     * @param updateOrgDto core패키지아래의UpdateOrgDto
     * @return UAP클라이언트의UpdateOrgDto
     */
    public com.iflytek.sec.uap.client.core.dto.org.UpdateOrgDto toUapUpdateOrgDto(UpdateOrgDto updateOrgDto) {
        if (updateOrgDto == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.org.UpdateOrgDto uapUpdateOrgDto =
                new com.iflytek.sec.uap.client.core.dto.org.UpdateOrgDto();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(updateOrgDto, uapUpdateOrgDto);

        return uapUpdateOrgDto;
    }

    /**
     * 를UAP클라이언트의UpdateOrgDto변환로core패키지아래의UpdateOrgDto
     *
     * @param uapUpdateOrgDto UAP클라이언트의UpdateOrgDto
     * @return core패키지아래의UpdateOrgDto
     */
    public UpdateOrgDto fromUapUpdateOrgDto(com.iflytek.sec.uap.client.core.dto.org.UpdateOrgDto uapUpdateOrgDto) {
        if (uapUpdateOrgDto == null) {
            return null;
        }

        UpdateOrgDto updateOrgDto = new UpdateOrgDto();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(uapUpdateOrgDto, updateOrgDto);

        return updateOrgDto;
    }
}