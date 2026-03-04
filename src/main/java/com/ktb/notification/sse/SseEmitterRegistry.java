package com.ktb.notification.sse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class SseEmitterRegistry {

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    public SseEmitter register(Long accountId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(accountId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(accountId, emitter));
        emitter.onTimeout(() -> remove(accountId, emitter));
        emitter.onError(e -> remove(accountId, emitter));
        return emitter;
    }

    public void send(Long accountId, boolean hasUnread) {
        List<SseEmitter> list = emitters.getOrDefault(accountId, new CopyOnWriteArrayList<>());
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("unread").data(hasUnread));
            } catch (IOException e) {
                log.debug("SSE emitter dead for accountId={}, removing", accountId);
                dead.add(emitter);
            }
        }
        dead.forEach(e -> remove(accountId, e));
    }

    private void remove(Long accountId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(accountId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
