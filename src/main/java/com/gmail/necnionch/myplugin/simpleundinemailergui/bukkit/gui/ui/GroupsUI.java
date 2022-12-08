package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class GroupsUI extends MailUI {
    public GroupsUI(Player player, Panel parent) {
        super(player, parent);
    }

    @Override
    public ItemStack getIcon() {
        return PanelItem.createItem(Material.NAME_TAG, ChatColor.GOLD + "グループ (?)").getItemStack();
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
        return "グループ";
    }

    @Override
    public void build(PanelItem[] slots) {

    }
}
