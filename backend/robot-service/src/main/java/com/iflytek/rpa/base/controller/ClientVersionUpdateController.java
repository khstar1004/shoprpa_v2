package com.iflytek.rpa.base.controller;

import com.iflytek.rpa.base.service.ClientVersionUpdateService;
import java.net.URI;
import javax.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 클라이언트버전업데이트제어기기
 */
@RestController
@RequestMapping("/client-version-update")
public class ClientVersionUpdateController {

    @Resource
    private ClientVersionUpdateService clientVersionUpdateService;

    /**
     * 조회클라이언트버전여부필요업데이트
     *
     * @param version 현재버전
     * @return 결과가완료예새버전반환204, 아니요이면반환302재지정까지새버전다운로드URL
     */
    @GetMapping("/update-check/{os}/{arch}/{version}/latest.yml")
    public ResponseEntity<Void> updateCheck(
            @PathVariable("os") String os, @PathVariable("arch") String arch, @PathVariable("version") String version)
            throws Exception {
        String latestVersionUrl = clientVersionUpdateService.checkVersionSimple(os, arch, version);
        if (latestVersionUrl == null) {
            return ResponseEntity.ok().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(latestVersionUrl));
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .headers(headers)
                .build();
    }
}