package com.github.rinity9801.maybepathfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
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
    private static final KeyBinding walkKey = new KeyBinding("Start Route Walk", Keyboard.KEY_R, "MaybePathfinder");

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

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            Vec3 pos = mc.thePlayer.getPositionVector();
            BlockPos blockPos = new BlockPos(pos);
            Vec3 center = new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            waypoints.add(center);
            sender.addChatMessage(new ChatComponentText("Added waypoint: " + center));
        }

        @Override public int getRequiredPermissionLevel() { return 0; }
    }

    public static class WalkerRemoveCommand extends CommandBase {
        @Override public String getCommandName() { return "walkerremove"; }
        @Override public String getCommandUsage(ICommandSender sender) { return "/walkerremove"; }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (!waypoints.isEmpty()) {
                Vec3 removed = waypoints.remove(waypoints.size() - 1);
                sender.addChatMessage(new ChatComponentText("Removed waypoint: " + removed));
            } else {
                sender.addChatMessage(new ChatComponentText("No waypoints to remove."));
            }
        }

        @Override public int getRequiredPermissionLevel() { return 0; }
    }

    public static class WalkerInsertCommand extends CommandBase {
        @Override public String getCommandName() { return "walkerinsert"; }
        @Override public String getCommandUsage(ICommandSender sender) { return "/walkerinsert <index>"; }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1) {
                sender.addChatMessage(new ChatComponentText("Usage: /walkerinsert <index>"));
                return;
            }

            int index;
            try {
                index = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText("Invalid index."));
                return;
            }

            if (index < 0 || index > waypoints.size()) {
                sender.addChatMessage(new ChatComponentText("Index out of bounds."));
                return;
            }

            Vec3 pos = mc.thePlayer.getPositionVector();
            BlockPos blockPos = new BlockPos(pos);
            Vec3 center = new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            waypoints.add(index, center);
            sender.addChatMessage(new ChatComponentText("Inserted waypoint at index " + index + ": " + center));
        }

        @Override public int getRequiredPermissionLevel() { return 0; }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (walkKey.isPressed()) {
            walking = !walking;
            walkIndex = 0;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            mc.thePlayer.setSprinting(false);
            mc.thePlayer.addChatMessage(new ChatComponentText("Route walking: " + (walking ? "Started" : "Stopped")));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        boolean shouldWalk = walking && walkIndex < waypoints.size();

        if (!shouldWalk) {
            if (walking) {
                walking = false;
                walkIndex = 0;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                mc.thePlayer.setSprinting(false);
                mc.thePlayer.addChatMessage(new ChatComponentText("Route walk stopped."));
            }
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        Vec3 target = waypoints.get(walkIndex);

        double dx = (target.xCoord + 0.5) - player.posX;
        double dz = (target.zCoord + 0.5) - player.posZ;
        double dy = target.yCoord - player.posY;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float desiredYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float currentYaw = player.rotationYaw;
        float yawDiff = wrapAngleTo180(desiredYaw - currentYaw);
        float maxTurn = 10f;
        float newYaw = currentYaw + Math.max(-maxTurn, Math.min(maxTurn, yawDiff));
        player.rotationYaw = newYaw;

        if (Math.abs(yawDiff) < 15f) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
            player.setSprinting(RouteWalkerConfig.sprintEnabled); // Use sprinting setting from config
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            player.setSprinting(false); // Disable Sprinting
        }

        // Simulate holding left-click (attack) if configured to do so
        if (RouteWalkerConfig.holdLeftClick) {
            simulateLeftClick();
        }

        if (dist < 0.5 && Math.abs(dy) <= 0.5) {
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    String.format("[RouteWalker] Reached waypoint #%d at [%.1f, %.1f, %.1f]",
                            walkIndex, target.xCoord, target.yCoord, target.zCoord)
            ));
            walkIndex++;
            if (walkIndex >= waypoints.size()) {
                if (RouteWalkerConfig.repeatRoute) {
                    walkIndex = 0; // Reset to start the route again
                    mc.thePlayer.addChatMessage(new ChatComponentText("Repeating the route from the beginning."));
                } else {
                    walking = false;
                    player.setSprinting(false);
                    player.addChatMessage(new ChatComponentText("Route walk complete."));
                }
            }
            return;
        }

        if (dy > 0.5 && player.onGround) {
            BlockPos front = new BlockPos(player.posX + dx * 0.5, player.posY, player.posZ + dz * 0.5);
            if (!mc.theWorld.isAirBlock(front)) {
                player.jump();
            }
        }
    }

    private void simulateLeftClick() {
        // Simulate holding the left mouse button continuously
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true); // Left-click button press
    }

    private float wrapAngleTo180(float angle) {
        angle %= 360f;
        if (angle >= 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
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
            int x = (int) Math.floor(vec.xCoord);
            int y = (int) Math.floor(vec.yCoord) - 1;
            int z = (int) Math.floor(vec.zCoord);
            double dx = x - px;
            double dy = y - py;
            double dz = z - pz;

            AxisAlignedBB bb = new AxisAlignedBB(dx, dy, dz, dx + 1, dy + 1, dz + 1);
            RenderGlobal.drawOutlinedBoundingBox(bb, 0, 0, 255, 255);
        }

        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (Vec3 vec : waypoints) {
            double x = vec.xCoord + 0.5 - px;
            double y = vec.yCoord - 0.5 - py;
            double z = vec.zCoord + 0.5 - pz;
            wr.pos(x, y, z).color(0f, 0f, 1f, 1f).endVertex();
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
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        mc.thePlayer.setSprinting(false);
    }
}
