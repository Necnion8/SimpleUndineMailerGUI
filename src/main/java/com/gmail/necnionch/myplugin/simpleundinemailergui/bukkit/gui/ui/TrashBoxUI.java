package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.MailPermission;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.MailsResult;
import com.google.common.collect.Lists;
import org.bitbucket.ucchy.undine.MailData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TrashBoxUI extends MailUI {

    private @Nullable List<MailData> mails;

    public TrashBoxUI(Player player, Panel parent) {
        super(player, parent);
        loadMails();
    }

    @Override
    public ItemStack getIcon() {
        String name = ChatColor.GOLD + "ゴミ箱";
        if (mails != null)
            name += ChatColor.GRAY + " (" + mails.size() + ")";
        return PanelItem.createItem(Material.CACTUS, name).getItemStack();
    }

    @Override
    public @Nullable String getTitle() {
        return "ゴミ箱";
    }

    @Override
    public @Nullable List<MailData> getMails() {
        return mails;
    }

    @Override
    public void build(PanelItem[] slots) {
        loadMails();
        super.build(slots);

        if (mails == null || MailPermission.TRASH.cannot(player))
            return;

        int inbox = 0;
        int outbox = 0;
        for (MailData mail : mails) {
            if (mail.isAllMail()
                    || (mail.getToTotal() != null && mail.getToTotal().contains(sender))
                    || mail.getTo().contains(sender)) {
                inbox++;
            } else if (mail.getFrom().equals(sender)) {
                outbox++;
            }
        }

        List<String> lore = Lists.newArrayList(ChatColor.GRAY + "受信メール: " + inbox + "通");
        slots[3] = PanelItem.createItem(Material.DIAMOND_SHOVEL, ChatColor.AQUA + "受信メールを全てゴミ箱から戻す", lore)
                .setClickListener(this::onRestoreInboxAllButton);

        lore = Lists.newArrayList(ChatColor.GRAY + "送信メール: " + outbox + "通");
        slots[4] = PanelItem.createItem(Material.DIAMOND_SHOVEL, ChatColor.AQUA + "送信メールを全てゴミ箱から戻す", lore)
                .setClickListener(this::onRestoreOutboxAllButton);

    }

    @Override
    public PanelItem[] createMailMenuItems(MailData mail) {
        if (mail.isEditmode() || !mail.isRelatedWith(sender) || !mail.isSetTrash(sender))
            return new PanelItem[0];

        List<PanelItem> items = Lists.newArrayList();

        List<String> lore = Lists.newArrayList();
        if (MailPermission.TRASH.cannot(player))
            lore.add(ChatColor.RED + "ゴミ箱から戻す権限がありません");

        items.add(PanelItem.createItem(Material.DIAMOND_SHOVEL, ChatColor.GOLD + "ゴミ箱から戻す", lore)
                .setClickListener(() -> onRestoreButton(mail)));

        return items.toArray(new PanelItem[0]);
    }

    public void loadMails() {
        mails = mailer.getMailManager().getTrashboxMails(sender);
    }

    private void onRestoreButton(MailData mail) {
        if (!MailGUIPlugin.getWrapper().available())
            return;

        if (MailPermission.TRASH.cannot(player) || !mail.isRelatedWith(sender))
            return;

        mail.removeTrashFlag(sender);
        mailer.getMailManager().saveMail(mail);
        update();
    }

    private void onRestoreInboxAllButton() {
        if (!MailGUIPlugin.getWrapper().available())
            return;
        if (MailPermission.TRASH.cannot(player))
            return;

        MailsResult res = mailer.removeTrashFlagInboxMails(sender);
        int done = res.getAll().size() - res.getFails().size();

        if (done > 0) {
            player.sendMessage(ChatColor.GOLD + "受信メール " + done + "通 を復元しました");
            update();
        }
    }

    private void onRestoreOutboxAllButton() {
        if (!MailGUIPlugin.getWrapper().available())
            return;
        if (MailPermission.TRASH.cannot(player))
            return;

        MailsResult res = mailer.removeTrashFlagOutboxMails(sender);
        int done = res.getAll().size() - res.getFails().size();

        if (done > 0) {
            player.sendMessage(ChatColor.GOLD + "送信メール " + done + "通 を復元しました");
            update();
        }
    }

}
