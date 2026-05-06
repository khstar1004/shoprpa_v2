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
 * 이름단일관리관리연결
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
     * 추가사용자까지이름단일
     *
     * @param dto 추가이름단일 DTO
     * @param request HTTP 요청 
     * @return 이름단일기록
     */
    @PostMapping("/add")
    public AppResponse<BlacklistVo> add(@RequestBody @Validated AddBlacklistDto dto, HttpServletRequest request) {
        try {
            // 가져오기현재사람
            UapUser loginUser = UapUserInfoAPI.getLoginUser(request);
            String operator = loginUser != null ? loginUser.getLoginName() : "ADMIN";

            log.info(
                    "추가이름단일, userId: {}, username: {}, reason: {}, operator: {}",
                    dto.getUserId(),
                    dto.getUsername(),
                    dto.getReason(),
                    operator);

            UserBlacklist blacklist =
                    blackListService.add(dto.getUserId(), dto.getUsername(), dto.getReason(), operator);

            return AppResponse.success(convertToVo(blacklist));
        } catch (Exception e) {
            log.error("추가이름단일실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "추가이름단일실패: " + e.getMessage());
        }
    }

    /**
     * 해제사용자
     *
     * @param dto 해제 DTO
     * @param request HTTP 요청 
     * @return 여부성공
     */
    @PostMapping("/unban")
    public AppResponse<Boolean> unban(@RequestBody @Validated UnbanDto dto, HttpServletRequest request) {
        try {
            // 가져오기현재사람
            UapUser loginUser = UapUserInfoAPI.getLoginUser(request);
            String operator = loginUser != null ? loginUser.getLoginName() : "ADMIN";

            log.info("해제사용자, userId: {}, operator: {}", dto.getUserId(), operator);

            boolean success = blackListService.unban(dto.getUserId(), operator);
            return AppResponse.success(success);
        } catch (Exception e) {
            log.error("해제사용자실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "해제사용자실패: " + e.getMessage());
        }
    }

    /**
     * 조회사용자여부에서이름단일중
     *
     * @param userId 사용자ID
     * @return 이름단일정보
     */
    @GetMapping("/check")
    public AppResponse<BlacklistCacheDto> check(@RequestParam @NotBlank(message = "사용자 ID는 비워 둘 수 없습니다") String userId) {
        try {
            log.info("조회사용자이름단일상태, userId: {}", userId);
            BlacklistCacheDto blacklist = blackListService.isBlocked(userId);
            return AppResponse.success(blacklist);
        } catch (Exception e) {
            log.error("조회이름단일실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회이름단일실패: " + e.getMessage());
        }
    }

    /**
     * 조회이름단일목록(분)
     *
     * @param queryDto 조회파일
     * @return 이름단일목록
     */
    @PostMapping("/list")
    public AppResponse<IPage<BlacklistVo>> list(@RequestBody BlacklistQueryDto queryDto) {
        try {
            log.info("조회이름단일목록, 파일: {}", queryDto);

            // 생성조회파일
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

            // 분조회
            Page<UserBlacklist> page = new Page<>(queryDto.getPageNum(), queryDto.getPageSize());
            IPage<UserBlacklist> result = userBlacklistDao.selectPage(page, wrapper);

            // 변환로 VO
            IPage<BlacklistVo> voPage = result.convert(this::convertToVo);

            return AppResponse.success(voPage);
        } catch (Exception e) {
            log.error("조회이름단일목록실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회이름단일목록실패: " + e.getMessage());
        }
    }

    /**
     * 조회사용자의
     *
     * @param userId 사용자ID
     * @return 목록
     */
    @GetMapping("/history")
    public AppResponse<List<BlacklistVo>> getHistory(@RequestParam @NotBlank(message = "사용자 ID는 비워 둘 수 없습니다") String userId) {
        try {
            log.info("조회사용자, userId: {}", userId);
            List<UserBlacklist> history = blackListService.getHistory(userId);
            List<BlacklistVo> voList = history.stream().map(this::convertToVo).collect(Collectors.toList());
            return AppResponse.success(voList);
        } catch (Exception e) {
            log.error("조회실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "조회실패: " + e.getMessage());
        }
    }

    /**
     * 트리거량해제완료경과사용자
     *
     * @return 해제수
     */
    @PostMapping("/batch-unban-expired")
    public AppResponse<Integer> batchUnbanExpired() {
        try {
            log.info("트리거량해제");
            int count = blackListService.batchUnbanExpired();
            return AppResponse.success(count);
        } catch (Exception e) {
            log.error("량해제실패", e);
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "량해제실패: " + e.getMessage());
        }
    }

    /**
     * 변환로이미지객체
     */
    private BlacklistVo convertToVo(UserBlacklist blacklist) {
        LocalDateTime now = LocalDateTime.now();
        long remainingSeconds = 0;
        String remainingTimeDesc = "완료경과";

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
                .statusDesc(blacklist.getStatus() == 1 ? "중" : "완료해제")
                .operator(blacklist.getOperator())
                .createTime(blacklist.getCreateTime())
                .build();
    }

    /**
     * 형식시길이
     */
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
            sb.append(days).append("");
        }
        if (hours > 0) {
            sb.append(hours).append("시간");
        }
        if (minutes > 0) {
            sb.append(minutes).append("분");
        }
        if (secs > 0 && days == 0 && hours == 0) {
            sb.append(secs).append("초");
        }

        return sb.length() > 0 ? sb.toString() : "0초";
    }
}