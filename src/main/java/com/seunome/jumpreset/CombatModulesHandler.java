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
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.Deque;

public class CombatModulesHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();

    // WTap
    private long wtapReleaseAt = -1L;
    private long wtapRestoreAt = -1L;
    private boolean wtapIsReleasing = false;

    // BlockHit
    private final Deque<Long> pendingBlockTimes = new ArrayDeque<Long>();
    private final Set<Integer> activePredictedSwings = new HashSet<Integer>();
    private long blockUntil = -1L;
    private long nextPulseAllowedAt = -1L;
    private boolean blocking = false;
    private int blockingKeyCode = Integer.MIN_VALUE;

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
            // Auto nao acumula ataques enquanto um pulso esta ativo ou aguardando.
            // Isso evita uma fila de bloqueios atrasados que parece manter o botao preso.
            if (!blocking && pendingBlockTimes.isEmpty()) {
                scheduleBlockPulse(player);
            }
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
            pendingBlockTimes.clear();
            activePredictedSwings.clear();
            stopBlocking();
            return;
        }

        long now = System.currentTimeMillis();

        if (blocking && now >= blockUntil) {
            stopBlocking();
        }

        // Cada evento de ataque/swing gera um unico pulso. Se outro pulso
        // estiver em andamento, o proximo fica na fila em vez de ser perdido.
        if (!blocking && !pendingBlockTimes.isEmpty()
                && now >= pendingBlockTimes.peekFirst()) {
            pendingBlockTimes.removeFirst();
            startBlocking();
        }

        if (CombatModules.blockHitMode == CombatModules.BlockHitMode.PREDICT
                && detectNewIncomingSwing(player)) {
            scheduleBlockPulse(player);
        }
    }

    private void scheduleBlockPulse(EntityPlayerSP player) {
        if (!canBlock(player)) return;

        long now = System.currentTimeMillis();

        double minCps = Math.max(1.0D, Math.min(30.0D, CombatModules.minCps));
        double maxCps = Math.max(minCps, Math.min(30.0D, CombatModules.maxCps));
        double cps = minCps + random.nextDouble() * (maxCps - minCps);

        long interval = Math.max(1L, Math.round(1000.0D / cps));
        long scheduledAt = Math.max(now, nextPulseAllowedAt);
        nextPulseAllowedAt = scheduledAt + interval;

        int minDelay = clamp(CombatModules.minDelayMs, 0, 500);
        int maxDelay = clamp(CombatModules.maxDelayMs, minDelay, 500);
        long delay = minDelay;
        if (maxDelay > minDelay) {
            delay += random.nextInt(maxDelay - minDelay + 1);
        }

        delay += getRandomOffset();
        pendingBlockTimes.addLast(scheduledAt + Math.max(0L, delay));
    }

    // Detecta o inicio de cada animacao de ataque inimiga apenas uma vez.
    // A previsao usa distancia e direcao do olhar; nao consegue confirmar
    // que o servidor realmente aceitara o golpe.
    private boolean detectNewIncomingSwing(EntityPlayerSP player) {
        List<Entity> entities =
                mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                        player,
                        player.getEntityBoundingBox().expand(4.0D, 3.0D, 4.0D)
                );

        Set<Integer> swingingNow = new HashSet<Integer>();
        boolean detected = false;

        for (Entity entity : entities) {
            if (!(entity instanceof EntityPlayer) || entity == player) continue;

            EntityPlayer enemy = (EntityPlayer) entity;
            if (enemy.isDead || !enemy.isSwingInProgress) continue;

            int entityId = enemy.getEntityId();
            swingingNow.add(entityId);
            if (activePredictedSwings.contains(entityId)) continue;
            activePredictedSwings.add(entityId);

            if (player.getDistanceToEntity(enemy) > 4.0D) continue;

            Vec3 direction = new Vec3(
                    player.posX - enemy.posX,
                    player.posY + player.getEyeHeight()
                            - enemy.posY - enemy.getEyeHeight(),
                    player.posZ - enemy.posZ
            ).normalize();

            if (enemy.getLookVec().dotProduct(direction) > 0.55D) {
                detected = true;
            }
        }

        activePredictedSwings.retainAll(swingingNow);
        return detected;
    }

    private boolean canBlock(EntityPlayerSP player) {
        if (player == null || player.getHeldItem() == null) return false;

        // Com Limit Items ligado, so permite espada. Desligado, deixa o
        // clique direito passar para o item equipado (comportamento vanilla).
        return !CombatModules.limitItems
                || player.getHeldItem().getItem() instanceof ItemSword;
    }

    private void startBlocking() {
        if (mc.thePlayer == null || !canBlock(mc.thePlayer)) return;

        int key = mc.gameSettings.keyBindUseItem.getKeyCode();
        if (!blocking) {
            blockingKeyCode = key;
            KeyBinding.setKeyBindState(blockingKeyCode, true);
            blocking = true;
        }

        int duration = clamp(CombatModules.blockDurationMs, 1, 500);
        duration += getRandomOffset();
        if (CombatModules.jitter) {
            duration += random.nextInt(5) - 2;
        }

        // Um pulso precisa durar pelo menos um tick do cliente para ser enviado.
        blockUntil = System.currentTimeMillis() + Math.max(50L, duration);
    }

    private void stopBlocking() {
        if (!blocking) return;

        // Restore the exact key that was pressed, even if the user changed
        // the key binding while the temporary block was active.
        if (mc.gameSettings != null && blockingKeyCode != Integer.MIN_VALUE) {
            KeyBinding.setKeyBindState(
                    blockingKeyCode,
                    isPhysicalKeyDown(blockingKeyCode)
            );
        }

        blocking = false;
        blockingKeyCode = Integer.MIN_VALUE;
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
        pendingBlockTimes.clear();
        activePredictedSwings.clear();
        blockUntil = -1L;
        nextPulseAllowedAt = -1L;
    }
}
