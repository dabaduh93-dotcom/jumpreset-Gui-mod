
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
import org.lwjgl.input.Mouse;

import java.util.List;
import java.util.Random;

public class CombatModulesHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();

    // WTap
    private long wtapReleaseAt = -1L;
    private long wtapRestoreAt = -1L;
    private boolean wtapIsReleasing = false;

    // BlockHit
    private long blockUntil = -1L;
    private int predictCooldown = 0;
    private boolean blocking = false;

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.entityPlayer != mc.thePlayer) return;

        EntityPlayerSP player = mc.thePlayer;

        // WTap
        if (CombatModules.wtapEnabled) {
            double distance = player.getDistanceToEntity(event.target);

            if (distance <= 3.0D + CombatModules.activeDistance) {
                wtapReleaseAt =
                    System.currentTimeMillis() + CombatModules.wtapDelay;

                wtapRestoreAt = -1L;
            }
        }

        // BlockHit AUTO
        if (CombatModules.blockHitEnabled
                && CombatModules.blockHitMode
                == CombatModules.BlockHitMode.AUTO) {
            startBlocking(player);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayerSP player = mc.thePlayer;

        if (player == null || mc.theWorld == null) {
            releaseEverything();
            return;
        }

        updateWTap(player);
        updateBlockHit(player);
    }

    // ---------------- WTap ----------------

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

            if (isPhysicalKeyDown(key)
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
        if (!wtapIsReleasing) return;

        int key = mc.gameSettings.keyBindForward.getKeyCode();

        KeyBinding.setKeyBindState(
            key,
            isPhysicalKeyDown(key)
        );

        wtapIsReleasing = false;
        wtapRestoreAt = -1L;
    }

    // ---------------- BlockHit ----------------

    private void updateBlockHit(EntityPlayerSP player) {
        if (!CombatModules.blockHitEnabled || !canBlock(player)) {
            stopBlocking();
            return;
        }

        long now = System.currentTimeMillis();

        if (blocking && now >= blockUntil) {
            stopBlocking();
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
        return player.getHeldItem() != null
            && player.getHeldItem().getItem() instanceof ItemSword;
    }

    private void startBlocking(EntityPlayerSP player) {
        if (!canBlock(player)) return;

        int key = mc.gameSettings.keyBindUseItem.getKeyCode();

        if (!blocking) {
            KeyBinding.setKeyBindState(key, true);
            blocking = true;
        }

        blockUntil = System.currentTimeMillis() + getBlockDuration();
    }

    private void stopBlocking() {
        if (!blocking) return;

        int key = mc.gameSettings.keyBindUseItem.getKeyCode();

        KeyBinding.setKeyBindState(
            key,
            isPhysicalKeyDown(key)
        );

        blocking = false;
        blockUntil = -1L;
    }

    private long getBlockDuration() {
        double min = Math.max(1.0D, CombatModules.minCps);
        double max = Math.max(min, CombatModules.maxCps);

        double cps = min + (random.nextDouble() * (max - min));
        long duration = (long) (1000.0D / cps);

        switch (CombatModules.randomization) {
            case LOW:
                duration += random.nextInt(3) - 1;
                break;

            case MEDIUM:
                duration += random.nextInt(7) - 3;
                break;

            case EXTRA:
                duration += random.nextInt(13) - 6;
                break;

            default:
                break;
        }

        if (CombatModules.jitter) {
            duration += random.nextInt(5) - 2;
        }

        return Math.max(1L, duration);
    }

    // ---------------- Key handling ----------------

    private boolean isPhysicalKeyDown(int key) {
        if (key >= 0) {
            return Keyboard.isKeyDown(key);
        }

        return Mouse.isButtonDown(key + 100);
    }

    private void releaseEverything() {
        restoreWTap();
        stopBlocking();

        wtapReleaseAt = -1L;
        wtapRestoreAt = -1L;
        predictCooldown = 0;
    }
}
