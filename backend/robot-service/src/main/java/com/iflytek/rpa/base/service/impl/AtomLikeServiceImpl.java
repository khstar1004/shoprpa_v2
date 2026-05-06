package com.iflytek.rpa.base.service.impl;

import static com.iflytek.rpa.utils.DeBounceUtils.deBounce;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iflytek.rpa.base.dao.AtomLikeDao;
import com.iflytek.rpa.base.dao.CAtomMetaDao;
import com.iflytek.rpa.base.dao.CAtomMetaNewDao;
import com.iflytek.rpa.base.entity.AtomLike;
import com.iflytek.rpa.base.entity.Atomic;
import com.iflytek.rpa.base.entity.CAtomMetaNew;
import com.iflytek.rpa.base.entity.vo.AtomLikeVo;
import com.iflytek.rpa.base.service.AtomLikeService;
import com.iflytek.rpa.common.feign.RpaAuthFeign;
import com.iflytek.rpa.common.feign.entity.User;
import com.iflytek.rpa.utils.IdWorker;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.exception.ServiceException;
import com.iflytek.rpa.utils.response.AppResponse;
import com.iflytek.rpa.utils.response.ErrorCodeEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service("AtomLikeService")
public class AtomLikeServiceImpl extends ServiceImpl<AtomLikeDao, AtomLike> implements AtomLikeService {

    @Resource
    private CAtomMetaDao atomMetaDao;

    @Resource
    private AtomLikeDao atomLikeDao;

    @Resource
    private IdWorker idWorker;

    @Value("${deBounce.prefix}")
    private String doBouncePrefix;

    @Value("${deBounce.window}")
    private Long deBounceWindow;

    @Autowired
    private RpaAuthFeign rpaAuthFeign;

    @Resource
    private CAtomMetaNewDao atomMetaNewDao;

    @Override
    public AppResponse<Boolean> createLikeAtom(String atomKey) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();

        if (StringUtils.isBlank(atomKey)) throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode(), "매개변수가 비어 있습니다");

        // 조회기존가능여부비어 있습니다(새기존가능)
        String atomContentByKey = atomMetaNewDao.getAtomContentByKey(atomKey);
        if (atomContentByKey == null || atomContentByKey.isEmpty()) {
            // 기존가능
            String oldAtomByKey = atomMetaDao.getLatestAtomByKey(atomKey);
            if (oldAtomByKey == null || oldAtomByKey.isEmpty())
                throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "기존 데이터를 찾을 수 없어 즐겨찾기할 수 없습니다");
        }

        // redis관리
        String createLikeKey = doBouncePrefix + userId + atomKey;
        deBounce(createLikeKey, deBounceWindow);

        // 삽입의시조회여부완료저장에서
        Integer count = atomLikeDao.getAtomLikeByUserIdAtomKey(userId, atomKey);
        if (count >= 1) throw new ServiceException(ErrorCodeEnum.E_SQL_REPEAT.getCode(), "즐겨찾기의기존가능완료저장에서, 필요하지 않습니다재복사즐겨찾기");

        AtomLike atomLike = new AtomLike();
        atomLike.setLikeId(idWorker.nextId());
        atomLike.setCreatorId(userId);
        atomLike.setTenantId(tenantId);
        atomLike.setUpdaterId(userId);
        atomLike.setAtomKey(atomKey);

        atomLikeDao.insert(atomLike);

        return AppResponse.success(true);
    }

    @Override
    public AppResponse<Boolean> cancelLikeAtom(Long likeId) throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();

        if (likeId == null) throw new ServiceException(ErrorCodeEnum.E_PARAM_CHECK.getCode());

        AtomLike atomLike = atomLikeDao.getAtomLikeById(userId, tenantId, likeId);
        if (atomLike == null) throw new ServiceException(ErrorCodeEnum.E_SQL_EMPTY.getCode(), "데이터비어 있습니다, 불가가져오기 즐겨찾기");
        atomLike.setIsDeleted(1);

        int i = atomLikeDao.deleteById(atomLike.getId());
        if (i < 1) throw new ServiceException(ErrorCodeEnum.E_SQL_EXCEPTION.getCode());
        return AppResponse.success(true);
    }

    @Override
    public AppResponse<List<AtomLikeVo>> likeList() throws NoLoginException {
        AppResponse<User> response = rpaAuthFeign.getLoginUser();
        if (response == null || !response.ok()) {
            throw new ServiceException("사용자 정보 조회 실패");
        }
        User loginUser = response.getData();
        String userId = loginUser.getId();
        AppResponse<String> resp = rpaAuthFeign.getTenantId();
        if (resp == null || resp.getData() == null) {
            throw new ServiceException("테넌트 정보 조회 실패");
        }
        String tenantId = resp.getData();

        List<AtomLike> atomLikeList = atomLikeDao.getAtomLikeList(userId, tenantId);
        if (CollectionUtils.isEmpty(atomLikeList)) return AppResponse.success(Collections.emptyList());

        List<AtomLikeVo> resVoList = getResVoList(atomLikeList);
        return AppResponse.success(resVoList);
    }

    private List<AtomLikeVo> getResVoList(List<AtomLike> atomLikeList) {
        List<AtomLikeVo> resVoList = new ArrayList<>();
        List<String> atomKeyList =
                atomLikeList.stream().map(AtomLike::getAtomKey).collect(Collectors.toList());

        Set<String> atomKeySet = atomKeyList.stream().collect(Collectors.toSet());

        List<CAtomMetaNew> atomMetaList = atomMetaNewDao.getAtomListByKeySet(atomKeySet);

        for (AtomLike atomLike : atomLikeList) {
            AtomLikeVo atomLikeVo = new AtomLikeVo();
            String atomKeyTmp = atomLike.getAtomKey();
            List<CAtomMetaNew> atomMetaListTmp = atomMetaList.stream()
                    .filter(cAtomMeta -> cAtomMeta.getAtomKey().equals(atomKeyTmp))
                    .collect(Collectors.toList());

            // 설명데이터있음제목, 직선연결건너뛰기
            if (CollectionUtils.isEmpty(atomMetaListTmp) || atomMetaListTmp.size() > 1) continue;
            CAtomMetaNew atomMetaTmp = atomMetaListTmp.get(0);
            String atomContentJson = atomMetaTmp.getAtomContent();
            Atomic atomic = JSON.parseObject(atomContentJson, Atomic.class);

            atomLikeVo.setAtomContent(atomContentJson);
            atomLikeVo.setLikeId(atomLike.getLikeId());
            atomLikeVo.setKey(atomKeyTmp);
            atomLikeVo.setIcon(atomic.getIcon());
            atomLikeVo.setTitle(atomic.getTitle());

            resVoList.add(atomLikeVo);
        }

        return resVoList;
    }
}
