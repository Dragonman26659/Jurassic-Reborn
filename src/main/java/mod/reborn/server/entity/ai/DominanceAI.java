package mod.reborn.server.entity.ai;

import mod.reborn.client.model.animation.EntityAnimation;
import mod.reborn.server.entity.DinosaurEntity;
import net.minecraft.entity.ai.EntityAIBase;

public class DominanceAI extends EntityAIBase {
    private enum ChallengeState {
        IDLE,
        CHALLENGING,
        FIGHTING,
        VICTORIOUS,
        DEFEATED
    }

    private final DinosaurEntity dinosaur;
    private ChallengeState currentState = ChallengeState.IDLE;
    private DinosaurEntity challengeTarget;
    private float initialHealth;
    private float targetInitialHealth;
    private long challengeStartTime;
    private static final float MIN_HEALTH_FOR_CHALLENGE = 0.8F;
    private static final float CHALLENGE_RANGE = 10.0F;
    private static final int CHALLENGE_DURATION = 600; // 30 seconds

    public DominanceAI(DinosaurEntity dinosaur) {
        this.dinosaur = dinosaur;
        this.setMutexBits(Mutex.ATTACK | Mutex.MOVEMENT);
    }

    @Override
    public boolean shouldExecute() {
        if (!canInitiateChallenge()) {
            this.currentState = ChallengeState.IDLE;
            return false;
        }

        // Check if we're already in a challenge
        if (this.currentState != ChallengeState.IDLE) {
            return true;
        }

        // Find the herd leader to challenge
        if (this.dinosaur.herd != null && this.dinosaur.herd.leader != null) {
            this.challengeTarget = this.dinosaur.herd.leader;
            return true;
        }

        return false;
    }

    private boolean canInitiateChallenge() {
        return this.dinosaur.getHealth() >= this.dinosaur.getMaxHealth() * MIN_HEALTH_FOR_CHALLENGE &&
                !this.dinosaur.getMetabolism().isHungry() &&
                this.dinosaur.getAgePercentage() > 75 &&
                this.dinosaur.herd != null &&
                this.dinosaur.herd.leader != null &&
                this.dinosaur.herd.leader != this.dinosaur;
    }

    @Override
    public void startExecuting() {
        if (this.currentState == ChallengeState.IDLE) {
            this.currentState = ChallengeState.CHALLENGING;
            this.challengeTarget = this.dinosaur.herd.leader;
            this.initialHealth = this.dinosaur.getHealth();
            this.targetInitialHealth = this.challengeTarget.getHealth();
            this.challengeStartTime = System.currentTimeMillis();

            // Play challenge roar
            this.dinosaur.EmmitSound(EntityAnimation.ROARING);
            this.dinosaur.setAnimation(EntityAnimation.ROARING.get());
        }
    }

    @Override
    public void updateTask() {
        if (this.currentState == ChallengeState.CHALLENGING) {
            if (this.dinosaur.getDistance(this.challengeTarget) <= CHALLENGE_RANGE) {
                this.currentState = ChallengeState.FIGHTING;
                this.challengeTarget.setAttackTarget(this.dinosaur);
                this.dinosaur.setAttackTarget(this.challengeTarget);
            }
        } else if (this.currentState == ChallengeState.FIGHTING) {
            // Check for victory conditions
            if (System.currentTimeMillis() - this.challengeStartTime >= CHALLENGE_DURATION * 1000) {
                resolveChallenge();
            } else if (this.challengeTarget.getHealth() <= this.targetInitialHealth * 0.5F) {
                this.currentState = ChallengeState.VICTORIOUS;
                resolveChallenge();
            } else if (this.dinosaur.getHealth() <= this.initialHealth * 0.5F) {
                this.currentState = ChallengeState.DEFEATED;
                resolveChallenge();
            }
        }
    }

    private void resolveChallenge() {
        if (this.currentState == ChallengeState.VICTORIOUS) {
            // Become new leader
            Herd herd = this.dinosaur.herd;
            herd.leader = this.dinosaur;

            // Play victory roar
            this.dinosaur.EmmitSound(EntityAnimation.ROARING);
            this.dinosaur.setAnimation(EntityAnimation.ROARING.get());
        } else if (this.currentState == ChallengeState.DEFEATED) {
            // Leave the herd
            if (this.dinosaur.herd != null) {
                this.dinosaur.herd.splitHerd(this.dinosaur);
            }

            // Move away from herd
            this.dinosaur.getNavigator().tryMoveToXYZ(
                    this.dinosaur.posX + (this.dinosaur.posX - this.challengeTarget.posX),
                    this.dinosaur.posY,
                    this.dinosaur.posZ + (this.dinosaur.posZ - this.challengeTarget.posZ),
                    1.0D
            );
        }

        // Clear attack targets for both dinosaurs
        this.dinosaur.setAttackTarget(null);
        if (this.challengeTarget != null) {
            this.challengeTarget.setAttackTarget(null);
        }

        this.currentState = ChallengeState.IDLE;
        this.challengeTarget = null;
    }

    @Override
    public void resetTask() {
        this.currentState = ChallengeState.IDLE;
        this.challengeTarget = null;
        this.dinosaur.setAttackTarget(null);
    }
}
