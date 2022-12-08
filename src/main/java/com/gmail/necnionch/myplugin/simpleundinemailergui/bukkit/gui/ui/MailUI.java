package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class MailUI {
    protected final Player player;

    public MailUI(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Nullable
    public abstract ItemStack getIcon();

    public abstract void onBackButton();
    public abstract void onNextButton();

    public abstract int getCurrentPage();
    public abstract int getMaxPage();

    @Nullable
    public abstract String getTitle();

    public abstract void build(PanelItem[] slots);

}
