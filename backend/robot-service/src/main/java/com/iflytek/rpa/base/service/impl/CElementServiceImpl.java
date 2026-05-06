package com.iflytek.rpa.base.service.impl;

import static com.iflytek.rpa.base.constants.BaseConstant.*;
import static com.iflytek.rpa.robot.constants.RobotConstant.EDITING;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.base.annotation.RobotVersionAnnotation;
import com.iflytek.rpa.base.dao.CElementDao;
import com.iflytek.rpa.base.dao.CGroupDao;
import com.iflytek.rpa.base.entity.CElement;
import com.iflytek.rpa.base.entity.CGroup;
import com.iflytek.rpa.base.entity.dto.ServerBaseDto;
import com.iflytek.rpa.base.entity.vo.ElementInfoVo;
import com.iflytek.rpa.base.entity.vo.ElementVo;
import com.iflytek.rpa.base.entity.vo.GroupInfoVo;
import com.iflytek.rpa.base.service.CElementService;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.robot.dao.RobotDesignDao;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클라이언트, 원정보(CElement)테이블서비스유형
 *
 * @author mjren
 * @since 2024-10-14 17:21:34
 */
@Service("cElementService")
public class CElementServiceImpl extends ServiceImpl<CElementDao, CElement> implements CElementService {
    @Resource
    private CElementDao cElementDao;

    @Resource
    private CGroupDao cGroupDao;

    @Resource
    private RobotDesignDao robotDesignDao;

    @Value("${resource.download.url}")
    private String prefix;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;
    //    @Override
    //    @RobotVersionAnnotation
    //    public AppResponse<?> getElementNameList(BaseDto baseDto) throws NoLoginException {
    //        String userId = UserUtils.nowUserId();
    ////        List<FrontElementDto> result = new ArrayList<>();
    ////        List<CElement> elementList = cElementDao.getElementInfo(baseDto.getRobotId(), baseDto.getRobotVersion(),
    // userId);
    ////        if(CollectionUtil.isEmpty(elementList)){
    ////            return AppResponse.success(result);
    ////        }
    ////        List<FrontElementDto> frontElementDtoList = new ArrayList<>();
    ////        for(CElement element:elementList) {
    ////            FrontElementDto frontElementDto = new FrontElementDto();
    ////            BeanUtils.copyProperties(element, frontElementDto);
    ////            frontElementDto.setName(element.getElementName());
    ////            frontElementDto.setImageUrl(element.getImageId() != null ? prefix + element.getImageId() : null);
    ////            frontElementDto.setParentImageUrl(element.getParentImageId() != null?  prefix +
    // element.getParentImageId() : null);
    ////            frontElementDtoList.add(frontElementDto);
    ////        }
    ////        //근거groupName분그룹
    ////        Map<String, List<FrontElementDto>> elementMap =
    // frontElementDtoList.stream().collect(Collectors.groupingBy(CElement::getGroupId));
    ////        elementMap.forEach((key, value)->{
    ////            FrontElementDto frontElementDto = new FrontElementDto();
    ////            frontElementDto.setGroupId(key);
    ////            frontElementDto.setName(key);
    ////            frontElementDto.setIcon("");
    ////            frontElementDto.setChild(value);
    ////            result.add(frontElementDto);
    ////        });
    //        return AppResponse.success(true);
    //    }

    @Override
    @RobotVersionAnnotation(clazz = ServerBaseDto.class)
    public AppResponse<?> getElementDetail(ServerBaseDto serverBaseDto) throws NoLoginException {
        CElement cElement = new CElement();
        BeanUtils.copyProperties(serverBaseDto, cElement);
        CElement element = cElementDao.getElementByElementId(cElement);
        if (null == element) {
            return AppResponse.success("");
        }
        ElementVo elementVo = new ElementVo();
        BeanUtils.copyProperties(element, elementVo);
        elementVo.setId(element.getElementId());
        elementVo.setName(element.getElementName());
        elementVo.setImageUrl(StringUtils.isNotBlank(element.getImageId()) ? prefix + element.getImageId() : null);
        elementVo.setParentImageUrl(
                StringUtils.isNotBlank(element.getParentImageId()) ? prefix + element.getParentImageId() : null);
        return AppResponse.success(elementVo);
    }

