package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.User;
import com.iflytek.sec.uap.client.core.dto.user.UapUser;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * User기기
 * 사용를UAP클라이언트의UapUser변환로core패키지아래의User
 *
 * @author xqcao2
 */
@Component
public class UserMapper {

    /**
     * 를UAP클라이언트의UapUser변환로User
     *
     * @param uapUser UAP클라이언트의UapUser
     * @return core패키지아래의User
     */
    public User fromUapUser(UapUser uapUser) {
        if (uapUser == null) {
            return null;
        }

        User user = new User();
        // 사용BeanUtils복사속성
        BeanUtils.copyProperties(uapUser, user);

        return user;
    }

    /**
     * 량를UAP클라이언트의UapUser목록변환로User목록
     *
     * @param uapUsers UAP클라이언트의UapUser목록
     * @return core패키지아래의User목록
     */
    public List<User> fromUapUsers(List<UapUser> uapUsers) {
        if (uapUsers == null || uapUsers.isEmpty()) {
            return Collections.emptyList();
        }

        return uapUsers.stream()
                .map(this::fromUapUser)
                .filter(user -> user != null)
                .collect(Collectors.toList());
    }
}