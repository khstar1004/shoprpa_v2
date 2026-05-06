package com.iflytek.rpa.dispatch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iflytek.rpa.dispatch.entity.CronJson;
import java.text.ParseException;
import java.util.List;

public interface CronJsonService {
    /**
     * 근거cron매칭계획미완료실행시간
     *
     * @param cronJson cron매칭JSON문자열
     * @return 미완료실행시간목록
     * @throws JsonProcessingException JSON파싱예외
     */
    List<String> getFutureList(String cronJson) throws Exception;

    List<String> calculateFutureExecuteTime(CronJson cron, Integer times) throws ParseException;
}