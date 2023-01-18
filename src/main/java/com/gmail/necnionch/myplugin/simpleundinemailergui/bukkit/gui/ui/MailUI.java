package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks.MailWrapper;
import com.google.common.collect.Lists;
import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.Utility;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class MailUI {
    public static final int SLOT_SIZE = 9 * 6;
    private static final int UI_SIZE = 9 * 2;
    protected final Player player;
    private final Panel parent;
    protected final MailWrapper mailer = MailGUIPlugin.getWrapper();
    protected final MailSenderPlayer sender;
    protected int pageIndex;
    protected int maxPageIndex;
    private MailData selectedMail;

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

    @Nullable
    public abstract String getTitle();


    public abstract @Nullable List<MailData> getMails();

    public PanelItem createMailItem(MailData mail) {
        boolean selected = mail.equals(selectedMail);
        boolean unread = !mail.isRead(sender);
        Material material = selected ? Material.FILLED_MAP : (unread ? Material.MAP : Material.PAPER);
        ChatColor color = unread ? ChatColor.GOLD : ChatColor.GRAY;

        String readableDateLabel = mailer.formatDateOrReadable(mail.getDate());
        String dateLabel = mailer.formatDate(mail.getDate());
        if (!readableDateLabel.equalsIgnoreCase(dateLabel))
            dateLabel += " (" + readableDateLabel + ")";

        List<String> lines = Lists.newArrayList(
                color + "メール #" + mail.getIndex() + (mail.getAttachments().isEmpty() ? "" : "  (送付: " + mail.getAttachments().size() + ")"),
                ChatColor.RED + "送信者: " + ChatColor.WHITE + mail.getFrom().getName() + "  " + ChatColor.RED + "宛先: " + ChatColor.WHITE + mailer.joinToAndGroup(mail),
                ChatColor.RED + "送信日時: " + ChatColor.WHITE + dateLabel
        );
        if (unread)
            lines.set(0, color + "***  " + lines.get(0) + "  ***");

        if (!selected) {
            lines.add(ChatColor.RED + "メッセージ: " + ChatColor.GRAY + ChatColor.ITALIC + mailer.summarySubstring(mail.getMessage().get(0)));
        } else {
            lines.add(ChatColor.RED + "メッセージ: ");
            mail.getMessage().stream()
                    .map(Utility::replaceColorCode)
                    .map(s -> ChatColor.WHITE + "  " + s)
                    .forEachOrdered(lines::add);

            if (mail.isAttachmentsRefused()) {
                lines.add("");
                lines.add(ChatColor.RED + "添付アイテム:");
                lines.add(ChatColor.YELLOW + "  受信者により受取拒否されました");

                if (mail.getAttachmentsRefusedReason() != null) {
                    lines.add("");
                    lines.add(ChatColor.WHITE + "  " + mail.getAttachmentsRefusedReason());
                }

            } else if (mail.isAttachmentsCancelled()) {
                lines.add("");
                lines.add(ChatColor.RED + "添付アイテム:");
                lines.add(ChatColor.YELLOW + "  送信者によりキャンセルされました");

            } else if (!mail.getAttachments().isEmpty()) {
                lines.add("");
                lines.add(ChatColor.RED + "添付アイテム:");
                mail.getAttachments().stream()
                        .map(item -> ChatColor.WHITE + "  " + mailer.itemDesc(item, true))
                        .forEachOrdered(lines::add);

                if (mail.getCostMoney() > 0) {
                    lines.add(ChatColor.GOLD + "着払い料金: " + ChatColor.WHITE + mailer.formatCostMoney(mail.getCostMoney()));
                } else if (mail.getCostItem() != null) {
                    lines.add(ChatColor.GOLD + "着払いアイテム: " + ChatColor.WHITE + mailer.itemDesc(mail.getCostItem(), true));
                }
            }

            if (mail.getAttachmentsOriginal() != null && !mail.getAttachmentsOriginal().isEmpty() && mail.getFrom().equals(sender)) {
                // 添付アイテムオリジナルがあり、表示先が送信者なら、元の添付アイテムを表示する。
                lines.add("");
                lines.add(ChatColor.RED + "送信時の添付アイテム: ");
                mail.getAttachmentsOriginal().stream()
                        .map(item -> ChatColor.WHITE + "  " + mailer.itemDesc(item, true))
                        .forEachOrdered(lines::add);
            }
        }

        lines.add("");
        lines.add(ChatColor.YELLOW + (selected ? "(クリックで閉じる)" : "(クリックで開く)"));

        return PanelItem.createItem(material, lines.remove(0), lines).setClickListener((e, p) -> {
            if (mail.equals(selectedMail)) {
                selectedMail = null;
            } else {
                selectedMail = mail;
            }
            this.update();
        });
    }


    public void build(PanelItem[] slots) {
        for (int i = 0; i < 9; i++) {
            slots[9 + i] = PanelItem.createItem(Material.STONE_BUTTON, ChatColor.RESET.toString());
        }

        List<MailData> mails = getMails();
        if (mails == null) {
            pageIndex = 0;
            maxPageIndex = -1;

        } else {
            int mailSlotCount = SLOT_SIZE - UI_SIZE;
            maxPageIndex = mails.size() / mailSlotCount;
            pageIndex = Math.max(0, Math.min(pageIndex, maxPageIndex));

            String pageLabel = ChatColor.GRAY + "(" + ChatColor.BOLD + (pageIndex+1) + "/" + (maxPageIndex+1) + ChatColor.GRAY + ")";
            slots[7] = PanelItem.createItem(
                    (pageIndex > 0) ? Material.LIGHT_BLUE_DYE : Material.GRAY_DYE,
                    ((pageIndex > 0) ? ChatColor.AQUA : ChatColor.GRAY + ChatColor.ITALIC.toString()) + "前のページへ " + pageLabel
            ).setClickListener((e, p) -> onBackButton());

            slots[8] = PanelItem.createItem(
                    (maxPageIndex > pageIndex) ? Material.ROSE_RED : Material.GRAY_DYE,
                    ((maxPageIndex > pageIndex) ? ChatColor.RED : ChatColor.GRAY + ChatColor.ITALIC.toString()) + "次のページへ " + pageLabel
            ).setClickListener((e, p) -> onNextButton());

            int selectedMailSlotIndex = -1;
            for (int i = 0; i < mailSlotCount && pageIndex * mailSlotCount + i < mails.size(); i++) {
                MailData mail = mails.get(pageIndex * mailSlotCount + i);
                slots[UI_SIZE + i] = createMailItem(mail);
                if (mail.equals(selectedMail))
                    selectedMailSlotIndex = UI_SIZE + i;
            }

            if (selectedMail != null && selectedMailSlotIndex != -1) {
                int rowIndex = selectedMailSlotIndex / 9;
                int colIndex = selectedMailSlotIndex % 9;
                int uiRowSize = UI_SIZE / 9;
                // 選択された行がスロット1行目以下ならメニューを選択された行の下の行にする
                int menuRowIndex = (rowIndex <= uiRowSize) ? rowIndex + 1 : rowIndex - 1;
                // UIスロットと選択スロット以外を空に
                for (int i = 0; i < SLOT_SIZE - UI_SIZE; i++) {
                    if (UI_SIZE + i != selectedMailSlotIndex) {
                        slots[UI_SIZE + i] = null;
                    }
                }

                PanelItem[] menu = createMailMenuItems(selectedMail);
                if (menu.length > 0) {
                    if (menu.length > 9)
                        throw new IllegalArgumentException("createMailMenuItems.length >= 9");
                    // メニューを埋める
                    int menuColIndex = Math.max(0, colIndex - menu.length + 1);
                    for (int i = 0; i < menu.length; i++) {
                        slots[menuRowIndex * 9 + menuColIndex + i] = menu[i];
                    }
                }
            }

        }
    }

    public void onBackButton() {
        pageIndex--;
        selectedMail = null;
        update();
    }

    public void onNextButton() {
        pageIndex++;
        selectedMail = null;
        update();
    }

    public void resetPageIndex() {
        pageIndex = 0;
        selectedMail = null;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getMaxPageIndex() {
        return maxPageIndex;
    }

    public @Nullable MailData getSelectedMail() {
        return selectedMail;
    }

    public abstract PanelItem[] createMailMenuItems(MailData mail);

}
