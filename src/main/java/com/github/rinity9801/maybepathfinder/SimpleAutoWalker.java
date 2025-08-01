package com.github.rinity9801.maybepathfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class SimpleAutoWalker {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean active = false;

    private static final KeyBinding toggleKey = new KeyBinding(
            "Toggle Simple Walker", Keyboard.KEY_APOSTROPHE, "key.categories.misc");

    public static void registerClient(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(toggleKey);
        MinecraftForge.EVENT_BUS.register(new SimpleAutoWalker());
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (toggleKey.isPressed()) {
            active = !active;
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                    "§7[AutoWalker] " + (active ? "§aEnabled" : "§cDisabled")));

            if (!active) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!active || mc.thePlayer == null || mc.theWorld == null) return;

        EntityPlayerSP player = mc.thePlayer;

        // 🎯 Smooth pitch to 45f
        float pitchDiff = 45f - player.rotationPitch;
        if (Math.abs(pitchDiff) > 1f) {
            player.rotationPitch += Math.signum(pitchDiff) * 1.5f;
        } else {
            player.rotationPitch = 45f;
        }

        // 🧭 Smooth yaw to nearest 90-degree direction
        float[] targets = {-180f, -90f, 90f, 180f};
        float currentYaw = normalizeYaw(player.rotationYaw);
        float closest = targets[0];
        float minDiff = Math.abs(normalizeYaw(targets[0] - currentYaw));
        for (float t : targets) {
            float diff = Math.abs(normalizeYaw(t - currentYaw));
            if (diff < minDiff) {
                minDiff = diff;
                closest = t;
            }
        }

        float yawDiff = normalizeYaw(closest - currentYaw);
        if (Math.abs(yawDiff) > 1f) {
            player.rotationYaw += Math.signum(yawDiff) * 2f;
        } else {
            player.rotationYaw = closest;
        }

        // 👣 Hold W, attack, and sprint
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
    }

    private float normalizeYaw(float angle) {
        angle %= 360f;
        if (angle >= 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }
}
