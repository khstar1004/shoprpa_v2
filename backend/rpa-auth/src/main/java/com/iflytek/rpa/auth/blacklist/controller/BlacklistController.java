package com.iflytek.rpa.auth.blacklist.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.rpa.auth.blacklist.dao.UserBlacklistDao;
import com.iflytek.rpa.auth.blacklist.dto.*;
import com.iflytek.rpa.auth.blacklist.entity.UserBlacklist;
import com.iflytek.rpa.auth.blacklist.service.BlackListService;
import com.iflytek.rpa.auth.utils.AppResponse;
import com.iflytek.rpa.auth.utils.ErrorCodeEnum;
import com.iflytek.sec.uap.client.api.UapUserInfoAPI;
import com.iflytek.sec.uap.client.core.dto.user.UapUser;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 차단 목록 관리 API
 *
 * @author system
 * @date 2025-12-16
 */
@Slf4j
@RestController
@RequestMapping("/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlackListService blackListService;
    private final UserBlacklistDao userBlacklistDao;

    /**
     * 사용자를 차단 목록에 추가합니다.
     *
     * @param dto 차단 추가 DTO
     * @param request HTTP 요청 
     * @return 차단 기록
     */
    @PostMapping("/add")
    public AppResponse<BlacklistVo> add(@RequestBody @Validated AddBlacklistDto dto, HttpServletRequest request) {
        try {
            UapUser loginUser = UapUserInfoAPI.getLoginUser(request);
            String operator = loginUser != null ? loginUser.getLoginName() : "ADMIN";

            log.info(
                    "사용자 차단 추가 요청, userId: {}, username: {}, reason: {}, operator: {}",
                    dto.getUserId(),
                    dto.getUsername(),
                    dto.getReason(),
                    operator);

            UserBlacklist blacklist =
                    blackListService.add(dto.getUserId(), dto.getUsername(), dto.getReason(), operator);

            return AppResponse.success(convertToVo(blacklist));
        } catch (Exception e) {
            log.error("사용자 차단 추가 실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자 차단 추가 실패: " + e.getMessage());
        }
    }

    /**
     * 사용자 차단을 해제합니다.
     *
     * @param dto 해제 DTO
     * @param request HTTP 요청 
     * @return 성공 여부
     */
    @PostMapping("/unban")
    public AppResponse<Boolean> unban(@RequestBody @Validated UnbanDto dto, HttpServletRequest request) {
        try {
            UapUser loginUser = UapUserInfoAPI.getLoginUser(request);
            String operator = loginUser != null ? loginUser.getLoginName() : "ADMIN";

            log.info("사용자 차단 해제 요청, userId: {}, operator: {}", dto.getUserId(), operator);

            boolean success = blackListService.unban(dto.getUserId(), operator);
            return AppResponse.success(success);
        } catch (Exception e) {
            log.error("사용자 차단 해제 실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자 차단 해제 실패: " + e.getMessage());
        }
    }

    /**
     * 사용자가 현재 차단 중인지 조회합니다.
     *
     * @param userId 사용자ID
     * @return 차단 정보
     */
    @GetMapping("/check")
    public AppResponse<BlacklistCacheDto> check(@RequestParam @NotBlank(message = "사용자 ID는 비워 둘 수 없습니다") String userId) {
        try {
            log.info("사용자 차단 상태 조회, userId: {}", userId);
            BlacklistCacheDto blacklist = blackListService.isBlocked(userId);
            return AppResponse.success(blacklist);
        } catch (Exception e) {
            log.error("사용자 차단 상태 조회 실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자 차단 상태 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 사용자 차단 목록을 페이지 단위로 조회합니다.
     *
     * @param queryDto 조회 조건
     * @return 차단 목록
     */
    @PostMapping("/list")
    public AppResponse<IPage<BlacklistVo>> list(@RequestBody BlacklistQueryDto queryDto) {
        try {
            log.info("사용자 차단 목록 조회, query: {}", queryDto);

            LambdaQueryWrapper<UserBlacklist> wrapper = new LambdaQueryWrapper<>();

            if (!StringUtils.isEmpty(queryDto.getUserId())) {
                wrapper.eq(UserBlacklist::getUserId, queryDto.getUserId());
            }

            if (!StringUtils.isEmpty(queryDto.getUsername())) {
                wrapper.like(UserBlacklist::getUsername, queryDto.getUsername());
            }

            if (queryDto.getStatus() != null) {
                wrapper.eq(UserBlacklist::getStatus, queryDto.getStatus());
            }

            wrapper.orderByDesc(UserBlacklist::getCreateTime);

            Page<UserBlacklist> page = new Page<>(queryDto.getPageNum(), queryDto.getPageSize());
            IPage<UserBlacklist> result = userBlacklistDao.selectPage(page, wrapper);

            IPage<BlacklistVo> voPage = result.convert(this::convertToVo);

            return AppResponse.success(voPage);
        } catch (Exception e) {
            log.error("사용자 차단 목록 조회 실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자 차단 목록 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 사용자의 차단 이력을 조회합니다.
     *
     * @param userId 사용자ID
     * @return 목록
     */
    @GetMapping("/history")
    public AppResponse<List<BlacklistVo>> getHistory(@RequestParam @NotBlank(message = "사용자 ID는 비워 둘 수 없습니다") String userId) {
        try {
            log.info("사용자 차단 이력 조회, userId: {}", userId);
            List<UserBlacklist> history = blackListService.getHistory(userId);
            List<BlacklistVo> voList = history.stream().map(this::convertToVo).collect(Collectors.toList());
            return AppResponse.success(voList);
        } catch (Exception e) {
            log.error("사용자 차단 이력 조회 실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "사용자 차단 이력 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 만료된 사용자 차단을 일괄 해제합니다.
     *
     * @return 해제 수
     */
    @PostMapping("/batch-unban-expired")
    public AppResponse<Integer> batchUnbanExpired() {
        try {
            log.info("만료된 사용자 차단 일괄 해제 요청");
            int count = blackListService.batchUnbanExpired();
            return AppResponse.success(count);
        } catch (Exception e) {
            log.error("만료된 사용자 차단 일괄 해제 실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "만료된 사용자 차단 일괄 해제 실패: " + e.getMessage());
        }
    }

    private BlacklistVo convertToVo(UserBlacklist blacklist) {
        LocalDateTime now = LocalDateTime.now();
        long remainingSeconds = 0;
        String remainingTimeDesc = "만료됨";

        if (blacklist.getStatus() == 1 && blacklist.getEndTime().isAfter(now)) {
            remainingSeconds = Duration.between(now, blacklist.getEndTime()).getSeconds();
            remainingTimeDesc = formatDuration(remainingSeconds);
        }

        return BlacklistVo.builder()
                .id(blacklist.getId())
                .userId(blacklist.getUserId())
                .username(blacklist.getUsername())
                .banReason(blacklist.getBanReason())
                .banLevel(blacklist.getBanLevel())
                .banCount(blacklist.getBanCount())
                .banDuration(blacklist.getBanDuration())
                .banDurationDesc(formatDuration(blacklist.getBanDuration()))
                .startTime(blacklist.getStartTime())
                .endTime(blacklist.getEndTime())
                .remainingSeconds(remainingSeconds)
                .remainingTimeDesc(remainingTimeDesc)
                .status(blacklist.getStatus())
                .statusDesc(blacklist.getStatus() == 1 ? "차단 중" : "해제됨")
                .operator(blacklist.getOperator())
                .createTime(blacklist.getCreateTime())
                .build();
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "0초";
        }

        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("일");
        }
        if (hours > 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(hours).append("시간");
        }
        if (minutes > 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(minutes).append("분");
        }
        if (secs > 0 && days == 0 && hours == 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(secs).append("초");
        }

        return sb.length() > 0 ? sb.toString() : "0초";
    }
}
