package com.iflytek.rpa.robot.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.Authority;
import com.iflytek.rpa.common.feign.entity.Org;
import com.iflytek.rpa.common.feign.entity.Role;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.robot.dao.SharedFileDao;
import com.iflytek.rpa.robot.entity.SharedFile;
import com.iflytek.rpa.robot.entity.SharedFileTag;
import com.iflytek.rpa.robot.entity.dto.SharedFileDto;
import com.iflytek.rpa.robot.entity.dto.SharedFilePageDto;
import com.iflytek.rpa.robot.entity.enums.FileIndexStatus;
import com.iflytek.rpa.robot.entity.vo.SharedFilePageVo;
import com.iflytek.rpa.robot.service.SharedFileService;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공유파일서비스유형
 *
 * @author yfchen40
 * @since 2025-07-21
 */
@Slf4j
@Service
public class SharedFileServiceImpl extends ServiceImpl<SharedFileDao, SharedFile> implements SharedFileService {
    @Autowired
    private IdWorker idWorker;

    @Autowired
    private SharedFileDao sharedFileDao;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Override
    //    @Transactional(readOnly = true)
    public AppResponse<IPage<SharedFilePageVo>> getSharedFilePageList(SharedFilePageDto queryDto) {
        // 생성분객체
        IPage<SharedFile> page = new Page<>(queryDto.getPageNo(), queryDto.getPageSize());
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();

        // 사용 XML 분조회방식
        IPage<SharedFile> sharedFilePage = baseMapper.selectSharedFilePageList(page, queryDto, tenantId);

        if (sharedFilePage.getSize() == 0) {
            return AppResponse.error(ErrorCodeEnum.E_SQL_EMPTY, "조회하지 못한기호합치기파일의파일");
        }

        // 변환로 VO 객체
        IPage<SharedFilePageVo> resultPage = sharedFilePage.convert(sharedFile -> {
            SharedFilePageVo vo = new SharedFilePageVo();
            BeanUtils.copyProperties(sharedFile, vo);
            // 금액외부의 VO 필드
            vo.setFileId(sharedFile.getFileId());
            if (StringUtils.isNotBlank(sharedFile.getTags())) {
                // 태그ID목록
                List<Long> tagIds = Arrays.stream(sharedFile.getTags().split(","))
                        .map(Long::valueOf)
                        .collect(Collectors.toList());

                // 조회태그이름목록
                List<SharedFileTag> tags = baseMapper.selectTagsByIds(tagIds, tenantId);
                List<String> tagNames =
                        tags.stream().map(SharedFileTag::getTagName).collect(Collectors.toList());
                vo.setTagsNames(tagNames);
            }
            if (sharedFile.getTags() != null && !sharedFile.getTags().isEmpty()) {
                vo.setTags(Arrays.asList(sharedFile.getTags().split(",")));
            } else {
                vo.setTags(null);
            }
            vo.setFilePath("/api/resource/file/download?fileId=" + sharedFile.getFileId());
            // creatorName, phone(계정), deptId, deptName
            String creatorId = sharedFile.getCreatorId();
            AppResponse<String> deptIdRes = rpaAuthFeign.getDeptIdByUserId(creatorId, tenantId);
            if (!deptIdRes.ok()) throw new ServiceException("rpa-auth 서비스위치");
            String deptId = deptIdRes.getData();

            AppResponse<String> realNameResp = rpaAuthFeign.getNameById(sharedFile.getCreatorId());
            if (realNameResp == null || realNameResp.getData() == null) {
                throw new ServiceException("사용자명가져오기실패");
            }
            String creatorName = realNameResp.getData();
            vo.setCreatorName(creatorName);

            AppResponse<User> userResp = rpaAuthFeign.getUserInfoById(creatorId);
            if (userResp == null || userResp.getData() == null) {
                throw new ServiceException("가져오기사용자 정보 조회 실패");
            }
            User loginUser = userResp.getData();
            vo.setPhone(loginUser.getPhone());
            vo.setDeptId(deptId);
            AppResponse<Org> deptInfoByDeptIdRes = rpaAuthFeign.getDeptInfoByDeptId(deptId);
            if (!deptInfoByDeptIdRes.ok()) throw new ServiceException("rpa-auth 서비스가 준비되지 않았습니다");
            Org dept = deptInfoByDeptIdRes.getData();
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            return vo;
        });

        // 반환성공
        return AppResponse.success(resultPage);
    }

