package com.xhulife.service;

import com.xhulife.dto.Result;
import com.xhulife.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long id);

    Result queryBlogLikes(Long id);

    Result queryBlogByUserId(Long userId, Integer current);

    Result likeBlog(Long id);
}

