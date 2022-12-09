package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import com.google.common.collect.Lists;
import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.MailManager;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.Utility;
import org.bitbucket.ucchy.undine.bridge.VaultEcoBridge;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class InboxUI extends MailUI {

    private static final int LIST_SIZE = 36;
    private final MailManager mail;
    private int unread;
    private int total;
    private int pageIndex;
    private int maxPageIndex;
    private int openedMailId = -1;

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
    public void onBackButton() {
        if (pageIndex > 0) {
            pageIndex--;
            this.update();
        }
    }

    @Override
    public void onNextButton() {
        if (pageIndex < maxPageIndex) {
            pageIndex++;
            this.update();
        }
    }

    @Override
    public int getCurrentPage() {
        return pageIndex + 1;
    }

    @Override
    public int getMaxPage() {
        return maxPageIndex + 1;
    }

    @Override
    public @Nullable String getTitle() {
        return "受信箱";
    }

    @Override
    public void build(PanelItem[] slots) {
        List<MailData> mails = loadInbox();
        for (int i = 0; i < LIST_SIZE && maxPageIndex * LIST_SIZE + i < mails.size(); i++) {
            MailData mail = mails.get(maxPageIndex * LIST_SIZE + i);
            boolean unread = !mail.isRead(sender);
            Material material = unread ? Material.MAP : Material.PAPER;
            ChatColor color = unread ? ChatColor.GOLD : ChatColor.GRAY;

            String toName = mailer.joinToAndGroup(mail);
            boolean open = openedMailId == mail.getIndex();
            List<String> lines = Lists.newArrayList(
                    color + "メール #" + mail.getIndex() + (mail.getAttachments().isEmpty() ? "" : " (*)"),
                    ChatColor.RED + "送信者: " + ChatColor.WHITE + mail.getFrom().getName(),
                    ChatColor.RED + "送信日時: " + ChatColor.WHITE + mailer.formatDate(mail.getDate())
            );

            if (open) {
                lines.set(1, lines.get(1) + ChatColor.RED + "  宛先: " + ChatColor.WHITE + toName);
                lines.add(ChatColor.RED + "メッセージ: ");
                for (String line : mail.getMessage()) {
                    lines.add(ChatColor.WHITE + "  " + Utility.replaceColorCode(line));
                }
            } else {
                lines.add(ChatColor.RED + "メッセージ: " + ChatColor.GRAY + ChatColor.ITALIC + mailer.summarySubstring(mail.getMessage().get(0)));
            }

            if (!mail.getAttachments().isEmpty()) {
                lines.add(ChatColor.RED + "送付アイテム: ");
                lines.add("");

                if (mail.isAttachmentsCancelled()) {
                    // cancelled
                } else {
                    // open window
                }

                mail.getAttachments().forEach(item -> {
                    lines.add("  " + ChatColor.WHITE + mailer.itemDesc(item, true));
                });

                if (mail.getCostMoney() > 0.0D || mail.getCostItem() != null) {
                    VaultEcoBridge eco = UndineMailer.getInstance().getVaultEco();
                    String costDesc = (eco != null) ? eco.format(mail.getCostMoney()) : mail.getCostMoney() + "";

                    if (mail.getCostMoney() > 0.0D) {
                        lines.add("  " + ChatColor.RED + "着払い料金: " + ChatColor.WHITE + costDesc);
                    } else {
                        lines.add("  " + ChatColor.RED + "着払いアイテム: " + ChatColor.WHITE + mailer.itemDesc(mail.getCostItem(), true));
                    }

                    if (mail.getTo().contains(sender)) {
                        // 自分宛だった場合に、受け取り拒否をするボタンを表示する
//                        msg.addText(" ");
//                        button = new MessageParts(Messages.get("MailDetailAttachmentBoxRefuse"), ChatColor.AQUA);
//                        button.setClickEvent(ClickEventType.SUGGEST_COMMAND, "/umail attach " + mail.getIndex() + " refuse ");
//                        button.setHoverText(Messages.get("MailDetailAttachmentBoxRefuseToolTip"));
//                        msg.addParts(button);
                    }
                }

            } else if (mail.isAttachmentsCancelled()) {
                if (mail.isAttachmentsRefused()) {
                    // cancelled and reason
                    if (mail.getAttachmentsRefusedReason() != null) {}
                } else {
                    // cancelled
                }
            }

            if (!open) {
                lines.add("");
                lines.add(ChatColor.YELLOW + "(クリックで開く)");
            }

            slots[i] = PanelItem.createItem(material, lines.remove(0), lines).setClickListener((e, p) -> {
                if (openedMailId != mail.getIndex()) {
                    openedMailId = mail.getIndex();
                    this.update();
                }
            });

        }

    }


    public List<MailData> loadInbox() {
        unread = 0;
        total = 0;
        maxPageIndex = 0;
        List<MailData> mails = mail.getInboxMails(sender);
        if (mails == null) {
            pageIndex = 0;
            return Collections.emptyList();
        }

        total = mails.size();
        for (MailData mail : mails) {
            if (!mail.isRead(sender))
                unread++;
        }

        maxPageIndex = total / LIST_SIZE;
        pageIndex = Math.max(0, Math.min(pageIndex, maxPageIndex));

        return mails;
    }

}
