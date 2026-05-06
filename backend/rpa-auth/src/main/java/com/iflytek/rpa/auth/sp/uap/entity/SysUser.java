package com.iflytek.rpa.auth.sp.uap.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 사용자정보
 * @author keler
 */
@Setter
@Getter
public class SysUser implements Serializable {
    private Long id;
    /** 사용자명 */
    private String name;

    private String username;
    private String password;

    private String phone;
    private String telephone;
    private String email;

    /** 사용자여부가능사용 */
    private Boolean enabled;
    /** 사용자여부미완료경과 */
    private Boolean accountNonExpired;
    /** 사용자여부지정되지 않았습니다 */
    private Boolean accountNonLocked;
    /** 사용자인증여부미완료 */
    private Boolean credentialsNonExpired;

    private List<String> userOwnedPath = new ArrayList<>();

    public SysUser() {}

    @Override
    public String toString() {
        return "User{" + "id="
                + id + ", username='"
                + name + '\'' + ", password='"
                + password + '\'' + ", enabled="
                + enabled + ", accountNonExpired="
                + accountNonExpired + ", accountNonLocked="
                + accountNonLocked + ", credentialsNonExpired="
                + credentialsNonExpired + ", roles="
                + userOwnedPath + '}';
    }
}