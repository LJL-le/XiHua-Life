package com.xhulife.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhulife.dto.LoginFormDTO;
import com.xhulife.dto.Result;
import com.xhulife.dto.PasswordChangeDTO;
import com.xhulife.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone);

    Result login(LoginFormDTO loginForm);
    Result changePassword(PasswordChangeDTO form);
}

