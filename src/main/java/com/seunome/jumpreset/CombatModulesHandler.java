
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

import org.lwjgl.input.Keyboard;

import java.util.List;

public class CombatModulesHandler {

    private final Minecraft mc = Minecraft.getMinecraft();

    // WTap
    private long wtapReleaseAt = -1L;
    private long wtapRestoreAt = -1L;
    private boolean wtapIsReleasing = false;
    private boolean forwardWasPressed = false;

    // BlockHit
    private int blockTicks = 0;
    private int predictCooldown = 0;
    private boolean blocking = false;
    private boolean useWasPressed = false;

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (event.entityPlayer != mc.thePlayer) {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        if (CombatModules.wtapEnabled) {
            double distance = player.getDistanceToEntity(event.target);

            if (distance <= 3.0D + CombatModules.activeDistance) {
                wtapReleaseAt = System.currentTimeMillis()
                        + CombatModules.wtapDelay;
                wtapRestoreAt = -1L;
            }
        }

        if (CombatModules.blockHitEnabled
                && CombatModules.blockHitMode
                == CombatModules.BlockHitMode.AUTO) {
            startBlocking(player);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

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
            wtapReleaseAt = -1L;
            wtapRestoreAt = -1L;
            return;
        }

        long now = System.currentTimeMillis();

        if (wtapReleaseAt >= 0L && now >= wtapReleaseAt) {
            int key = mc.gameSettings.keyBindForward.getKeyCode();

            forwardWasPressed = key >= 0 && Keyboard.isKeyDown(key);

            if (forwardWasPressed
                    && player.movementInput.moveForward > 0.0F) {
                KeyBinding.setKeyBindState(key, false);
                wtapIsReleasing = true;
                wtapRestoreAt = now + CombatModules.postDelay;
            }

            wtapReleaseAt = -1L;
        }

        if (wtapIsReleasing && now >= wtapRestoreAt) {
            restoreWTap();
        }
    }

    private void restoreWTap() {
        if (!wtapIsReleasing) {
            return;
        }

        int key = mc.gameSettings.keyBindForward.getKeyCode();

        boolean stillPressed = key >= 0 && Keyboard.isKeyDown(key);

        KeyBinding.setKeyBindState(
                key,
                forwardWasPressed && stillPressed
        );

        wtapIsReleasing = false;
        forwardWasPressed = false;
        wtapRestoreAt = -1L;
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
                        player.getEntityBoundingBox().expand(
                                4.0D, 3.0D, 4.0D
                        )
                );

        for (Entity entity : entities) {
            if (!(entity instanceof EntityPlayer)
                    || entity == player) {
                continue;
            }

            EntityPlayer enemy = (EntityPlayer) entity;

            if (enemy.isDead || !enemy.isSwingInProgress) {
                continue;
            }

            if (player.getDistanceToEntity(enemy) > 4.0D) {
                continue;
            }

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
        return player.getHeldItem() != null
                && player.getHeldItem().getItem() instanceof ItemSword;
    }

    private void startBlocking(EntityPlayerSP player) {
        if (blocking || !canBlock(player)) {
            return;
        }

        int key = mc.gameSettings.keyBindUseItem.getKeyCode();

        useWasPressed = key >= 0 && Keyboard.isKeyDown(key);

        KeyBinding.setKeyBindState(key, true);

        blocking = true;
        blockTicks = 1;
    }

    private void stopBlocking() {
        if (!blocking) {
            return;
        }

        int key = mc.gameSettings.keyBindUseItem.getKeyCode();

        boolean stillPressed = key >= 0 && Keyboard.isKeyDown(key);

        KeyBinding.setKeyBindState(
                key,
                useWasPressed && stillPressed
        );

        blocking = false;
        useWasPressed = false;
        blockTicks = 0;
    }

    private void releaseEverything() {
        restoreWTap();
        stopBlocking();
        wtapReleaseAt = -1L;
        wtapRestoreAt = -1L;
    }
}
