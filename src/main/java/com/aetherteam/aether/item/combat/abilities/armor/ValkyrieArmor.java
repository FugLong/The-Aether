package com.aetherteam.aether.item.combat.abilities.armor;

import com.aetherteam.aether.attachment.AetherDataAttachments;
import com.aetherteam.aether.item.EquipmentUtil;
import com.aetherteam.aether.mixin.mixins.common.accessor.ServerGamePacketListenerImplAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public interface ValkyrieArmor {
    /**
     * Allows an entity temporary upwards flight if wearing a full set of Valkyrie Armor. This only works for players.
     *
     * @param entity The {@link LivingEntity} wearing the armor.
     * @see com.aetherteam.aether.event.listeners.abilities.ArmorAbilityListener#onEntityUpdate(PlayerTickEvent.Post)
     */
    static void handleFlight(LivingEntity entity) {
        if (!(entity instanceof Player player) || player.getAbilities().flying || !EquipmentUtil.hasFullValkyrieSet(entity)) {
            return;
        }
        var data = player.getAttachedOrCreate(AetherDataAttachments.AETHER_PLAYER);
        Vec3 deltaMovement = player.getDeltaMovement();
        if (data.isJumping() && !onGround(player)) {
            if (data.getFlightModifier() >= data.getFlightModifierMax()) {
                data.setFlightModifier(data.getFlightModifierMax());
            }
            if (data.getFlightTimer() > 2) {
                if (data.getFlightTimer() < data.getFlightTimerMax()) {
                    data.setFlightModifier(data.getFlightModifier() + 0.25F);
                    data.setFlightTimer(data.getFlightTimer() + 1);
                }
            } else {
                data.setFlightTimer(data.getFlightTimer() + 1);
            }
        } else if (!data.isJumping()) {
            data.setFlightModifier(1.0F);
        }
        if (onGround(player)) {
            data.setFlightTimer(0);
            data.setFlightModifier(1.0F);
        }
        if (data.isJumping() && !onGround(player) && data.getFlightTimer() > 2 && data.getFlightTimer() < data.getFlightTimerMax() && data.getFlightModifier() > 1.0F) {
            player.setDeltaMovement(deltaMovement.x(), 0.025F * data.getFlightModifier(), deltaMovement.z());
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ServerGamePacketListenerImplAccessor serverGamePacketListenerImplAccessor = (ServerGamePacketListenerImplAccessor) serverPlayer.connection;
            serverGamePacketListenerImplAccessor.aether$setAboveGroundTickCount(0);
        }
    }

    private static boolean onGround(Player player) {
        return player.onGround() || player.isInFluidType();
    }
}
