package com.iflytek.rpa.feedback.entity.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 반대제출DTO
 *
 * @author system
 * @since 2024-12-15
 */
@Data
public class FeedbackSubmitDto {

    /**
     * 사용자로그인이름
     */
    @NotBlank(message = "사용자로그인이름비워 둘 수 없습니다")
    private String username;

    /**
     * 제목분유형목록(JSON형식문자열)
     * 형식: {"내용설치전체유형":["완료법/정보","내용"],"공가능유형":["완료프로세스코드오류, 불가실행"]}
     */
    @NotBlank(message = "제목분유형비워 둘 수 없습니다")
    private String categories;

    /**
     * 제목설명
     */
    @NotBlank(message = "제목설명비워 둘 수 없습니다")
    @Size(max = 5000, message = "제목설명길이정도할 수 없음초과경과5000문자기호")
    private String description;

    /**
     * 이미지파일ID목록(다중3개)
     * 프론트엔드필요호출Python서비스업로드이미지, 가져오기fileId후
     */
    @Size(max = 3, message = "다중가능업로드3이미지")
    private List<@NotBlank(message = "이미지파일ID비워 둘 수 없습니다") String> imageIds;
}