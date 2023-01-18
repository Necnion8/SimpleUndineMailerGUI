package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.MailPermission;
import com.google.common.collect.Lists;
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
        if (mail.isEditmode() || !mail.isRecipient(sender) || !mailer.getMailManager().getInboxMails(sender).contains(mail))
            return new PanelItem[0];

        List<PanelItem> items = Lists.newArrayList();

        if (mail.getAttachments().isEmpty() && MailPermission.TRASH.can(player)) {
            items.add(PanelItem.createItem(Material.CACTUS, ChatColor.RED + "ゴミ箱に移動する").setClickListener((e, p) -> {
                if (!MailGUIPlugin.getWrapper().available())
                    return;

                if (MailPermission.TRASH.can(player) && mail.isRelatedWith(sender) && mail.isRead(sender)) {
                    mail.setTrashFlag(sender);
                    mailer.getMailManager().saveMail(mail);
                    update();
                }
            }));
        }

        if (mail.isAttachmentsRefused()) {
            items.add(PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックス", Lists.newArrayList(
                    ChatColor.DARK_RED + "受信者により添付が受取拒否されました")));
        } else if (mail.isAttachmentsCancelled()) {
            items.add(PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックス", Lists.newArrayList(
                    ChatColor.DARK_RED + "送信者により添付がキャンセルされました")));
        } else if (mail.getAttachments().isEmpty()) {
            return items.toArray(new PanelItem[0]);
        }

        boolean refuseButton = true;
        if (!mailer.checkAttachInboxPermission(player)) {
            refuseButton = false;
            items.add(PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックス", Lists.newArrayList(
                    ChatColor.DARK_RED + "添付ボックスを開く権限がありません")));

        } else if (mail.getCostMoney() > 0) {
            items.add(PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックス", Lists.newArrayList(
                    ChatColor.YELLOW + "お金を支払って添付ボックスを開く")).setClickListener((e, p) -> {

            }));
        } else if (mail.getCostItem() != null) {
            items.add(PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックス", Lists.newArrayList(
                    ChatColor.YELLOW + "アイテムを支払って添付ボックスを開く")).setClickListener((e, p) -> {

            }));
        } else {
            items.add(PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックス", Lists.newArrayList(
                    ChatColor.YELLOW + "添付ボックスを開く")).setClickListener((e, p) -> {

            }));
            refuseButton = false;
        }

        if (refuseButton)
            items.add(PanelItem.createItem(Material.SKELETON_SKULL, ChatColor.RED + "受け取りを拒否する").setClickListener((e, p) -> {

            }));

        return items.toArray(new PanelItem[0]);
    }

}
