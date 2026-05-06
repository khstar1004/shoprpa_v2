package com.iflytek.rpa.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DateUtils {

    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    public static SimpleDateFormat sdfday = new SimpleDateFormat("yyyyMMdd");
    public static SimpleDateFormat sdfdaytime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 통신경과시간초초데이터개시간의(아니요24시간0)
     * @param date1
     * @param date2
     * @return
     */
    public static int differentDaysByMillisecond(Date date1, Date date2) {
        int days = (int) Math.ceil(Double.valueOf((date2.getTime() - date1.getTime()) / (1000 * 3600 * 24)));
        return days;
    }

    /**
     * 통신경과시간초초데이터개시간의(아니요24시간0)
     * @param date1
     * @param date2
     * @return
     */
    public static int differentHoursByMillisecond(Date date1, Date date2) {
        int days = (int) Math.ceil(Double.valueOf((date2.getTime() - date1.getTime()) / (1000 * 3600)));
        return days;
    }

    /**
     * 통신경과시간초초데이터개시간의(아니요24시간0)
     * @param date1
     * @param date2
     * @return
     */
    public static int differentMinutesByMillisecond(Date date1, Date date2) {
        int days = (int) Math.ceil(Double.valueOf((date2.getTime() - date1.getTime()) / (1000 * 60)));
        return days;
    }

    /**
     * 통신경과시간초초데이터개시간의(아니요24시간0)
     * @param date1
     * @param date2
     * @return
     */
    public static int differentSecondsByMillisecond(Date date1, Date date2) {
        int days = (int) Math.ceil(Double.valueOf((date2.getTime() - date1.getTime()) / (1000)));
        return days;
    }

    public static String getDayFormat() {
        return sdfday.format(new Date(System.currentTimeMillis()));
    }

    public static String getDayFormatByDate(Date date) {
        return sdf.format(date);
    }

    public static String getDayTimeFormat(Date date) {
        return sdfdaytime.format(date);
    }

    /*
     * 반환n전/후의날짜
     * */
    public static Date getCalDay(Date date, int calDays) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, calDays);
        return c.getTime();
    }

    /*
     * 반환n전/후의날짜
     * */
    public static Date getCalMinute(Date date, int calMinutes) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MINUTE, calMinutes);
        return c.getTime();
    }

    public static boolean haveToday(Date countTime) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String cTime = simpleDateFormat.format(countTime);
        String nowTime = simpleDateFormat.format(new Date());

        countTime = simpleDateFormat.parse(cTime);
        Date today = simpleDateFormat.parse(nowTime);

        return countTime.compareTo(today) >= 0;
    }

    public static Date getTodayZero() throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String nowTime = simpleDateFormat.format(new Date());
        Date today = simpleDateFormat.parse(nowTime);
        return today;
    }

    // 가져오기 대시간 2020-02-19 23:59:59
    public static Date getEndOfDay(Date date) {
        LocalDateTime localDateTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
        LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
        return Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    // 가져오기 시간 2020-02-19 00:00:00
    public static Date getStartOfDay(Date date) {
        LocalDateTime localDateTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
        LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
        return Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date getYesterdayEnd() {
        Date date = new Date();
        return getEndHourTimeOfDay(getCalDay(date, -1));
    }

    // date 변환 localDateTime
    public static LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static String localDateToYyyMdDdStr(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return localDateTime.format(formatter);
    }

    public static Date getEndHourTimeOfDay(Date date) {
        return getCalMinute(getEndOfDay(date), -2);
    }

    public static String getStartStrOfDay(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(date) + " 00:00:00"; // 직선연결연결 00:00:00
    }

    public static String getEndStrOfDay(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(date) + " 24:00:00"; // 직선연결연결 24:00:00
    }

    public static List<String> getStartAndEndOfDay(Date date) {
        // 가져오기 의시작 시간및종료 시간
        String startOfDay = getStartStrOfDay(date);
        String endOfDay = getEndStrOfDay(date);
        List<String> startAndEndOfDay = new ArrayList<>();
        startAndEndOfDay.add(startOfDay);
        startAndEndOfDay.add(endOfDay);
        return startAndEndOfDay;
    }

    public static List<String> getStartToDate(Date date) {
        String endOfDay = getEndStrOfDay(date);
        List<String> startAndEndOfDay = new ArrayList<>();
        startAndEndOfDay.add("1970-01-01 00:00:00");
        startAndEndOfDay.add(endOfDay);
        return startAndEndOfDay;
    }
}