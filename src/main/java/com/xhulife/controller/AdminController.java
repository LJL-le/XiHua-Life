package com.xhulife.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xhulife.dto.PageResult;
import com.xhulife.dto.Result;
import com.xhulife.entity.*;
import com.xhulife.repository.FreeQualificationRedisRepository;
import com.xhulife.service.*;
import com.xhulife.utils.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static com.xhulife.utils.RedisConstants.*;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Resource private IShopService shopService;
    @Resource private IUserService userService;
    @Resource private IBlogService blogService;
    @Resource private IBlogCommentsService commentsService;
    @Resource private IShopReviewService reviewService;
    @Resource private IFreeQualificationActivityService activityService;
    @Resource private IFreeQualificationRecordService recordService;
    @Resource private IMessageService messageService;
    @Resource private FreeQualificationRedisRepository redisRepository;
    @Resource private StringRedisTemplate redis;

    @GetMapping("/shops") public Result shops(@RequestParam(defaultValue="1") Integer current, @RequestParam(defaultValue="20") Integer size) {
        Page<Shop> p = shopService.page(new Page<>(current, Math.min(size, 50)));
        return Result.ok(new PageResult<>(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal()));
    }
    @PostMapping("/shops") public Result addShop(@RequestBody Shop shop) { return shopService.save(shop) ? Result.ok(shop.getId()) : Result.fail("新增商铺失败"); }
    @PutMapping("/shops") public Result updateShop(@RequestBody Shop shop) { return shopService.update1(shop); }

    @GetMapping("/users") public Result users(@RequestParam(defaultValue="1") Integer current, @RequestParam(defaultValue="20") Integer size) {
        Page<User> p = userService.page(new Page<>(current, Math.min(size, 50)));
        p.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(new PageResult<>(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal()));
    }
    @PostMapping("/users") public Result addUser(@RequestBody User user) {
        if (user.getPhone() == null || user.getPassword() == null || user.getPassword().length() < 6) return Result.fail("手机号和至少 6 位密码不能为空");
        if (userService.query().eq("phone", user.getPhone()).count() > 0) return Result.fail("手机号已存在");
        user.setId(null); user.setPassword(PasswordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) user.setRole("USER");
        return userService.save(user) ? Result.ok(user.getId()) : Result.fail("创建用户失败");
    }
    @PutMapping("/users/{id}/password") public Result resetPassword(@PathVariable Long id, @RequestBody java.util.Map<String,String> body) {
        String password = body.get("password"); if (password == null || password.length() < 6) return Result.fail("密码至少 6 位");
        return userService.update().set("password", PasswordEncoder.encode(password)).eq("id", id).update() ? Result.ok() : Result.fail("用户不存在");
    }

    @GetMapping("/free-qualification/activities") public Result activities() { return Result.ok(activityService.list()); }
    @PostMapping("/free-qualification/activities") public Result addActivity(@RequestBody FreeQualificationActivity a) { return activityService.createActivity(a); }
    @PutMapping("/free-qualification/activities") @Transactional public Result updateActivity(@RequestBody FreeQualificationActivity input) {
        FreeQualificationActivity old = input.getId() == null ? null : activityService.getById(input.getId());
        if (old == null) return Result.fail("活动不存在");
        int claimed = old.getTotalQuota() - old.getRemainingQuota();
        if (input.getTotalQuota() != null && input.getTotalQuota() < claimed) return Result.fail("总名额不能小于已领取数");
        if (old.getBeginTime().isBefore(LocalDateTime.now()) && input.getBeginTime() != null && !input.getBeginTime().equals(old.getBeginTime())) return Result.fail("已开始活动不能修改开始时间");
        if (input.getTotalQuota() != null) input.setRemainingQuota(input.getTotalQuota() - claimed);
        activityService.updateById(input);
        redis.delete(java.util.Arrays.asList(FREE_QUALIFICATION_ACTIVITY_KEY + input.getId(), FREE_QUALIFICATION_STOCK_KEY + input.getId(), FREE_QUALIFICATION_USERS_KEY + input.getId()));
        redisRepository.ensureInitialized(input.getId()); return Result.ok();
    }
    @GetMapping("/free-qualification/records") public Result records(@RequestParam(defaultValue="1") Integer current, @RequestParam(defaultValue="20") Integer size) {
        Page<FreeQualificationRecord> p = recordService.page(new Page<>(current, Math.min(size, 100)));
        return Result.ok(new PageResult<>(p.getRecords(), p.getCurrent(), p.getSize(), p.getTotal()));
    }
    @PostMapping("/free-qualification/records/redeem") @Transactional public Result redeem(@RequestBody java.util.Map<String,String> body) {
        String code = body.get("redeemCode");
        FreeQualificationRecord record = recordService.query().eq("redeem_code", code).one();
        if (record == null) return Result.fail("核销码不存在");
        if (Integer.valueOf(1).equals(record.getStatus())) return Result.ok(record.getId());
        if (!Integer.valueOf(0).equals(record.getStatus())) return Result.fail("资格不可核销");
        FreeQualificationActivity activity = activityService.getById(record.getActivityId());
        if (activity == null || activity.getEndTime().isBefore(LocalDateTime.now())) return Result.fail("资格已过期");
        record.setStatus(1); record.setUseTime(LocalDateTime.now()); recordService.updateById(record);
        messageService.notify(record.getUserId(), null, "QUALIFICATION", "免单资格已核销", "QUALIFICATION", record.getId(), "REDEEM:" + record.getId());
        return Result.ok(record.getId());
    }

    @PutMapping("/content/{type}/{id}/status") public Result contentStatus(@PathVariable String type, @PathVariable Long id, @RequestBody java.util.Map<String,Integer> body) {
        Integer status = body.get("status"); boolean ok;
        if ("blog".equals(type)) ok = blogService.update().set("status", status).eq("id", id).update();
        else if ("comment".equals(type)) ok = commentsService.update().set("status", status).eq("id", id).update();
        else if ("review".equals(type)) ok = reviewService.update().set("status", status).eq("id", id).update();
        else return Result.fail("不支持的内容类型");
        return ok ? Result.ok() : Result.fail("内容不存在");
    }
}
