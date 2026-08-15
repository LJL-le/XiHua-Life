package com.xhulife.service.impl;

import com.xhulife.entity.ShopType;
import com.xhulife.mapper.ShopTypeMapper;
import com.xhulife.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public List<ShopType> queryTypeList() {
        // 从缓存中查询全部类型
        List<String> shopTypeJsonList = stringRedisTemplate.opsForList().range("cache:shop:type", 0, -1);
        if (shopTypeJsonList != null && !shopTypeJsonList.isEmpty()) {
            // 缓存中有数据，转换为 ShopType 对象
            return shopTypeJsonList.stream()
                .map(json -> {
                    try {
                        return cn.hutool.json.JSONUtil.toBean(json, ShopType.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(shopType -> shopType != null)
                .collect(java.util.stream.Collectors.toList());
        }

        // 缓存中没有，从数据库中查询
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        if (shopTypes != null && !shopTypes.isEmpty()) {
            // 写入缓存
            shopTypes.forEach(shopType -> {
                String json = cn.hutool.json.JSONUtil.toJsonStr(shopType);
                stringRedisTemplate.opsForList().rightPush("cache:shop:type", json);
            });
        }
        // 返回结果
        return shopTypes;
    }
}

