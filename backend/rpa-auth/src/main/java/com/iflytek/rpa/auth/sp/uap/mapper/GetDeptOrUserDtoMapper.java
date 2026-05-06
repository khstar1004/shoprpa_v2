package com.iflytek.rpa.auth.sp.uap.mapper;

import com.iflytek.rpa.auth.core.entity.GetDeptOrUserDto;
import com.iflytek.rpa.auth.core.entity.Org;
import com.iflytek.rpa.auth.core.entity.User;
import com.iflytek.sec.uap.client.core.dto.org.UapOrg;
import com.iflytek.sec.uap.client.core.dto.user.UapUser;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * GetDeptOrUserDto기기
 * 사용를UAP목록변환로core목록 생성GetDeptOrUserDto
 *
 * @author xqcao2
 */
@Component
public class GetDeptOrUserDtoMapper {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrgMapper orgMapper;

    /**
     * 를UAP사용자및모듈목록변환로core의GetDeptOrUserDto
     *
     * @param uapUsers UAP사용자목록
     * @param uapOrgs UAP모듈목록
     * @return 패키지core의GetDeptOrUserDto
     */
    public GetDeptOrUserDto toCoreGetDeptOrUserDto(List<UapUser> uapUsers, List<UapOrg> uapOrgs) {
        GetDeptOrUserDto target = new GetDeptOrUserDto();

        // 사용자목록
        if (uapUsers != null && !uapUsers.isEmpty()) {
            List<User> userList = uapUsers.stream()
                    .map(userMapper::fromUapUser)
                    .filter(user -> user != null)
                    .collect(Collectors.toList());
            target.setUserList(userList);
        } else {
            target.setUserList(Collections.emptyList());
        }

        // 모듈목록
        if (uapOrgs != null && !uapOrgs.isEmpty()) {
            List<Org> orgList = uapOrgs.stream()
                    .map(orgMapper::fromUapOrg)
                    .filter(org -> org != null)
                    .collect(Collectors.toList());
            target.setDeptList(orgList);
        } else {
            target.setDeptList(Collections.emptyList());
        }

        return target;
    }
}