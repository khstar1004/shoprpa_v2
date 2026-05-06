package com.iflytek.rpa.terminal.service.impl;

import static com.iflytek.rpa.terminal.constants.TerminalConstant.*;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.terminal.dao.TerminalDao;
import com.iflytek.rpa.terminal.entity.Terminal;
import com.iflytek.rpa.terminal.entity.TerminalLoginRecord;
import com.iflytek.rpa.terminal.entity.dto.BeatDto;
import com.iflytek.rpa.terminal.entity.dto.RegistryDto;
import com.iflytek.rpa.terminal.service.TerminalLoginRecordService;
import com.iflytek.rpa.terminal.service.TerminalService;
import com.iflytek.rpa.utils.HttpUtils;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.RedisUtils;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TerminalServiceImpl extends ServiceImpl<TerminalDao, Terminal> implements TerminalService {
    @Resource
    private TerminalDao terminalDao;

    @Autowired
    private TerminalLoginRecordService terminalLoginRecordService;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    public AppResponse<String> registry(RegistryDto registryDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || response.getData() == null) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        //        근거스케줄링방식및작업실행계획상태: 
        String terminalStatus = calculateState(registryDto.getStatus(), registryDto.getIsDispatch());
        registryDto.setStatus(terminalStatus);

        // 관리데이터
        processStaticData(registryDto);
        // 관리데이터
        processDynamicData(registryDto);
        // 기록단말의기록
        // 해당단말있음(있음로그인, 없음로그아웃)의기록, 필요있음해당단말의로그인기록, 필요로그인로그아웃.이면삽입새의로그인기록
        Integer recordNum = terminalLoginRecordService.countUnLogoutRecordByTerminalId(registryDto.getTerminalId());
        if (recordNum == null || recordNum.equals(0)) {
            String deptId = "";
            String levelCode = "";
            AppResponse<String> deptIdRes = rpaAuthFeign.getCurrentDeptId();
            if (deptIdRes.ok()) {
                deptId = deptIdRes.getData();
            }
            AppResponse<String> currentLevelCodeRes = rpaAuthFeign.getCurrentLevelCode();
            if (currentLevelCodeRes.ok()) {
                levelCode = currentLevelCodeRes.getData();
            }
            // 삽입
            TerminalLoginRecord loginRecord = new TerminalLoginRecord();
            loginRecord.setId(idWorker.nextId() + "");
            loginRecord.setTerminalId(registryDto.getTerminalId());
            loginRecord.setDeptId(deptId);
            loginRecord.setDeptIdPath(levelCode);
            loginRecord.setIp(registryDto.getIp());
            loginRecord.setLoginTime(new Date());
            loginRecord.setLogoutTime(null);
            loginRecord.setLoginStatus(1);
            loginRecord.setCreatorId(userId);
            loginRecord.setUpdaterId(userId);
            loginRecord.setCreateTime(new Date());
            loginRecord.setUpdateTime(new Date());
            loginRecord.setDeleted(0);
            terminalLoginRecordService.insertRecord(loginRecord);
        }
        // 있음로그인, 있음로그아웃, 설명위의일직선로그아웃, 이면아니요추가기록, 아니요관리

        return AppResponse.success("ok");
    }

    private void processIp(Terminal terminal) {
        // 가져오기클라이언트연결IP(경과관리정상후의)
        terminal.setActualClientIp(getClientIp());
    }

    public String getClientIp() {
        HttpServletRequest request = HttpUtils.getRequest();
        //  에서X-Forwarded-For가져오기(사용다중관리)
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            // 시도X-Real-IP(사용단일관리, 예Nginx)
            ip = request.getHeader("X-Real-IP");
        }
        // 관리X-Forwarded-For의다중IP(가져오기 일개unknown의IP)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    // 상태계획
    private String calculateState(String status, Integer isDispatch) {
        //        - 결과가열기시작스케줄링방식: 있음작업실행 -> 상태로실행중; 없음작업실행 -> 상태비어 있습니다
        //        - 미완료열기시작스케줄링방식 -> 상태로단일기기중
        if (isDispatch.equals(1)) {
            if (TERMINAL_STATUS_BUSY.equals(status)) {
                return TERMINAL_STATUS_BUSY;
            } else if (TERMINAL_STATUS_FREE.equals(status)) {
                return TERMINAL_STATUS_FREE;
            } else {
                log.error("단말상태오류: {}", status);
                throw new ServiceException(ErrorCodeEnum.E_PARAM_LOSE.getCode(), "단말상태오류");
            }
        }
        return TERMINAL_STATUS_STANDALONE;
    }

    private void processStaticData(RegistryDto registryDto) throws NoLoginException {
        Terminal terminal = new Terminal();
        BeanUtils.copyProperties(registryDto, terminal);

        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || response.getData() == null) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();

        AppResponse<String> deptIdRes = rpaAuthFeign.getCurrentDeptId();
        String deptId = null, deptIdPath = null;
        if (deptIdRes.ok()) {
            deptId = deptIdRes.getData();
            AppResponse<String> currentLevelCodeRes = rpaAuthFeign.getCurrentLevelCode();
            if (currentLevelCodeRes.ok()) {
                deptIdPath = currentLevelCodeRes.getData();
            }
        }
        terminal.setTenantId(tenantId);
        terminal.setUserId(userId);
        terminal.setDeptId(deptId);
        terminal.setDeptIdPath(deptIdPath);
        terminal.setStatus(registryDto.getStatus());
        // 관리ip
        processIp(terminal);
        // 근거준비id에서mysql조회준비본정보및상태, 결과가저장에서이면업데이트, 찾을 수 없습니다이면삽입(준비이름, MAC주소, IP대기);
        Terminal existingTerminal = terminalDao.getByTerminalId(terminal.getTerminalId());
        if (null == existingTerminal) {
            terminalDao.insert(terminal);
        } else {
            terminal.setId(existingTerminal.getId());
            terminal.setUpdateTime(new Date());
            terminalDao.updateById(terminal);
        }
    }

    private void processDynamicData(RegistryDto registryDto) {
        // 삽입Hash: `terminal:real_time:{준비id}`, status, cpu, memory, disk, last_report_time(현재시간), is_schedule.경과시간30분
        String redisKey = TERMINAL_KEY_REAL_TIME + registryDto.getTerminalId();
        Map<String, Object> terminalData = new HashMap<>();
        terminalData.put("status", registryDto.getStatus());
        terminalData.put("lastHeartbeat", System.currentTimeMillis());
        terminalData.put("isDispatch", registryDto.getIsDispatch());
        terminalData.put("cpu", registryDto.getCpu());
        terminalData.put("memory", registryDto.getMemory());
        terminalData.put("disk", registryDto.getDisk());
        RedisUtils.hmset(redisKey, terminalData, 30 * 60);
        // 삽입Sorted Set: `terminal:online_status`, 추가준비id, score로현재시간.
        RedisUtils.zAdd(TERMINAL_KEY_STATUS, registryDto.getTerminalId(), System.currentTimeMillis());
    }

    @Override
    public AppResponse<String> processBeat(BeatDto beatDto) {
        //        근거준비id에서redis중조회데이터, 결과가찾을 수 없습니다, 반환미완료회원가입, 결과가저장에서, 계속다음 단계;
        String redisKey = TERMINAL_KEY_REAL_TIME + beatDto.getTerminalId();
        Map<Object, Object> terminalData = RedisUtils.hmget(redisKey);
        if (terminalData.isEmpty()) {
            return AppResponse.success(TERMINAL_NOT_FOUND);
        }
        // 계획상태
        String terminalStatus = calculateState(beatDto.getStatus(), beatDto.getIsDispatch());
        if (!terminalStatus.equals(terminalData.get("status"))) {
            // 준비상태발송변수변경, 업데이트mysql의상태필드
            Terminal terminal = new Terminal();
            terminal.setTerminalId(beatDto.getTerminalId());
            terminal.setStatus(terminalStatus);
            terminalDao.updateByTerminalId(terminal);
        }
        // 업데이트Redis: 
        // 업데이트Hash: `device:real_time:{준비id}`, status, cpu, memory, disk, last_report_time(현재시간), is_schedule.경과시간30분(중지길이의준비데이터사용메모리)
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", terminalStatus);
        updateData.put("lastHeartbeat", System.currentTimeMillis());
        updateData.put("isDispatch", beatDto.getIsDispatch());
        updateData.put("cpu", beatDto.getCpu());
        updateData.put("memory", beatDto.getMemory());
        updateData.put("disk", beatDto.getDisk());
        RedisUtils.hmset(redisKey, updateData, 30 * 60);
        // 업데이트Sorted Set: `device:online_status`, 추가준비id, score로현재시간.
        RedisUtils.zAdd(TERMINAL_KEY_STATUS, beatDto.getTerminalId(), System.currentTimeMillis());
        return AppResponse.success("ok");
    }

    @Override
    public void updateStatusByTerminalIdList(List<String> terminalIdList, String status) {
        if (!terminalIdList.isEmpty()) {
            terminalDao.updateStatusByTerminalIdList(terminalIdList, status);
        }
    }
}