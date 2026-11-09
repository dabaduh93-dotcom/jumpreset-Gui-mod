
package com.seunome.jumpreset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.util.Vec3;

import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Random;

public class CombatModulesHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();

    private int wtapTicks;
    private int postTicks;
    private boolean wtapReleased;
    private boolean restoreForward;

    private int blockTicks;
    private int predictCooldown;
    private boolean blocking;
    private boolean restoreUse;

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.entityPlayer != mc.thePlayer) return;

        EntityPlayerSP player = mc.thePlayer;

        // WTap
        if (CombatModules.wtapEnabled) {
            double distance = player.getDistanceToEntity(event.target);

            if (distance <= 3.0D + CombatModules.activeDistance) {
                wtapTicks = Math.max(1, CombatModules.wtapDelay / 50);
                postTicks = Math.max(1, CombatModules.postDelay / 50);
            }
        }

        // BlockHit Auto
        if (CombatModules.blockHitEnabled
                && CombatModules.blockHitMode
                == CombatModules.BlockHitMode.AUTO) {
            startBlocking(player);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayerSP player = mc.thePlayer;

        if (player == null || mc.theWorld == null) {
            releaseEverything();
            return;
        }

        updateWTap(player);
        updateBlockHit(player);
    }

    private void updateWTap(EntityPlayerSP player) {
        if (!CombatModules.wtapEnabled) {
            restoreWTap();
            wtapTicks = 0;
            postTicks = 0;
            return;
        }

        if (wtapTicks > 0) {
            wtapTicks--;
            return;
        }

        if (postTicks > 0) {
            postTicks--;

            if (!wtapReleased
                    && player.movementInput.moveForward > 0.0F
                    && mc.gameSettings.keyBindForward.isKeyDown()) {

                restoreForward = true;

                KeyBinding.setKeyBindState(
                    mc.gameSettings.keyBindForward.getKeyCode(),
                    false
                );

                wtapReleased = true;
            } else if (wtapReleased) {
                restoreWTap();
            }
        } else {
            restoreWTap();
        }
    }

    private void restoreWTap() {
        if (!wtapReleased) return;

        if (restoreForward) {
            KeyBinding.setKeyBindState(
                mc.gameSettings.keyBindForward.getKeyCode(),
                true
            );
        }

        wtapReleased = false;
        restoreForward = false;
    }

    private void updateBlockHit(EntityPlayerSP player) {
        if (!CombatModules.blockHitEnabled || !canBlock(player)) {
            stopBlocking();
            return;
        }

        if (blockTicks > 0) {
            blockTicks--;

            if (blockTicks == 0) {
                stopBlocking();
            }
        }

        if (predictCooldown > 0) {
            predictCooldown--;
        }

        if (CombatModules.blockHitMode
                == CombatModules.BlockHitMode.PREDICT
                && predictCooldown == 0
                && shouldPredict(player)) {

            startBlocking(player);
            predictCooldown = 2;
        }
    }

    private boolean shouldPredict(EntityPlayerSP player) {
        List<Entity> entities =
            mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                player,
                player.getEntityBoundingBox().expand(4.0D, 3.0D, 4.0D)
            );

        for (Entity entity : entities) {
            if (!(entity instanceof EntityPlayer)) continue;
            if (entity == player) continue;

            EntityPlayer enemy = (EntityPlayer) entity;

            if (enemy.isDead || !enemy.isSwingInProgress) continue;

            if (player.getDistanceToEntity(enemy) > 4.0D) continue;

            Vec3 direction = new Vec3(
                player.posX - enemy.posX,
                player.posY + player.getEyeHeight()
                    - enemy.posY - enemy.getEyeHeight(),
                player.posZ - enemy.posZ
            ).normalize();

            Vec3 enemyLook = enemy.getLookVec();

            if (enemyLook.dotProduct(direction) > 0.55D) {
                return true;
            }
        }

        return false;
    }

    private boolean canBlock(EntityPlayerSP player) {
        if (player.getHeldItem() == null) return false;

        if (CombatModules.limitItems
                && !(player.getHeldItem().getItem() instanceof ItemSword)) {
            return false;
        }

        return true;
    }

    private void startBlocking(EntityPlayerSP player) {
        if (!canBlock(player) || blocking) return;

        if (CombatModules.jitter && random.nextInt(4) == 0) {
            return;
        }

        restoreUse = mc.gameSettings.keyBindUseItem.isKeyDown();

        KeyBinding.setKeyBindState(
            mc.gameSettings.keyBindUseItem.getKeyCode(),
            true
        );

        blocking = true;
        blockTicks = 1;
    }

    private void stopBlocking() {
        if (!blocking) return;

        KeyBinding.setKeyBindState(
            mc.gameSettings.keyBindUseItem.getKeyCode(),
            restoreUse
        );

        blocking = false;
        restoreUse = false;
    }

    private void releaseEverything() {
        restoreWTap();
        stopBlocking();
    }
}
