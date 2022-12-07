package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MainPanel extends Panel {
    public MainPanel(Player player) {
        super(player, 54, ChatColor.DARK_AQUA + "メール一覧", new ItemStack(Material.AIR));
    }

    @Override
    public PanelItem[] build() {
        PanelItem[] slots = new PanelItem[getSize()];

        // 1行目に write, ~, inbox, outbox, trash, groups, ~, pageBack, pageNext
        // 2行目、区切り
        // 3行目以降、エントリ一覧

        slots[0] = PanelItem.createItem(Material.PAPER, "New Mail");

        slots[2] = PanelItem.createItem(Material.DROPPER, "Inbox");
        slots[3] = PanelItem.createItem(Material.DISPENSER, "Outbox");
        slots[4] = PanelItem.createItem(Material.CACTUS, "Trash Box");
        slots[5] = PanelItem.createItem(Material.NAME_TAG, "Groups");

        slots[7] = PanelItem.createItem(Material.PINK_DYE, "Back Page");
        slots[8] = PanelItem.createItem(Material.CYAN_DYE, "Next Page");

        for (int i = 0; i < 9; i++) {
            slots[9 + i] = PanelItem.createItem(Material.STRING, "");
        }


        return slots;
    }

}
