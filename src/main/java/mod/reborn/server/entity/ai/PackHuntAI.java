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
import net.minecraft.util.math.BlockPos;
import java.util.Comparator;

import java.util.List;


public class PackHuntAI<T extends DinosaurEntity> extends EntityAIBase {
    private enum HuntState {
        IDLE,
        STALKING,
        COORDINATING,
        ATTACKING
    }

    private HuntState currentState = HuntState.IDLE;
    private final DinosaurEntity dinosaur;
    private EntityLivingBase target;
    private List<DinosaurEntity> packMembers;
    private static final int HUNT_RANGE = 15;
    private static final int COORDINATION_RANGE = 20;
    private static final int MIN_PACK_SIZE = 2;

    private long coordinationStartTime = 0L;

    public PackHuntAI(T dinosaur) {
        this.dinosaur = dinosaur;
        this.setMutexBits(Mutex.ATTACK | Mutex.MOVEMENT);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.dinosaur.getMetabolism().isHungry()) {
            this.currentState = HuntState.IDLE;
            return false;
        }

        this.target = this.dinosaur.getAttackTarget();
        if (this.target == null) {
            // Check for potential prey in range
            this.target = this.findPotentialPrey();
            if (this.target == null) {
                this.currentState = HuntState.IDLE;
                return false;
            }
        }

        return this.canSeeTarget() &&
                this.isWithinHuntRange() &&
                this.dinosaur.getAgePercentage() > 75;
    }

    private EntityLivingBase findPotentialPrey() {
        return this.dinosaur.world.getEntitiesWithinAABB(
                        EntityLivingBase.class,
                        new AxisAlignedBB(
                                this.dinosaur.posX - COORDINATION_RANGE,
                                this.dinosaur.posY - COORDINATION_RANGE,
                                this.dinosaur.posZ - COORDINATION_RANGE,
                                this.dinosaur.posX + COORDINATION_RANGE,
                                this.dinosaur.posY + COORDINATION_RANGE,
                                this.dinosaur.posZ + COORDINATION_RANGE
                        ),
                        entity -> entity.isEntityAlive() &&
                                !entity.isEntityEqual(this.dinosaur) &&
                                this.hasLineOfSight(entity)
                ).stream()
                .min(Comparator.comparingDouble(entity ->
                        entity.getDistance(this.dinosaur)))
                .orElse(null);
    }

    private boolean canSeeTarget() {
        Vec3d start = this.dinosaur.getPositionEyes(1.0F);
        Vec3d end = this.target.getPositionEyes(1.0F);
        return this.dinosaur.world.rayTraceBlocks(start, end, false, true, false) == null;
    }

    private boolean isWithinHuntRange() {
        return this.dinosaur.getDistance(this.target) <= HUNT_RANGE;
    }

    private void playHuntCall() {
        this.dinosaur.setAnimation(EntityAnimation.CALLING.get());

        // Play sound for nearby dinosaurs
        List<Entity> entities = this.dinosaur.world.getEntitiesWithinAABBExcludingEntity(
                this.dinosaur,
                new AxisAlignedBB(
                        this.dinosaur.posX - COORDINATION_RANGE,
                        this.dinosaur.posY - COORDINATION_RANGE,
                        this.dinosaur.posZ - COORDINATION_RANGE,
                        this.dinosaur.posX + COORDINATION_RANGE,
                        this.dinosaur.posY + COORDINATION_RANGE,
                        this.dinosaur.posZ + COORDINATION_RANGE
                )
        );

        for (Entity entity : entities) {
            if (entity instanceof DinosaurEntity) {
                DinosaurEntity otherDino = (DinosaurEntity) entity;
                if (otherDino != this.dinosaur) {
                    otherDino.hearHuntCall(this.dinosaur);
                }
            }
            if (this.dinosaur.getClass().isInstance(entity)) {
                this.dinosaur.playSound(this.dinosaur.getSoundForAnimation(EntityAnimation.CALLING.get()), this.dinosaur.getSoundVolume() > 0.0F ? this.dinosaur.getSoundVolume() + 1.25F : 0.0F, this.dinosaur.getSoundPitch());
            }
        }
    }

    @Override
    public void startExecuting() {
        this.currentState = HuntState.STALKING;
        this.playHuntCall();
        this.packMembers = this.getNearbyPackMembers();

        // Coordinate with pack members
        for (DinosaurEntity member : this.packMembers) {
            if (member != this.dinosaur && member.getAttackTarget() == null) {
                member.setAttackTarget(this.target);
                member.getNavigator().tryMoveToEntityLiving(this.target, 1.0D);
            }
        }
        this.dinosaur.getNavigator().tryMoveToEntityLiving(this.target, 1.0D);
    }

    @Override
    public void updateTask() {
        System.out.println("[" + this.dinosaur.getName() + "] Current state: " + this.currentState);
        switch (this.currentState) {
            case STALKING:
                if (this.isWithinHuntRange()) {
                    this.currentState = HuntState.COORDINATING;
                    this.coordinatePackAttack();
                }
                break;

            case COORDINATING:
                if (this.isPackReadyToAttack()) {
                    this.currentState = HuntState.ATTACKING;
                    this.executePackAttack();
                }
                break;

            case ATTACKING:
                if (!this.target.isEntityAlive()) {
                    this.currentState = HuntState.IDLE;
                    this.resetTask();
                }
                break;
        }
    }

    private void coordinatePackAttack() {
        // Position pack members around target
        this.packMembers.forEach(member -> {
            if (member != this.dinosaur) {
                double angle = (Math.PI * 2 *
                        this.packMembers.indexOf(member)) / this.packMembers.size();
                double offsetX = Math.cos(angle) * HUNT_RANGE;
                double offsetZ = Math.sin(angle) * HUNT_RANGE;

                // Calculate target coordinates
                double targetX = this.target.posX + offsetX;
                double targetY = this.target.posY;
                double targetZ = this.target.posZ + offsetZ;

                // Move to the calculated position
                member.getNavigator().tryMoveToXYZ(
                        targetX,
                        targetY,
                        targetZ,
                        1.0D
                );
            }
        });
    }


    private boolean isPackReadyToAttack() {
        // Check if we've exceeded the timeout
        if (coordinationStartTime == 0L) {
            coordinationStartTime = System.currentTimeMillis();
        }

        if (System.currentTimeMillis() - coordinationStartTime >= 1500L) {
            return true;
        }

        return this.packMembers.size() >= MIN_PACK_SIZE &&
                this.packMembers.stream()
                        .allMatch(member -> {
                            boolean inPosition = member.getDistance(this.target) <= HUNT_RANGE;
                            if (!inPosition) {
                                // Try to move to position if not already there
                                double angle = (Math.PI * 2 *
                                        this.packMembers.indexOf(member)) / this.packMembers.size();
                                double offsetX = Math.cos(angle) * HUNT_RANGE;
                                double offsetZ = Math.sin(angle) * HUNT_RANGE;
                                double targetX = this.target.posX + offsetX;
                                double targetY = this.target.posY;
                                double targetZ = this.target.posZ + offsetZ;
                                member.getNavigator().tryMoveToXYZ(targetX, targetY, targetZ, 1.0D);
                            }
                            return inPosition;
                        });
    }

    private void executePackAttack() {
        this.packMembers.forEach(member -> {
            if (member.getDistance(this.target) <= HUNT_RANGE) {
                member.attackEntityAsMob(this.target);
            }
        });
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
