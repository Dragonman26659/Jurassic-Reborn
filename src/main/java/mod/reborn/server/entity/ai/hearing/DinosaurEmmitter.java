package mod.reborn.server.entity.ai.hearing;

import java.util.List;
import mod.reborn.client.model.animation.EntityAnimation;
import mod.reborn.client.sound.SoundHandler;
import mod.reborn.server.entity.DinosaurEntity;
import mod.reborn.server.entity.ai.Mutex;
import net.ilexiconn.llibrary.server.animation.IAnimatedEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.AxisAlignedBB;

import javax.swing.*;


// use to send sounds
public class DinosaurEmmitter {
    protected DinosaurEntity emmitter;


    public DinosaurEmmitter(IAnimatedEntity entity) {
        this.emmitter = (DinosaurEntity) entity;
    }


    public List<Entity> getEntitiesWithinDistance(Entity entity, double width, double height) {
        return entity.world.getEntitiesWithinAABBExcludingEntity(entity, new AxisAlignedBB(entity.posX - width, entity.posY - height, entity.posZ - width, entity.posX + width, entity.posY + height, entity.posZ + width));
    }

    public void EmmitSound(EntityAnimation animation) {
        // Play the sound locally first

        if (animation == EntityAnimation.ROARING || animation == EntityAnimation.CALLING) {
            this.emmitter.playSound(this.emmitter.getSoundForAnimation(animation.get()),
                    this.emmitter.getSoundVolume() > 0.0F ? this.emmitter.getSoundVolume() + 1.25F : 0.0F,
                    this.emmitter.getSoundPitch());
        } else if (animation == EntityAnimation.DILOPHOSAURUS_SPIT) {
            this.emmitter.playSound(SoundHandler.DILOPHOSAURUS_SPIT,
                    this.emmitter.getSoundVolume(),
                    this.emmitter.getSoundPitch());
        } else {
            this.emmitter.playSound(this.emmitter.getSoundForAnimation(animation.get()),
                    this.emmitter.getSoundVolume(),
                    this.emmitter.getSoundPitch());
        }

        // Notify nearby listeners within 150 blocks
        List<Entity> entities = this.getEntitiesWithinDistance(this.emmitter, 150, 150);

        for (Entity entity : entities) {
            if (entity instanceof DinosaurEntity) {
                DinosaurEntity otherDino = (DinosaurEntity) entity;
                if (otherDino != this.emmitter) {
                    otherDino.listener.hearSound(this.emmitter, animation);
                }
            }
        }
    }
}
