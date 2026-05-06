package com.iflytek.rpa.task.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * 스케줄링작업(ScheduleTask)유형
 *
 * @author makejava
 * @since 2024-09-29 15:33:31
 */
public class ScheduleTask implements Serializable {
    private static final long serialVersionUID = -68626113322208001L;

    private Long id;
    /**
     * 예약 작업id
     */
    private String taskId;
    /**
     * 작업이름
     */
    @NotNull(message = "작업이름비워 둘 수 없습니다")
    private String name;
    /**
     * 설명
     */
    private String description;
    /**
     * 실행봇순서열
     */
    //    @JsonSerialize(using = ListRobotJsonSerializer.class)
    private List<String> executeSequence;

    private String allRobotName;

    /**
     * 예외관리방식: stop중지  skip건너뛰기
     */
    private String exceptionHandleWay;
    /**
     * 실행방식, circular,예약fixed,지정custom
     */
    private String runMode;
    /**
     * , -1로있음일, 3600, , , custom
     */
    private String cycleFrequency;
    /**
     * 유형, 매1시간, 매3시간, , 지정
     */
    private String cycleNum;
    /**
     * 단일위치: minutes, hour
     */
    private String cycleUnit;
    /**
     * 상태: doing실행중 close완료결과 ready대기실행
     */
    private String status;
    /**
     * 시작/사용 안 함
     */
    private Integer enable;
    /**
     * 예약방식,day,month,week(type로`schedule`시)
     */
    private String scheduleType;
    /**
     * 예약매칭(매칭객체)
     */
    private String scheduleRule;
    /**
     * 시작 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date startAt;
    /**
     * 종료 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date endAt;

    private String tenantId;
    /**
     * 여부정렬팀실행
     */
    private Integer enableQueueExecution;

    /**
     * cron테이블방식
     */
    private String cronExpression;

    /**
     * cron테이블방식또는초데이터
     */
    private String scheduleConf;
    /**
     * 위실행시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastTime;
    /**
     * 아래실행시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date nextTime;
    /**
     * 생성사람ID
     */
    private String creatorId;
    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    /**
     * 수정자id
     */
    private String updaterId;
    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    private Integer deleted;
    /**
     * 가져오기시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date pullTime;

    private String logEnable;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAllRobotName() {
        return allRobotName;
    }

    public void setAllRobotName(String allRobotName) {
        this.allRobotName = allRobotName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getExecuteSequence() {
        return executeSequence;
    }

    public void setExecuteSequence(List<String> executeSequence) {
        this.executeSequence = executeSequence;
    }

    public String getExceptionHandleWay() {
        return exceptionHandleWay;
    }

    public void setExceptionHandleWay(String exceptionHandleWay) {
        this.exceptionHandleWay = exceptionHandleWay;
    }

    public String getRunMode() {
        return runMode;
    }

    public void setRunMode(String runMode) {
        this.runMode = runMode;
    }

    public String getCycleFrequency() {
        return cycleFrequency;
    }

    public void setCycleFrequency(String cycleFrequency) {
        this.cycleFrequency = cycleFrequency;
    }

    public String getCycleNum() {
        return cycleNum;
    }

    public void setCycleNum(String cycleNum) {
        this.cycleNum = cycleNum;
    }

    public String getCycleUnit() {
        return cycleUnit;
    }

    public void setCycleUnit(String cycleUnit) {
        this.cycleUnit = cycleUnit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getEnable() {
        return enable;
    }

    public void setEnable(Integer enable) {
        this.enable = enable;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getScheduleRule() {
        return scheduleRule;
    }

    public void setScheduleRule(String scheduleRule) {
        this.scheduleRule = scheduleRule;
    }

    public Date getStartAt() {
        return startAt;
    }

    public void setStartAt(Date startAt) {
        this.startAt = startAt;
    }

    public Date getEndAt() {
        return endAt;
    }

    public void setEndAt(Date endAt) {
        this.endAt = endAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getEnableQueueExecution() {
        return enableQueueExecution;
    }

    public void setEnableQueueExecution(Integer enableQueueExecution) {
        this.enableQueueExecution = enableQueueExecution;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getScheduleConf() {
        return scheduleConf;
    }

    public void setScheduleConf(String scheduleConf) {
        this.scheduleConf = scheduleConf;
    }

    public Date getLastTime() {
        return lastTime;
    }

    public void setLastTime(Date lastTime) {
        this.lastTime = lastTime;
    }

    public Date getNextTime() {
        return nextTime;
    }

    public void setNextTime(Date nextTime) {
        this.nextTime = nextTime;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdaterId() {
        return updaterId;
    }

    public void setUpdaterId(String updaterId) {
        this.updaterId = updaterId;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Date getPullTime() {
        return pullTime;
    }

    public void setPullTime(Date pullTime) {
        this.pullTime = pullTime;
    }

    public String getLogEnable() {
        return logEnable;
    }

    public void setLogEnable(String logEnable) {
        this.logEnable = logEnable;
    }
}