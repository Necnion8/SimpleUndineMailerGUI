package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class MailUI {
    protected final Player player;
    private final Panel parent;

    public MailUI(Player player, Panel parent) {
        this.player = player;
        this.parent = parent;
    }

    public Player getPlayer() {
        return player;
    }

    public Panel getParent() {
        return parent;
    }

    protected void update() {
        parent.update();
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
