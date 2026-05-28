package com.aetherteam.aether.event.listeners.abilities;

import com.aetherteam.aether.Aether;
import com.aetherteam.aether.attachment.AetherDataAttachments;
import com.aetherteam.aether.event.hooks.AbilityHooks;
import com.aetherteam.aether.item.EquipmentUtil;
import com.aetherteam.aether.item.combat.abilities.armor.GravititeArmor;
import com.aetherteam.aether.item.combat.abilities.armor.NeptuneArmor;
import com.aetherteam.aether.item.combat.abilities.armor.PhoenixArmor;
import com.aetherteam.aether.item.combat.abilities.armor.ValkyrieArmor;
import com.aetherteam.nitrogen.fabric.events.FallHelper;
import com.aetherteam.nitrogen.fabric.events.LivingEntityEvents;
import com.aetherteam.nitrogen.fabric.events.PlayerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ArmorAbilityListener {
    /**
     * @see Aether#eventSetup()
     */
    public static void listen() {
        PlayerTickEvents.AFTER.register(ArmorAbilityListener::onPlayerUpdate);
        LivingEntityEvents.ON_JUMP.register(ArmorAbilityListener::onEntityJump);
        LivingEntityEvents.ON_FALL.register(ArmorAbilityListener::onEntityFall);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> ArmorAbilityListener.onEntityAttack(entity, source));
    }

    /**
     * @see ValkyrieArmor#handleFlight(LivingEntity)
     * @see NeptuneArmor#boostWaterSwimming(LivingEntity)
     * @see PhoenixArmor#boostLavaSwimming(LivingEntity)
     * @see PhoenixArmor#damageArmor(LivingEntity)
     */
    public static void onPlayerUpdate(Player player) {
        if (!EquipmentUtil.mayHaveAbilityArmor(player)
                && player.getAttachedOrCreate(AetherDataAttachments.AETHER_PLAYER).getNeptuneSubmergeLength() <= 0.0) {
            return;
        }
        if (EquipmentUtil.wearsValkyrieArmorPiece(player)) {
            ValkyrieArmor.handleFlight(player);
        }
        if (EquipmentUtil.wearsNeptuneArmorPiece(player) || player.getAttachedOrCreate(AetherDataAttachments.AETHER_PLAYER).getNeptuneSubmergeLength() > 0.0) {
            NeptuneArmor.boostWaterSwimming(player);
        }
        if (EquipmentUtil.wearsPhoenixArmorPiece(player)) {
            PhoenixArmor.boostLavaSwimming(player);
            PhoenixArmor.damageArmor(player);
        }
    }

    /**
     * @see GravititeArmor#boostedJump(LivingEntity)
     */
    public static void onEntityJump(LivingEntity livingEntity) {
        GravititeArmor.boostedJump(livingEntity);
    }

    /**
     * @see AbilityHooks.ArmorHooks#fallCancellation(LivingEntity)
     */
    public static void onEntityFall(LivingEntity entity, FallHelper event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(entity instanceof Player) && !EquipmentUtil.hasSentryBoots(entity)) {
            return;
        }
        event.setCanceled(AbilityHooks.ArmorHooks.fallCancellation(entity));
    }

    /**
     * @see PhoenixArmor#extinguishUser(LivingEntity, DamageSource)
     */
    public static boolean onEntityAttack(LivingEntity livingEntity, DamageSource damageSource) {
        if (!damageSource.is(DamageTypeTags.IS_FIRE) || !EquipmentUtil.wearsPhoenixArmorPiece(livingEntity)) {
            return true;
        }
        return !PhoenixArmor.extinguishUser(livingEntity, damageSource);
    }
}
