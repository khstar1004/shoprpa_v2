package com.iflytek.rpa.auth.sp.uap.entity;

import lombok.Data;

/**
 * 통신사용 ID, NUM
 * @author keler
 * @date 2020/2/27
 */
@Data
public class CommonIdNum<T> {
    private Long id;
    private Long num;
    private T target;
}