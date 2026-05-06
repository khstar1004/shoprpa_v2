package com.iflytek.rpa.terminal.controller;

import com.iflytek.rpa.base.annotation.NoApiLog;
import com.iflytek.rpa.terminal.entity.dto.BeatDto;
import com.iflytek.rpa.terminal.entity.dto.RegistryDto;
import com.iflytek.rpa.terminal.service.TerminalService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 단말정보
 */
@RestController
@RequestMapping("/terminal")
public class TerminalController {

    @Autowired
    private TerminalService terminalService;

    /**
     * 회원가입단말정보
     * @param registryDto
     * @return
     * @throws NoLoginException
     */
    @PostMapping("/register")
    public AppResponse<String> registry(@RequestBody @Valid RegistryDto registryDto) throws NoLoginException {

        return terminalService.registry(registryDto);
    }

    /**
     * 단말데이터-
     * @param beatDto
     * @return
     */
    @NoApiLog("문의연결-단말")
    @PostMapping("/beat")
    public AppResponse<String> processBeat(@RequestBody BeatDto beatDto) {

        return terminalService.processBeat(beatDto);
    }
}