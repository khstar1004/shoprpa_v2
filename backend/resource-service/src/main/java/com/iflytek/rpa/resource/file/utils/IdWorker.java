package com.iflytek.rpa.resource.file.utils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import org.springframework.stereotype.Component;

/**
 * <p>이름: IdWorker.java</p>
 * <p>설명: 분방식증가길이ID</p>
 * <pre>
 *     Twitter의 Snowflake JAVA방법
 * </pre>
 * 코드로IdWorker개유형, 기존관리결과예아래, 분사용일개0테이블일위치, 사용—분열기모듈분의사용: 
 * 1||0---0000000000 0000000000 0000000000 0000000000 0 --- 00000 ---00000 ---000000000000
 * 에서위의문자열중, 일위치로사용할 수 없습니다(위가능로long의기호위치), 연결아래의41위치로초단계시간, 
 * 후5위치datacenter식별자위치, 5위치기기ID(아니요식별자기호, 예로식별자), 
 * 후12위치해당초내부의현재초내부의계획데이터, 추가64위치, 로일개Long유형.
 * 의예, 위시간증가정렬, 개분방식시스템내부아니요제품ID(datacenter및기기ID분), 
 * 높이, 시도, snowflake매초가능제품26ID왼쪽오른쪽, 전체가득필요.
 * <p>
 * 64위치ID (42(초)+5(기기ID)+5(서비스코드)+12(재복사추가))
 */
@Component
public class IdWorker {
    // 시간, 로, 일가져오기시스템의시간(일지정할 수 없음변수)
    private static final long twepoch = 1288834974657L;
    // 기기식별자위치데이터
    private static final long workerIdBits = 5L;
    // 데이터중식별자위치데이터
    private static final long datacenterIdBits = 5L;
    // 기기ID대값
    private static final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    // 데이터중ID대값
    private static final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    // 초내부증가위치
    private static final long sequenceBits = 12L;
    // 기기ID왼쪽12위치
    private static final long workerIdShift = sequenceBits;
    // 데이터중ID왼쪽17위치
    private static final long datacenterIdShift = sequenceBits + workerIdBits;
    // 시간초왼쪽22위치
    private static final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    private static final long sequenceMask = -1L ^ (-1L << sequenceBits);
    /* 위제품id시간 */
    private static long lastTimestamp = -1L;
    private final long workerId;
    // 데이터식별자id모듈분
    private final long datacenterId;
    // 0, 발송제어
    private long sequence = 0L;

    public IdWorker() {
        this.datacenterId = getDatacenterId(maxDatacenterId);
        this.workerId = getMaxWorkerId(datacenterId, maxWorkerId);
    }

    /**
     * @param workerId     기기ID
     * @param datacenterId 순서열
     */
    public IdWorker(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * <p>
     * 가져오기 maxWorkerId
     * </p>
     */
    protected static long getMaxWorkerId(long datacenterId, long maxWorkerId) {
        StringBuffer mpid = new StringBuffer();
        mpid.append(datacenterId);
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (!name.isEmpty()) {
            /*
             * GET jvmPid
             */
            mpid.append(name.split("@")[0]);
        }
        /*
         * MAC + PID 의 hashcode 가져오기16개낮음위치
         */
        return (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
    }

    /**
     * <p>
     * 데이터식별자id모듈분
     * </p>
     */
    protected static long getDatacenterId(long maxDatacenterId) {
        long id = 0L;
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                id = 1L;
            } else {
                byte[] mac = network.getHardwareAddress();
                id = ((0x000000FF & (long) mac[mac.length - 1]) | (0x0000FF00 & (((long) mac[mac.length - 2]) << 8)))
                        >> 6;
                id = id % (maxDatacenterId + 1);
            }
        } catch (Exception e) {
            System.out.println(" getDatacenterId: " + e.getMessage());
        }
        return id;
    }

    public static void main(String[] args) {

        IdWorker idWorker = new IdWorker(0, 0);

        for (int i = 0; i < 10000; i++) {
            long nextId = idWorker.nextId();
            System.out.println(nextId);
        }
    }

    /**
     * 가져오기아래일개ID
     *
     * @return
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format(
                    "Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        if (lastTimestamp == timestamp) {
            // 현재초내부, 이면+1
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // 현재초내부계획데이터가득완료, 이면대기아래일초
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        // ID그룹합치기완료종료의ID, 반환ID
        long nextId = ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;

        return nextId;
    }

    private long tilNextMillis(final long lastTimestamp) {
        long timestamp = this.timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = this.timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }
}