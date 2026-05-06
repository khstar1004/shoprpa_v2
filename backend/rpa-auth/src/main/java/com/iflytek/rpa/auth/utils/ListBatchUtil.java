package com.iflytek.rpa.auth.utils;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author mjren
 * @date 2025-01-15 16:32
 * @copyright Copyright (c) 2025 mjren
 * 일사용량업데이트및삽입데이터
 */
public class ListBatchUtil {
    /**
     * 량목록 데이터
     *
     * @param dataList  데이터목록
     * @param batchSize 매크기
     * @param operationMethod  데이터의데이터
     * @param <T>       데이터 유형
     */
    public static <T> void process(List<T> dataList, int batchSize, Consumer<List<T>> operationMethod) {
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("Data list must not be null or empty.");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive.");
        }

        int totalSize = dataList.size();

        // 사용 for 분관리
        for (int fromIndex = 0; fromIndex < totalSize; fromIndex += batchSize) {
            int toIndex = Math.min(fromIndex + batchSize, totalSize);
            List<T> batchList = dataList.subList(fromIndex, toIndex);
            operationMethod.accept(batchList); // 실행삽입
        }
    }
}