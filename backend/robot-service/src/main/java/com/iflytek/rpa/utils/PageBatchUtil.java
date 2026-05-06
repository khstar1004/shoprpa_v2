package com.iflytek.rpa.utils;

import java.util.function.BiConsumer;

public class PageBatchUtil {
    /**
     * 관리분관리방법법
     *
     * @param total     기록데이터
     * @param batchSize 매크기
     * @param consumer  관리데이터, 수신 limit 및 offset
     */
    public static void process(int total, int batchSize, BiConsumer<Integer, Integer> consumer) {
        // todo offset수정성공long
        if (total < 0 || batchSize <= 0) {
            throw new IllegalArgumentException("Total must be non-negative and batchSize must be positive.");
        }
        if (total == 0) {
            return; // 있음데이터, 직선연결반환
        }

        int pages = (int) Math.ceil((double) total / batchSize);
        for (int page = 0; page < pages; page++) {
            int offset = page * batchSize;
            int limit = Math.min(batchSize, total - offset);
            consumer.accept(limit, offset);
        }
    }
}