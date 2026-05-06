package com.iflytek.rpa.resource.file.controller;

import com.iflytek.rpa.resource.common.exp.ServiceException;
import com.iflytek.rpa.resource.common.response.AppResponse;
import com.iflytek.rpa.resource.common.response.ErrorCodeEnum;
import com.iflytek.rpa.resource.file.entity.enums.FileType;
import com.iflytek.rpa.resource.file.entity.vo.ShareFileUploadVo;
import com.iflytek.rpa.resource.file.service.FileService;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${file.maxFileSize}")
    private long maxFileSize;

    @Value("${file.maxShareSize}")
    private long maxShareSize;

    @Autowired
    private FileService fileService;

    /**
     * 근거파일ID다운로드파일
     *
     * @param fileId 파일ID
     * @return 파일
     */
    @GetMapping("/download")
    public AppResponse<Boolean> downloadFile(@RequestParam("fileId") String fileId) throws IOException {
        // 매개변수검증
        if (StringUtils.isEmpty(fileId)) throw new ServiceException(ErrorCodeEnum.E_PARAM_LOSE.getCode());

        // 호출Service관리서비스
        return fileService.downloadFile(fileId);
    }

    /**
     * 업로드파일
     *
     * @param file 파일객체
     * @return 업로드결과
     */
    @PostMapping("/upload")
    public AppResponse<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        // 매개변수검증
        checkParam(file, maxFileSize);

        // 호출Service관리서비스
        return fileService.uploadFile(file);
    }

    /**
     * 업로드파일
     *
     * @param file 파일객체
     * @return 업로드결과
     */
    @PostMapping("/upload-video")
    public AppResponse<String> uploadVideoFile(@RequestParam("file") MultipartFile file) throws IOException {
        // 매개변수검증
        checkParam(file, maxFileSize);

        // 검증형식
        checkVideo(file);

        // 호출Service관리서비스
        return fileService.uploadFile(file);
    }

    /**
     * 업로드공유파일
     *
     * @param file 파일객체
     * @return 업로드결과
     */
    @PostMapping("/share-file-upload")
    public AppResponse<ShareFileUploadVo> shareFileUpload(@RequestParam("file") MultipartFile file) throws IOException {
        // 매개변수검증
        checkParam(file, maxShareSize);

        AppResponse<String> response = fileService.uploadFile(file);
        if (!response.ok()) throw new ServiceException(ErrorCodeEnum.E_SERVICE.getCode(), "업로드공유파일실패");

        String fileId = response.getData();
        String filename = file.getOriginalFilename();
        Integer type = FileType.getFileType(filename).getValue();

        ShareFileUploadVo resVo = new ShareFileUploadVo(fileId, type, filename);
        return AppResponse.success(resVo);
    }

    private void checkVideo(MultipartFile file) {
        // 검증파일형식
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename
                .substring(originalFilename.lastIndexOf(".") + 1)
                .toLowerCase();
        String[] allowedVideoFormats = {"mp4", "webm", "ogg", "avi", "mov", "mpeg"};

        boolean isValidFormat = false;
        for (String format : allowedVideoFormats) {
            if (format.equals(fileExtension)) {
                isValidFormat = true;
                break;
            }
        }

        if (!isValidFormat) {
            throw new ServiceException(
                    ErrorCodeEnum.E_PARAM_CHECK.getCode(), "파일형식지원하지 않음, 지원: mp4, webm, ogg, avi, mov, mpeg");
        }
    }

    private void checkParam(MultipartFile file, long maxSize) {
        if (file == null || file.isEmpty()) throw new ServiceException(ErrorCodeEnum.E_PARAM_LOSE.getCode(), "파일은 비워 둘 수 없습니다");

        if (file.getSize() > maxSize) throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "파일 크기는 50MB를 초과할 수 없습니다");

        if (StringUtils.isBlank(file.getOriginalFilename()))
            throw new ServiceException(ErrorCodeEnum.E_PARAM_LOSE.getCode(), "파일 이름은 비워 둘 수 없습니다");
    }
}