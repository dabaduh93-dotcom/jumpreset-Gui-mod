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
    private static final double TARGET_DOT = 0.5D;      // ~60 graus do crosshair
    private static final double NEARBY_RANGE = 5.0D;
    private static final int GIVE_UP_TICKS = 8;

    private int lastHurtTime;
    private int pendingJump = -1;
    private int waitTicks;
    private int jumpHoldTicks;
    private int antikbTicks;
    private boolean jumpedForHit;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START) {
            tickStart(mc);
        } else {
            tickEnd(mc);
        }
    }

    // ------------------------------------------------------------------
    // START: detecta o hit ANTES do update da entidade -> pula no tick certo
    // ------------------------------------------------------------------
    private void tickStart(Minecraft mc) {
        if (jumpHoldTicks > 0) {
            jumpHoldTicks--;
            if (jumpHoldTicks == 0) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
            }
        }

        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null) {
            releaseJumpKey(mc);
            reset();
            lastHurtTime = 0;
            return;
        }

        tickAntikb(player);

        if (!JumpResetMod.enabled) {
            releaseJumpKey(mc);
            reset();
            jumpedForHit = false;
            lastHurtTime = player.hurtTime;
            return;
        }

        if (player.hurtTime > lastHurtTime) {
            jumpedForHit = false;
            pendingJump = -1;
            waitTicks = 0;
            onHit(mc, player);
        }
        lastHurtTime = player.hurtTime;
    }

    // ------------------------------------------------------------------
    // END: apenas para os pulos que nao puderam sair no tick do hit
    // ------------------------------------------------------------------
    private void tickEnd(Minecraft mc) {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || !JumpResetMod.enabled) {
            return;
        }
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
        pendingJump = -1;
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
        // velocidade horizontal antes de mexer (para saber se foi hit de verdade)
        double rawHorizontal = Math.sqrt(player.motionX * player.motionX
                + player.motionZ * player.motionZ);

        // ---- velocity / antikb (sempre aplica, inclusive hit de mob) ----
        if (JumpResetMod.velocityEnabled) {
            if (JumpResetMod.mode == JumpResetMod.Mode.BLATANT) {
                applyBlatantVelocity(player);
            } else {
                applyLegitVelocity(player);
            }
        }

        // ---- filtros: so conta hit de jogador real ----
        if (JumpResetMod.knockbackThreshold > 0.0D && rawHorizontal < JumpResetMod.knockbackThreshold) {
            return; // queda, fogo, afogamento: sem knockback
        }
        if (JumpResetMod.nearbyPlayerCheck && !isPlayerNear(mc, player, NEARBY_RANGE)) {
            return; // nenhum jogador por perto -> nao foi hit de jogador
        }
        if (JumpResetMod.waterCheck && (player.isInWater() || player.isInLava())) {
            return;
        }
        if (JumpResetMod.onlyWhenTargeting && !isTargeting(mc, player)) {
            return;
        }
        if (player.capabilities.isFlying) {
            return;
        }

        if (RANDOM.nextInt(100) >= JumpResetMod.chance) {
            return;
        }

        boolean perfect = RANDOM.nextInt(100) < JumpResetMod.accuracy;

        if (perfect) {
            if (JumpResetMod.instantJump && canJump(player)) {
                performJump(mc, player);   // MESMO tick: sem atraso
                pendingJump = -1;
            } else {
                pendingJump = JumpResetMod.instantJump ? 0 : JumpResetMod.delay;
                waitTicks = 0;
            }
        } else {
            // timing errado de proposito (Accuracy): pula atrasado, KB nao cai
            pendingJump = 2 + RANDOM.nextInt(3);
            waitTicks = 0;
        }
    }

    /**
     * Blatant: antikb. Zera o KB horizontal e o pulinho do hit.
     * fica segurando por antikbHold ticks para pegar o packet de velocity atrasado.
     */
    private void applyBlatantVelocity(EntityPlayerSP player) {
        if (RANDOM.nextInt(100) < JumpResetMod.velocityMissChance) {
            return;
        }

        antikbTicks = JumpResetMod.antikbHold;

        double keep = JumpResetMod.blatantKeep / 100.0D;
        player.motionX *= keep;
        player.motionZ *= keep;

        // mata o impulso vertical do hit (senao voce sai do chao e o pulo atrasa)
        if (JumpResetMod.blatantAntiY && player.motionY > 0.0D) {
            player.motionY = 0.0D;
        }
    }

    private void tickAntikb(EntityPlayerSP player) {
        if (antikbTicks <= 0) {
            return;
        }
        antikbTicks--;

        if (JumpResetMod.mode != JumpResetMod.Mode.BLATANT) {
            return;
        }

        boolean hasInput = player.movementInput.moveForward != 0.0F
                || player.movementInput.moveStrafe != 0.0F;

        if (!hasInput) {
            double keep = JumpResetMod.blatantKeep / 100.0D;
            player.motionX *= keep;
            player.motionZ *= keep;

            if (JumpResetMod.blatantAntiY
                    && !jumpedForHit
                    && !player.onGround
                    && player.motionY > 0.0D) {
                player.motionY = 0.0D;
            }
        }
    }

    /** Semi / Legit: reducao disfarcada com fator sorteado. */
    private void applyLegitVelocity(EntityPlayerSP player) {
        if (RANDOM.nextInt(100) < JumpResetMod.velocityMissChance) {
            return;
        }

        double factor;
        if (JumpResetMod.velocityRandomize) {
            double range = JumpResetMod.mode.randomizeRange;
            factor = JumpResetMod.mode.velocityFactor + (RANDOM.nextDouble() * 2.0D - 1.0D) * range;
        } else {
            factor = JumpResetMod.mode.velocityFactor;
        }

        double mx = player.motionX * factor;
        double mz = player.motionZ * factor;

        if (JumpResetMod.velocityRandomize) {
            double angle = Math.toRadians((RANDOM.nextDouble() * 4.0D) - 2.0D);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double nx = mx * cos - mz * sin;
            double nz = mx * sin + mz * cos;
            mx = nx;
            mz = nz;
        }

        player.motionX = mx;
        player.motionZ = mz;

        if (JumpResetMod.velocityRandomize && RANDOM.nextInt(4) == 0) {
            player.motionY *= 1.0D + (RANDOM.nextDouble() * 0.04D - 0.02D);
        }
    }

    private boolean canJump(EntityPlayerSP player) {
        return player.onGround
                && !player.isInWater()
                && !player.isInLava()
                && !player.isRiding()
                && !player.capabilities.isFlying;
    }

    private void performJump(Minecraft mc, EntityPlayerSP player) {
        jumpedForHit = true;

        if (JumpResetMod.mode == JumpResetMod.Mode.LEGIT) {
            // segura a tecla por 1 tick: o pulo sai no onLivingUpdate deste tick
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
            jumpHoldTicks = 1;
            return;
        }

        if (!player.isSprinting()
                && player.movementInput.moveForward > 0.0F
                && player.getFoodStats().getFoodLevel() > 6) {
            player.setSprinting(true);
        }
        player.jump();
    }

    private void releaseJumpKey(Minecraft mc) {
        if (jumpHoldTicks > 0) {
            jumpHoldTicks = 0;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
        }
    }

    private boolean isPlayerNear(Minecraft mc, EntityPlayerSP player, double range) {
        List<Entity> entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                player, player.getEntityBoundingBox().expand(range, range, range));

        for (Entity entity : entities) {
            if (entity instanceof EntityPlayer && entity != player) {
                return true;
            }
        }
        return false;
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
