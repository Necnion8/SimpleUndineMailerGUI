package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.MailPermission;
import com.google.common.collect.Lists;
import org.bitbucket.ucchy.undine.MailData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OutboxUI extends MailUI {
    private @Nullable List<MailData> mails;
    public OutboxUI(Player player, Panel parent) {
        super(player, parent);
        loadMails();
    }

    @Override
    public ItemStack getIcon() {
        String name = ChatColor.GOLD + "送信箱";
        return PanelItem.createItem(Material.DISPENSER, name).getItemStack();
    }

    @Override
    public @Nullable String getTitle() {
        return "送信箱";
    }

    @Override
    public @Nullable List<MailData> getMails() {
        return mails;
    }

    @Override
    public void build(PanelItem[] slots) {
        loadMails();
        super.build(slots);
    }

    @Override
    public PanelItem[] createMailMenuItems(MailData mail) {
        if (mail.isEditmode() || !mail.getFrom().equals(sender) || !mailer.getMailManager().getOutboxMails(sender).contains(mail))
            return new PanelItem[0];

        List<PanelItem> items = Lists.newArrayList();

        if (!mail.getAttachments().isEmpty()) {
            List<String> lines;
            if (mail.isAttachmentsCancelled()) {
                if (!mailer.checkAttachInboxPermission(player)) {
                    lines = Lists.newArrayList(ChatColor.DARK_RED + "添付ボックスを開く権限がありません");
                } else {
                    lines = null;
                }
                items.add(PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックスを開く", lines)
                        .setClickListener(() -> onAttachOpenButton(mail)));

            } else if (!mail.isAttachmentsOpened()) {
                if (!mailer.checkAttachInboxPermission(player)) {
                    lines = Lists.newArrayList(ChatColor.DARK_RED + "添付ボックスを開く権限がありません");
                } else {
                    lines = null;
                }
                items.add(PanelItem.createItem(Material.FIREWORK_STAR, ChatColor.RED + "添付アイテムをキャンセルする", lines)
                        .setClickListener(() -> onAttachCancelButton(mail)));
            }

        } else if (MailPermission.TRASH.can(player)) {
            items.add(PanelItem.createItem(Material.CACTUS, ChatColor.RED + "ゴミ箱に移動する")
                    .setClickListener(() -> onTrashButton(mail)));
        }

        return items.toArray(new PanelItem[0]);
    }

    public void loadMails() {
        mails = mailer.getMailManager().getOutboxMails(sender);
    }

    private void onTrashButton(MailData mail) {
        if (!MailGUIPlugin.getWrapper().available())
            return;

        if (MailPermission.TRASH.can(player) && mail.isRelatedWith(sender) && mail.isRead(sender)) {
            mail.setTrashFlag(sender);
            mailer.getMailManager().saveMail(mail);
            update();
        }
    }

    private void onAttachOpenButton(MailData mail) {
        if (!MailGUIPlugin.getWrapper().available())
            return;

        if (!mail.isAttachmentsCancelled())
            return;

        if (!mailer.checkAttachInboxPermission(player))
            return;

        mailer.openAttachmentInventory(sender, mail, this::open);
    }

    private void onAttachCancelButton(MailData mail) {
        if (!MailGUIPlugin.getWrapper().available())
            return;

        if (mail.isAttachmentsCancelled() || mail.isAttachmentsOpened())
            return;

        if (!mailer.checkAttachInboxPermission(player))
            return;

        mail.cancelAttachments();
        mailer.getMailManager().saveMail(mail);
    }

}
