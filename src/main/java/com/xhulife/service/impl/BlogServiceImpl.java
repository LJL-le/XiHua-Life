package com.xhulife.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhulife.dto.Result;
import com.xhulife.dto.UserDTO;
import com.xhulife.entity.Blog;
import com.xhulife.entity.User;
import com.xhulife.mapper.BlogMapper;
import com.xhulife.service.IBlogService;
import com.xhulife.service.IUserService;
import com.xhulife.service.IMessageService;
import com.xhulife.utils.RedisConstants;
import com.xhulife.utils.SystemConstants;
import com.xhulife.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    private static final DefaultRedisScript<Long> LIKE_TOGGLE_SCRIPT = new DefaultRedisScript<>();

    static {
        LIKE_TOGGLE_SCRIPT.setLocation(new ClassPathResource("blog_like_toggle.lua"));
        LIKE_TOGGLE_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource private IMessageService messageService;

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if (blog == null || !Integer.valueOf(0).equals(blog.getStatus())) {
            return Result.fail("笔记不存在");
        }
        // 填充用户信息
        User user = userService.getById(blog.getUserId());
        if (user != null) {
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        }
        // 判断当前用户是否点赞
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO != null) {
            Boolean isLiked = stringRedisTemplate.opsForSet().isMember(RedisConstants.BLOG_LIKED_KEY + id, userDTO.getId().toString());
            blog.setIsLike(isLiked);
        } else {
            blog.setIsLike(false);
        }
        return Result.ok(blog);
    }

    @Override
    public Result queryBlogLikes(Long id) {
        Set<String> userIds = stringRedisTemplate.opsForSet().members(RedisConstants.BLOG_LIKED_KEY + id);
        if (userIds == null || userIds.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 取前5个
        List<Long> ids = userIds.stream()
                .map(Long::valueOf)
                .limit(5)
                .collect(Collectors.toList());
        List<User> users = userService.listByIds(ids);
        List<UserDTO> userDTOs = users.stream().map(user -> {
            UserDTO dto = new UserDTO();
            dto.setId(user.getId());
            dto.setNickName(user.getNickName());
            dto.setIcon(user.getIcon());
            return dto;
        }).collect(Collectors.toList());
        return Result.ok(userDTOs);
    }

    @Override
    public Result queryBlogByUserId(Long userId, Integer current) {
        Page<Blog> page = query()
                .eq("user_id", userId).eq("status", 0)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        records.forEach(blog -> {
            User user = userService.getById(blog.getUserId());
            if (user != null) {
                blog.setName(user.getNickName());
                blog.setIcon(user.getIcon());
            }
        });
        return Result.ok(records);
    }

    @Override
    public Result likeBlog(Long id) {
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            return Result.fail("请先登录");
        }
        if (getById(id) == null) {
            return Result.fail("笔记不存在");
        }
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        String userId = userDTO.getId().toString();
        Long delta = stringRedisTemplate.execute(
                LIKE_TOGGLE_SCRIPT,
                Collections.singletonList(key),
                userId
        );
        if (delta == null) {
            return Result.fail("点赞失败，请稍后重试");
        }
        boolean updated;
        if (delta > 0) {
            updated = update().setSql("liked = COALESCE(liked, 0) + 1").eq("id", id).update();
        } else {
            updated = update().setSql("liked = GREATEST(COALESCE(liked, 0) - 1, 0)").eq("id", id).update();
        }
        if (!updated) {
            // 数据库更新失败时撤销 Redis 状态，尽量保持两端一致。
            stringRedisTemplate.execute(LIKE_TOGGLE_SCRIPT, Collections.singletonList(key), userId);
            return Result.fail("点赞失败，请稍后重试");
        }
        if (delta > 0) {
            Blog blog = getById(id);
            messageService.notify(blog.getUserId(), userDTO.getId(), "LIKE", "赞了你的笔记", "BLOG", id,
                    "LIKE:" + id + ":" + userDTO.getId());
        }
        return Result.ok();
    }
}