    // 사용자있음없음파일관리권한
    private boolean hasFileManagementPermission(HttpServletRequest request) throws NoLoginException {
        // 결과가예테넌트관리관리원, 직선연결반환true
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
        // 통신경과Feign호출rpa-auth서비스가져오기테넌트사용자유형
        AppResponse<Integer> tenantUserTypeResponse = rpaAuthFeign.getTenantUserType(userId, tenantId);
        Integer tenantUserType = null;
        if (tenantUserTypeResponse != null && tenantUserTypeResponse.ok() && tenantUserTypeResponse.getData() != null) {
            tenantUserType = tenantUserTypeResponse.getData();
        }
        if (tenantUserType != null && tenantUserType == 1) {
            return true;
        }
        AppResponse<List<Role>> roleResponse = rpaAuthFeign.getUserRoleList();
        if (roleResponse == null || roleResponse.getData() == null) {
            throw new ServiceException("사용자역할정보가져오기실패");
        }
        List<Role> roleList = roleResponse.getData();
        List<Authority> authList = roleList.stream()
                .map(Role::getId)
                .flatMap(roleId -> {
                    AppResponse<List<Authority>> listAppResponse =
                            rpaAuthFeign.queryAuthorityListByRoleId(tenantId, roleId);
                    if (!listAppResponse.ok()) throw new ServiceException("rpa-auth서비스예외");
                    List<Authority> authorities = listAppResponse.getData();
                    return authorities != null ? authorities.stream() : Stream.empty();
                })
                .collect(Collectors.toList());
        return authList.stream().anyMatch(auth -> "파일관리관리".equals(auth.getName()));
    }

    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> addSharedFileInfo(HttpServletRequest request, SharedFileDto dto) throws NoLoginException {
        if (!hasFileManagementPermission(request)) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "있음파일관리관리권한");
        }
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || response.getData() == null) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppResponse<String> deptIdRes = rpaAuthFeign.getDeptIdByUserId(userId, tenantId);
        if (!deptIdRes.ok()) throw new ServiceException("rpa-auth 서비스가 준비되지 않았습니다");
        String deptId = deptIdRes.getData();
        String fileId = dto.getFileId();
        String fileName = dto.getFileName();

        SharedFile file = baseMapper.selectFileByName(fileName, tenantId);
        if (file != null) {
            return AppResponse.error("요청 업로드이름파일");
        }
        // 조회태그여부저장된 재
        List<Long> uniqueTagIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(dto.getTags())) {
            // 재
            uniqueTagIds = dto.getTags().stream().distinct().collect(Collectors.toList());
            // 조회태그여부저장에서
            List<SharedFileTag> existingTags = baseMapper.selectTagsByIds(uniqueTagIds, tenantId);
            List<Long> existingTagIds =
                    existingTags.stream().map(SharedFileTag::getTagId).collect(Collectors.toList());
            // 조회여부있음찾을 수 없습니다의태그
            List<Long> nonExistingTagIds = uniqueTagIds.stream()
                    .filter(tagId -> !existingTagIds.contains(tagId))
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(nonExistingTagIds)) {
                return AppResponse.error(ErrorCodeEnum.E_PARAM, "태그찾을 수 없습니다: " + nonExistingTagIds);
            }
        } else {
            uniqueTagIds = new ArrayList<>();
        }
        String tagsString = uniqueTagIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        // sharedFile객체
        SharedFile sharedFile = new SharedFile();
        sharedFile.setId(idWorker.nextId());
        sharedFile.setFileId(fileId);
        sharedFile.setFileName(fileName);
        sharedFile.setDeptId(deptId);
        sharedFile.setFileType(dto.getFileType());
        sharedFile.setFileIndexStatus(FileIndexStatus.START.getValue());
        AppResponse<String> res = rpaAuthFeign.getTenantId();
        if (res == null || res.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String nowTenantId = resp.getData();
        sharedFile.setTenantId(nowTenantId);
        sharedFile.setTags(tagsString);
        sharedFile.setUpdaterId(userId);
        sharedFile.setUpdateTime(new Date());
        sharedFile.setCreatorId(userId);
        sharedFile.setCreateTime(new Date());
        sharedFile.setDeleted(0);
        this.save(sharedFile);
        return AppResponse.success("추가성공");
    }
}