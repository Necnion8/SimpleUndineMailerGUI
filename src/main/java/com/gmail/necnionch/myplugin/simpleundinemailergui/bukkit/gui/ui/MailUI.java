package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks.MailWrapper;
import com.google.common.collect.Lists;
import org.bitbucket.ucchy.undine.MailData;
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
    protected final Player player;
    private final Panel parent;
    protected final MailWrapper mailer = MailGUIPlugin.getWrapper();
    protected final MailSenderPlayer sender;
    protected int pageIndex;
    protected int maxPageIndex;

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
        boolean unread = !mail.isRead(sender);
        Material material = unread ? Material.MAP : Material.PAPER;
        ChatColor color = unread ? ChatColor.GOLD : ChatColor.GRAY;

        List<String> lines = Lists.newArrayList(
                color + "メール #" + mail.getIndex() + (mail.getAttachments().isEmpty() ? "" : " (*)"),
                ChatColor.RED + "送信者: " + ChatColor.WHITE + mail.getFrom().getName(),
                ChatColor.RED + "送信日時: " + ChatColor.WHITE + mailer.formatDate(mail.getDate()),
                ChatColor.RED + "メッセージ: " + ChatColor.GRAY + ChatColor.ITALIC + mailer.summarySubstring(mail.getMessage().get(0)),
                "",
                ChatColor.YELLOW + "(クリックで開く)"
        );

        return PanelItem.createItem(material, lines.remove(0), lines).setClickListener((e, p) -> {
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
            int mailSlotCount = SLOT_SIZE - 9 * 2;
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

            for (int i = 0; i < mailSlotCount && maxPageIndex * mailSlotCount + i < mails.size(); i++) {
                MailData mail = mails.get(maxPageIndex * mailSlotCount + i);
                slots[9 * 2] = createMailItem(mail);
            }
        }
    }

    public void onBackButton() {
        pageIndex--;
        update();
    }

    public void onNextButton() {
        pageIndex++;
        update();
    }

    public void resetPageIndex() {
        pageIndex = 0;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getMaxPageIndex() {
        return maxPageIndex;
    }
}
