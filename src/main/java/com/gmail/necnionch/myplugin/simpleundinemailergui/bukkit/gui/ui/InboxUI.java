package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.MailPermission;
import com.google.common.collect.Lists;
import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.MailManager;
import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.sender.MailSenderConsole;
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
            items.add(PanelItem.createItem(Material.CACTUS, ChatColor.RED + "ゴミ箱に移動する")
                    .setClickListener(() -> onTrashButton(mail)));
        }

        if (mail.isAttachmentsRefused()) {
            items.add(PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックスを開く", Lists.newArrayList(
                    ChatColor.DARK_RED + "受信者により添付が受取拒否されました")));
            return items.toArray(new PanelItem[0]);

        } else if (mail.isAttachmentsCancelled()) {
            items.add(PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックスを開く", Lists.newArrayList(
                    ChatColor.DARK_RED + "送信者により添付がキャンセルされました")));
            return items.toArray(new PanelItem[0]);

        } else if (mail.getAttachments().isEmpty()) {
            return items.toArray(new PanelItem[0]);
        }

        List<String> attachLines = Lists.newArrayList("", ChatColor.RED + "添付アイテム:");
        mail.getAttachments().stream()
                .map(item -> ChatColor.WHITE + "  " + mailer.itemDesc(item, true))
                .forEachOrdered(attachLines::add);
        if (mail.getCostMoney() > 0) {
            attachLines.add("");
            attachLines.add(ChatColor.GOLD + "着払い料金: " + ChatColor.WHITE + mailer.formatCostMoney(mail.getCostMoney()));
        } else if (mail.getCostItem() != null) {
            attachLines.add("");
            attachLines.add(ChatColor.GOLD + "着払いアイテム: " + ChatColor.WHITE + mailer.itemDesc(mail.getCostItem(), true));
        }
        List<String> lines;

        boolean refuseButton = true;
        if (!mailer.checkAttachInboxPermission(player)) {
            refuseButton = false;
            lines = Lists.newArrayList(ChatColor.DARK_RED + "添付ボックスを開く権限がありません");
            lines.addAll(attachLines);
            items.add(PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックスを開く", lines)
                    .setClickListener(() -> onAttachOpenButton(mail)));

        } else if (mail.getCostMoney() > 0) {
            lines = Lists.newArrayList(ChatColor.YELLOW + "受け取るには着払い料金を支払う必要があります");
            if (mailer.isDisabledAttachmentWorld(sender)) {
                lines.add(ChatColor.RED + "別のワールドに移動してから添付ボックスを開いてください。");
            }
            lines.addAll(attachLines);
            items.add(PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックスを開く", lines)
                    .setClickListener(() -> onAttachOpenButton(mail)));
            boolean hasMoney = mailer.checkCostMoney(sender, mail);
            lines = Lists.newArrayList(
                    "",
                    (hasMoney ? ChatColor.GOLD : ChatColor.DARK_RED) + "必要: " + (hasMoney ? ChatColor.WHITE : ChatColor.RED) + mailer.formatCostMoney(mail.getCostMoney())
            );
            if (!hasMoney)
                lines.add(0, ChatColor.RED + "必要なお金が足りません！");

            items.add(PanelItem.createItem(Material.EMERALD, ChatColor.GOLD + "着払い料金を支払う", lines)
                    .setClickListener(() -> onAttachCostMoneyAcceptButton(mail)));

        } else if (mail.getCostItem() != null) {
            lines = Lists.newArrayList(ChatColor.YELLOW + "受け取るには着払いアイテムを支払う必要があります");
            if (mailer.isDisabledAttachmentWorld(sender)) {
                lines.add(ChatColor.RED + "別のワールドに移動してから添付ボックスを開いてください。");
            }
            lines.addAll(attachLines);
            items.add(PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックスを開く", lines)
                    .setClickListener(() -> onAttachOpenButton(mail)));

            boolean hasItem = mailer.checkCostItem(player, sender, mail);
            lines = Lists.newArrayList(
                    "",
                    (hasItem ? ChatColor.GOLD : ChatColor.DARK_RED) + "必要: " + (hasItem ? ChatColor.WHITE : ChatColor.RED) + mailer.itemDesc(mail.getCostItem(), true)
            );
            if (!hasItem)
                lines.add(0, ChatColor.RED + "必要なアイテムが足りません！");

            items.add(PanelItem.createItem(Material.EMERALD, ChatColor.GOLD + "着払いアイテムを支払う", lines)
                    .setClickListener(() -> onAttachCostItemAcceptButton(mail)));

        } else {
            items.add(PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックスを開く", attachLines)
                    .setClickListener(() -> onAttachOpenButton(mail)));
            refuseButton = false;
        }

        if (refuseButton)
            items.add(PanelItem.createItem(Material.SKELETON_SKULL, ChatColor.RED + "添付アイテムの受け取りを拒否する")
                    .setClickListener(() -> onAttachRefuseButton(mail)));

        return items.toArray(new PanelItem[0]);
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

        if (mail.isEditmode() || !mail.isRecipient(sender) || !mailer.getMailManager().getInboxMails(sender).contains(mail))
            return;

        if (mail.isAttachmentsRefused() || mail.isAttachmentsCancelled() || mail.getAttachments().isEmpty())
            return;

        if (!mailer.checkAttachInboxPermission(player))
            return;

        if (mail.getCostMoney() > 0 || mail.getCostItem() != null)
            return;

        if (mailer.isDisabledAttachmentWorld(sender))
            return;

        mailer.openAttachmentInventory(sender, mail, this::open);
    }

    private void onAttachCostItemAcceptButton(MailData mail) {
        if (!MailGUIPlugin.getWrapper().available())
            return;

        if (mail.isEditmode() || !mail.isRecipient(sender) || !mailer.getMailManager().getInboxMails(sender).contains(mail))
            return;

        if (mail.isAttachmentsRefused() || mail.isAttachmentsCancelled() || mail.getAttachments().isEmpty())
            return;

        if (!mailer.checkAttachInboxPermission(player) || mail.getCostItem() == null)
            return;

        if (!mailer.checkCostItem(player, sender, mail))
            return;

        if (mailer.tryAcceptCostItem(player, sender, mail))
            update();
    }

    private void onAttachCostMoneyAcceptButton(MailData mail) {
        if (!MailGUIPlugin.getWrapper().available())
            return;

        if (mail.isEditmode() || !mail.isRecipient(sender) || !mailer.getMailManager().getInboxMails(sender).contains(mail))
            return;

        if (mail.isAttachmentsRefused() || mail.isAttachmentsCancelled() || mail.getAttachments().isEmpty())
            return;

        if (!mailer.checkAttachInboxPermission(player) || mail.getCostMoney() <= 0)
            return;

        if (!mailer.checkCostMoney(sender, mail))
            return;

        if (mailer.tryAcceptCostMoney(sender, mail))
            update();
    }
    
    private void onAttachRefuseButton(MailData mail) {
        if (!MailGUIPlugin.getWrapper().available())
            return;

        if (!(mail.isAttachmentsCancelled() || !mail.getToTotal().contains(sender) || mail.isAttachmentsOpened())) {
            // 添付を拒否し、添付をクリアして、メールを保存する。
            List<ItemStack> attachments = Lists.newArrayList(mail.getAttachments());
            mail.refuseAttachments(null);
            mail.getAttachments().clear();
            mailer.getMailManager().saveMail(mail);

            // 送信者側に新規メールで、アイテムを差し戻す
            MailData reply = new MailData();
            reply.setTo(0, mail.getFrom());
            reply.setFrom(MailSenderConsole.getMailSenderConsole());
            reply.addMessage(Messages.get(
                    "BoxRefuseSenderResult",
                    new String[]{"%to", "%num"},
                    new String[]{sender.getName(), mail.getIndex() + ""}));
            reply.setAttachments(attachments);
            mailer.getMailManager().sendNewMail(reply);

            // 受信者側に、拒否した該当メールの詳細画面を開く
            player.sendMessage("アイテムの受け取りを拒否し、送信者へ返送メールを送信しました。");
        }
        update();
    }

}
