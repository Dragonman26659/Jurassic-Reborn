package mod.reborn.server.entity.ai.hearing;

import mod.reborn.server.entity.DinosaurEntity;
import net.ilexiconn.llibrary.server.animation.IAnimatedEntity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import mod.reborn.client.model.animation.EntityAnimation;

import java.util.Queue;
import java.util.stream.Collectors;


public class DinosaurListener {
    protected DinosaurEntity listener;
    private final Queue<SoundEventInfo> recentSounds;
    private final int maxHistorySize = 10;

    public DinosaurListener(IAnimatedEntity entity) {
        this.listener = (DinosaurEntity) entity;
        this.recentSounds = new LinkedList<>();
    }

    public void hearSound(DinosaurEntity sender, EntityAnimation soundType) {
        synchronized (recentSounds) {
            recentSounds.offer(new SoundEventInfo(sender, soundType));
            if (recentSounds.size() > maxHistorySize) {
                recentSounds.poll();
            }
        }
    }

    // Get sounds heard within last few ticks
    public List<SoundEventInfo> getRecentSounds() {
        synchronized (recentSounds) {
            return new ArrayList<>(recentSounds);
        }
    }

    // Filter sounds by type
    public List<SoundEventInfo> getRecentSoundsOfType(EntityAnimation type) {
        synchronized (recentSounds) {
            return recentSounds.stream()
                    .filter(info -> info.getSoundType().equals(type))
                    .collect(Collectors.toList());
        }
    }

    // Get sounds from specific sender
    public List<SoundEventInfo> getSoundsFromSender(DinosaurEntity sender) {
        synchronized (recentSounds) {
            return recentSounds.stream()
                    .filter(info -> info.getSender().equals(sender))
                    .collect(Collectors.toList());
        }
    }
}
