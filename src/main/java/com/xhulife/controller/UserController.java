package com.xhulife.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.xhulife.dto.LoginFormDTO;
import com.xhulife.dto.Result;
import com.xhulife.dto.UserDTO;
import com.xhulife.dto.PasswordChangeDTO;
import com.xhulife.entity.User;
import com.xhulife.entity.UserInfo;
import com.xhulife.service.IUserInfoService;
import com.xhulife.service.IUserService;
import com.xhulife.utils.RedisConstants;
import com.xhulife.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone) {
        // TODO 发送短信验证码并保存验证码
        return userService.sendCode(phone);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm){
        // TODO 实现登录功能
        return userService.login(loginForm);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        if (StrUtil.isNotBlank(token)) {
            stringRedisTemplate.delete(RedisConstants.LOGIN_USER_KEY + token);
        }
        UserHolder.removeUser();
        return Result.ok();
    }

    @PutMapping("/password")
    public Result changePassword(@RequestBody PasswordChangeDTO form) {
        return userService.changePassword(form);
    }

    @GetMapping("/me")
    public Result me(){
        // TODO 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }

    @PutMapping("/info")
    public Result updateUserInfo(@RequestBody UserInfo userInfo) {
        UserDTO user = UserHolder.getUser();
        if (user == null || !user.getId().equals(userInfo.getUserId())) {
            return Result.fail("无权修改");
        }
        // 业务计数、积分和等级不能由客户端批量修改。
        boolean updated = userInfoService.update()
                .set("city", userInfo.getCity())
                .set("introduce", userInfo.getIntroduce())
                .set("gender", userInfo.getGender())
                .set("birthday", userInfo.getBirthday())
                .eq("user_id", user.getId())
                .update();
        if (!updated) {
            UserInfo newInfo = new UserInfo();
            newInfo.setUserId(user.getId());
            newInfo.setCity(userInfo.getCity());
            newInfo.setIntroduce(userInfo.getIntroduce());
            newInfo.setGender(userInfo.getGender());
            newInfo.setBirthday(userInfo.getBirthday());
            return userInfoService.save(newInfo) ? Result.ok() : Result.fail("保存用户资料失败");
        }
        return Result.ok();
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }
}

