package mod.reborn.server.entity.ai;

import mod.reborn.client.model.animation.EntityAnimation;
import mod.reborn.server.entity.DinosaurEntity;
import mod.reborn.server.entity.ai.hearing.SoundEventInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

import java.util.LinkedList;
import java.util.List;

public class FleeEntityAI extends EntityAIBase {
    private DinosaurEntity dinosaur;
    private List<EntityLivingBase> attackers;

    public FleeEntityAI(DinosaurEntity dinosaur) {
        this.dinosaur = dinosaur;
    }

    @Override
    public boolean shouldExecute() {
        if (this.dinosaur.ticksExisted % 5 == 0) {
            // Check for enemies within range
            List<DinosaurEntity> entities = this.dinosaur.world.getEntitiesWithinAABB(
                    DinosaurEntity.class,
                    this.dinosaur.getEntityBoundingBox().expand(10, 40, 10)
            );

            // Initialize attackers list
            this.attackers = new LinkedList<>();

            // Process visually detected enemies
            for (DinosaurEntity entity : entities) {
                if (entity != this.dinosaur && !entity.isCarcass()) {
                    for (Class<? extends EntityLivingBase> target : entity.getAttackTargets()) {
                        if (target.isAssignableFrom(this.dinosaur.getClass())) {
                            this.attackers.add(entity);
                            if (entity.getAttackTarget() == null) {
                                entity.setAttackTarget(this.dinosaur);
                            }

                            // Handle herd dynamics
                            if (entity.herd != null) {
                                if (this.dinosaur.herd != null) {
                                    entity.herd.enemies.addAll(this.dinosaur.herd.members);
                                } else {
                                    entity.herd.enemies.add(this.dinosaur);
                                }
                            }
                            break;
                        }
                    }
                }
            }

            // Check for threatening sounds
            List<SoundEventInfo> recentSounds = this.dinosaur.listener.getRecentSounds();
            for (SoundEventInfo sound : recentSounds) {
                DinosaurEntity sender = sound.getSender();

                // Skip if sender is self or already detected visually
                if (sender == null || sender == this.dinosaur || this.attackers.contains(sender)) {
                    continue;
                }

                // Check if sound indicates threat
                if (sound.getSoundType().equals(EntityAnimation.ROARING) ||
                        sound.getSoundType().equals(EntityAnimation.CALLING)) {

                    // Add to attackers list
                    this.attackers.add(sender);

                    // Handle herd dynamics for audio-detected threats
                    if (sender.herd != null) {
                        if (this.dinosaur.herd != null) {
                            sender.herd.enemies.addAll(this.dinosaur.herd.members);
                        } else {
                            sender.herd.enemies.add(this.dinosaur);
                        }
                    }
                }
            }

            return this.attackers.size() > 0;
        }

        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return false;
    }

    @Override
    public void startExecuting() {
        Herd herd = this.dinosaur.herd;

        if (herd != null && this.attackers != null && this.attackers.size() > 0) {
            for (EntityLivingBase attacker : this.attackers) {
                if (!herd.enemies.contains(attacker)) {
                    herd.enemies.add(attacker);
                }
            }

            herd.fleeing = true;
        }

        this.dinosaur.EmmitSound(EntityAnimation.CALLING);
        this.dinosaur.setAnimation(EntityAnimation.CALLING.get());
    }
}
