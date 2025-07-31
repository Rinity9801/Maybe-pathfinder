package com.github.rinity9801.maybepathfinder;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.UUID;

public class GiveHeadCommand extends CommandBase {

    private static final String BODY_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYyNTA3MjMxNDE2OCwKICAicHJvZmlsZUlkIiA6ICIwNWQ0NTNiZWE0N2Y0MThiOWI2ZDUzODg0MWQxMDY2MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJFY2hvcnJhIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzk2MjQxNjBlYjk5YmRjNjUxZGEzOGRiOTljZDdjMDlmMWRhNjY5ZWQ4MmI5Y2JjMjgyODc0NmU2NTBjNzY1ZGEiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
    private static final String HEAD_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYyMDQ0NTc2NDQ1MSwKICAicHJvZmlsZUlkIiA6ICJmNDY0NTcxNDNkMTU0ZmEwOTkxNjBlNGJmNzI3ZGNiOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWxhcGFnbzA1IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2RmMDNhZDk2MDkyZjNmNzg5OTAyNDM2NzA5Y2RmNjlkZTZiNzI3YzEyMWIzYzJkYWVmOWZmYTFjY2FlZDE4NmMiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";

    @Override
    public String getCommandName() {
        return "givehead";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/givehead <head|body>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Must be a player to use this command."));
            return;
        }

        if (args.length != 1 || (!args[0].equalsIgnoreCase("head") && !args[0].equalsIgnoreCase("body"))) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage: /givehead <head|body>"));
            return;
        }

        String texture = args[0].equalsIgnoreCase("head") ? HEAD_TEXTURE : BODY_TEXTURE;

        ItemStack skull = new ItemStack(Items.skull, 1, 3); // meta 3 = player head

        GameProfile profile = new GameProfile(UUID.randomUUID(), "CustomHead");
        profile.getProperties().put("textures", new Property("textures", texture));

        NBTTagCompound skullOwner = new NBTTagCompound();
        skullOwner.setString("Id", profile.getId().toString());

        NBTTagCompound properties = new NBTTagCompound();
        NBTTagCompound textureTag = new NBTTagCompound();
        textureTag.setString("Value", texture);
        net.minecraft.nbt.NBTTagList textures = new net.minecraft.nbt.NBTTagList();
        textures.appendTag(textureTag);
        properties.setTag("textures", textures);

        skullOwner.setTag("Properties", properties);

        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("SkullOwner", skullOwner);
        skull.setTagCompound(tag);

        EntityPlayer player = (EntityPlayer) sender;
        player.inventory.addItemStackToInventory(skull);
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Gave you the " + args[0] + " head."));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
