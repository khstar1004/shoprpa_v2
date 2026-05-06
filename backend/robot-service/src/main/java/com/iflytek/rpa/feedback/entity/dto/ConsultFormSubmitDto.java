package com.iflytek.rpa.feedback.entity.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 문의테이블단일제출DTO
 *
 * @author system
 * @since 2024-12-15
 */
@Data
public class ConsultFormSubmitDto {

    /**
     * 테이블단일유형 1=버전 2=버전
     */
    //    @NotNull(message = "테이블단일유형비워 둘 수 없습니다")
    private Integer formType;

    /**
     * 이름
     */
    @NotBlank(message = "이름비워 둘 수 없습니다")
    @Size(max = 128, message = "이름길이정도할 수 없음초과경과128문자기호")
    private String companyName;

    /**
     * 시스템사람이름
     */
    @NotBlank(message = "시스템사람이름비워 둘 수 없습니다")
    @Size(max = 64, message = "시스템사람이름길이정도할 수 없음초과경과64문자기호")
    private String contactName;

    /**
     * 휴대폰 번호
     */
    @NotBlank(message = "휴대폰 번호는 비워 둘 수 없습니다")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "휴대폰 번호 형식이 올바르지 않습니다")
    @Size(max = 20, message = "휴대폰 번호길이정도할 수 없음초과경과20문자기호")
    private String mobile;

    /**
     * 메일함()
     */
    @Email(message = "메일함형식아니요정상")
    @Size(max = 128, message = "메일함길이정도할 수 없음초과경과128문자기호")
    private String email;

    /**
     * 팀사람데이터(딕셔너리값)
     */
    @Size(max = 32, message = "팀사람데이터길이정도할 수 없음초과경과32문자기호")
    private String teamSize;
}