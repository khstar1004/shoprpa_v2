package com.iflytek.rpa.terminal.entity.vo;

import lombok.Data;

@Data
public class TerminalDetailVo {
    Long id; // 단말ID
    String terminalId; // 단말일식별자 MAC주소
    String name; // 단말이름
    String account; // 단말계정
    String os; // 단말운영체제
    String ip; // 단말IP
    Integer port; // 단말단말
}