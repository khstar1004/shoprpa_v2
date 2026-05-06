package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.Authority;
import com.iflytek.sec.uap.client.core.dto.authority.UapAuthority;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Authority기기
 * 사용를UAP클라이언트의UapAuthority변환로core패키지아래의Authority
 *
 * @author xqcao2
 */
@Component
public class AuthorityMapper {

    /**
     * 를UAP클라이언트의UapAuthority변환로Authority
     *
     * @param uapAuthority UAP클라이언트의UapAuthority
     * @return core패키지아래의Authority
     */
    public Authority fromUapAuthority(UapAuthority uapAuthority) {
        if (uapAuthority == null) {
            return null;
        }

        Authority authority = new Authority();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(uapAuthority, authority);

        return authority;
    }

    /**
     * 량를UAP클라이언트의UapAuthority목록변환로Authority목록
     *
     * @param uapAuthorities UAP클라이언트의UapAuthority목록
     * @return core패키지아래의Authority목록
     */
    public List<Authority> fromUapAuthorities(List<UapAuthority> uapAuthorities) {
        if (uapAuthorities == null || uapAuthorities.isEmpty()) {
            return Collections.emptyList();
        }

        return uapAuthorities.stream()
                .map(this::fromUapAuthority)
                .filter(authority -> authority != null)
                .collect(Collectors.toList());
    }
}