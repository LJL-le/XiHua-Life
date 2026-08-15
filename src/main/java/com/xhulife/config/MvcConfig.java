package com.xhulife.config;

import com.xhulife.utils.LoginInterceptor;
import com.xhulife.utils.RefreshInterceptor;
import com.xhulife.utils.AdminInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/blog/hot",
                        "/blog/{id}",
                        "/blog/likes/{id}",
                        "/blog/of/user",
                        "/user/{id}",
                        "/shop-type/**",
                        "/free-qualification/activities/shop/**"
                ).order(1);
        registry.addInterceptor(new AdminInterceptor()).addPathPatterns("/admin/**").order(2);

        registry.addInterceptor(new RefreshInterceptor( stringRedisTemplate)).addPathPatterns("/**").order(0);
    }
}

