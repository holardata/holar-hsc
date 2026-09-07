package com.hsc.api.service;


import com.hsc.system.domain.query.login.LoginQuery;
import com.hsc.system.domain.vo.login.LoginUserVo;

/**
 * @author danmo
 * @date 2024-02-21 15:13
 **/
public interface ILoginService {
    LoginUserVo login(LoginQuery query);

    void logout();

}
