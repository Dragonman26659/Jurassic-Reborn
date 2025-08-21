package mod.reborn.server.entity.ai;

import mod.reborn.server.entity.DinosaurEntity;
import mod.reborn.server.entity.ai.hearing.SoundEventInfo;
import net.minecraft.client.audio.Sound;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;

public class RespondToAttackEntityAI extends EntityAIBase {
    private DinosaurEntity dinosaur;
    private EntityLivingBase attacker;

    public RespondToAttackEntityAI(DinosaurEntity dinosaur) {
        this.dinosaur = dinosaur;
        this.setMutexBits(0);
    }

    @Override
    public boolean shouldExecute() {
        this.attacker = this.dinosaur.getAttackTarget();
        if(this.attacker != null) {
            return this.dinosaur.canEntityBeSeen(this.attacker) &&
                    !this.attacker.isDead &&
                    !(this.attacker instanceof DinosaurEntity && ((DinosaurEntity) this.attacker).isCarcass()) &&
                    !(this.attacker instanceof EntityPlayer && ((EntityPlayer) this.attacker).capabilities.isCreativeMode);
        }

        // Check for threatening sounds from nearby entities
        List<SoundEventInfo> recentSounds = this.dinosaur.listener.getRecentSounds();
        for (SoundEventInfo sound : recentSounds) {
            EntityLivingBase sender = (EntityLivingBase)sound.getSender();
            if (sender != null &&
                    this.dinosaur.canEntityBeSeen(sender) &&
                    !sender.isDead &&
                    !(sender instanceof DinosaurEntity && ((DinosaurEntity) sender).isCarcass()) &&
                    !(sender instanceof EntityPlayer && ((EntityPlayer) sender).capabilities.isCreativeMode)) {
                return true;
            }
        }

        return false;
    }
    @Override
    public void startExecuting() {
        this.dinosaur.respondToAttack(this.attacker);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return false;
    }
}