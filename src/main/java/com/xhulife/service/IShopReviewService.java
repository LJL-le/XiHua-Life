package com.xhulife.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhulife.dto.Result;
import com.xhulife.entity.ShopReview;

public interface IShopReviewService extends IService<ShopReview> {
    Result queryByShop(Long shopId, Integer current, Integer size);
    Result createReview(ShopReview review);
    Result updateReview(ShopReview review);
    Result deleteReview(Long id);
}