    @Override
    @RobotVersionAnnotation
    public AppResponse<?> moveElementOrImage(ServerBaseDto serverBaseDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        serverBaseDto.setCreatorId(userId);
        CGroup cGroup = new CGroup();
        BeanUtils.copyProperties(serverBaseDto, cGroup);
        // 조회목록 분그룹여부저장에서
        CGroup group = cGroupDao.getGroupById(cGroup);
        if (null == group) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM, "그룹 목록을 찾을 수 없습니다");
        }
        CElement cElement = new CElement();
        BeanUtils.copyProperties(serverBaseDto, cElement);
        cElementDao.updateElement(cElement);
        return AppResponse.success(true);
    }

    @Override
    public AppResponse<?> deleteElementOrImage(ServerBaseDto serverBaseDto) throws NoLoginException {
        if (null == serverBaseDto.getElementId()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "요소id비워 둘 수 없습니다");
        }
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        serverBaseDto.setCreatorId(userId);
        CElement cElement = new CElement();
        BeanUtils.copyProperties(serverBaseDto, cElement);
        cElementDao.deleteElementOrImage(cElement);
        return AppResponse.success(true);
    }

    @Override
    public AppResponse<?> createImageName(ServerBaseDto serverBaseDto) throws NoLoginException {
        String name = createNextName(serverBaseDto, "이미지_");
        return AppResponse.success(name);
    }

    private String createNextName(ServerBaseDto serverBaseDto, String elementNameBase) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        serverBaseDto.setCreatorId(userId);
        serverBaseDto.setElementName(elementNameBase);
        CElement cElement = new CElement();
        BeanUtils.copyProperties(serverBaseDto, cElement);
        List<String> getElementNameList = cElementDao.getElementNameList(cElement);
        int elementNameIndex = 1;
        List<Integer> elementNameIndexList = new ArrayList<>();
        for (String elementName : getElementNameList) {
            String[] elementNameSplit = elementName.split(elementNameBase);
            if (elementNameSplit.length == 2 && elementNameSplit[1].matches("^[1-9]\\d*$")) {
                int elementNameNum = Integer.parseInt(elementNameSplit[1]);
                elementNameIndexList.add(elementNameNum);
            }
        }
        Collections.sort(elementNameIndexList);
        for (int i = 0; i < elementNameIndexList.size(); i++) {
            if (elementNameIndexList.get(i) != i + 1) {
                elementNameIndex = i + 1;
                break;
            } else {
                elementNameIndex += 1;
            }
        }
        return elementNameBase + elementNameIndex;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> createElement(ServerBaseDto serverBaseDto) throws NoLoginException {
        if (null == serverBaseDto.getElement()) {
            return AppResponse.error(ErrorCodeEnum.E_PARAM_LOSE, "원정보비워 둘 수 없습니다");
        }
        String elementType = serverBaseDto.getElementType();
        if (TYPE_COMMON.equals(elementType)) {
            return createElementByType(serverBaseDto);
        } else if (TYPE_CV.equals(elementType)) {
            if (StringUtils.isBlank(serverBaseDto.getGroupName())) {
                serverBaseDto.setGroupName("분그룹");
            }
            return createElementByType(serverBaseDto);
        }
        return AppResponse.error(ErrorCodeEnum.E_SERVICE, "지원하지 않음해당유형요소");
    }

    public AppResponse<?> createElementByType(ServerBaseDto serverBaseDto) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        String elementId = idWorker.nextId() + "";
        String groupName = serverBaseDto.getGroupName();
        CGroup cGroup = new CGroup();
        cGroup.setGroupName(groupName);
        cGroup.setCreatorId(userId);
        cGroup.setUpdaterId(userId);
        cGroup.setRobotId(serverBaseDto.getRobotId());
        cGroup.setRobotVersion(serverBaseDto.getRobotVersion());
        cGroup.setElementType(serverBaseDto.getElementType());
        String groupId;
        CGroup existGroup = cGroupDao.getGroupByGroupName(cGroup);
        if (null == existGroup) {
            // 생성분그룹
            groupId = idWorker.nextId() + "";
            cGroup.setGroupId(groupId);
            cGroupDao.insertGroup(cGroup);
        } else {
            groupId = existGroup.getGroupId();
        }
        CElement element = serverBaseDto.getElement();
        String elementName = element.getElementName();
        String[] elementNameSplit = elementName.split("이미지_");
        if (elementNameSplit.length == 2 && elementNameSplit[1].matches("^[1-9]\\d*$")) {
            try {
                Integer.parseInt(elementNameSplit[1]);
            } catch (NumberFormatException e) {
                return AppResponse.error(ErrorCodeEnum.E_SERVICE, "이미지이름순서경과대");
            }
        }
        element.setGroupId(groupId);
        element.setElementId(elementId);
        element.setCreatorId(userId);
        element.setUpdaterId(userId);
        // 일유형아래여부재이름, 아니요유형허용재이름
        CElement sameNameElement = cElementDao.getElementSameName(
                element.getRobotId(),
                element.getRobotVersion(),
                element.getElementId(),
                element.getElementName(),
                cGroup.getElementType());
        if (null != sameNameElement) {
            // todo 삭제oss이미지
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "이름재복사, 요청다시 명령이름");
        }
        cElementDao.insertElement(element);
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("elementId", elementId);
        resultMap.put("groupId", groupId);
        return AppResponse.success(resultMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResponse<?> updateElement(ServerBaseDto serverBaseDto) throws NoLoginException {
        CElement element = serverBaseDto.getElement();
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        element.setCreatorId(userId);
        // 가져오기원정보
        CElement elementInfo = cElementDao.getElementByElementId(element);
        if (null == elementInfo || null == elementInfo.getGroupId()) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "요소를 찾을 수 없습니다");
        }
        String groupId = elementInfo.getGroupId();
        // 가져오기분그룹정보, 유형정보
        CGroup cGroup = new CGroup();
        cGroup.setGroupId(groupId);
        cGroup.setRobotId(element.getRobotId());
        cGroup.setRobotVersion(element.getRobotVersion());
        CGroup cGroupInfo = cGroupDao.getGroupById(cGroup);
        if (null == cGroupInfo || null == cGroupInfo.getElementType()) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "그룹을 찾을 수 없습니다");
        }
        // 재이름검증
        // 일유형아래여부재이름, 아니요유형허용재이름
        CElement sameNameElement = cElementDao.getElementSameName(
                element.getRobotId(),
                element.getRobotVersion(),
                element.getElementId(),
                element.getElementName(),
                cGroupInfo.getElementType());
        if (null != sameNameElement) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "이름재복사, 요청다시 명령이름");
        }

        // robotDesign 변수성공editing상태
        robotDesignDao.updateTransformStatus(userId, serverBaseDto.getRobotId(), null, EDITING);

        Long id = cElementDao.getId(element);
        element.setId(id);
        cElementDao.updateElementById(element);
        return AppResponse.success(true);
    }

    @Override
    public AppResponse<?> copyElement(ServerBaseDto serverBaseDto) throws NoLoginException {
        CElement cElement = new CElement();
        BeanUtils.copyProperties(serverBaseDto, cElement);
        String newElementId = idWorker.nextId() + "";
        CElement oldElement = cElementDao.getElementByElementId(cElement);
        String elementName = oldElement.getElementName();
        if (StringUtils.isBlank(elementName)) {
            return AppResponse.error(ErrorCodeEnum.E_SERVICE, "원본 이름을 찾을 수 없습니다");
        }
        String newElementName = createNextName(serverBaseDto, elementName + "_본");
        cElement.setElementName(newElementName);
        cElementDao.copyElement(cElement, newElementId);
        return AppResponse.success(true);
    }

    @Override
    @RobotVersionAnnotation(clazz = ServerBaseDto.class)
    public AppResponse<?> getAllGroupInfo(ServerBaseDto serverBaseDto) {
        List<GroupInfoVo> result = new ArrayList<>();

        // 조회 group 테이블, 가져오기모든분그룹정보
        List<CGroup> groupList = cGroupDao.getGroupByRobotId(
                serverBaseDto.getRobotId(), serverBaseDto.getRobotVersion(), serverBaseDto.getElementType());
        if (CollectionUtil.isEmpty(groupList)) {
            return AppResponse.success(result); // 결과가있음분그룹정보, 직선연결반환빈목록
        }

        // 가져오기모든 groupId
        List<String> groupIds = groupList.stream().map(CGroup::getGroupId).collect(Collectors.toList());

        // 조회 element 테이블, 가져오기모든분그룹의요소
        CElement cElement = new CElement();
        cElement.setRobotId(serverBaseDto.getRobotId());
        cElement.setRobotVersion(serverBaseDto.getRobotVersion());
        List<CElement> elementList = cElementDao.getElementsByGroupIds(cElement, groupIds);

        // 를 element  groupId 분유형
        Map<String, List<ElementInfoVo>> elementMap = new HashMap<>();
        for (CElement element : elementList) {
            ElementInfoVo elementInfo = new ElementInfoVo();
            elementInfo.setId(element.getElementId());
            elementInfo.setName(element.getElementName());
            elementInfo.setImageUrl(
                    StringUtils.isNotBlank(element.getImageId()) ? prefix + element.getImageId() : null);
            elementInfo.setParentImageUrl(
                    StringUtils.isNotBlank(element.getParentImageId()) ? prefix + element.getParentImageId() : null);
            elementInfo.setCommonSubType(element.getCommonSubType());
            // 근거 groupId 분그룹
            List<ElementInfoVo> elementInfoList = elementMap.get(element.getGroupId());
            if (elementInfoList == null) {
                elementInfoList = new ArrayList<>();
            }
            elementInfoList.add(elementInfo);
            elementMap.put(element.getGroupId(), elementInfoList);
        }

        // 생성 GroupInfoDto, 
        for (CGroup group : groupList) {
            GroupInfoVo groupInfoVo = new GroupInfoVo();
            groupInfoVo.setId(group.getGroupId());
            groupInfoVo.setName(group.getGroupName());
            groupInfoVo.setElements(elementMap.getOrDefault(group.getGroupId(), new ArrayList<>())); // 결과가있음요소, 이면비어 있습니다목록
            if (DEFAULT_GROUP.equals(group.getGroupName())) {
                result.add(0, groupInfoVo);
            } else {
                result.add(groupInfoVo);
            }
        }

        // 반환생성결과
        return AppResponse.success(result);
    }
}