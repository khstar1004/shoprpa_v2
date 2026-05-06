package com.iflytek.rpa.conf.service;

import com.iflytek.rpa.conf.entity.vo.UserRegisterVo;
import com.iflytek.rpa.utils.response.AppResponse;

/**
 * 사용자회원가입서비스연결
 */
public interface UserRegisterService {

    /**
     * 사용자회원가입
     * @param phone 휴대폰 번호
     * @return 회원가입결과, 패키지계정및비밀번호
     */
    AppResponse<UserRegisterVo> register(String phone);
}