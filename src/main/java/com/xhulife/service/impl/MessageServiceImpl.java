package com.xhulife.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhulife.entity.Message;
import com.xhulife.mapper.MessageMapper;
import com.xhulife.service.IMessageService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {
    @Override
    public void notify(Long userId, Long actorId, String type, String content,
                       String targetType, Long targetId, String dedupeKey) {
        if (userId == null || userId.equals(actorId)) return;
        Message message = new Message();
        message.setUserId(userId);
        message.setActorId(actorId);
        message.setType(type);
        message.setContent(content);
        message.setTargetType(targetType);
        message.setTargetId(targetId);
        message.setIsRead(false);
        message.setDedupeKey(dedupeKey);
        message.setCreateTime(LocalDateTime.now());
        try {
            save(message);
        } catch (DuplicateKeyException ignored) {
            // A repeated like/follow action must not create notification spam.
        }
    }
}
