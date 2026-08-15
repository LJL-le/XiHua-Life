package com.xhulife.service;

import com.xhulife.dto.Result;
import com.xhulife.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    Result queryByIds(Long id);

    Result update1(Shop shop);
}

