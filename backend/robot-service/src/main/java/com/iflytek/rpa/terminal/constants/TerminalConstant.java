package com.iflytek.rpa.terminal.constants;

/**
 * @author mjren
 * @date 2025-06-16 16:46
 * @copyright Copyright (c) 2025 mjren
 */
public class TerminalConstant {

    public static final String TERMINAL_NOT_FOUND = "TERMINAL_NOT_FOUND";

    /**
     * 준비상태, 실행중busy, 빈free, offline, 단일기기중standalone
     */
    public static final String TERMINAL_STATUS_BUSY = "busy";

    public static final String TERMINAL_STATUS_FREE = "free";

    public static final String TERMINAL_STATUS_OFFLINE = "offline";

    public static final String TERMINAL_STATUS_STANDALONE = "standalone";

    /**
     * redis key
     */
    public static final String TERMINAL_KEY_REAL_TIME = "terminalManage:realTime:";

    public static final String TERMINAL_KEY_STATUS = "terminalManage:onlineStatus";
}