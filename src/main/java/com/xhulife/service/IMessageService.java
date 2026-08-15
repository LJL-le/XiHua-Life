package com.xhulife.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhulife.entity.Message;

public interface IMessageService extends IService<Message> {
    void notify(Long userId, Long actorId, String type, String content,
                String targetType, Long targetId, String dedupeKey);
}
