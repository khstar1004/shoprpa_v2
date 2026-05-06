package com.iflytek.rpa.triggerTask.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 스케줄링작업
 * </p>
 *
 * @author keler
 * @since 2021-10-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("task_mail")
public class TaskMail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String userId; // 사용자id

    private String tenantId; // 테넌트id

    private String resourceId; // 메일함id

    private String emailService; // 메일함서비스서버, 163Email, 126Email, qqEmail, customEmail

    private String emailProtocol; // 사용, POP3,IMAP

    private String emailServiceAddress; // 메일함서비스서버주소

    private String port; // 메일함서비스서버단말

    @TableField(value = "enable_ssl")
    private Boolean enableSSL; // 여부사용SSL

    private String emailAccount; // 메일함계정

    private String authorizationCode; // 메일함권한 부여코드

    @TableLogic(value = "0", delval = "1")
    private Integer deleted; // 삭제 여부 0: 삭제되지 않음, 1: 삭제됨
}