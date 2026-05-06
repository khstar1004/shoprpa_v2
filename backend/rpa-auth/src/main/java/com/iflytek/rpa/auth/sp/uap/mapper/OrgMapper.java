package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.Org;
import com.iflytek.sec.uap.client.core.dto.org.UapOrg;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Org기기
 * 사용를UAP클라이언트의UapOrg변환로core패키지아래의Org
 *
 * @author xqcao2
 */
@Component
public class OrgMapper {

    /**
     * 를UAP클라이언트의UapOrg변환로Org
     *
     * @param uapOrg UAP클라이언트의UapOrg
     * @return core패키지아래의Org
     */
    public Org fromUapOrg(UapOrg uapOrg) {
        if (uapOrg == null) {
            return null;
        }

        Org org = new Org();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(uapOrg, org);

        return org;
    }

    /**
     * 량를UAP클라이언트의UapOrg목록변환로Org목록
     *
     * @param uapOrgs UAP클라이언트의UapOrg목록
     * @return core패키지아래의Org목록
     */
    public List<Org> fromUapOrgs(List<UapOrg> uapOrgs) {
        if (uapOrgs == null || uapOrgs.isEmpty()) {
            return Collections.emptyList();
        }

        return uapOrgs.stream().map(this::fromUapOrg).filter(org -> org != null).collect(Collectors.toList());
    }
}