package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks.MailWrapper;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class MailUI {
    protected final Player player;
    private final Panel parent;
    protected final MailWrapper mailer = MailGUIPlugin.getWrapper();
    protected final MailSenderPlayer sender;

    public MailUI(Player player, Panel parent) {
        this.player = player;
        this.parent = parent;
        this.sender = (MailSenderPlayer) MailSender.getMailSender(player);
    }

    public Player getPlayer() {
        return player;
    }

    public Panel getParent() {
        return parent;
    }

    public MailWrapper getMailer() {
        return mailer;
    }

    public MailSenderPlayer getMailSender() {
        return sender;
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
