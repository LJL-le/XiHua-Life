package com.xhulife.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhulife.dto.LoginFormDTO;
import com.xhulife.dto.PasswordChangeDTO;
import com.xhulife.dto.Result;
import com.xhulife.dto.UserDTO;
import com.xhulife.entity.User;
import com.xhulife.entity.UserInfo;
import com.xhulife.mapper.UserMapper;
import com.xhulife.service.IUserInfoService;
import com.xhulife.service.IUserService;
import com.xhulife.utils.PasswordEncoder;
import com.xhulife.utils.RedisConstants;
import com.xhulife.utils.RegexUtils;
import com.xhulife.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xhulife.utils.RedisConstants.*;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired private StringRedisTemplate redis;
    @Autowired private IUserInfoService userInfoService;
    @Value("${app.dev-mode:false}") private boolean devMode;

    @Override
    public Result sendCode(String phone) {
        if (RegexUtils.isPhoneInvalid(phone)) return Result.fail("手机号格式错误");
        String cooldown = "login:code:cooldown:" + phone;
        if (Boolean.TRUE.equals(redis.hasKey(cooldown))) return Result.fail("验证码发送过于频繁，请稍后再试");
        String code = RandomUtil.randomNumbers(6);
        redis.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        redis.opsForValue().set(cooldown, "1", 60, TimeUnit.SECONDS);
        return devMode ? Result.ok(code) : Result.ok();
    }

    @Override
    public Result login(LoginFormDTO form) {
        if (form == null || RegexUtils.isPhoneInvalid(form.getPhone())) return Result.fail("手机号格式错误");
        boolean byPassword = hasText(form.getPassword());
        boolean byCode = hasText(form.getCode());
        if (byPassword == byCode) return Result.fail("请使用验证码或密码其中一种方式登录");
        User user = query().eq("phone", form.getPhone()).one();
        if (byPassword) {
            if (user == null || !PasswordEncoder.matches(user.getPassword(), form.getPassword())) return Result.fail("手机号或密码错误");
        } else {
            String failKey = "login:code:fail:" + form.getPhone();
            String failures = redis.opsForValue().get(failKey);
            if (failures != null && Integer.parseInt(failures) >= 5) return Result.fail("验证码错误次数过多，请稍后再试");
            String expected = redis.opsForValue().get(LOGIN_CODE_KEY + form.getPhone());
            if (!form.getCode().equals(expected)) {
                redis.opsForValue().increment(failKey);
                redis.expire(failKey, 10, TimeUnit.MINUTES);
                return Result.fail("验证码错误");
            }
            redis.delete(LOGIN_CODE_KEY + form.getPhone()); redis.delete(failKey);
            if (user == null) user = createUserWithPhone(form.getPhone());
        }
        UserDTO dto = BeanUtil.copyProperties(user, UserDTO.class);
        String token = UUID.randomUUID().toString(true);
        Map<String,Object> values = BeanUtil.beanToMap(dto, new HashMap<>(), CopyOptions.create()
                .setIgnoreNullValue(true).setFieldValueEditor((name, value) -> value == null ? null : value.toString()));
        redis.opsForHash().putAll(LOGIN_USER_KEY + token, values);
        redis.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    @Override
    public Result changePassword(PasswordChangeDTO form) {
        UserDTO current = UserHolder.getUser();
        if (current == null) return Result.fail("请先登录");
        if (form == null || !hasText(form.getNewPassword()) || form.getNewPassword().length() < 6 || form.getNewPassword().length() > 32)
            return Result.fail("新密码长度应为 6 到 32 位");
        User user = getById(current.getId());
        if (hasText(user.getPassword()) && !PasswordEncoder.matches(user.getPassword(), form.getOldPassword())) return Result.fail("旧密码错误");
        user.setPassword(PasswordEncoder.encode(form.getNewPassword()));
        return updateById(user) ? Result.ok() : Result.fail("密码修改失败");
    }

    private User createUserWithPhone(String phone) {
        User user = new User(); user.setPhone(phone); user.setNickName("user_" + RandomUtil.randomString(10)); user.setRole("USER"); save(user);
        UserInfo info = new UserInfo(); info.setUserId(user.getId()); info.setFans(0); info.setFollowee(0); info.setCredits(0); info.setLevel(false); userInfoService.save(info);
        return user;
    }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
}
