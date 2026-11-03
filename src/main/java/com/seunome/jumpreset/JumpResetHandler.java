package com.seunome.jumpreset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

@SideOnly(Side.CLIENT)
public class JumpResetHandler {

    private static final Random RANDOM = new Random();

    private static final double TARGET_RANGE = 4.5D;
    private static final double TARGET_DOT = 0.5D;   // ~60 graus do crosshair
    private static final int GIVE_UP_TICKS = 12;

    private int lastHurtTime;
    private int pendingJump = -1;
    private boolean pendingPerfect;
    private int waitTicks;
    private int jumpHoldTicks;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) {
            reset();
            lastHurtTime = 0;
            return;
        }

        EntityPlayerSP player = mc.thePlayer;

        // libera a tecla de pulo simulada (modo Legit)
        if (jumpHoldTicks > 0 && --jumpHoldTicks == 0) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
        }

        if (!JumpResetMod.enabled) {
            reset();
            lastHurtTime = player.hurtTime;
            return;
        }

        // hurtTime so sobe quando entra dano novo -> detecta o hit
        if (player.hurtTime > lastHurtTime) {
            onHit(mc, player);
        }
        lastHurtTime = player.hurtTime;

        if (pendingJump < 0) {
            return;
        }
        if (pendingJump > 0) {
            pendingJump--;
            return;
        }
        if (mc.currentScreen != null || !canJump(player)) {
            if (++waitTicks > GIVE_UP_TICKS) {
                reset();
            }
            return;
        }

        performJump(mc, player);
        reset();
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (JumpResetMod.toggleKey != null && JumpResetMod.toggleKey.isPressed()) {
            JumpResetMod.toggle();
        }
        if (JumpResetMod.guiKey != null && JumpResetMod.guiKey.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiJumpReset());
        }
    }

    private void onHit(Minecraft mc, EntityPlayerSP player) {
        // ---- Velocity ----
        if (JumpResetMod.velocityEnabled) {
            if (JumpResetMod.mode == JumpResetMod.Mode.BLATANT) {
                applyBlatantVelocity(player);
            } else {
                applyLegitVelocity(player);
            }
        }

        // ---- filtros estilo Vape ----
        if (JumpResetMod.waterCheck && (player.isInWater() || player.isInLava())) {
            return;
        }
        if (JumpResetMod.onlyWhenTargeting && !isTargeting(mc, player)) {
            return;
        }

        // ---- Chance ----
        if (RANDOM.nextInt(100) >= JumpResetMod.chance) {
            return;
        }

        // ---- Accuracy ----
        pendingPerfect = RANDOM.nextInt(100) < JumpResetMod.accuracy;

        if (pendingPerfect) {
            pendingJump = (JumpResetMod.mode == JumpResetMod.Mode.BLATANT) ? 0 : JumpResetMod.delay;
        } else {
            pendingJump = 2 + RANDOM.nextInt(4); // pula, mas atrasado: sem reducao
        }
        waitTicks = 0;
    }

    /**
     * Blatant: mantem ~40% do KB horizontal (reducao de 60%), SEM tocar no vertical.
     * Faixa sorteada por hit + miss ocasional para nao virar valor fixo.
     */
    private void applyBlatantVelocity(EntityPlayerSP player) {
        if (RANDOM.nextInt(100) < JumpResetMod.velocityMissChance) {
            return; // KB integral: parece jump reset que nao saiu
        }

        double base = JumpResetMod.blatantKeep / 100.0D;              // 0.40
        double factor = base + (RANDOM.nextDouble() * 2 - 1) * 0.03D; // 0.37 .. 0.43

        double angle = Math.toRadians((RANDOM.nextDouble() * 2.0D) - 1.0D); // -1..+1 grau
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double mx = player.motionX * factor;
        double mz = player.motionZ * factor;

        player.motionX = mx * cos - mz * sin;
        player.motionZ = mx * sin + mz * cos;
        // motionY intocado de proposito
    }

    /** Semi/Legit: reducao suave e disfarcada (usa o fator do modo + randomizacao). */
    private void applyLegitVelocity(EntityPlayerSP player) {
        if (RANDOM.nextInt(100) < JumpResetMod.velocityMissChance) {
            return;
        }

        double factor;
        if (JumpResetMod.velocityRandomize) {
            double range = JumpResetMod.mode.randomizeRange;
            factor = JumpResetMod.mode.velocityFactor + (RANDOM.nextDouble() * 2 - 1) * range;
        } else {
            factor = JumpResetMod.mode.velocityFactor;
        }

        double mx = player.motionX * factor;
        double mz = player.motionZ * factor;

        if (JumpResetMod.velocityRandomize) {
            double angle = Math.toRadians((RANDOM.nextDouble() * 4.0D) - 2.0D); // -2..+2 graus
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double nx = mx * cos - mz * sin;
            double nz = mx * sin + mz * cos;
            mx = nx;
            mz = nz;
        }

        player.motionX = mx;
        player.motionZ = mz;

        // vertical: variacao maxima de 2%, ocasional
        if (JumpResetMod.velocityRandomize && RANDOM.nextInt(4) == 0) {
            player.motionY *= 1.0D + (RANDOM.nextDouble() * 0.04D - 0.02D);
        }
    }

    private boolean canJump(EntityPlayerSP player) {
        return player.onGround
                && !player.isInWater()
                && !player.isInLava()
                && !player.isRiding()
                && !player.capabilities.isFlying
                && !player.capabilities.allowFlying;
    }

    private void performJump(Minecraft mc, EntityPlayerSP player) {
        if (JumpResetMod.mode == JumpResetMod.Mode.LEGIT) {
            // segura a tecla de pulo por 2 ticks, igual a um jogador apertando espaco
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
            jumpHoldTicks = 2;
            return;
        }

        if (!player.isSprinting()
                && player.movementInput.moveForward > 0.0F
                && player.getFoodStats().getFoodLevel() > 6) {
            player.setSprinting(true);
        }
        player.jump();
    }

    private boolean isTargeting(Minecraft mc, EntityPlayerSP player) {
        Vec3 look = player.getLookVec();
        List<Entity> entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                player, player.getEntityBoundingBox().expand(TARGET_RANGE, TARGET_RANGE, TARGET_RANGE));

        for (Entity entity : entities) {
            if (!(entity instanceof EntityPlayer) || entity == player) {
                continue;
            }
            Vec3 to = new Vec3(
                    entity.posX - player.posX,
                    (entity.posY + entity.getEyeHeight()) - (player.posY + player.getEyeHeight()),
                    entity.posZ - player.posZ).normalize();

            if (look.dotProduct(to) >= TARGET_DOT) {
                return true;
            }
        }
        return false;
    }

    private void reset() {
        pendingJump = -1;
        waitTicks = 0;
    }
              }
