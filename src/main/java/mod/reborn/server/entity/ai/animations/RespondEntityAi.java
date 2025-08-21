package mod.reborn.server.entity.ai.animations;

import mod.reborn.client.model.animation.EntityAnimation;
import mod.reborn.server.entity.DinosaurEntity;
import mod.reborn.server.entity.ai.Mutex;
import mod.reborn.server.entity.ai.hearing.SoundEventInfo;
import net.minecraft.entity.ai.EntityAIBase;

import java.util.List;

public class RespondEntityAi extends EntityAIBase {
    private final DinosaurEntity entity;
    private static final float RESPONSE_CHANCE = 0.04F; // 4% chance to respond
    private static final int COOLDOWN_TICKS = 300; // 10 seconds cooldown
    private int cooldownTicks;

    public RespondEntityAi(DinosaurEntity entity) {
        this.entity = entity;
        this.setMutexBits(Mutex.ANIMATION);
    }

    @Override
    public boolean shouldExecute() {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return false;
        }

        List<SoundEventInfo> recentSounds = this.entity.listener.getRecentSounds();
        for (SoundEventInfo sound : recentSounds) {
            if (shouldRespondToSound(sound)) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldRespondToSound(SoundEventInfo sound) {
        if (sound.getSender() == null ||
                sound.getSender() == this.entity ||
                sound.getSoundType() != EntityAnimation.CALLING) {
            return false;
        }

        // Check if sender is same species
        if (!this.entity.getClass().equals(sound.getSender().getClass())) {
            return false;
        }

        // Check response chance
        return this.entity.getRNG().nextFloat() < RESPONSE_CHANCE;
    }

    @Override
    public void startExecuting() {
        // Play calling animation and sound
        this.entity.setAnimation(EntityAnimation.CALLING.get());
        this.entity.EmmitSound(EntityAnimation.CALLING);

        // Reset cooldown
        this.cooldownTicks = COOLDOWN_TICKS;
    }

    @Override
    public void resetTask() {
        this.entity.setAnimation(null);
    }
}
