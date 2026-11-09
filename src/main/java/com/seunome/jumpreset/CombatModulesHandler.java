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
    private long pendingBlockAt = -1L;
    private long blockUntil = -1L;
    private long nextPulseAllowedAt = -1L;
    private int predictCooldown = 0;
    private boolean blocking = false;

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.entityPlayer != mc.thePlayer) return;

        EntityPlayerSP player = mc.thePlayer;

        if (CombatModules.wtapEnabled) {
            double distance = player.getDistanceToEntity(event.target);
            if (distance <= 3.0D + CombatModules.activeDistance) {
                wtapReleaseAt = System.currentTimeMillis()
                        + Math.max(0, CombatModules.wtapDelay);
                wtapRestoreAt = -1L;
            }
        }

        if (CombatModules.blockHitEnabled
                && CombatModules.blockHitMode == CombatModules.BlockHitMode.AUTO) {
            scheduleBlockPulse(player);
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
                wtapRestoreAt = now + Math.max(0, CombatModules.postDelay);
            }

            wtapReleaseAt = -1L;
        }

        if (wtapIsReleasing && now >= wtapRestoreAt) {
            restoreWTap();
        }
    }

    private void restoreWTap() {
        if (!wtapIsReleasing || mc.gameSettings == null) return;

        int key = mc.gameSettings.keyBindForward.getKeyCode();
        KeyBinding.setKeyBindState(key, isPhysicalKeyDown(key));

        wtapIsReleasing = false;
        wtapRestoreAt = -1L;
    }

    // ---------------- BlockHit ----------------

    private void updateBlockHit(EntityPlayerSP player) {
        if (!CombatModules.blockHitEnabled || !canBlock(player)) {
            pendingBlockAt = -1L;
            stopBlocking();
            return;
        }

        long now = System.currentTimeMillis();

        if (pendingBlockAt >= 0L && now >= pendingBlockAt) {
            pendingBlockAt = -1L;
            startBlocking();
        }

        if (blocking && now >= blockUntil) {
            stopBlocking();
        }

        if (predictCooldown > 0) {
            predictCooldown--;
        }

        if (CombatModules.blockHitMode == CombatModules.BlockHitMode.PREDICT
                && predictCooldown == 0
                && !blocking
                && pendingBlockAt < 0L
                && shouldPredict(player)) {
            scheduleBlockPulse(player);
            predictCooldown = 2;
        }
    }

    private void scheduleBlockPulse(EntityPlayerSP player) {
        if (!canBlock(player) || blocking || pendingBlockAt >= 0L) return;

        long now = System.currentTimeMillis();
        if (now < nextPulseAllowedAt) return;

        double minCps = Math.max(1.0D, Math.min(30.0D, CombatModules.minCps));
        double maxCps = Math.max(minCps, Math.min(30.0D, CombatModules.maxCps));
        double cps = minCps + random.nextDouble() * (maxCps - minCps);

        long interval = Math.max(1L, Math.round(1000.0D / cps));
        nextPulseAllowedAt = now + interval;

        int minDelay = clamp(CombatModules.minDelayMs, 0, 500);
        int maxDelay = clamp(CombatModules.maxDelayMs, minDelay, 500);
        long delay = minDelay;
        if (maxDelay > minDelay) {
            delay += random.nextInt(maxDelay - minDelay + 1);
        }

        delay += getRandomOffset();
        pendingBlockAt = now + Math.max(0L, delay);
    }

    private boolean shouldPredict(EntityPlayerSP player) {
        List<Entity> entities =
                mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                        player,
                        player.getEntityBoundingBox().expand(4.0D, 3.0D, 4.0D)
                );

        for (Entity entity : entities) {
            if (!(entity instanceof EntityPlayer) || entity == player) continue;

            EntityPlayer enemy = (EntityPlayer) entity;
            if (enemy.isDead || !enemy.isSwingInProgress) continue;
            if (player.getDistanceToEntity(enemy) > 4.0D) continue;

            Vec3 direction = new Vec3(
                    player.posX - enemy.posX,
                    player.posY + player.getEyeHeight()
                            - enemy.posY - enemy.getEyeHeight(),
                    player.posZ - enemy.posZ
            ).normalize();

            if (enemy.getLookVec().dotProduct(direction) > 0.55D) {
                return true;
            }
        }
        return false;
    }

    private boolean canBlock(EntityPlayerSP player) {
        return player != null
                && player.getHeldItem() != null
                && player.getHeldItem().getItem() instanceof ItemSword;
    }

    private void startBlocking() {
        if (mc.thePlayer == null || !canBlock(mc.thePlayer)) return;

        int key = mc.gameSettings.keyBindUseItem.getKeyCode();
        if (!blocking) {
            KeyBinding.setKeyBindState(key, true);
            blocking = true;
        }

        int duration = clamp(CombatModules.blockDurationMs, 1, 500);
        duration += getRandomOffset();
        if (CombatModules.jitter) {
            duration += random.nextInt(5) - 2;
        }

        blockUntil = System.currentTimeMillis() + Math.max(1L, duration);
    }

    private void stopBlocking() {
        if (!blocking || mc.gameSettings == null) return;

        int key = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(key, isPhysicalKeyDown(key));

        blocking = false;
        blockUntil = -1L;
    }

    private int getRandomOffset() {
        int offset = 0;
        switch (CombatModules.randomization) {
            case LOW:
                offset = random.nextInt(3) - 1;
                break;
            case MEDIUM:
                offset = random.nextInt(7) - 3;
                break;
            case EXTRA:
                offset = random.nextInt(13) - 6;
                break;
            default:
                break;
        }
        return offset;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ---------------- Key handling ----------------

    private boolean isPhysicalKeyDown(int key) {
        if (key >= 0) return Keyboard.isKeyDown(key);
        return Mouse.isButtonDown(key + 100);
    }

    private void releaseEverything() {
        restoreWTap();
        stopBlocking();

        wtapReleaseAt = -1L;
        wtapRestoreAt = -1L;
        pendingBlockAt = -1L;
        blockUntil = -1L;
        nextPulseAllowedAt = -1L;
        predictCooldown = 0;
    }
}
