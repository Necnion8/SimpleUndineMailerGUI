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

public class TrashBoxUI extends MailUI {

    private @Nullable List<MailData> mails;

    public TrashBoxUI(Player player, Panel parent) {
        super(player, parent);
    }

    @Override
    public ItemStack getIcon() {
        return PanelItem.createItem(Material.CACTUS, ChatColor.GOLD + "ゴミ箱").getItemStack();
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

}
