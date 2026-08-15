package com.xhulife.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xhulife.dto.PageResult;
import com.xhulife.dto.Result;
import com.xhulife.dto.UserDTO;
import com.xhulife.entity.Message;
import com.xhulife.service.IMessageService;
import com.xhulife.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/messages")
public class MessageController {
    @Resource private IMessageService messageService;
    @GetMapping public Result list(@RequestParam(defaultValue="1") Integer current, @RequestParam(defaultValue="10") Integer size) {
        UserDTO user = UserHolder.getUser();
        Page<Message> page = messageService.query().eq("user_id", user.getId()).orderByDesc("create_time")
                .page(new Page<>(current, Math.min(size, 50)));
        return Result.ok(new PageResult<>(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
    }
    @GetMapping("/unread-count") public Result unread() {
        return Result.ok(messageService.query().eq("user_id", UserHolder.getUser().getId()).eq("is_read", false).count());
    }
    @PutMapping("/{id}/read") public Result read(@PathVariable Long id) {
        boolean ok = messageService.update().set("is_read", true).eq("id", id).eq("user_id", UserHolder.getUser().getId()).update();
        return ok ? Result.ok() : Result.fail("消息不存在");
    }
    @PutMapping("/read-all") public Result readAll() {
        messageService.update().set("is_read", true).eq("user_id", UserHolder.getUser().getId()).eq("is_read", false).update(); return Result.ok();
    }
}
