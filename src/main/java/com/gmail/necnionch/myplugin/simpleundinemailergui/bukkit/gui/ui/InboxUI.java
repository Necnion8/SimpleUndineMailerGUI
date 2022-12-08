package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class InboxUI extends MailUI {
    public InboxUI(Player player) {
        super(player);
    }

    @Override
    public ItemStack getIcon() {
        return PanelItem.createItem(Material.DROPPER, ChatColor.GOLD + "受信箱 (?)").getItemStack();
    }

    @Override
    public void onBackButton() {

    }

    @Override
    public void onNextButton() {

    }

    @Override
    public int getCurrentPage() {
        return 0;
    }

    @Override
    public int getMaxPage() {
        return 0;
    }

    @Override
    public @Nullable String getTitle() {
        return "受信箱";
    }

    @Override
    public void build(PanelItem[] slots) {

    }
}
