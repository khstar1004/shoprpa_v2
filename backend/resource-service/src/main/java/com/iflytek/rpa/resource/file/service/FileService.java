package com.iflytek.rpa.resource.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iflytek.rpa.resource.common.response.AppResponse;
import com.iflytek.rpa.resource.file.entity.File;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일테이블 서비스유형
 *
 * @author system
 * @since 2024-01-01
 */
public interface FileService extends IService<File> {

    /**
     * 근거파일ID다운로드파일
     *
     * @param fileId 파일ID
     * @return 파일
     */
    AppResponse<Boolean> downloadFile(String fileId) throws IOException;

    /**
     * 업로드파일
     *
     * @param file 파일객체
     * @return 파일ID
     */
    AppResponse<String> uploadFile(MultipartFile file) throws IOException;
}