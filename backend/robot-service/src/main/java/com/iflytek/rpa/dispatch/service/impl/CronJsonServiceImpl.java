package com.iflytek.rpa.dispatch.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.rpa.dispatch.entity.CronJson;
import com.iflytek.rpa.dispatch.service.CronJsonService;
import com.iflytek.rpa.task.service.CronExpression;
import com.iflytek.rpa.utils.DateUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("CronJsonService")
public class CronJsonServiceImpl implements CronJsonService {
    @Override
    public List<String> getFutureList(String cronJson) throws Exception {
        if (null == cronJson || cronJson.isEmpty()) {
            return Collections.emptyList();
        }
        ObjectMapper mapper = new ObjectMapper();
        CronJson object = mapper.readValue(cronJson, CronJson.class);
        Integer times = Integer.valueOf(object.getTimes());
        if (times <= 0) {
            times = 5;
        }
        // 사용파싱출력의CronJson객체계획실행시간
        return calculateFutureExecuteTime(object, times);
    }

    @Override
    public List<String> calculateFutureExecuteTime(CronJson cron, Integer times) throws ParseException {
        List<String> futureExecTimes = new ArrayList<>();
        // 완료cron테이블방식
        String cronExpression = generateCronExpression(cron);
        if (cronExpression == null) {
            return futureExecTimes;
        }

        // 파싱종료 시간(결과가저장에서)
        Date endTime = null;
        String endTimeStr = cron.getEndTime();
        // 결과가종료 시간아니요비어 있습니다, 이면파싱로Date객체
        if (endTimeStr != null && !endTimeStr.trim().isEmpty()) {
            endTime = DateUtils.sdfdaytime.parse(endTimeStr);
        }
        // 생성CronExpression객체
        CronExpression cronExpr = new CronExpression(cronExpression);
        // 에서현재시간열기 계획
        Date nextRun = new Date();
        // 계획 5
        // 계획미완료N실행시간
        for (int i = 0; i < times; i++) {
            nextRun = cronExpr.getNextValidTimeAfter(nextRun);
            if (nextRun == null) {
                break;
            }
            // 초과경과예약종료 시간, 중지계획
            if (endTime != null && nextRun.after(endTime)) {
                break;
            }
            futureExecTimes.add(DateUtils.sdfdaytime.format(nextRun));
        }
        return futureExecTimes;
    }

    /**
     * 근거아니요의유형완료cron테이블방식
     */
    private String generateCronExpression(CronJson cron) {
        String frequencyFlag = cron.getFrequencyFlag();

        if (frequencyFlag == null) {
            return null;
        }

        switch (frequencyFlag.toLowerCase()) {
            case "regular":
                return generateRegularCron(cron.getTimeExpression());
            case "minutes":
                return generateMinutesCron(cron.getMinutes());
            case "hours":
                return generateHoursCron(cron.getMinutes(), cron.getHours());
            case "days":
                return generateDaysCron(cron.getMinutes(), cron.getHours());
            case "weeks":
                return generateWeeksCron(cron.getMinutes(), cron.getHours(), cron.getWeeks());
            case "months":
                return generateMonthsCron(cron.getMinutes(), cron.getHours(), cron.getWeeks(), cron.getMonths());
            case "advance":
                return cron.getCronExpression();
            default:
                throw new IllegalArgumentException("지원하지 않음의유형: " + frequencyFlag);
        }
    }

    /**
     * 완료예약실행의cron테이블방식 (regular유형)
     */
    private String generateRegularCron(String timeExpression) {
        if (timeExpression == null) {
            return null;
        }
        // 파싱시간테이블방식
        String[] parts = timeExpression.split(" ");
        if (parts.length != 2) {
            return null;
        }
        String[] dateParts = parts[0].split("-");
        String[] timeParts = parts[1].split(":");
        if (dateParts.length != 3 || timeParts.length != 3) {
            return null;
        }
        String second = timeParts[2];
        String minute = timeParts[1];
        String hour = timeParts[0];
        String day = dateParts[2];
        String month = dateParts[1];
        String year = dateParts[0];
        // Spring의cron테이블방식형식: 초 분 시 일 월  [년]
        return String.format("%s %s %s %s %s ? %s", second, minute, hour, day, month, year);
    }

