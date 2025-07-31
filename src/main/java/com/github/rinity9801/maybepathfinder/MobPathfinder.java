package com.github.rinity9801.maybepathfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.Set;

public class MobPathfinder {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final KeyBinding scanKey = new KeyBinding(
            "Scan Armor Stand Texture", Keyboard.KEY_H, "key.categories.misc");

    private static final String BODY_TEXTURE =
            "ewogICJ0aW1lc3RhbXAiIDogMTYyNTA3MjMxNDE2OCwKICAicHJvZmlsZUlkIiA6ICIwNWQ0NTNiZWE0N2Y0MThiOWI2ZDUzODg0MWQxMDY2MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJFY2hvcnJhIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzk2MjQxNjBlYjk5YmRjNjUxZGEzOGRiOTljZDdjMDlmMWRhNjY5ZWQ4MmI5Y2JjMjgyODc0NmU2NTBjNzY1ZGEiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";

    private static final String HEAD_TEXTURE =
            "ewogICJ0aW1lc3RhbXAiIDogMTYyMDQ0NTc2NDQ1MSwKICAicHJvZmlsZUlkIiA6ICJmNDY0NTcxNDNkMTU0ZmEwOTkxNjBlNGJmNzI3ZGNiOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWxhcGFnbzA1IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2RmMDNhZDk2MDkyZjNmNzg5OTAyNDM2NzA5Y2RmNjlkZTZiNzI3YzEyMWIzYzJkYWVmOWZmYTFjY2FlZDE4NmMiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";

    private final Set<EntityArmorStand> highlightedStands = new HashSet<>();

    public static void registerClient(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(scanKey);
        MinecraftForge.EVENT_BUS.register(new MobPathfinder());
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (scanKey.isPressed()) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§7[MobPathfinder] Scanning..."));
            scanForArmorStands();
        }
    }

    private void scanForArmorStands() {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        highlightedStands.clear();

        for (Object entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityArmorStand) {
                EntityArmorStand stand = (EntityArmorStand) entity;
                ItemStack helmet = stand.getCurrentArmor(3);
                if (helmet != null && helmet.hasTagCompound()) {
                    NBTTagCompound skullOwner = helmet.getTagCompound().getCompoundTag("SkullOwner");
                    if (skullOwner.hasKey("Properties")) {
                        NBTTagCompound props = skullOwner.getCompoundTag("Properties");
                        if (props.hasKey("textures")) {
                            String value = props.getTagList("textures", 10)
                                    .getCompoundTagAt(0).getString("Value");
                            if (value.equals(BODY_TEXTURE) || value.equals(HEAD_TEXTURE)) {
                                highlightedStands.add(stand);

                                BlockPos pos = stand.getPosition();
                                int y = (int) Math.floor(mc.thePlayer.posY);
                                String command = "/goto " + pos.getX() + " " + y + " " + pos.getZ();
                                mc.thePlayer.addChatMessage(new ChatComponentText("§dTarget found. Running: " + command));
                                mc.thePlayer.sendChatMessage(command);
                            }
                        }
                    }
                }
            }
        }

        if (highlightedStands.isEmpty()) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§cNo matching armor stand found."));
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (highlightedStands.isEmpty() || mc.thePlayer == null || mc.theWorld == null) return;

        EntityPlayerSP player = mc.thePlayer;
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        for (EntityArmorStand stand : highlightedStands) {
            double x = stand.posX - px;
            double y = stand.posY - py;
            double z = stand.posZ - pz;

            // Slightly lower and larger skull box
            AxisAlignedBB skullBox = new AxisAlignedBB(
                    x - 0.3, y + 1.52, z - 0.3,  // lowered further from 1.68
                    x + 0.3, y + 2.07, z + 0.3   // lowered from 2.23
            );


            // Hot pink
            RenderGlobal.drawOutlinedBoundingBox(skullBox, 255, 105, 180, 255);
        }

        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}
