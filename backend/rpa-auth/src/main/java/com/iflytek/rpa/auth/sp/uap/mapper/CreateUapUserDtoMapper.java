package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.CreateUapUserDto;
import com.iflytek.rpa.auth.core.entity.CreateUserDto;
import com.iflytek.rpa.auth.core.entity.UapExtendPropertyDto;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * CreateUapUserDto기기
 * 사용를core패키지아래의CreateUapUserDto변환로UAP클라이언트의CreateUapUserDto
 *
 * @author xqcao2
 */
@Component
public class CreateUapUserDtoMapper {

    /**
     * 를core패키지아래의CreateUapUserDto변환로UAP클라이언트의CreateUapUserDto
     *
     * @param createUapUserDto core패키지아래의CreateUapUserDto
     * @return UAP클라이언트의CreateUapUserDto
     */
    public com.iflytek.sec.uap.client.core.dto.user.CreateUapUserDto toUapCreateUapUserDto(
            CreateUapUserDto createUapUserDto) {
        if (createUapUserDto == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.user.CreateUapUserDto uapCreateUapUserDto =
                new com.iflytek.sec.uap.client.core.dto.user.CreateUapUserDto();

        // 변환user속성
        if (createUapUserDto.getUser() != null) {
            com.iflytek.sec.uap.client.core.dto.user.CreateUserDto uapCreateUserDto =
                    toUapCreateUserDto(createUapUserDto.getUser());
            uapCreateUapUserDto.setUser(uapCreateUserDto);
        }

        // 변환extands속성
        if (createUapUserDto.getExtands() != null
                && !createUapUserDto.getExtands().isEmpty()) {
            List<com.iflytek.sec.uap.client.core.dto.extand.UapExtendPropertyDto> uapExtands =
                    createUapUserDto.getExtands().stream()
                            .map(this::toUapExtendPropertyDto)
                            .collect(Collectors.toList());
            uapCreateUapUserDto.setExtands(uapExtands);
        }

        return uapCreateUapUserDto;
    }

    /**
     * 를UAP클라이언트의CreateUapUserDto변환로core패키지아래의CreateUapUserDto
     *
     * @param uapCreateUapUserDto UAP클라이언트의CreateUapUserDto
     * @return core패키지아래의CreateUapUserDto
     */
    public CreateUapUserDto fromUapCreateUapUserDto(
            com.iflytek.sec.uap.client.core.dto.user.CreateUapUserDto uapCreateUapUserDto) {
        if (uapCreateUapUserDto == null) {
            return null;
        }

        CreateUapUserDto createUapUserDto = new CreateUapUserDto();

        // 변환user속성
        if (uapCreateUapUserDto.getUser() != null) {
            CreateUserDto createUserDto = fromUapCreateUserDto(uapCreateUapUserDto.getUser());
            createUapUserDto.setUser(createUserDto);
        }

        // 변환extands속성
        if (uapCreateUapUserDto.getExtands() != null
                && !uapCreateUapUserDto.getExtands().isEmpty()) {
            List<UapExtendPropertyDto> extands = uapCreateUapUserDto.getExtands().stream()
                    .map(this::fromUapExtendPropertyDto)
                    .collect(Collectors.toList());
            createUapUserDto.setExtands(extands);
        }

        return createUapUserDto;
    }

    /**
     * 를core패키지아래의CreateUserDto변환로UAP클라이언트의CreateUserDto
     */
    private com.iflytek.sec.uap.client.core.dto.user.CreateUserDto toUapCreateUserDto(CreateUserDto createUserDto) {
        if (createUserDto == null) {
            return null;
        }

        com.iflytek.sec.uap.client.core.dto.user.CreateUserDto uapCreateUserDto =
                new com.iflytek.sec.uap.client.core.dto.user.CreateUserDto();
        BeanUtils.copyProperties(createUserDto, uapCreateUserDto);
        return uapCreateUserDto;
    }

    /**
     * 를UAP클라이언트의CreateUserDto변환로core패키지아래의CreateUserDto
     */
    private CreateUserDto fromUapCreateUserDto(
            com.iflytek.sec.uap.client.core.dto.user.CreateUserDto uapCreateUserDto) {
        if (uapCreateUserDto == null) {
            return null;
        }

        CreateUserDto createUserDto = new CreateUserDto();
        BeanUtils.copyProperties(uapCreateUserDto, createUserDto);
        return createUserDto;
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