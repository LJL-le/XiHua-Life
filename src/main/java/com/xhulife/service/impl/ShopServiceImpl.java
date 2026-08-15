package com.xhulife.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhulife.dto.Result;
import com.xhulife.entity.Shop;
import com.xhulife.mapper.ShopMapper;
import com.xhulife.service.IShopService;
import com.xhulife.utils.CatchClient;
import com.xhulife.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * Shop service implementation.
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CatchClient catchClient;

    @Override
    public Result queryByIds(Long id) {
        Shop shop = catchClient.queryWithLogicalExpire(
                RedisConstants.CACHE_SHOP_KEY,
                RedisConstants.LOCK_SHOP_KEY,
                id,
                Shop.class,
                RedisConstants.CACHE_SHOP_TTL,
                TimeUnit.MINUTES,
                RedisConstants.CACHE_SHOP_PHYSICAL_TTL,
                TimeUnit.HOURS,
                RedisConstants.CACHE_NULL_TTL,
                TimeUnit.MINUTES,
                this::getById
        );
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update1(Shop shop) {
        if (shop.getId() == null) {
            return Result.fail("店铺不存在");
        }

        boolean updated = updateById(shop);
        if (!updated) {
            return Result.fail("店铺更新失败");
        }

        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
