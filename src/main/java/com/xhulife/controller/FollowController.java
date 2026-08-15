package com.xhulife.controller;


import cn.hutool.core.bean.BeanUtil;
import com.xhulife.dto.Result;
import com.xhulife.dto.UserDTO;
import com.xhulife.entity.Follow;
import com.xhulife.service.IFollowService;
import com.xhulife.service.IUserService;
import com.xhulife.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    @Resource
    private IUserService userService;

    @PutMapping("/{id}/{followed}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("followed") Boolean followed) {
        return followService.follow(followUserId, followed);
    }

    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.ok(false);
        }
        Integer count = followService.query()
                .eq("user_id", user.getId())
                .eq("follow_user_id", followUserId)
                .count();
        return Result.ok(count > 0);
    }

    @GetMapping("/common/{id}")
    public Result commonFollows(@PathVariable("id") Long userId) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        List<Follow> myFollows = followService.query()
                .eq("user_id", user.getId()).list();
        if (myFollows.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        Set<Long> myFollowIds = myFollows.stream()
                .map(Follow::getFollowUserId)
                .collect(Collectors.toSet());
        List<Follow> targetFollows = followService.query()
                .eq("user_id", userId).list();
        List<Long> commonIds = targetFollows.stream()
                .map(Follow::getFollowUserId)
                .filter(myFollowIds::contains)
                .collect(Collectors.toList());
        List<UserDTO> users = userService.listByIds(commonIds).stream()
                .map(u -> BeanUtil.copyProperties(u, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }
}

