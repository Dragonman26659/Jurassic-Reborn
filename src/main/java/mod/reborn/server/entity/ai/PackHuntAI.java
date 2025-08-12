package mod.reborn.server.entity.ai;

import mod.reborn.client.model.animation.EntityAnimation;
import mod.reborn.server.entity.DinosaurEntity;
import mod.reborn.server.entity.ai.Mutex;
import net.ilexiconn.llibrary.server.animation.IAnimatedEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import java.util.stream.Collectors;
import net.minecraft.util.math.Vec3d;

import java.util.List;


public class PackHuntAI<T extends DinosaurEntity> extends EntityAIBase {
    private final DinosaurEntity dinosaur;
    private EntityLivingBase target;

    private Class<T> dinosaurClazz;
    private List<DinosaurEntity> packMembers;
    private static final int MAX_CALL_DISTANCE = 10;
    private static final int MIN_ATTACKERS = 2;

    public PackHuntAI(T dinosaur) {
        this.dinosaur = dinosaur;
        this.dinosaurClazz = (Class<T>) dinosaur.getClass();
        this.setMutexBits(Mutex.ATTACK);
    }

    @Override
    public boolean shouldExecute() {
        // Dont hunt unless hungry
        if (!this.entity.getMetabolism().isHungry()) {
            return false;
        }

        if (this.dinosaur.isBusy() || this.dinosaur.getAgePercentage() <= 75) {
            return false;
        }

        this.target = this.dinosaur.getAttackTarget();
        if (target == null) {
            return false;
        }

        // Check line of sight and distance
        if (!this.hasLineOfSight(target)|| this.isTargetInHuntingRange(this.target)) {
            return false;
        }

        return true;
    }

    @Override
    public void startExecuting() {
        this.PlayCallSound();

        // Find nearby pack members
        this.packMembers = this.getNearbyPackMembers();

        // Set attack target for all pack members
        for (DinosaurEntity member : this.packMembers) {
            if (member != this.dinosaur && member.getAttackTarget() == null) {
                member.setAttackTarget(this.target);
            }
        }
    }
    private void PlayCallSound() {
        this.dinosaur.setAnimation(EntityAnimation.CALLING.get());

        List<Entity> entities = this.getEntitiesWithinDistance(this.dinosaur, this.dinosaur.getDinosaur().getPackCallRadius(), this.dinosaur.getDinosaur().getPackCallRadius() / 2);

        for (Entity entity : entities) {
            if (this.dinosaur.getClass().isInstance(entity)) {
                this.dinosaur.playSound(this.dinosaur.getSoundForAnimation(EntityAnimation.CALLING.get()), this.dinosaur.getSoundVolume() > 0.0F ? this.dinosaur.getSoundVolume() + 1.25F : 0.0F, this.dinosaur.getSoundPitch());
            }
        }
    }

    public boolean isTargetInHuntingRange(EntityLivingBase target) {
        return this.getEntitiesWithinDistance(this.dinosaur, this.dinosaur.getDinosaur().getPackCallRadius() / 3, this.dinosaur.getDinosaur().getPackCallRadius() / 3).stream()
                .anyMatch(entity -> entity.getUniqueID().equals(target.getUniqueID()));
    }

    public List<DinosaurEntity> getNearbyPackMembers() {
        return this.getEntitiesWithinDistance(this.dinosaur, 8, 8).stream()
                .filter(entity -> entity instanceof DinosaurEntity)
                .map(entity -> (DinosaurEntity) entity)
                .filter(member -> member.getClass().equals(this.dinosaur.getClass()))
                .filter(member -> member.getAgePercentage() > 75)
                .collect(Collectors.toList());
    }

    public List<Entity> getEntitiesWithinDistance(Entity entity, double width, double height) {
        return entity.world.getEntitiesWithinAABBExcludingEntity(entity, new AxisAlignedBB(entity.posX - width, entity.posY - height, entity.posZ - width, entity.posX + width, entity.posY + height, entity.posZ + width));
    }

    private boolean hasLineOfSight(EntityLivingBase target) {
        Vec3d start = this.dinosaur.getPositionEyes(1.0F);
        Vec3d end = target.getPositionEyes(1.0F);

        return this.dinosaur.world.rayTraceBlocks(start, end, false, true, false) == null;
    }
}
