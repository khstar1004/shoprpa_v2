package com.iflytek.rpa.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.util.Date;
import lombok.Data;

/**
 * 즐겨찾기의기존테이블
 * 즐겨찾기까지 "사용자" 단계
 */
@Data
public class AtomLike {
    @TableId(value = "id", type = IdType.AUTO)
    Long id;

    Long likeId; // 즐겨찾기id
    String atomKey; // 기존가능의key, 전체영역일
    String creatorId; // 사용자id
    String updaterId;
    String tenantId; // 테넌트id

    @TableLogic(value = "0", delval = "1")
    Integer isDeleted;

    Date createTime;
    Date updateTime;
}