    /**
     * 완료분실행의cron테이블방식
     */
    private String generateMinutesCron(Integer minutes) {
        if (minutes == null || minutes < 1 || minutes >= 60) {
            throw new IllegalArgumentException("분에서1-59");
        }
        // 매N분실행일
        return String.format("0 0/%d * * * ?", minutes);
    }

    /**
     * 완료시간실행의cron테이블방식
     */
    private String generateHoursCron(Integer minutes, Integer hours) {
        if (minutes == null || hours == null) {
            throw new IllegalArgumentException("분및시간비워 둘 수 없습니다");
        }
        if (minutes < 0 || minutes >= 60) {
            throw new IllegalArgumentException("분에서0-59");
        }
        if (hours < 1 || hours > 24) {
            throw new IllegalArgumentException("시간에서1-24");
        }
        // 매N시간의M분실행
        return String.format("0 %d 0/%d * * ?", minutes, hours);
    }

    /**
     * 완료매일실행의cron테이블방식
     */
    private String generateDaysCron(Integer minutes, Integer hours) {
        if (minutes == null || hours == null) {
            throw new IllegalArgumentException("분및시간비워 둘 수 없습니다");
        }
        if (minutes < 0 || minutes >= 60) {
            throw new IllegalArgumentException("분에서0-59");
        }
        if (hours < 0 || hours >= 24) {
            throw new IllegalArgumentException("시간에서0-23");
        }
        // 매일의예약실행
        return String.format("0 %d %d * * ?", minutes, hours);
    }

    /**
     * 완료실행의cron테이블방식
     */
    private String generateWeeksCron(Integer minutes, Integer hours, List<Integer> weeks) {
        if (minutes == null || hours == null || weeks == null || weeks.isEmpty()) {
            throw new IllegalArgumentException("분, 시간및Shoprpa비워 둘 수 없습니다");
        }
        if (minutes < 0 || minutes >= 60) {
            throw new IllegalArgumentException("분에서0-59");
        }
        if (hours < 0 || hours >= 24) {
            throw new IllegalArgumentException("시간에서0-23");
        }
        // 인증Shoprpa값 (0=일요일, 1=월요일, ... 6=토요일)
        for (Integer week : weeks) {
            if (week < 0 || week > 6) {
                throw new IllegalArgumentException("Shoprpa에서0-6");
            }
        }
        // 변환로Spring cron형식의Shoprpa (1=일요일, 2=월요일, ... 7=토요일)
        List<String> springWeeks = new ArrayList<>();
        for (Integer week : weeks) {
            springWeeks.add(String.valueOf(week + 2));
        }
        String weekExpression = String.join(",", springWeeks);
        return String.format("0 %d %d ? * %s", minutes, hours, weekExpression);
    }

    /**
     * 완료월실행의cron테이블방식
     */
    private String generateMonthsCron(Integer minutes, Integer hours, List<Integer> weeks, List<Integer> months) {
        if (minutes == null
                || hours == null
                || weeks == null
                || months == null
                || weeks.isEmpty()
                || months.isEmpty()) {
            throw new IllegalArgumentException("분, 시간, Shoprpa및월비워 둘 수 없습니다");
        }
        if (minutes < 0 || minutes >= 60) {
            throw new IllegalArgumentException("분에서0-59");
        }
        if (hours < 0 || hours >= 24) {
            throw new IllegalArgumentException("시간에서0-23");
        }
        // 인증Shoprpa값
        for (Integer week : weeks) {
            if (week < 0 || week > 6) {
                throw new IllegalArgumentException("Shoprpa에서0-6");
            }
        }
        // 인증월값
        for (Integer month : months) {
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("월에서1-12");
            }
        }
        // 변환Shoprpa형식
        List<String> springWeeks = new ArrayList<>();
        // 변환로Spring cron형식의Shoprpa (1=일요일, 2=월요일, ... 7=토요일)
        for (Integer week : weeks) {
            springWeeks.add(String.valueOf(week + 2));
        }
        String weekExpression = String.join(",", springWeeks);
        String monthExpression = months.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return String.format("0 %d %d ? %s %s", minutes, hours, monthExpression, weekExpression);
    }
}