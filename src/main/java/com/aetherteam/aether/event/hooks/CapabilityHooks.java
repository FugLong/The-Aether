package com.aetherteam.aether.event.hooks;

import com.aetherteam.aether.attachment.AetherDataAttachments;
import com.aetherteam.aether.attachment.AetherPlayerAttachment;
import com.aetherteam.aether.data.resources.registries.AetherDimensions;
import com.aetherteam.nitrogen.attachment.INBTSynchable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class CapabilityHooks {
    public static class AetherPlayerHooks {
        /**
         * @see AetherPlayerAttachment#onLogin(Player)
         * @see com.aetherteam.aether.event.listeners.capability.AetherPlayerListener#onPlayerLogin(Player)
         */
        public static void login(Player player) {
            player.getAttachedOrCreate(AetherDataAttachments.AETHER_PLAYER).onLogin(player);
        }

        /**
         * @see AetherPlayerAttachment#onLogout(Player)
         * @see com.aetherteam.aether.event.listeners.capability.AetherPlayerListener#onPlayerLogout(Player)
         */
        public static void logout(Player player) {
            player.getAttachedOrCreate(AetherDataAttachments.AETHER_PLAYER).onLogout(player);
        }

        /**
         * @see AetherPlayerAttachment#onJoinLevel(Player)
         * @see com.aetherteam.aether.event.listeners.capability.AetherPlayerListener#onPlayerJoinLevel(Entity)
         */
        public static void joinLevel(Player player) {
            player.getAttachedOrCreate(AetherDataAttachments.AETHER_PLAYER).onJoinLevel(player);
        }

        /**
         * @see AetherPlayerAttachment#onUpdate(Player)
         * @see com.aetherteam.aether.event.listeners.capability.AetherPlayerListener#onPlayerUpdate(Entity)
         */
        public static void update(Player player) {
            player.getAttachedOrCreate(AetherDataAttachments.AETHER_PLAYER).onUpdate(player);
        }

        /**
         * @see AetherPlayerAttachment#handleRespawn(boolean)
         * @see com.aetherteam.aether.event.listeners.capability.AetherPlayerListener#onPlayerClone(Player, boolean)
         */
        public static void clone(Player player, boolean wasDeath) {
            player.getAttachedOrCreate(AetherDataAttachments.AETHER_PLAYER).handleRespawn(wasDeath);
        }

        /**
         * Syncs capability data to the client when the player changes dimensions.
         *
         * @param player The {@link Player}.
         * @see com.aetherteam.aether.event.listeners.capability.AetherPlayerListener#onPlayerChangeDimension(Player)
         */
        public static void changeDimension(Player player) {
            if (!player.level().isClientSide()) {
                player.getAttachedOrCreate(AetherDataAttachments.AETHER_PLAYER).forceSync(player.getId(), INBTSynchable.Direction.CLIENT);
            }
        }
    }

    public static class AetherTimeHooks {
        /**
         * Sync the Aether's time to the player on login.
         *
         * @param player The {@link Player}.
         */
        public static void login(Player player) {
            syncAetherTime(player);
        }

        /**
         * Sync the Aether's time to the player on dimension change.
         *
         * @param player The {@link Player}.
         */
        public static void changeDimension(Player player) {
            syncAetherTime(player);
        }

        /**
         * Sync the Aether's time to the player on respawn.
         *
         * @param player The {@link Player}.
         */
        public static void respawn(Player player) {
            syncAetherTime(player);
        }

        /**
         * Sync the Aether's time to the player.
         *
         * @param player The {@link Player}.
         */
        private static void syncAetherTime(Player player) {
            if (player instanceof ServerPlayer serverPlayer) {
                if (player.level().dimensionType().effectsLocation().equals(AetherDimensions.AETHER_DIMENSION_TYPE.location())) {
                    player.level().getAttachedOrCreate(AetherDataAttachments.AETHER_TIME).updateEternalDay(serverPlayer);
                }
            }
        }
    }
}
