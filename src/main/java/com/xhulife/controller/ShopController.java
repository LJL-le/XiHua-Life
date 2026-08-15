package com.xhulife.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xhulife.dto.Result;
import com.xhulife.entity.Shop;
import com.xhulife.service.IShopService;
import com.xhulife.utils.SystemConstants;
import com.xhulife.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    public IShopService shopService;

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.queryByIds(id);
    }

    /**
     * 新增商铺信息
     * @param shop 商铺数据
     * @return 商铺id
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        if (!isAdmin()) return Result.fail("仅管理员可新增商铺");
        // 写入数据库
        shopService.save(shop);
        // 返回店铺id
        return Result.ok(shop.getId());
    }

    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        if (!isAdmin()) return Result.fail("仅管理员可修改商铺");
        // 写入数据库
        return shopService.update1(shop);
    }

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId 商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y
    ) {
        // 根据类型分页查询
        QueryChainWrapper<Shop> query = shopService.query().eq("type_id", typeId);
        if ("comments".equals(sortBy)) {
            query.orderByDesc("comments");
        } else if ("score".equals(sortBy)) {
            query.orderByDesc("score");
        } else if (x != null && y != null) {
            query.last(String.format(Locale.US,
                    "ORDER BY POW(x - %.6f, 2) + POW(y - %.6f, 2) ASC", x, y));
        }
        Page<Shop> page = query.page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        List<Shop> records = page.getRecords();
        if (x != null && y != null) {
            records.forEach(shop -> {
                if (shop.getX() != null && shop.getY() != null) {
                    shop.setDistance(distanceMeters(x, y, shop.getX(), shop.getY()));
                }
            });
        }
        // 返回数据
        return Result.ok(records);
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     * @param name 商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }

    private double distanceMeters(double x1, double y1, double x2, double y2) {
        double earthRadius = 6371000D;
        double lat1 = Math.toRadians(y1);
        double lat2 = Math.toRadians(y2);
        double deltaLat = Math.toRadians(y2 - y1);
        double deltaLng = Math.toRadians(x2 - x1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private boolean isAdmin() {
        return UserHolder.getUser() != null && "ADMIN".equals(UserHolder.getUser().getRole());
    }
}

