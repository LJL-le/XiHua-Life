package com.xhulife.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xhulife.dto.Result;
import com.xhulife.dto.UserDTO;
import com.xhulife.entity.BlogComments;
import com.xhulife.entity.User;
import com.xhulife.service.IBlogCommentsService;
import com.xhulife.service.IBlogService;
import com.xhulife.service.IUserService;
import com.xhulife.service.IMessageService;
import com.xhulife.utils.SystemConstants;
import com.xhulife.utils.UserHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog-comments")
public class BlogCommentsController {

    @Resource
    private IBlogCommentsService blogCommentsService;

    @Resource
    private IUserService userService;

    @Resource
    private IBlogService blogService;
    @Resource private IMessageService messageService;

    @GetMapping("/{blogId}")
    public Result queryComments(@PathVariable Long blogId,
                                @RequestParam(value = "current", defaultValue = "1") Integer current) {
        Page<BlogComments> page = blogCommentsService.query()
                .eq("blog_id", blogId)
                .eq("status", 0)
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        List<BlogComments> records = page.getRecords();
        records.forEach(comment -> {
            User user = userService.getById(comment.getUserId());
            if (user != null) {
                comment.setIcon(user.getIcon());
                comment.setName(user.getNickName());
            }
        });
        return Result.ok(records);
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result saveComment(@RequestBody BlogComments comment) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        if (comment.getBlogId() == null || blogService.getById(comment.getBlogId()) == null) {
            return Result.fail("笔记不存在");
        }
        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            return Result.fail("评论内容不能为空");
        }
        comment.setId(null);
        comment.setUserId(user.getId());
        comment.setLiked(0);
        comment.setStatus(false);
        comment.setCreateTime(LocalDateTime.now());
        if (!blogCommentsService.save(comment)) {
            return Result.fail("发表评论失败");
        }
        boolean updated = blogService.update().setSql("comments = COALESCE(comments, 0) + 1")
                .eq("id", comment.getBlogId()).update();
        if (!updated) {
            throw new IllegalStateException("更新评论数失败");
        }
        com.xhulife.entity.Blog blog = blogService.getById(comment.getBlogId());
        messageService.notify(blog.getUserId(), user.getId(), "COMMENT", "评论了你的笔记", "BLOG", blog.getId(), null);
        return Result.ok(comment.getId());
    }

    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result deleteComment(@PathVariable Long id) {
        BlogComments comment = blogCommentsService.getById(id);
        if (comment == null) {
            return Result.fail("评论不存在");
        }
        UserDTO user = UserHolder.getUser();
        if (user == null || !user.getId().equals(comment.getUserId())) {
            return Result.fail("无权删除");
        }
        boolean removed = blogCommentsService.remove(new LambdaQueryWrapper<BlogComments>()
                .eq(BlogComments::getId, id)
                .eq(BlogComments::getUserId, user.getId()));
        if (!removed) {
            return Result.fail("评论已被删除");
        }
        boolean updated = blogService.update()
                .setSql("comments = GREATEST(COALESCE(comments, 0) - 1, 0)")
                .eq("id", comment.getBlogId()).update();
        if (!updated) {
            throw new IllegalStateException("更新评论数失败");
        }
        return Result.ok();
    }
}

