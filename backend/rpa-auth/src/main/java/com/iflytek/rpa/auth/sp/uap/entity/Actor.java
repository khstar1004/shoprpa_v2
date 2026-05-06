package com.iflytek.rpa.auth.sp.uap.entity;

import lombok.Data;

/**
 * 현재사람
 *
 * 연결또는사용자
 *
 * @author keler
 * @date 2020/3/8
 */
@Data
public class Actor {
    /** ID */
    private Long id;

    /** 계정:사용자계정또는연결계정 */
    private String account;

    /** 이름:사용자이름또는연결이름 */
    private String name;
}