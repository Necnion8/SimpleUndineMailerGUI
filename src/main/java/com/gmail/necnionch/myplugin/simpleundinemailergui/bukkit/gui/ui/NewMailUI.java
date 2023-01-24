package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.PanelItem;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.MailPermission;
import com.google.common.collect.Lists;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.wesjd.anvilgui.AnvilGUI;
import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.Utility;
import org.bitbucket.ucchy.undine.group.SpecialGroupAllConnected;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class NewMailUI extends MailUI {

    private final MailGUIPlugin plugin = JavaPlugin.getPlugin(MailGUIPlugin.class);

    public NewMailUI(Player player, Panel parent) {
        super(player, parent);
    }

    @Override
    public ItemStack getIcon() {
        return PanelItem.createItem(Material.PAPER, ChatColor.GOLD + "メールを書く").getItemStack();
    }

    @Override
    public @Nullable String getTitle() {
        return "新規メール編集";
    }

    @Override
    public @Nullable List<MailData> getMails() {
        return null;
    }

    @Override
    public PanelItem[] createMailMenuItems(MailData mail) {
        return null;
    }

    @Override
    public int getMaxPageIndex() {
        return -1;
    }

    @Override
    public void build(PanelItem[] slots) {
        if (MailPermission.WRITE.cannot(player) || MailPermission.SEND.cannot(player))
            return;

        MailData mail = mailer.getMailManager().getEditmodeMail(sender);
        if (mail == null) {
            slots[22] = PanelItem.createItem(Material.MAP, ChatColor.GOLD + "新規メールを作成").setClickListener(() -> {
                mailer.getMailManager().makeEditmodeMail(sender);
                update();
            });
            return;
        }

        List<String> lore = Lists.newArrayList();
        if (!mail.getAttachments().isEmpty())
            lore.add(ChatColor.RED + "添付ボックスにアイテムが入っています");
        slots[8] = PanelItem.createItem(Material.BARRIER, ChatColor.RED + "編集を破棄", lore).setClickListener(() -> {
            if (mail.getAttachments().isEmpty()) {
                mailer.getMailManager().clearEditmodeMail(sender);
                update();
            }
        });

        lore = Lists.newArrayList();
        if (mail.getTo() != null) {
            for (MailSender to : mail.getTo())
                lore.add(ChatColor.GRAY + "  " + to.getName());
        }
        if (mail.getToGroups() != null) {
            for (String to : mail.getToGroups())
                lore.add(ChatColor.GRAY + "  @" + to);
        }
        slots[13] = PanelItem.createItem(Material.PLAYER_HEAD, ChatColor.GOLD + "宛先を選択", lore)
                .setClickListener(this::openToSelector);

        lore = mail.getMessage().stream().map(s -> ChatColor.GRAY + "  " + s).collect(Collectors.toList());
        slots[21] = PanelItem.createItem(Material.WRITABLE_BOOK, ChatColor.GOLD + "メッセージ編集", lore)
                .setClickListener(this::openContentEditor);

        if (!mailer.checkAttachSendMailPermission(player)) {
            lore = Lists.newArrayList(ChatColor.RED + "添付ボックスを開く権限がありません");
        } else if (mailer.isDisabledAttachmentWorld(sender)) {
            lore = Lists.newArrayList(ChatColor.RED + "現在のワールドでは添付ボックスを開けません");
        } else {
            lore = mail.getAttachments().stream().map(i -> ChatColor.GRAY + "  " + mailer.itemDesc(i, true)).collect(Collectors.toList());
        }
        slots[23] = PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "添付ボックス編集", lore)
                .setClickListener(this::openAttachBox);

        if (!mail.getAttachments().isEmpty() && mail.getCostItem() == null && mailer.getMailer().getUndineConfig().isEnableCODMoney()) {
            lore = Lists.newArrayList();
            if (mail.getCostMoney() > 0)
                lore.add(ChatColor.GRAY + "要求: " + mailer.formatCostMoney(mail.getCostMoney()));
            slots[24] = PanelItem.createItem(Material.GOLD_NUGGET, ChatColor.GOLD + "着払い金額の設定", lore)
                    .setClickListener(this::openCostMoneyEditor);
        }

        lore = createEditingMailPreview(mail);
        slots[31] = PanelItem.createItem(Material.ANVIL, ChatColor.GOLD + "メールを送信", lore)
                .setClickListener(this::sendMail);

    }


    private void openToEditor(@Nullable String title) {
        MailData mail = mailer.getMailManager().getEditmodeMail(sender);
        if (mail == null)
            return;
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title(Optional.ofNullable(title).orElse("宛先を入力"))
                .itemLeft(new ItemStack(Material.WRITABLE_BOOK))
                .text(mail.getTo().isEmpty() ? "" : mail.getTo().get(0).getName())
                .onComplete((comp) -> {
                    String name = comp.getText();
                    MailSender target = (name == null || name.isEmpty()) ? null : MailSender.getMailSenderFromString(name);

                    if (target == null || !target.isValidDestination())
                        return Collections.singletonList(AnvilGUI.ResponseAction.run(() -> openToEditor(ChatColor.RED + "宛先が見つかりませんでした")));

                    if (!mailer.getMailer().getUndineConfig().isEnableSendSelf() && sender.equals(target))
                        return Collections.singletonList(AnvilGUI.ResponseAction.run(() -> openToEditor(ChatColor.RED + "自分自身を指定できません")));

                    mail.setTo(0, target);
                    return Collections.singletonList(AnvilGUI.ResponseAction.run(this::open));
                })
                .open(player);
    }

    private void openToSelector() {
        if (Bukkit.getOnlinePlayers().size() > 1) {
            new ToSelectorPanel().open();
        } else {
            openToEditor(null);
        }
    }

    private void openContentEditor() {
        MailData mail = mailer.getMailManager().getEditmodeMail(sender);
        if (mail == null)
            return;
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("メッセージを入力")
                .text(mail.getMessage().get(0))
                .itemLeft(new ItemStack(Material.WRITABLE_BOOK))
                .onComplete((comp) -> {
                    mail.getMessage().set(0, comp.getText());
                    return Collections.singletonList(AnvilGUI.ResponseAction.run(this::open));
                })
                .open(player);
    }

    private void openAttachBox() {
        MailData mail = mailer.getMailManager().getEditmodeMail(sender);
        if (mail == null)
            return;
        if (!mailer.checkAttachSendMailPermission(player) || mailer.isDisabledAttachmentWorld(sender))
            return;
        mailer.openAttachmentInventory(sender, mail, this::open);
    }

    private void openCostMoneyEditor() {
        openCostMoneyEditor(null);
    }

    private void openCostMoneyEditor(@Nullable String title) {
        MailData mail = mailer.getMailManager().getEditmodeMail(sender);
        if (mail == null)
            return;
        if (mail.getAttachments().isEmpty() || mail.getCostItem() != null && !mailer.getMailer().getUndineConfig().isEnableCODMoney())
            return;
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title(Optional.ofNullable(title).orElse("金額を入力"))
                .itemLeft(new ItemStack(Material.WRITABLE_BOOK))
                .text(String.valueOf(mail.getCostMoney()).replaceFirst("\\.0$", ""))
                .onComplete((comp) -> {
                    double cost;
                    String input = comp.getText();
                    try {
                        cost = (input.isEmpty()) ? 0 : Double.parseDouble(input);
                    } catch (NumberFormatException e) {
                        return Collections.singletonList(AnvilGUI.ResponseAction.run(() ->
                                openCostMoneyEditor(ChatColor.RED + "数値が無効です")));
                    }
                    if (cost > 0) {
                        mail.setCostItem(null);
                        mail.setCostMoney(cost);
                    }
                    return Collections.singletonList(AnvilGUI.ResponseAction.run(this::open));
                })
                .open(player);
    }

    private void sendMail() {
        if (!MailGUIPlugin.getWrapper().available())
            return;
        MailData mail = mailer.getMailManager().getEditmodeMail(sender);
        if (mail == null) {
            update();
            return;
        }

        if (mail.getTo().isEmpty() && mail.getToGroups().isEmpty()) {
            player.sendMessage(ChatColor.RED + "宛先が設定されていません");
            return;
        }

        // check spam
        long gap = mailer.getGapWithSpamProtectionMilliSeconds(sender);
        if (gap > 0) {
            int remain = (int)(gap / 1000) + 1;
            player.sendMessage(ChatColor.RED + "連続してメールを送信することはできません。" + remain + "秒後に送信してください。");
            return;
        }

        // check attach limit
        int attachBoxUsageCount = mailer.getMailManager().getAttachBoxUsageCount(sender);
        int attachBoxMaxCount = mailer.getMailer().getUndineConfig().getMaxAttachmentBoxCount();
        if (!mail.getAttachments().isEmpty()
                && MailPermission.ATTACH_INFINITY.cannot(player)
                && attachBoxUsageCount >= attachBoxMaxCount) {
            player.sendMessage(ChatColor.RED + "あなたは現在 " + attachBoxUsageCount + "個の添付ボックスを使用しており、制限数 " + attachBoxMaxCount + "を超えているため、添付付きメールを送信することはできません。");
            return;
        }

        // 宛先にAllConnectedが含まれていて、PlayerCacheのロードが完了していない場合は、エラーを表示して終了
        if (mail.getToGroups().contains(SpecialGroupAllConnected.NAME)
                && !mailer.getMailer().isPlayerCacheLoaded()) {
            player.sendMessage(ChatColor.RED + "プレイヤーのキャッシュが完了していないため、AllConnected宛てメールが作成できません。しばらく待ってから送信してください。");
            return;
        }

        // 複数の宛先に、添付付きメールを送信しようとしたときの処理
        if ( mail.getAttachments().size() > 0
                && (mail.getTo().size() > 1 || mail.getToGroups().size() > 0) ) {
            player.sendMessage(ChatColor.RED + "添付アイテム付きのメールを、複数の宛先に出すことはできません。");
            return;
        }

        // 送信にお金がかかる場合
        double fee = mailer.getSendFee(mail);
        if (fee > 0) {
            player.spigot().sendMessage(new ComponentBuilder("メールの送信に課金が必要です。\n").color(ChatColor.RED)
                    .append("/umail send").event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/umail send"))
                    .append(" を実行して操作を続けてください。").create());
            return;
        }

        // 送信
        mailer.getMailManager().sendNewMail(mail);
        mailer.getMailManager().clearEditmodeMail(sender);
        mailer.getMailer().getBoxManager().clearEditmodeBox(player);

        player.sendMessage(ChatColor.GOLD + "メールを送信しました");
        update();
    }
    
    private List<String> createEditingMailPreview(MailData mail) {
        List<String> lines = Lists.newArrayList(
                ChatColor.RED + "送信者: " + ChatColor.WHITE + mail.getFrom().getName() + "  " + ChatColor.RED + "宛先: " + ChatColor.WHITE + mailer.joinToAndGroup(mail),
                ChatColor.RED + "メッセージ: "
        );
        mail.getMessage().stream()
                .map(Utility::replaceColorCode)
                .map(s -> ChatColor.WHITE + "  " + s)
                .forEachOrdered(lines::add);

        if (!mail.getAttachments().isEmpty()) {
            lines.add("");
            lines.add(ChatColor.RED + "添付アイテム:");
            mail.getAttachments().stream()
                    .map(item -> ChatColor.WHITE + "  " + mailer.itemDesc(item, true))
                    .forEachOrdered(lines::add);

            if (mail.getCostMoney() > 0) {
                lines.add("");
                lines.add(ChatColor.GOLD + "着払い料金: " + ChatColor.WHITE + mailer.formatCostMoney(mail.getCostMoney()));
            } else if (mail.getCostItem() != null) {
                lines.add("");
                lines.add(ChatColor.GOLD + "着払いアイテム: " + ChatColor.WHITE + mailer.itemDesc(mail.getCostItem(), true));
            }
        }
        return lines;
    }


    public final class ToSelectorPanel extends Panel {
        public ToSelectorPanel() {
            super(player, 9 * 6, ChatColor.DARK_AQUA + "宛先を選択してください", new ItemStack(Material.AIR));
        }

        @Override
        public PanelItem[] build() {
            PanelItem[] items = new PanelItem[getSize()];
            items[0] = PanelItem.createItem(Material.ANVIL, ChatColor.GOLD + "名前を入力")
                    .setClickListener(() -> openToEditor(null));

            List<Player> players = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !player.equals(p))
                    .collect(Collectors.toList());

            int slotSize = getSize() - 1;
            for (int i = 0; i < players.size() && i < slotSize; i++) {
                Player player = players.get(i);
                items[1 + i] = PanelItem.createBlankItem()
                        .setItemBuilder((p) -> {
                            ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
                            SkullMeta meta = Objects.requireNonNull((SkullMeta) itemStack.getItemMeta());
                            meta.setOwningPlayer(player);
                            itemStack.setItemMeta(meta);
                            return itemStack;
                        })
                        .setClickListener(() -> {
                            MailData mail = mailer.getMailManager().getEditmodeMail(sender);
                            if (mail != null) {
                                mail.addTo(MailSender.getMailSender(player));
                            }
                            NewMailUI.this.open();
                        });
            }
            return items;
        }
    }

}
