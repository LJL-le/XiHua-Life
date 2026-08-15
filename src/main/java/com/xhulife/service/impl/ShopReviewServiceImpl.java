package com.xhulife.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhulife.dto.PageResult;
import com.xhulife.dto.Result;
import com.xhulife.dto.UserDTO;
import com.xhulife.entity.ShopReview;
import com.xhulife.entity.User;
import com.xhulife.mapper.ShopReviewMapper;
import com.xhulife.service.IShopReviewService;
import com.xhulife.service.IShopService;
import com.xhulife.service.IUserService;
import com.xhulife.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.xhulife.utils.RedisConstants;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class ShopReviewServiceImpl extends ServiceImpl<ShopReviewMapper, ShopReview> implements IShopReviewService {
    @Resource private IShopService shopService;
    @Resource private IUserService userService;
    @Resource private StringRedisTemplate redis;

    @Override
    public Result queryByShop(Long shopId, Integer current, Integer size) {
        Page<ShopReview> page = query().eq("shop_id", shopId).eq("status", 0)
                .orderByDesc("create_time").page(new Page<>(current, Math.min(size, 50)));
        page.getRecords().forEach(review -> {
            User user = userService.getById(review.getUserId());
            if (user != null) {
                review.setUserName(user.getNickName());
                review.setUserIcon(user.getIcon());
            }
        });
        return Result.ok(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public Result createReview(ShopReview review) {
        UserDTO user = UserHolder.getUser();
        if (user == null) return Result.fail("请先登录");
        String error = validate(review);
        if (error != null) return Result.fail(error);
        if (shopService.getById(review.getShopId()) == null) return Result.fail("商铺不存在");
        if (query().eq("shop_id", review.getShopId()).eq("user_id", user.getId()).count() > 0)
            return Result.fail("每位用户只能评价一次，可修改已有评价");
        review.setId(null); review.setUserId(user.getId()); review.setStatus(0);
        review.setCreateTime(LocalDateTime.now()); review.setUpdateTime(LocalDateTime.now());
        save(review); recalculate(review.getShopId());
        return Result.ok(review.getId());
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public Result updateReview(ShopReview input) {
        UserDTO user = UserHolder.getUser();
        ShopReview existing = input.getId() == null ? null : getById(input.getId());
        if (user == null || existing == null || !user.getId().equals(existing.getUserId())) return Result.fail("无权修改评价");
        String error = validate(input); if (error != null) return Result.fail(error);
        existing.setScore(input.getScore()); existing.setTasteScore(input.getTasteScore());
        existing.setEnvironmentScore(input.getEnvironmentScore()); existing.setServiceScore(input.getServiceScore());
        existing.setContent(input.getContent()); existing.setImages(input.getImages()); existing.setUpdateTime(LocalDateTime.now());
        updateById(existing); recalculate(existing.getShopId()); return Result.ok();
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public Result deleteReview(Long id) {
        UserDTO user = UserHolder.getUser(); ShopReview existing = getById(id);
        if (user == null || existing == null || !user.getId().equals(existing.getUserId())) return Result.fail("无权删除评价");
        removeById(id); recalculate(existing.getShopId()); return Result.ok();
    }

    private String validate(ShopReview r) {
        if (r.getShopId() == null) return "请选择商铺";
        if (!validScore(r.getScore()) || !validScore(r.getTasteScore()) || !validScore(r.getEnvironmentScore()) || !validScore(r.getServiceScore())) return "评分必须为 1 到 5";
        if (r.getContent() == null || r.getContent().trim().isEmpty() || r.getContent().length() > 1000) return "评价内容长度应为 1 到 1000 字";
        return null;
    }
    private boolean validScore(Integer score) { return score != null && score >= 1 && score <= 5; }
    private void recalculate(Long shopId) {
        java.util.List<ShopReview> reviews = query().eq("shop_id", shopId).eq("status", 0).list();
        int count = reviews.size();
        int score = count == 0 ? 0 : (int)Math.round(reviews.stream().mapToInt(ShopReview::getScore).average().orElse(0) * 10);
        shopService.update().set("comments", count).set("score", score).eq("id", shopId).update();
        redis.delete(RedisConstants.CACHE_SHOP_KEY + shopId);
    }
}
