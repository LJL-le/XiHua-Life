package com.xhulife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xhulife.dto.Result;
import com.xhulife.dto.UserDTO;
import com.xhulife.entity.Follow;
import com.xhulife.mapper.FollowMapper;
import com.xhulife.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhulife.service.IUserInfoService;
import com.xhulife.service.IUserService;
import com.xhulife.service.IMessageService;
import com.xhulife.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private IUserService userService;
    @Resource private IMessageService messageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result follow(Long followUserId, Boolean followed) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");        }
        Long userId = user.getId();
        if (followUserId == null || userId.equals(followUserId)) {
            return Result.fail("不能关注自己");
        }
        if (userService.getById(followUserId) == null) {
            return Result.fail("用户不存在");
        }
        if (Boolean.TRUE.equals(followed)) {
            // 检查是否已关注
            Integer count = query()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId)
                    .count();
            if (count > 0) {
                return Result.ok();
            }
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            if (!save(follow)) {
                return Result.fail("关注失败");
            }
            // 更新当前用户的关注数
            userInfoService.update()
                    .setSql("followee = COALESCE(followee, 0) + 1")
                    .eq("user_id", userId)
                    .update();
            messageService.notify(followUserId, userId, "FOLLOW", "关注了你", "USER", userId,
                    "FOLLOW:" + followUserId + ":" + userId);
            // 更新对方的粉丝数
            userInfoService.update()
                    .setSql("fans = COALESCE(fans, 0) + 1")
                    .eq("user_id", followUserId)
                    .update();
        } else {
            LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Follow::getUserId, userId)
                    .eq(Follow::getFollowUserId, followUserId);
            if (!remove(wrapper)) {
                return Result.ok();
            }
            // 更新当前用户的关注数
            userInfoService.update()
                    .setSql("followee = GREATEST(COALESCE(followee, 0) - 1, 0)")
                    .eq("user_id", userId)
                    .update();
            // 更新对方的粉丝数
            userInfoService.update()
                    .setSql("fans = GREATEST(COALESCE(fans, 0) - 1, 0)")
                    .eq("user_id", followUserId)
                    .update();
        }
        return Result.ok();
    }
}

