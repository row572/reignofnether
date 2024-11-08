package com.solegendary.reignofnether.mixin;


import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.buildings.monsters.SculkCatalyst;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.SculkCatalystBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SculkCatalystBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkCatalystBlockEntity.class)
public abstract class SculkCatalystBlockEntityMixin extends BlockEntity {

    private @Final SculkSpreader sculkSpreader;

    public SculkCatalystBlockEntityMixin(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    private boolean isWithinRangeOfMaxedCatalystBuilding(LivingEntity entity) {
        if (entity.level.isClientSide())
            return true;

        for (Building building : BuildingServerEvents.getBuildings()) {
            if (building instanceof SculkCatalyst sc && entity.distanceToSqr(Vec3.atCenterOf(sc.centrePos)) <
                    SculkCatalyst.ESTIMATED_RANGE * SculkCatalyst.ESTIMATED_RANGE) {
                if (sc.getUncappedNightRange() >= SculkCatalyst.nightRangeMax * 1.5f)
                    return true;
            }
        }
        return false;
    }

    @Inject(
            method = "handleGameEvent",
            at = @At("HEAD"),
            cancellable = true
    )
    private void handleGameEvent(ServerLevel pLevel, GameEvent.Message pEventMessage, CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();

        if (this.isRemoved()) {
            cir.setReturnValue(false);
        } else {
            GameEvent.Context $$2 = pEventMessage.context();
            if (pEventMessage.gameEvent() == GameEvent.ENTITY_DIE) {
                Entity var5 = $$2.sourceEntity();
                if (var5 instanceof LivingEntity) {
                    LivingEntity $$3 = (LivingEntity)var5;
                    if (isWithinRangeOfMaxedCatalystBuilding($$3)) {
                        cir.setReturnValue(false);
                        return;
                    }
                    if (!$$3.wasExperienceConsumed()) {
                        int $$4 = $$3.getExperienceReward();
                        if ($$3.shouldDropExperience() && $$4 > 0) {
                            this.sculkSpreader.addCursors(new BlockPos(pEventMessage.source().relative(Direction.UP, 0.5)), $$4);
                            LivingEntity $$5 = $$3.getLastHurtByMob();
                            if ($$5 instanceof ServerPlayer) {
                                ServerPlayer $$6 = (ServerPlayer)$$5;
                                DamageSource $$7 = $$3.getLastDamageSource() == null ? DamageSource.playerAttack($$6) : $$3.getLastDamageSource();
                                CriteriaTriggers.KILL_MOB_NEAR_SCULK_CATALYST.trigger($$6, $$2.sourceEntity(), $$7);
                            }
                        }
                        $$3.skipDropExperience();
                        SculkCatalystBlock.bloom(pLevel, this.worldPosition, this.getBlockState(), pLevel.getRandom());
                    }
                    cir.setReturnValue(true);
                    return;
                }
            }
            cir.setReturnValue(false);
        }
    }
}
