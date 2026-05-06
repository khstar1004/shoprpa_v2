package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.UapExtendPropertyDto;
import com.iflytek.rpa.auth.core.entity.UpdateUapUserDto;
import com.iflytek.rpa.auth.core.entity.UpdateUserDto;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * UpdateUapUserDto기기
 * 사용를core패키지아래의UpdateUapUserDto변환로UAP클라이언트의UpdateUapUserDto
 *
 * @author xqcao2
 */
@Component
public class UpdateUapUserDtoMapper {

    /**
     * 를core패키지아래의UpdateUapUserDto변환로UAP클라이언트의UpdateUapUserDto
     *
     * @param updateUapUserDto core패키지아래의UpdateUapUserDto
     * @return UAP클라이언트의UpdateUapUserDto
     */
    public com.iflytek.sec.uap.client.core.dto.user.UpdateUapUserDto toUapUpdateUapUserDto(
            UpdateUapUserDto updateUapUserDto) {
        if (updateUapUserDto == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.user.UpdateUapUserDto uapUpdateUapUserDto =
                new com.iflytek.sec.uap.client.core.dto.user.UpdateUapUserDto();

        // 변환user속성
        if (updateUapUserDto.getUser() != null) {
            com.iflytek.sec.uap.client.core.dto.user.UpdateUserDto uapUpdateUserDto =
                    toUapUpdateUserDto(updateUapUserDto.getUser());
            uapUpdateUapUserDto.setUser(uapUpdateUserDto);
        }

        // 변환extands속성
        if (updateUapUserDto.getExtands() != null
                && !updateUapUserDto.getExtands().isEmpty()) {
            List<com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto> uapExtands =
                    updateUapUserDto.getExtands().stream()
                            .map(this::toUapExtendPropertyDto)
                            .collect(Collectors.toList());
            uapUpdateUapUserDto.setExtands(uapExtands);
        }

        return uapUpdateUapUserDto;
    }

    /**
     * 를UAP클라이언트의UpdateUapUserDto변환로core패키지아래의UpdateUapUserDto
     *
     * @param uapUpdateUapUserDto UAP클라이언트의UpdateUapUserDto
     * @return core패키지아래의UpdateUapUserDto
     */
    public UpdateUapUserDto fromUapUpdateUapUserDto(
            com.iflytek.sec.uap.client.core.dto.user.UpdateUapUserDto uapUpdateUapUserDto) {
        if (uapUpdateUapUserDto == null) {
            return null;
        }

        UpdateUapUserDto updateUapUserDto = new UpdateUapUserDto();

        // 변환user속성
        if (uapUpdateUapUserDto.getUser() != null) {
            UpdateUserDto updateUserDto = fromUapUpdateUserDto(uapUpdateUapUserDto.getUser());
            updateUapUserDto.setUser(updateUserDto);
        }

        // 변환extands속성
        if (uapUpdateUapUserDto.getExtands() != null
                && !uapUpdateUapUserDto.getExtands().isEmpty()) {
            List<UapExtendPropertyDto> extands = uapUpdateUapUserDto.getExtands().stream()
                    .map(this::fromUapExtendPropertyDto)
                    .collect(Collectors.toList());
            updateUapUserDto.setExtands(extands);
        }

        return updateUapUserDto;
    }

    /**
     * 를core패키지아래의UpdateUserDto변환로UAP클라이언트의UpdateUserDto
     */
    private com.iflytek.sec.uap.client.core.dto.user.UpdateUserDto toUapUpdateUserDto(UpdateUserDto updateUserDto) {
        if (updateUserDto == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.user.UpdateUserDto uapUpdateUserDto =
                new com.iflytek.sec.uap.client.core.dto.user.UpdateUserDto();
        BeanUtils.copyProperties(updateUserDto, uapUpdateUserDto);
        return uapUpdateUserDto;
    }

    /**
     * 를UAP클라이언트의UpdateUserDto변환로core패키지아래의UpdateUserDto
     */
    private UpdateUserDto fromUapUpdateUserDto(
            com.iflytek.sec.uap.client.core.dto.user.UpdateUserDto uapUpdateUserDto) {
        if (uapUpdateUserDto == null) {
            return null;
        }

        UpdateUserDto updateUserDto = new UpdateUserDto();
        BeanUtils.copyProperties(uapUpdateUserDto, updateUserDto);
        return updateUserDto;
    }

    /**
     * 를core패키지아래의UapExtendPropertyDto변환로UAP클라이언트의UapExtendPropertyDto
     */
    private com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto toUapExtendPropertyDto(
            UapExtendPropertyDto uapExtendPropertyDto) {
        if (uapExtendPropertyDto == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto uapExtendProperty =
                new com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto();
        BeanUtils.copyProperties(uapExtendPropertyDto, uapExtendProperty);
        return uapExtendProperty;
    }

    /**
     * 를UAP클라이언트의UapExtendPropertyDto변환로core패키지아래의UapExtendPropertyDto
     */
    private UapExtendPropertyDto fromUapExtendPropertyDto(
            com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto uapExtendPropertyDto) {
        if (uapExtendPropertyDto == null) {
            return null;
        }

        UapExtendPropertyDto uapExtendProperty = new UapExtendPropertyDto();
        BeanUtils.copyProperties(uapExtendPropertyDto, uapExtendProperty);
        return uapExtendProperty;
    }
}