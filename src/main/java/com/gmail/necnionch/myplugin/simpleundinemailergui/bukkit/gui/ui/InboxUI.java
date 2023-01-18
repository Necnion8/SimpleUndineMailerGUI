package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.MailManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InboxUI extends MailUI {
    private final MailManager mail;
    private long unread;
    private @Nullable List<MailData> mails;

    public InboxUI(Player player, Panel parent) {
        super(player, parent);
        mail = mailer.getMailManager();
        loadInbox();
    }

    @Override
    public ItemStack getIcon() {
        String name = ChatColor.GOLD + "受信箱";
        if (unread > 0)
            name += ChatColor.GRAY + " (未読 " + unread + "通)";
        return PanelItem.createItem(Material.DROPPER, name).getItemStack();
    }

    @Override
    public @Nullable String getTitle() {
        return "受信箱";
    }

    @Override
    public @Nullable List<MailData> getMails() {
        return mails;
    }

    @Override
    public void build(PanelItem[] slots) {
        loadInbox();
        super.build(slots);
    }


    public void loadInbox() {
        unread = 0;
        mails = mail.getInboxMails(sender);
        if (mails == null)
            return;
        unread = mails.stream().filter(m -> !m.isRead(sender)).count();
    }

    @Override
    public PanelItem[] createMailMenuItems(MailData mail) {
        PanelItem[] items = new PanelItem[4];
        items[0] = PanelItem.createItem(Material.STONE, "1");
        items[1] = PanelItem.createItem(Material.STONE, "2");
        items[2] = PanelItem.createItem(Material.STONE, "3");
        items[3] = PanelItem.createItem(Material.STONE, "4");
        return items;
    }

}
