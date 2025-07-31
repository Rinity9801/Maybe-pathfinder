package com.github.rinity9801.maybepathfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class MobPathfinder {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final KeyBinding scanKey = new KeyBinding(
            "Scan Armor Stand Texture", Keyboard.KEY_H, "key.categories.misc");

    private static final String BODY_TEXTURE =
            "ewogICJ0aW1lc3RhbXAiIDogMTYyNTA3MjMxNDE2OCwKICAicHJvZmlsZUlkIiA6ICIwNWQ0NTNiZWE0N2Y0MThiOWI2ZDUzODg0MWQxMDY2MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJFY2hvcnJhIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzk2MjQxNjBlYjk5YmRjNjUxZGEzOGRiOTljZDdjMDlmMWRhNjY5ZWQ4MmI5Y2JjMjgyODc0NmU2NTBjNzY1ZGEiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";

    private static final String HEAD_TEXTURE =
            "ewogICJ0aW1lc3RhbXAiIDogMTYyMDQ0NTc2NDQ1MSwKICAicHJvZmlsZUlkIiA6ICJmNDY0NTcxNDNkMTU0ZmEwOTkxNjBlNGJmNzI3ZGNiOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWxhcGFnbzA1IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2RmMDNhZDk2MDkyZjNmNzg5OTAyNDM2NzA5Y2RmNjlkZTZiNzI3YzEyMWIzYzJkYWVmOWZmYTFjY2FlZDE4NmMiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";

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
                                BlockPos pos = stand.getPosition();
                                int y = (int) Math.floor(mc.thePlayer.posY); // Use player Y
                                String command = "/goto " + pos.getX() + " " + y + " " + pos.getZ();
                                mc.thePlayer.addChatMessage(new ChatComponentText("§aTarget found. Running: " + command));
                                mc.thePlayer.sendChatMessage(command);
                                return;
                            }
                        }
                    }
                }
            }
        }

        mc.thePlayer.addChatMessage(new ChatComponentText("§cNo matching armor stand found."));
    }
}
