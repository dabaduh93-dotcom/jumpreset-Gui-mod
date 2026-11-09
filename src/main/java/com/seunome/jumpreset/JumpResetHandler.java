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

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class JumpResetHandler {

    private static final Random RANDOM = new Random();

    private static final double TARGET_RANGE = 4.5D;
    private static final double TARGET_DOT = 0.5D;
    private static final double NEARBY_RANGE = 5.0D;
    private static final int GIVE_UP_TICKS = 8;

    private int lastHurtTime;
    private int pendingJump = -1;
    private int waitTicks;
    private int jumpHoldTicks;
    private int jumpKeyCode = Integer.MIN_VALUE;
    private int antikbTicks;
    private boolean jumpedForHit;
    private boolean groundedAtHit;

    // deteccao de setback (Grim devolve sua posicao)
    private double lastPosX;
    private double lastPosY;
    private double lastPosZ;
    private boolean hasLastPos;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START) {
            tickStart(mc);
        } else {
            tickEnd(mc);
        }
    }

    private void tickStart(Minecraft mc) {
        if (jumpHoldTicks > 0) {
            jumpHoldTicks--;
            if (jumpHoldTicks == 0) {
                releaseJumpKey(mc);
            }
        }

        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null) {
            releaseJumpKey(mc);
            reset();
            lastHurtTime = 0;
            hasLastPos = false;
            return;
        }

        // setback: se a posicao deu um salto grande, limpa o estado
        if (hasLastPos) {
            double dx = player.posX - lastPosX;
            double dy = player.posY - lastPosY;
            double dz = player.posZ - lastPosZ;
            if (dx * dx + dy * dy + dz * dz > 9.0D) {
                reset();
                antikbTicks = 0;
            }
        }
        lastPosX = player.posX;
        lastPosY = player.posY;
        lastPosZ = player.posZ;
        hasLastPos = true;

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
        double rawHorizontal = Math.sqrt(player.motionX * player.motionX
                + player.motionZ * player.motionZ);

        if (JumpResetMod.velocityEnabled) {
            if (JumpResetMod.mode == JumpResetMod.Mode.BLATANT) {
                applyBlatantVelocity(player);
            } else {
                applyLegitVelocity(player);
            }
        }

        if (JumpResetMod.knockbackThreshold > 0.0D && rawHorizontal < JumpResetMod.knockbackThreshold) {
            return;
        }
        if (JumpResetMod.nearbyPlayerCheck && !isPlayerNear(mc, player, NEARBY_RANGE)) {
            return;
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
                performJump(mc, player);
                pendingJump = -1;
            } else {
                pendingJump = JumpResetMod.instantJump ? 0 : JumpResetMod.delay;
                waitTicks = 0;
            }
        } else {
            pendingJump = 2 + RANDOM.nextInt(3);
            waitTicks = 0;
        }
    }

    private void applyBlatantVelocity(EntityPlayerSP player) {
        if (RANDOM.nextInt(100) < JumpResetMod.velocityMissChance) {
            return;
        }

        antikbTicks = JumpResetMod.antikbHold;
        groundedAtHit = player.onGround;

        double keep = JumpResetMod.blatantKeep / 100.0D;
        player.motionX *= keep;
        player.motionZ *= keep;

        // vertical: SO se estiver no chao no hit (nunca no ar -> evita setback do Grim)
        if (JumpResetMod.blatantAntiY && player.onGround && player.motionY > 0.0D) {
            player.motionY = 0.0D;
        }
    }

    /**
     * Hold do antikb. Regras para nao quebrar a predicao do Grim:
     *  - nunca mexe em motionY com o player no ar
     *  - so anula o horizontal quando voce nao esta dando input
     *  - se voce pulou no hit, nao encosta no movimento
     */
    private void tickAntikb(EntityPlayerSP player) {
        if (antikbTicks <= 0) {
            return;
        }
        antikbTicks--;

        if (JumpResetMod.mode != JumpResetMod.Mode.BLATANT || jumpedForHit) {
            return;
        }

        boolean hasInput = player.movementInput.moveForward != 0.0F
                || player.movementInput.moveStrafe != 0.0F;

        if (hasInput) {
            return;
        }

        double keep = JumpResetMod.blatantKeep / 100.0D;
        player.motionX *= keep;
        player.motionZ *= keep;

        if (JumpResetMod.blatantAntiY
                && groundedAtHit
                && player.onGround
                && player.motionY > 0.0D) {
            player.motionY = 0.0D;
        }
    }

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
            jumpKeyCode = mc.gameSettings.keyBindJump.getKeyCode();
            KeyBinding.setKeyBindState(jumpKeyCode, true);
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
        jumpHoldTicks = 0;

        if (jumpKeyCode == Integer.MIN_VALUE) {
            return;
        }

        KeyBinding.setKeyBindState(jumpKeyCode, isPhysicalKeyDown(jumpKeyCode));
        jumpKeyCode = Integer.MIN_VALUE;
    }

    private boolean isPhysicalKeyDown(int keyCode) {
        if (keyCode >= 0) {
            return Keyboard.isKeyDown(keyCode);
        }
        return Mouse.isButtonDown(keyCode + 100);
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
