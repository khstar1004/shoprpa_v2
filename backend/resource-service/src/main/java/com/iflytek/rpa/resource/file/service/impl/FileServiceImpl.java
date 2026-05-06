package com.iflytek.rpa.resource.file.service.impl;

import static software.amazon.awssdk.core.sync.RequestBody.fromBytes;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.resource.common.exp.ServiceException;
import com.iflytek.rpa.resource.common.response.AppResponse;
import com.iflytek.rpa.resource.common.response.ErrorCodeEnum;
import com.iflytek.rpa.resource.file.config.S3Config;
import com.iflytek.rpa.resource.file.dao.FileMapper;
import com.iflytek.rpa.resource.file.entity.File;
import com.iflytek.rpa.resource.file.service.FileService;
import com.iflytek.rpa.resource.file.utils.IdWorker;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/**
 * 파일테이블 서비스유형
 *
 * @author system
 * @since 2024-01-01
 */
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    @Autowired
    private S3Config s3Config;

    @Autowired
    private HttpServletResponse response;

    @Autowired
    private IdWorker idWorker;

    @Override
    public AppResponse<Boolean> downloadFile(String fileId) throws IOException {
        // 1. 조회데이터베이스가져오기파일정보

        File file = baseMapper.getFile(fileId);
        if (file == null) {
            throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode());
        }

        // 검증파일 경로
        String filePath = file.getPath();
        if (StringUtils.isBlank(filePath)) {
            throw new ServiceException(ErrorCodeEnum.E_SQL.getCode());
        }

        // 2. 에서S3다운로드파일
        downloadFileFromS3(file.getPath());

        return AppResponse.success(true);
    }

    /**
     * 에서S3다운로드파일
     *
     * @param filePath 파일 경로
     * @return 파일내용문자배열
     */
    private void downloadFileFromS3(String filePath) throws IOException {

        S3Client s3Client = null;
        ResponseInputStream<GetObjectResponse> s3Object = null;

        try {
            // 생성S3클라이언트
            s3Client = buildS3Client();

            // 생성의S3객체
            String objectKey = filePath;

            // 에서S3다운로드파일
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Config.getBucket())
                    .key(objectKey)
                    .build();

            s3Object = s3Client.getObject(getObjectRequest);

            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);

            response.reset();
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/octet-stream");
            response.addHeader(
                    "Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            IOUtils.copy(s3Object, response.getOutputStream());

        } catch (NoSuchKeyException e) {
            throw new ServiceException(ErrorCodeEnum.E_SERVICE_INFO_LOSE.getCode(), "S3에서 파일을 찾을 수 없습니다");
        } catch (S3Exception e) {
            throw new ServiceException(ErrorCodeEnum.E_API_EXCEPTION.getCode(), "S3서비스 예외: " + e.getMessage());
        } catch (IOException e) {
            throw new ServiceException(ErrorCodeEnum.E_COMMON.getCode(), "파일가져오기예외: " + e.getMessage());
        } catch (Exception e) {
            throw new ServiceException(ErrorCodeEnum.E_EXCEPTION.getCode(), "S3다운로드예외: " + e.getMessage());
        } finally {
            if (s3Client != null) s3Client.close();
            if (s3Object != null) s3Object.close();
        }
    }

    @Override
    public AppResponse<String> uploadFile(MultipartFile file) throws IOException {

        // 1. 가져오기기존파일이름URL해제코드
        String originalFileName = file.getOriginalFilename();

        // URL해제코드
        String decodedFileName = URLDecoder.decode(originalFileName, StandardCharsets.UTF_8);

        // 2. 완료파일ID및기기파일이름
        String fileId = String.valueOf(idWorker.nextId());
        String fileExtension = getFileExtension(decodedFileName);
        String replaceName = fileId + fileExtension;

        // 3. 생성목록경로
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String targetPath = "rpa/" + date + "/" + replaceName;

        // 4. 업로드파일까지S3
        uploadFileToS3(file.getBytes(), targetPath);

        // 5. 저장파일정보까지데이터베이스
        saveFileInfo(fileId, targetPath, decodedFileName);

        return AppResponse.success(fileId);
    }

    /**
     * 가져오기파일이름
     *
     * @param fileName 파일이름
     * @return 이름
     */
    private String getFileExtension(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex);
    }

    /**
     * 업로드파일까지S3
     *
     * @param fileContent 파일내용
     * @param targetPath  목록경로
     */
    private void uploadFileToS3(byte[] fileContent, String targetPath) {
        S3Client s3Client = null;
        try {
            s3Client = buildS3Client();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucket())
                    .key(targetPath)
                    .build();

            s3Client.putObject(putObjectRequest, fromBytes(fileContent));

        } catch (S3Exception e) {
            throw new ServiceException(ErrorCodeEnum.E_API_EXCEPTION.getCode(), "S3업로드예외: " + e.getMessage());
        } catch (Exception e) {
            throw new ServiceException(ErrorCodeEnum.E_EXCEPTION.getCode(), "파일업로드예외: " + e.getMessage());
        } finally {
            if (s3Client != null) {
                s3Client.close();
            }
        }
    }

    /**
     * 저장파일정보까지데이터베이스
     *
     * @param fileId     파일ID
     * @param targetPath 목록경로
     * @param fileName   파일이름
     */
    private void saveFileInfo(String fileId, String targetPath, String fileName) {
        File file = new File();
        file.setFileId(fileId);
        file.setPath(targetPath);
        file.setFileName(fileName);
        file.setCreateTime(new Date());
        file.setUpdateTime(new Date());

        baseMapper.insert(file);
    }

    /**
     * 생성S3클라이언트
     */
    private S3Client buildS3Client() {
        AwsBasicCredentials awsCredentials =
                AwsBasicCredentials.create(s3Config.getAccessKey(), s3Config.getSecretKey());

        return S3Client.builder()
                .region(Region.US_EAST_1) // 근거조정
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .endpointOverride(java.net.URI.create(s3Config.getUrl()))
                .build();
    }
}