package com.iflytek.rpa.triggerTask.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.iflytek.rpa.triggerTask.entity.TaskMail;
import com.iflytek.rpa.utils.exception.NoLoginException;
import java.util.List;

/**
 * <p>
 * 작업실행큐 서비스유형
 * </p>
 *
 * @author jiechen39
 * @since 2022-12-16
 */
public interface ITaskMailService extends IService<TaskMail> {

    IPage<TaskMail> getTaskMailPage(Long pageNum, Long pageSize, String userId) throws NoLoginException;

    void saveMail(TaskMail mail) throws NoLoginException;

    String connectMail(TaskMail mail);

    boolean deleteMail(String resourceId);

    List<TaskMail> getTaskMailsByResourceIds(List<String> mailIds);
}