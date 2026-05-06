package com.iflytek.rpa.auth.core.entity;

import lombok.Data;

@Data
public class UserVo {
    String userId; // 사용자ID
    String userName; // 사용자명
    String userPhone; // 사용자휴대폰 번호
}