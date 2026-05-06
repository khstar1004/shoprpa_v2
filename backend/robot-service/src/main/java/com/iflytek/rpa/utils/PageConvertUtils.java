package com.iflytek.rpa.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;

/**
 * 분결과변환도구유형
 */
public class PageConvertUtils {

    /**
     * 분결과변환
     * @param page 분객체
     * @param targetClass 목록유형
     * @param <T> 유형
     * @param <R> 목록유형
     * @return 변환후의분객체
     */
    public static <T, R> IPage<R> convertPage(IPage<T> page, Class<R> targetClass) {
        if (page == null) {
            return null;
        }

        // 변환기록목록
        List<R> records = page.getRecords().stream()
                .map(source -> {
                    try {
                        R target = targetClass.newInstance();
                        BeanUtils.copyProperties(source, target);
                        return target;
                    } catch (Exception e) {
                        throw new RuntimeException("객체변환실패", e);
                    }
                })
                .collect(Collectors.toList());

        // 생성새의분객체
        IPage<R> resultPage = new Page<>();
        resultPage.setRecords(records);
        resultPage.setCurrent(page.getCurrent());
        resultPage.setSize(page.getSize());
        resultPage.setTotal(page.getTotal());
        resultPage.setPages(page.getPages());

        return resultPage;
    }

    /**
     * 분결과변환(사용지정변환기기)
     * @param page 분객체
     * @param converter 변환기기데이터
     * @param <T> 유형
     * @param <R> 목록유형
     * @return 변환후의분객체
     */
    public static <T, R> IPage<R> convertPage(IPage<T> page, java.util.function.Function<T, R> converter) {
        if (page == null) {
            return null;
        }

        // 변환기록목록
        List<R> records = page.getRecords().stream().map(converter).collect(Collectors.toList());

        // 생성새의분객체
        IPage<R> resultPage = new Page<>();
        resultPage.setRecords(records);
        resultPage.setCurrent(page.getCurrent());
        resultPage.setSize(page.getSize());
        resultPage.setTotal(page.getTotal());
        resultPage.setPages(page.getPages());

        return resultPage;
    }
}