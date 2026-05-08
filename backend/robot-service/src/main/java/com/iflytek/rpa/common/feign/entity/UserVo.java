package com.iflytek.rpa.common.feign.entity;

import lombok.Data;

/**
 * @desc: ShopRPA authentication compatibility component.
 * @author: weilai <laiwei3@iflytek.com>
 * @create: 2025/11/27 17:22
 */
@Data
public class UserVo {
    String userId; // 사용자ID
    String userName; // 사용자명
    String userPhone; // 사용자휴대폰 번호
}