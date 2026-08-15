package com.xhulife.controller;

import com.xhulife.dto.Result;
import com.xhulife.entity.ShopReview;
import com.xhulife.service.IShopReviewService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/shop-reviews")
public class ShopReviewController {
    @Resource private IShopReviewService reviewService;
    @GetMapping public Result list(@RequestParam Long shopId, @RequestParam(defaultValue="1") Integer current,
                                   @RequestParam(defaultValue="10") Integer size) { return reviewService.queryByShop(shopId, current, size); }
    @PostMapping public Result create(@RequestBody ShopReview review) { return reviewService.createReview(review); }
    @PutMapping public Result update(@RequestBody ShopReview review) { return reviewService.updateReview(review); }
    @DeleteMapping("/{id}") public Result delete(@PathVariable Long id) { return reviewService.deleteReview(id); }
}
