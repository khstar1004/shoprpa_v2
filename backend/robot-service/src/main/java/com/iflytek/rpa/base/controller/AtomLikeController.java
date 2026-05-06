package com.iflytek.rpa.base.controller;

import com.iflytek.rpa.base.entity.vo.AtomLikeVo;
import com.iflytek.rpa.base.service.AtomLikeService;
import com.iflytek.rpa.utils.exception.NoLoginException;
import com.iflytek.rpa.utils.response.AppResponse;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/atomLike")
public class AtomLikeController {
    @Resource
    private AtomLikeService atomLikeService;

    /**
     * 추가즐겨찾기
     * @param atomKey
     * @return
     * @throws NoLoginException
     */
    @GetMapping("create")
    AppResponse<Boolean> createLikeAtom(@RequestParam String atomKey) throws NoLoginException {
        return atomLikeService.createLikeAtom(atomKey);
    }

    /**
     * 가져오기 즐겨찾기
     * @param likeId
     * @return
     * @throws NoLoginException
     */
    @GetMapping("cancel")
    AppResponse<Boolean> createLikeAtom(@RequestParam Long likeId) throws NoLoginException {
        return atomLikeService.cancelLikeAtom(likeId);
    }

    /**
     * 기존가능즐겨찾기목록
     * @return
     * @throws NoLoginException
     */
    @GetMapping("list")
    AppResponse<List<AtomLikeVo>> likeList() throws NoLoginException {
        return atomLikeService.likeList();
    }
}