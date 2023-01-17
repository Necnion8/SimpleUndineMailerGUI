package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import org.bitbucket.ucchy.undine.MailData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OutboxUI extends MailUI {
    public OutboxUI(Player player, Panel parent) {
        super(player, parent);
    }

    @Override
    public ItemStack getIcon() {
        return PanelItem.createItem(Material.DISPENSER, ChatColor.GOLD + "送信箱 (?)").getItemStack();
    }

    @Override
    public @Nullable String getTitle() {
        return "送信箱";
    }

    @Override
    public @Nullable List<MailData> getMails() {
        return null;
    }

    @Override
    public void build(PanelItem[] slots) {

    }
}
