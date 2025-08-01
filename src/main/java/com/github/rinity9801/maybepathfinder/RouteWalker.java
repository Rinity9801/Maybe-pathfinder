package com.github.rinity9801.maybepathfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class RouteWalker {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final List<Vec3> waypoints = new ArrayList<>();
    private static boolean walking = false;
    private static int walkIndex = 0;
    private static float currentTurnSpeed = 0f;

    private static final KeyBinding walkKey = new KeyBinding("Toggle Route Walk", Keyboard.KEY_R, "Maybe Pathfinder");

    public static void registerClient(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(walkKey);
        MinecraftForge.EVENT_BUS.register(new RouteWalker());
    }

    public static void registerServer(FMLServerStartingEvent event) {
        event.registerServerCommand(new WalkerAddCommand());
        event.registerServerCommand(new WalkerRemoveCommand());
        event.registerServerCommand(new WalkerInsertCommand());
    }

    public static class WalkerAddCommand extends CommandBase {
        @Override public String getCommandName() { return "walkeradd"; }
        @Override public String getCommandUsage(ICommandSender sender) { return "/walkeradd"; }
        @Override public void processCommand(ICommandSender sender, String[] args) {
            Vec3 pos = mc.thePlayer.getPositionVector();
            BlockPos blockPos = new BlockPos(pos);
            Vec3 center = new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            waypoints.add(center);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[Walker] Added waypoint: " + EnumChatFormatting.YELLOW + center));
        }
        @Override public int getRequiredPermissionLevel() { return 0; }
    }

    public static class WalkerRemoveCommand extends CommandBase {
        @Override public String getCommandName() { return "walkerremove"; }
        @Override public String getCommandUsage(ICommandSender sender) { return "/walkerremove"; }
        @Override public void processCommand(ICommandSender sender, String[] args) {
            if (!waypoints.isEmpty()) {
                Vec3 removed = waypoints.remove(waypoints.size() - 1);
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[Walker] Removed waypoint: " + EnumChatFormatting.YELLOW + removed));
                if (walkIndex >= waypoints.size()) walkIndex = waypoints.size() - 1;
            } else {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[Walker] No waypoints to remove."));
            }
        }
        @Override public int getRequiredPermissionLevel() { return 0; }
    }

    public static class WalkerInsertCommand extends CommandBase {
        @Override public String getCommandName() { return "walkerinsert"; }
        @Override public String getCommandUsage(ICommandSender sender) { return "/walkerinsert <index>"; }
        @Override public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /walkerinsert <index>"));
                return;
            }
            int index;
            try { index = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Invalid index."));
                return;
            }
            if (index < 0 || index > waypoints.size()) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Index out of bounds."));
                return;
            }
            Vec3 pos = mc.thePlayer.getPositionVector();
            BlockPos blockPos = new BlockPos(pos);
            Vec3 center = new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            waypoints.add(index, center);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[Walker] Inserted waypoint #" + index + ": " + EnumChatFormatting.YELLOW + center));
        }
        @Override public int getRequiredPermissionLevel() { return 0; }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (walkKey.isPressed()) {
            walking = !walking;
            currentTurnSpeed = 0f;
            if (mc.thePlayer != null) mc.thePlayer.setSprinting(false);
            if (RouteWalkerConfig.debugLogs && mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.AQUA + "[Walker] Route walking: " +
                                (walking ? EnumChatFormatting.GREEN + "Started" : EnumChatFormatting.RED + "Stopped") +
                                EnumChatFormatting.GRAY + " (Index: " + walkIndex + ")"));
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!walking || mc.thePlayer == null || mc.theWorld == null || waypoints.isEmpty() || walkIndex < 0 || walkIndex >= waypoints.size()) {
            stopRoute();
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        Vec3 current = waypoints.get(walkIndex);
        double dx = (current.xCoord + 0.5) - player.posX;
        double dz = (current.zCoord + 0.5) - player.posZ;
        double dy = current.yCoord - player.posY;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float desiredYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float yawDiff = wrapAngleTo180(desiredYaw - player.rotationYaw);

        float maxSpeed = 20f;
        float turnAccel = 3.2f;
        float turnFriction = 1.0f;

        float targetSpeed = Math.min(Math.abs(yawDiff), maxSpeed);
        if (currentTurnSpeed < targetSpeed)
            currentTurnSpeed += turnAccel;
        else
            currentTurnSpeed -= turnFriction;

        currentTurnSpeed = Math.max(0f, Math.min(currentTurnSpeed, maxSpeed));
        float step = Math.signum(yawDiff) * Math.min(currentTurnSpeed, Math.abs(yawDiff));
        player.rotationYaw += step;

        if (RouteWalkerConfig.pitchLockEnabled) {
            player.rotationPitch = RouteWalkerConfig.pitchValue;
        }

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
        if (RouteWalkerConfig.sprintEnabled) player.setSprinting(true);
        if (RouteWalkerConfig.holdLeftClick) simulateLeftClick();

        boolean closeEnough = dist < 0.75;
        boolean alignedVertically = Math.abs(dy) <= 0.3;
        boolean alignedYaw = Math.abs(wrapAngleTo180(desiredYaw - player.rotationYaw)) < 8f;

        if (closeEnough && alignedVertically && alignedYaw) {
            walkIndex++;
            if (walkIndex >= waypoints.size()) {
                if (RouteWalkerConfig.repeatRoute) {
                    walkIndex = 0;
                } else {
                    walking = false;
                    player.setSprinting(false);
                    if (RouteWalkerConfig.debugLogs) {
                        player.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[Walker] Route walk complete."));
                    }
                    return;
                }
            }
            currentTurnSpeed = 0f;
            return;
        }

        if (dy > 0.5 && player.onGround && dist < 1.2) {
            BlockPos front = new BlockPos(
                    player.posX + dx * 0.5, player.posY, player.posZ + dz * 0.5);
            if (!mc.theWorld.isAirBlock(front)) player.jump();
        }
    }

    private float wrapAngleTo180(float angle) {
        angle %= 360f;
        if (angle >= 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }

    private void simulateLeftClick() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (waypoints.isEmpty() || mc.thePlayer == null || mc.theWorld == null) return;
        EntityPlayerSP player = mc.thePlayer;
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        for (Vec3 vec : waypoints) {
            AxisAlignedBB bb = new AxisAlignedBB(
                    vec.xCoord - px, vec.yCoord - 1 - py, vec.zCoord - pz,
                    vec.xCoord + 1 - px, vec.yCoord - py, vec.zCoord + 1 - pz);
            RenderGlobal.drawOutlinedBoundingBox(bb, 0, 0, 255, 255);
        }
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (Vec3 vec : waypoints) {
            wr.pos(vec.xCoord + 0.5 - px, vec.yCoord - 0.5 - py, vec.zCoord + 0.5 - pz)
                    .color(0f, 0f, 1f, 1f).endVertex();
        }
        tess.draw();
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    public static List<Vec3> getWaypoints() {
        return waypoints;
    }

    public static void stopRoute() {
        walking = false;
        walkIndex = 0;
        currentTurnSpeed = 0f;
        if (mc.thePlayer != null) {
            mc.thePlayer.setSprinting(false);
        }
    }
}
