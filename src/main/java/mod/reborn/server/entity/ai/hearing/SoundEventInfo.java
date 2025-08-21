package mod.reborn.server.entity.ai.hearing;

import mod.reborn.server.entity.DinosaurEntity;
import mod.reborn.client.model.animation.EntityAnimation;

public class SoundEventInfo {
    private final DinosaurEntity sender;
    private final EntityAnimation soundType;
    private final long timestamp;

    public SoundEventInfo(DinosaurEntity sender, EntityAnimation soundType) {
        this.sender = sender;
        this.soundType = soundType;
        this.timestamp = System.currentTimeMillis();
    }

    public DinosaurEntity getSender() {
        return sender;
    }

    public EntityAnimation getSoundType() {
        return soundType;
    }

    public long getTimestamp() {
        return timestamp;
    }
}