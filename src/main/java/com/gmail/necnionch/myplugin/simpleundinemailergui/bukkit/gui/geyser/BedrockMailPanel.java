package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.geyser;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.MainPanel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks.MailWrapper;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.*;
import com.google.common.collect.Lists;
import net.md_5.bungee.api.ChatColor;
import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineConfig;
import org.bitbucket.ucchy.undine.Utility;
import org.bitbucket.ucchy.undine.group.SpecialGroupAllConnected;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderConsole;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BedrockMailPanel {
    private final JavaPlugin owner = JavaPlugin.getProvidingPlugin(MailGUIPlugin.class);
    private final FloodgatePlayer player;
    private final Player bukkitPlayer;
    private final MailSender mailSender;
    private final MailWrapper mailer;

    public BedrockMailPanel(Player bukkitPlayer, FloodgatePlayer floodgatePlayer, MailSender mailSender, @Nullable MainPanel.UIType ui) {
        this.player = floodgatePlayer;
        this.bukkitPlayer = bukkitPlayer;
        this.mailSender = mailSender;
        this.mailer = MailGUIPlugin.getWrapper();

        if (MainPanel.UIType.NEW_MAIL.equals(ui)) {
            if (MailPermission.WRITE.can(bukkitPlayer) && MailPermission.SEND.can(bukkitPlayer)) {
                openNewMailPanel();
                return;
            }
        } else if (MainPanel.UIType.INBOX.equals(ui)) {
            if (MailPermission.INBOX.can(bukkitPlayer)) {
                openInboxPanel();
                return;
            }
        } else if (MainPanel.UIType.OUTBOX.equals(ui)) {
            if (MailPermission.OUTBOX.can(bukkitPlayer)) {
                openOutboxPanel();
                return;
            }
        } else if (MainPanel.UIType.TRASH_BOX.equals(ui)) {
            if (MailPermission.TRASH.can(bukkitPlayer)) {
                openTrashPanel();
                return;
            }
        }
        openMainPanel();
    }

    public static BedrockMailPanel open(Player bukkitPlayer, FloodgatePlayer floodgatePlayer, MailSender mailSender, @Nullable MainPanel.UIType ui) {
        return new BedrockMailPanel(bukkitPlayer, floodgatePlayer, mailSender, ui);
    }

    public static BedrockMailPanel open(FloodgatePlayer floodgatePlayer, MailSender mailSender, @Nullable MainPanel.UIType ui) {
        return new BedrockMailPanel(mailSender.getPlayer(), floodgatePlayer, mailSender, ui);
    }

    // list

    private void openMainPanel() {
        if (checkMailerLoadingWithPrompt(null))
            return;

        List<MailData> inMails = mailer.getMailManager().getInboxMails(mailSender);
        List<MailData> outMails = mailer.getMailManager().getOutboxMails(mailSender);
        List<MailData> trashMails = mailer.getMailManager().getTrashboxMails(mailSender);

        long unread = inMails.stream()
                .filter(mail -> !mail.isRead(mailSender))
                .count();

        SimpleButtonForm form = SimpleButtonForm.builder(owner).title("メールメニュー");
        boolean activePermission = false;

        if (MailPermission.WRITE.can(bukkitPlayer) && MailPermission.SEND.can(bukkitPlayer)) {
            activePermission = true;
            form.button("メールを書く", this::openNewMailPanel);

            if (checkAttachSendMailPermission(bukkitPlayer) && checkAttachmentWorldAccess())
                form.button("送付するアイテムを選ぶ", this::openNewMailPanelWithAttachBox);
        }

        if (MailPermission.READ.can(bukkitPlayer)) {
            if (MailPermission.INBOX.can(bukkitPlayer)) {
                form.button(StrGen.builder()
                        .text("受信箱 (" + inMails.size() + ")")
                        .text(StrGen.builder(() -> unread > 0)
                                .text(ChatColor.DARK_RED).text(ChatColor.BOLD).text("\n(未読 " + unread + "通)"))
                        .toString(), this::openInboxPanel);
                activePermission = true;
            }

            if (MailPermission.OUTBOX.can(bukkitPlayer)) {
                form.button("送信箱 (" + outMails.size() + ")", this::openOutboxPanel);
                activePermission = true;
            }

            if (MailPermission.TRASH.can(bukkitPlayer)) {
                form.button("ゴミ箱 (" + trashMails.size() + ")", this::openTrashPanel);
                activePermission = true;
            }
        }

        if (!activePermission)
            form.content(ChatColor.RED + "メールを見る権限がありません");

        player.sendForm(form.build());
    }

    private void openInboxPanel() {
        if (checkMailerLoadingWithPrompt(this::openInboxPanel))
            return;

        List<MailData> mails = Lists.newArrayList(mailer.getMailManager().getInboxMails(mailSender));

        SimpleButtonForm b = SimpleButtonForm.builder(owner)
                .title("受信箱  " + mails.size() + "件")
                .button("メニューに戻る", this::openMainPanel)
                .button("メール管理", this::openInboxManagePanel);

        for (MailData mail : mails) {
            boolean unread = !mail.isRead(mailSender);
            b.button(StrGen.builder()
                    .join(() -> unread, ChatColor.DARK_RED + "***  ")
                    .text("#" + mail.getIndex())
                    .text("  送信日時: " + mailer.formatDateOrReadable(mail.getDate()))
                    .join(() -> !mail.getAttachments().isEmpty(), "  (添付: " + mail.getAttachments().size() + ")")
                    .join(() -> unread, "  ***")
                    .text("\n")
                    .text(mail.getFrom().getName())
                    .text(" : ").text(mailer.formatContentSummary(mail))
                    .toString(),
                    () -> openViewPanel(mail));
        }

        player.sendForm(b.build());
    }

    private void openOutboxPanel() {
        if (checkMailerLoadingWithPrompt(this::openOutboxPanel))
            return;

        List<MailData> mails = Lists.newArrayList(mailer.getMailManager().getOutboxMails(mailSender));
        SimpleButtonForm b = SimpleButtonForm.builder(owner)
                .title("送信箱  " + mails.size() + "件")
                .button("メニューに戻る", this::openMainPanel)
                .button("メール管理", this::openOutboxManagePanel);

        for (MailData mail : mails) {
            b.button(StrGen.builder()
                            .text("#" + mail.getIndex())
                            .text("  送信日時: " + mailer.formatDateOrReadable(mail.getDate()))
                            .join(() -> !mail.getAttachments().isEmpty(), "  (添付: " + mail.getAttachments().size() + ")")
                            .text("\n")
                            .text(mail.getFrom().getName())
                            .text(" : ").text(mailer.formatContentSummary(mail))
                            .toString(),
                    () -> openViewPanel(mail));
        }

        player.sendForm(b.build());
    }

    private void openTrashPanel() {
        if (checkMailerLoadingWithPrompt(this::openTrashPanel))
            return;

        List<MailData> mails = mailer.getMailManager().getTrashboxMails(mailSender);

        SimpleButtonForm b = SimpleButtonForm.builder(owner)
                .title("ゴミ箱  " + mails.size() + "件")
                .button("メニューに戻る", this::openMainPanel)
                .button("メール管理", this::openTrashManagePanel);

        for (MailData mail : mails) {
            b.button(StrGen.builder()
                            .text("#" + mail.getIndex())
                            .text("  送信日時: " + mailer.formatDateOrReadable(mail.getDate()))
                            .text("\n")
                            .text(mail.getFrom().getName())
                            .text(" : ").text(mailer.formatContentSummary(mail))
                            .toString(),
                    () -> openViewPanel(mail)
            );
        }

        player.sendForm(b.build());
    }

    private void openNewMailPanel() {
        if (checkMailerLoadingWithPrompt(null))
            return;

        MailData mail = mailer.getMailManager().makeEditmodeMail(mailSender);
        UndineConfig config = mailer.getMailer().getUndineConfig();

        while (mail.getMessage().size() < 3)  // MailManager.MESSAGE_ADD_SIZE
            mail.addMessage("");

        CustomForm.Builder f = CustomForm.builder();
        f.title("新規メール");

        String toName = mailer.joinToAndGroup(mail);
        StrGen content = StrGen.builder()
                .text(ChatColor.RED).text("送信者: ").text(ChatColor.WHITE).text(mail.getFrom().getName() + "\n")
                .text(ChatColor.RED).text("宛先: ").text(ChatColor.WHITE).text(toName).text("\n");

        if (!mail.getAttachments().isEmpty()) {
            content.text(ChatColor.RED).text("添付アイテム: ").text("\n");
            mail.getAttachments().forEach(item ->
                    content.text(ChatColor.WHITE + "  " + mailer.itemDesc(item, true) + "\n"));

            if (mail.getCostMoney() > 0) {
                String costDesc = mailer.formatCostMoney(mail.getCostMoney());
                content.text(ChatColor.GOLD + "着払い料金: ").text(ChatColor.WHITE + costDesc);
                content.text("\n");
            } else if (mail.getCostItem() != null) {
                content.text(ChatColor.GOLD + "着払いアイテム: ").text(ChatColor.WHITE + mailer.itemDesc(mail.getCostItem(), true));
                content.text("\n");
            }
        }
        f.label(content.toString());

        List<Player> players = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(bukkitPlayer))
                .collect(Collectors.toList());

        boolean setTo = mail.getTo().isEmpty() && mail.getToGroups().isEmpty();
        if (setTo) {
            if (players.isEmpty()) {
                f.input("宛先", "");
            } else {
                f.dropdown("宛先", players.stream().map(Player::getName).collect(Collectors.toList()));
            }
        }

        f.input("見出し文", "", mail.getMessage().get(0));

        final boolean setCost;
        if (!mail.getAttachments().isEmpty() && config.isEnableCODMoney() && mail.getCostItem() == null) {
            f.input("着払い金額", "", mail.getCostMoney() + "");
            setCost = true;
        } else {
            setCost = false;
        }

        player.sendForm(f.validResultHandler(r -> {
            if (checkMailerLoadingWithPrompt(this::openNewMailPanel))
                return;

            SimpleButtonForm f2 = SimpleButtonForm.builder(owner);

            if (setTo) {
                MailSender target;
                if (players.isEmpty()) {
                    String name = r.asInput();
                    target = (name == null || name.isEmpty()) ? null : MailSender.getMailSenderFromString(name);
                } else {
                    target = MailSenderPlayer.getMailSender(players.get(r.asDropdown()));
                }

                if (target == null || !target.isValidDestination()) {
                    f2.content(ChatColor.RED + "宛先が見つかりませんでした");
                    f2.button("やり直す", this::openNewMailPanel);
                    player.sendForm(f2);
                    return;
                } else if (!config.isEnableSendSelf() && mailSender.equals(target)) {
                    f2.content(ChatColor.RED + "自分自身にメールを送信することはできません");
                    f2.button("やり直す", this::openNewMailPanel);
                    player.sendForm(f2);
                    return;
                }

                mail.addTo(target);
            }

            String line = r.asInput();
            if (line != null && !line.isEmpty())
                mail.getMessage().set(0, line);

            if (setCost) {
                String val = Optional.ofNullable(r.asInput()).orElse("");
                double cost;
                if (val.isEmpty()) {
                    cost = 0;
                } else {
                    try {
                        cost = Double.parseDouble(val);
                    } catch (NumberFormatException e) {
                        f2.content(ChatColor.RED + "着払い金額に指定されている数値が無効です");
                        f2.button("やり直す", this::openNewMailPanel);
                        player.sendForm(f2);
                        return;
                    }
                }

                if (cost > 0) {
                    mail.setCostItem(null);
                    mail.setCostMoney(cost);
                }
            }

            if (mail.getTo().isEmpty() && mail.getToGroups().isEmpty()) {
                f2.content(ChatColor.RED + "宛先が設定されていません");
                f2.button("やり直す", this::openNewMailPanel);
                player.sendForm(f2);
                return;
            }

            // check spam
            long gap = mailer.getGapWithSpamProtectionMilliSeconds(mailSender);
            if (gap > 0) {
                int remain = (int)(gap / 1000) + 1;
                f2.content(ChatColor.RED + "連続してメールを送信することはできません。" + remain + "秒後に送信してください。");
                f2.button("やり直す", this::openNewMailPanel);
                player.sendForm(f2);
                return;
            }

            // check attach limit
            int attachBoxUsageCount = mailer.getMailManager().getAttachBoxUsageCount(mailSender);
            int attachBoxMaxCount = config.getMaxAttachmentBoxCount();
            if (!mail.getAttachments().isEmpty()
                    && MailPermission.ATTACH_INFINITY.cannot(bukkitPlayer)
                    && attachBoxUsageCount >= attachBoxMaxCount) {
                f2.content(ChatColor.RED + "あなたは現在 " + attachBoxUsageCount + "個の添付ボックスを使用しており、制限数 " + attachBoxMaxCount + "を超えているため、添付付きメールを送信することはできません。");
                f2.button("添付ボックスを開く", () -> {
                    if (checkDeniedAttachmentWorldPrompt("閉じる", () -> {}))
                        return;
                    openAttachmentInventory(mail, this::openNewMailPanel);
                });
                player.sendForm(f2);
                return;
            }

            // 宛先にAllConnectedが含まれていて、PlayerCacheのロードが完了していない場合は、エラーを表示して終了
            if (mail.getToGroups().contains(SpecialGroupAllConnected.NAME)
                    && !mailer.getMailer().isPlayerCacheLoaded()) {
                f2.content(ChatColor.RED + "プレイヤーのキャッシュが完了していないため、AllConnected宛てメールが作成できません。しばらく待ってから送信してください。");
                f2.button("やり直す", this::openNewMailPanel);
                player.sendForm(f2);
                return;
            }

            // 複数の宛先に、添付付きメールを送信しようとしたときの処理
            if ( mail.getAttachments().size() > 0
                    && (mail.getTo().size() > 1 || mail.getToGroups().size() > 0) ) {
                f2.content(ChatColor.RED + "添付アイテム付きのメールを、複数の宛先に出すことはできません。");
                f2.button("やり直す", this::openNewMailPanel);
                f2.button("添付ボックスを開く", () -> {
                    if (checkDeniedAttachmentWorldPrompt("閉じる", () -> {}))
                        return;
                    openAttachmentInventory(mail, this::openNewMailPanel);
                });
                player.sendForm(f2);
                return;
            }

            // 送信にお金がかかる場合
            double fee = mailer.getSendFee(mail);
            if ( (mailSender instanceof MailSenderPlayer) && fee > 0 ) {
                f2.content(ChatColor.RED + "メールの送信に課金が必要です。\n/umail send を実行して操作を続けてください。");
                player.sendForm(f2);
                return;
            }

            // 送信
            mailer.getMailManager().sendNewMail(mail);
            mailer.getMailManager().clearEditmodeMail(mailSender);
            mailer.getMailer().getBoxManager().clearEditmodeBox(bukkitPlayer);

            f2.content("メールを送信しました");
            f2.button("送信メールを見る", () -> openViewPanel(mail));
            f2.button("送信箱を見る", this::openOutboxPanel);
            player.sendForm(f2);
        }));
    }

    private void openNewMailPanelWithAttachBox() {
        if (checkMailerLoadingWithPrompt(null))
            return;

        MailData mail = mailer.getMailManager().makeEditmodeMail(mailSender);
        openAttachmentInventory(mail, () ->
                Bukkit.getScheduler().runTaskLater(owner, this::openNewMailPanel, 1));
    }

    // action

    private void openInboxActionPanel(MailData mail) {
        if (checkMailerLoadingWithPrompt(() -> openInboxActionPanel(mail)))
            return;

        String panelTitle = "メール #" + mail.getIndex();
        SimpleButtonForm b = SimpleButtonForm.builder(JavaPlugin.getProvidingPlugin(MailGUIPlugin.class))
                .title(panelTitle)
                .button("メール画面に戻る", () -> openViewPanel(mail));

        if (mail.isEditmode() || !mail.isRecipient(mailSender) || !mailer.getMailManager().getInboxMails(mailSender).contains(mail)) {
            player.sendForm(b.build());
            return;
        }

        if (mail.getAttachments().isEmpty() && MailPermission.TRASH.can(bukkitPlayer)) {
            b.button("ゴミ箱に移動する", () -> {
                SimpleButtonForm form = SimpleButtonForm.builder(owner)
                        .title(panelTitle);

                if (checkMailerLoadingWithPrompt(null))
                    return;

                if (!MailPermission.TRASH.can(bukkitPlayer)) {
                    form.content(ChatColor.RED + "ゴミ箱に移動する権限がありません");
                    form.button("メール画面に戻る", () -> openViewPanel(mail));
                } else if (!mail.isRelatedWith(mailSender)) {
                    form.content(ChatColor.RED + "指定されたメールはあなた宛ではないので表示できません");
                    form.button("メール画面に戻る", () -> openViewPanel(mail));
                } else if (!mail.isRead(mailSender)) {
                    form.content(ChatColor.RED + "未読メールのためゴミ箱に移動できません");
                    form.button("メール画面に戻る", () -> openViewPanel(mail));
                } else {
                    form.content("ゴミ箱に移動しました");
                    form.button("メール画面に戻る", () -> openViewPanel(mail));
                    if (MailPermission.INBOX.can(bukkitPlayer))
                        form.button("受信箱に戻る", this::openInboxPanel);
                    if (MailPermission.TRASH.can(bukkitPlayer))
                        form.button("ゴミ箱に戻る", this::openTrashPanel);
                    mail.setTrashFlag(mailSender);
                    mailer.getMailManager().saveMail(mail);
                }
                player.sendForm(form.build());
            });
        }

        if (mail.isAttachmentsRefused()) {
            b.content(ChatColor.RED + "受信者により添付が受取拒否されました");
            player.sendForm(b.build());
            return;
        } else if (mail.isAttachmentsCancelled()) {
            b.content(ChatColor.RED + "送信者により添付がキャンセルされました");
            player.sendForm(b.build());
            return;
        } else if (mail.getAttachments().isEmpty()) {
            player.sendForm(b.build());
            return;
        }

        boolean refuseButton = true;

        if (!checkAttachInboxPermission(bukkitPlayer)) {
            refuseButton = false;
            b.content(ChatColor.RED + "添付ボックスを開く権限がありません");

        } else if (mail.getCostMoney() > 0) {
            String costDesc = mailer.formatCostMoney(mail.getCostMoney());
            boolean hasMoney = mailer.checkCostMoney(mailSender, mail);
            b.button(
                    StrGen.builder()
                            .text("お金を支払う\n")
                            .text(StrGen.builder()
                                    .text(hasMoney ? "" : ChatColor.DARK_RED.toString())
                                    .text("必要: " + ChatColor.BOLD + costDesc))
                            .toString(),
                    () -> {
                        if (checkMailerLoadingWithPrompt(null)
                                || checkDeniedAttachInboxPermissionPrompt(bukkitPlayer, "メール画面に戻る", () -> openViewPanel(mail))
                                || checkDeniedAttachmentWorldPrompt("メール画面に戻る", () -> openViewPanel(mail)))
                            return;

                        if (mailer.tryAcceptCostMoney(mailSender, mail)) {
                            openAttachmentInventory(mail, null);
                        } else {
                            player.sendForm(SimpleButtonForm.builder(owner)
                                    .title(panelTitle)
                                    .content(ChatColor.RED + "必要なお金が足りません！\n" + ChatColor.WHITE + "要求: " + ChatColor.GOLD + ChatColor.BOLD + costDesc)
                                    .button("メール画面に戻る", () -> openViewPanel(mail))
                                    .build());
                        }
                    });

        } else if (mail.getCostItem() != null) {
            String costDesc = mailer.itemDesc(mail.getCostItem(), true);
            boolean hasItem = mailer.checkCostItem(bukkitPlayer, mailSender, mail);
            b.button(
                    StrGen.builder()
                            .text("商品を支払う\n")
                            .text(StrGen.builder()
                                    .text(hasItem ? "" : ChatColor.DARK_RED.toString())
                                    .text("必要: " + ChatColor.BOLD + costDesc))
                            .toString(),
                    () -> {
                        if (checkMailerLoadingWithPrompt(null)
                                || checkDeniedAttachInboxPermissionPrompt(bukkitPlayer, "メール画面に戻る", () -> openViewPanel(mail))
                                || checkDeniedAttachmentWorldPrompt("メール画面に戻る", () -> openViewPanel(mail)))
                            return;

                        if (mailer.tryAcceptCostItem(bukkitPlayer, mailSender, mail)) {
                            openAttachmentInventory(mail, null);
                        } else {
                            player.sendForm(SimpleButtonForm.builder(owner)
                                    .title(panelTitle)
                                    .content(ChatColor.RED + "必要なアイテムが足りません！\n" + ChatColor.WHITE + "要求: " + ChatColor.GOLD + ChatColor.BOLD + costDesc)
                                    .button("メール画面に戻る", () -> openViewPanel(mail))
                                    .build());
                        }
                    });

        } else {
            b.button("添付ボックスを開く", () -> {
                if (checkDeniedAttachInboxPermissionPrompt(bukkitPlayer, "メール画面に戻る", () -> openViewPanel(mail))
                        || checkDeniedAttachmentWorldPrompt("メール画面に戻る", () -> openViewPanel(mail)))
                    return;

                openAttachmentInventory(mail, null);
            });
            refuseButton = false;
        }

        if (refuseButton) {
            b.button("受け取りを拒否する", () -> {
                SimpleButtonForm form = SimpleButtonForm.builder(owner)
                        .title(panelTitle);
                if (checkMailerLoadingWithPrompt(null))
                    return;

                if (mail.isAttachmentsCancelled()) {
                    form.content(ChatColor.RED + "既に添付アイテムはキャンセルされています！");
                } else if (!mail.getToTotal().contains(mailSender)) {
                    form.content(ChatColor.RED + "指定されたメールの受信者ではないので、受取拒否できません。");
                } else if (mail.isAttachmentsOpened()) {
                    form.content(ChatColor.RED + "既に受信者がボックスを開いたため、キャンセルできません。");

                } else {
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
                            new String[]{mailSender.getName(), mail.getIndex() + ""}));
                    reply.setAttachments(attachments);
                    mailer.getMailManager().sendNewMail(reply);

                    // 受信者側に、拒否した該当メールの詳細画面を開く
                    form.content("アイテムの受け取りを拒否し、送信者へ返送メールを送信しました。");
                }
                player.sendForm(form.button("メール画面に戻る", () -> openViewPanel(mail)));
            });
        }

        player.sendForm(b.build());
    }

    private void openOutboxActionPanel(MailData mail) {
        if (checkMailerLoadingWithPrompt(() -> openOutboxActionPanel(mail)))
            return;

        String panelTitle = "メール #" + mail.getIndex();
        SimpleButtonForm b = SimpleButtonForm.builder(JavaPlugin.getProvidingPlugin(MailGUIPlugin.class))
                .title(panelTitle)
                .button("メール画面に戻る", () -> openViewPanel(mail));

        if (mail.isEditmode() || !mail.getFrom().equals(mailSender) || !mailer.getMailManager().getOutboxMails(mailSender).contains(mail)) {
            player.sendForm(b.build());
            return;
        }

        if (!mail.getAttachments().isEmpty()) {
            if (mail.isAttachmentsCancelled()) {
                if (!checkAttachInboxPermission(bukkitPlayer)) {
                    b.content(ChatColor.RED + "添付ボックスを開く権限がありません");
                } else {
                    b.button("添付ボックスを開く", () -> {
                        if (checkMailerLoadingWithPrompt(null)
                                || checkDeniedAttachInboxPermissionPrompt(bukkitPlayer, "メール画面に戻る", () -> openViewPanel(mail))
                                || checkDeniedAttachmentWorldPrompt("メール画面に戻る", () -> openViewPanel(mail)))
                            return;

                        openAttachmentInventory(mail, null);
                    });
                }

            } else if (!mail.isAttachmentsOpened()) {
                if (!checkAttachInboxPermission(bukkitPlayer)) {
                    b.content(ChatColor.RED + "添付ボックスを開く権限がありません");
                } else {
                    b.button("添付アイテムをキャンセルする", () -> {
                        SimpleButtonForm form = SimpleButtonForm.builder(owner).title(panelTitle);

                        if (checkMailerLoadingWithPrompt(null))
                            return;

                        if (mail.isAttachmentsCancelled()) {
                            form.content(ChatColor.RED + "既に添付アイテムはキャンセルされています");
                            form.button("メール画面に戻る", () -> openViewPanel(mail));
                        } else if (mail.isAttachmentsOpened()) {
                            form.content(ChatColor.RED + "既に受信者がボックスを開いたため、キャンセルできません");
                            form.button("メール画面に戻る", () -> openViewPanel(mail));
                        } else {
                            if (checkDeniedAttachInboxPermissionPrompt(bukkitPlayer, "メール画面に戻る", () -> openViewPanel(mail))
                                    || checkDeniedAttachmentWorldPrompt("メール画面に戻る", () -> openViewPanel(mail)))
                                return;

                            mail.cancelAttachments();
                            mailer.getMailManager().saveMail(mail);
                            openAttachmentInventory(mail, null);

                            // 受信者側にメッセージを表示する
                            String message = Messages.get(
                                    "InformationAttachWasCanceledBySender",
                                    new String[]{"%num", "%sender"},
                                    new String[]{mail.getIndex() + "", player.getUsername()});
                            mail.getToTotal().stream()
                                    .filter(MailSender::isOnline)
                                    .forEach(to -> to.sendMessage(message));
                            return;
                        }
                        player.sendForm(form.build());
                    });
                }
            }

        } else if (MailPermission.TRASH.can(bukkitPlayer)) {
            b.button("ゴミ箱に移動する", () -> {
                SimpleButtonForm form = SimpleButtonForm.builder(owner)
                        .title(panelTitle);
                if (checkMailerLoadingWithPrompt(null))
                    return;

                if (!MailPermission.TRASH.can(bukkitPlayer)) {
                    form.content(ChatColor.RED + "ゴミ箱に移動する権限がありません");
                    form.button("メール画面に戻る", () -> openViewPanel(mail));
                } else if (!mail.isRelatedWith(mailSender)) {
                    form.content(ChatColor.RED + "指定されたメールはあなた宛ではないので表示できません");
                    form.button("メール画面に戻る", () -> openViewPanel(mail));
                } else if (!mail.isRead(mailSender)) {
                    form.content(ChatColor.RED + "未読メールのためゴミ箱に移動できません");
                    form.button("メール画面に戻る", () -> openViewPanel(mail));
                } else {
                    form.content("ゴミ箱に移動しました");
                    form.button("メール画面に戻る", () -> openViewPanel(mail));
                    if (MailPermission.OUTBOX.can(bukkitPlayer))
                        form.button("送信箱に戻る", this::openOutboxPanel);
                    if (MailPermission.TRASH.can(bukkitPlayer))
                        form.button("ゴミ箱に戻る", this::openTrashPanel);
                    mail.setTrashFlag(mailSender);
                    mailer.getMailManager().saveMail(mail);
                }
                player.sendForm(form.build());
            });
        }
        player.sendForm(b.build());
    }

    private void openTrashActionPanel(MailData mail) {
        if (checkMailerLoadingWithPrompt(() -> openTrashActionPanel(mail)))
            return;

        String panelTitle = "メール #" + mail.getIndex();
        SimpleButtonForm b = SimpleButtonForm.builder(JavaPlugin.getProvidingPlugin(MailGUIPlugin.class))
                .title(panelTitle)
                .button("メール画面に戻る", () -> openViewPanel(mail));

        if (mail.isEditmode() || !mail.isRelatedWith(mailSender) || !mail.isSetTrash(mailSender) || !MailPermission.TRASH.can(bukkitPlayer)) {
            player.sendForm(b.build());
            return;
        }

        b.button("ゴミ箱から戻す", () -> {
            SimpleButtonForm form = SimpleButtonForm.builder(owner)
                    .title(panelTitle);
            if (checkMailerLoadingWithPrompt(null))
                return;

            if (!MailPermission.TRASH.can(bukkitPlayer)) {
                form.content(ChatColor.RED + "ゴミ箱から戻す権限がありません");
                form.button("メール画面に戻る", () -> openViewPanel(mail));
            } else if (!mail.isRelatedWith(mailSender)) {
                form.content(ChatColor.RED + "指定されたメールはあなた宛ではないので表示できません");
                form.button("メール画面に戻る", () -> openViewPanel(mail));
            } else {
                mail.removeTrashFlag(mailSender);
                mailer.getMailManager().saveMail(mail);
                form.content("ゴミ箱から戻しました");
                form.button("メール画面に戻る", () -> openViewPanel(mail));
                if (mailer.getMailManager().getInboxMails(mailSender).contains(mail) && MailPermission.INBOX.can(bukkitPlayer))
                    form.button("受信箱に戻る", this::openInboxPanel);
                if (mailer.getMailManager().getOutboxMails(mailSender).contains(mail) && MailPermission.OUTBOX.can(bukkitPlayer))
                    form.button("送信箱に戻る", this::openOutboxPanel);
                if (MailPermission.TRASH.can(bukkitPlayer)) {
                    form.button("ゴミ箱に戻る", this::openTrashPanel);
                } else {
                    form.button("メニューに戻る", this::openMainPanel);
                }
            }
            player.sendForm(form.build());
        });

        player.sendForm(b.build());
    }

    // manage

    private void openInboxManagePanel() {
        if (checkMailerLoadingWithPrompt(this::openInboxManagePanel))
            return;

        int readMails = 0;
        int trashMails = 0;
        int attachItems = 0;

        for (MailData mail : mailer.getMailManager().getInboxMails(mailSender)) {
            if (!mail.isRead(mailSender) && (mail.getAttachments().isEmpty() || mail.isAttachmentsCancelled()))
                readMails++;
            if (mail.isRead(mailSender) && mail.getAttachments().isEmpty())
                trashMails++;
            if (!mail.getAttachments().isEmpty() && !mail.isAttachmentsCancelled() && mail.getCostItem() == null && mail.getCostMoney() <= 0)
                attachItems += mail.getAttachments().stream().mapToInt(ItemStack::getAmount).sum();
        }

        SimpleButtonForm b = SimpleButtonForm.builder(owner)
                .title("受信メール管理")
                .button("受信箱に戻る", this::openInboxPanel);

        b.button("未読メールを全て既読にする\n未読メール: " + readMails + "通", () -> {
            if (checkMailerLoadingWithPrompt(this::openInboxManagePanel))
                return;

            MailsResult res = mailer.setReadFlagInboxMails(mailSender);
            int done = res.getAll().size() - res.getFails().size();

            SimpleButtonForm b2 = SimpleButtonForm.builder(owner)
                    .title("受信メール管理")
                    .button("メール管理", this::openInboxManagePanel);

            if (done > 0) {
                b2.content("受信メール " + done + "通 を既読にしました");
            } else if (res.getAll().isEmpty()) {
                b2.content(ChatColor.RED + "既読にできるメールがありませんでした");
            } else {
                b2.content(ChatColor.RED + "既読にできるメールがありませんでした\n添付アイテムがあるメールは既読にできません");
            }
            player.sendForm(b2.build());
        });

        if (MailPermission.TRASH.can(bukkitPlayer)) {
            b.button("既読メールを全てゴミ箱に移動する\n既読メール: " + trashMails + "通", () -> {
                if (checkMailerLoadingWithPrompt(this::openInboxManagePanel))
                    return;

                SimpleButtonForm form = SimpleButtonForm.builder(owner).title("受信メール管理");

                if (!MailPermission.TRASH.can(bukkitPlayer)) {
                    form.content(ChatColor.RED + "ゴミ箱に移動する権限がありません");

                } else {
                    MailsResult res = mailer.setTrashFlagInboxMails(mailSender);
                    int done = res.getAll().size() - res.getFails().size();

                    if (done <= 0) {
                         form.content(ChatColor.RED + "ゴミ箱に移動できるメールがありませんでした");
                    } else {
                        form.content("既読メール " + done + "通 をゴミ箱に移動しました");
                        form.button("ゴミ箱", this::openTrashPanel);
                    }
                }
                form.button("メール管理", this::openInboxManagePanel);
                player.sendForm(form.build());
            });
        }

        if (checkAttachInboxPermission(bukkitPlayer)) {
            b.button("添付アイテムを全て受け取る\n受け取り可能な添付アイテム: " + attachItems + "個", () -> {
                if (checkMailerLoadingWithPrompt(this::openInboxManagePanel))
                    return;
                if (checkDeniedAttachmentWorldPrompt("メール管理", this::openInboxManagePanel))
                    return;

                SimpleButtonForm form = SimpleButtonForm.builder(owner).title("受信メール管理");

                if (!checkAttachInboxPermission(bukkitPlayer)) {
                    form.content(ChatColor.RED + "添付ボックスを開く権限がありません");

                } else {
                    ItemMailsResult result = mailer.takeAllAttachmentsInboxMails(mailSender, bukkitPlayer.getInventory());
                    if (!result.getAll().isEmpty()) {
                        int doneItems = result.totalItemCount() - result.failItemCount();
                        int openMails = result.getAll().size() - result.getFails().size();

                        if (doneItems <= 0) {
                            form.content(ChatColor.RED + "添付されたアイテムを1個も受け取れませんでした。\n手持ちに空きがないか、受け取りに支払いが必要です。\n\n失敗したメール: " + result.getFails().size() + "通");
                        } else {
                            StringBuilder sb = new StringBuilder("添付されたアイテムを");
                            sb.append((result.failItemCount() <= 0) ? "全て" : "一部");
                            sb.append("受け取りました\n\n");
                            sb.append("受け取ったアイテム: ").append(doneItems).append("個\n");
                            sb.append("開封したメール: ").append(openMails).append("通\n");
                            if (result.failItemCount() > 0)
                                sb.append(ChatColor.RED).append("失敗したメール: ").append(result.getFails().size()).append("通");
                            form.content(sb.toString());
                        }
                    } else {
                        form.content(ChatColor.RED + "添付アイテムがあるメールがありません");
                    }
                }
                form.button("メール管理", this::openInboxManagePanel);
                player.sendForm(form.build());
            });
        }
        player.sendForm(b.build());
    }

    private void openOutboxManagePanel() {
        if (checkMailerLoadingWithPrompt(this::openOutboxManagePanel))
            return;

        long trashMails = mailer.getMailManager().getOutboxMails(mailSender).stream()
                .filter(mail -> mail.getAttachments().isEmpty())
                .count();

        SimpleButtonForm b = SimpleButtonForm.builder(owner)
                .title("送信メール管理")
                .button("送信箱に戻る", this::openOutboxPanel);

        if (MailPermission.TRASH.can(bukkitPlayer)) {
            b.button("送信メールを全てゴミ箱に移動する\n送信メール: " + trashMails + "通", () -> {
                if (checkMailerLoadingWithPrompt(this::openOutboxManagePanel))
                    return;

                SimpleButtonForm form = SimpleButtonForm.builder(owner).title("送信メール管理");

                if (!MailPermission.TRASH.can(bukkitPlayer)) {
                    form.content(ChatColor.RED + "ゴミ箱に移動する権限がありません");

                } else {
                    MailsResult res = mailer.setTrashFlagOutboxMails(mailSender);
                    int done = res.getAll().size() - res.getFails().size();

                    if (res.getAll().isEmpty()) {
                        form.content(ChatColor.RED + "ゴミ箱に移動できるメールがありませんでした");
                    } else if (done <= 0) {
                        form.content(ChatColor.RED + "ゴミ箱に移動できるメールがありませんでした\n添付アイテムが残っているメールはゴミ箱に移動できません");
                    } else {
                        form.content("送信メール " + done + "通 をゴミ箱に移動しました");
                        form.button("ゴミ箱", this::openTrashPanel);
                    }
                }
                form.button("メール管理", this::openOutboxManagePanel);
                player.sendForm(form.build());
            });
        }

        player.sendForm(b.build());
    }

    private void openTrashManagePanel() {
        if (checkMailerLoadingWithPrompt(this::openTrashManagePanel))
            return;

        int inboxMails = 0;
        int outboxMails = 0;
        for (MailData mail : mailer.getMailManager().getTrashboxMails(mailSender)) {
            if (mail.isAllMail()
                    || (mail.getToTotal() != null && mail.getToTotal().contains(mailSender))
                    || mail.getTo().contains(mailSender))
                inboxMails++;
            else if (mail.getFrom().equals(mailSender))
                outboxMails++;
        }

        SimpleButtonForm b = SimpleButtonForm.builder(owner)
                .title("ゴミ箱 管理")
                .button("ゴミ箱に戻る", this::openTrashPanel);

        if (MailPermission.TRASH.can(bukkitPlayer)) {
            b.button("受信メールを全てゴミ箱から戻す\n受信メール: " + inboxMails + "通", () -> {
                if (checkMailerLoadingWithPrompt(this::openTrashManagePanel))
                    return;

                SimpleButtonForm form = SimpleButtonForm.builder(owner).title("ゴミ箱 管理");

                if (!MailPermission.TRASH.can(bukkitPlayer)) {
                    form.content(ChatColor.RED + "ゴミ箱から戻す権限がありません");

                } else {
                    MailsResult res = mailer.removeTrashFlagInboxMails(mailSender);
                    int done = res.getAll().size() - res.getFails().size();

                    if (done <= 0) {
                        form.content(ChatColor.RED + "ゴミ箱から戻せるメールがありませんでした");
                    } else {
                        form.content("受信メール " + done + "通 を復元しました");
                        form.button("受信箱", this::openInboxPanel);
                    }
                }
                form.button("メール管理", this::openTrashManagePanel);
                player.sendForm(form.build());
            });

            b.button("送信メールを全てゴミ箱から戻す\n送信メール: " + outboxMails + "通", () -> {
                if (checkMailerLoadingWithPrompt(this::openTrashManagePanel))
                    return;

                SimpleButtonForm form = SimpleButtonForm.builder(owner).title("ゴミ箱 管理");

                if (!MailPermission.TRASH.can(bukkitPlayer)) {
                    form.content(ChatColor.RED + "ゴミ箱から戻す権限がありません");

                } else {
                    MailsResult res = mailer.removeTrashFlagOutboxMails(mailSender);
                    int done = res.getAll().size() - res.getFails().size();

                    if (done <= 0) {
                        form.content(ChatColor.RED + "ゴミ箱から戻せるメールがありませんでした");
                    } else {
                        form.content("送信メール " + done + "通 を復元しました");
                        form.button("送信箱", this::openOutboxPanel);
                    }
                }
                form.button("メール管理", this::openTrashManagePanel);
                player.sendForm(form.build());
            });
        }

        player.sendForm(b.build());
    }

    // other

    private void openViewPanel(MailData mail) {
        if (checkMailerLoadingWithPrompt(() -> openViewPanel(mail)))
            return;

        String toName = mailer.joinToAndGroup(mail);
        StrGen content = StrGen.builder()
                .text(ChatColor.RED).text("送信者: ").text(ChatColor.WHITE).text(mail.getFrom().getName() + "  ")
                .text(ChatColor.RED).text("宛先: ").text(ChatColor.WHITE).text(toName)
                .text("\n")
                .text(ChatColor.RED).text("送信日時: ").text(ChatColor.WHITE).text(mailer.formatDate(mail.getDate()))
                .text("\n");

        if (mail.getMessage().stream().anyMatch(((Predicate<String>) String::isEmpty).negate())) {
            content .text(ChatColor.RED).text("メッセージ:\n  ")
                    .text(ChatColor.WHITE)
                    .text(mail.getMessage().stream()
                            .map(Utility::replaceColorCode)
                            .collect(Collectors.joining("\n"))
                            .replaceAll("\\n+$", "")
                            .replace("\n", "\n  ") + "\n");
        }
        content.text("\n");

        if (mail.isAttachmentsRefused()) {
            content.text(ChatColor.RED).text("添付アイテム: ").text("\n")
                    .text(ChatColor.YELLOW + "  受信者により受取拒否されました\n");

            if (mail.getAttachmentsRefusedReason() != null) {
                content.text("\n  ").text(ChatColor.WHITE).text(mail.getAttachmentsRefusedReason()).text("\n");
            }

        } else if (mail.isAttachmentsCancelled()) {
            content.text(ChatColor.RED).text("添付アイテム: ").text("\n")
                    .text(ChatColor.YELLOW + "  送信者によりキャンセルされました\n");

        } else if (!mail.getAttachments().isEmpty()) {
            content.text(ChatColor.RED).text("添付アイテム: ").text("\n");
            mail.getAttachments().forEach(item ->
                    content.text(ChatColor.WHITE + "  " + mailer.itemDesc(item, true) + "\n"));

            if (mail.getCostMoney() > 0) {
                String costDesc = mailer.formatCostMoney(mail.getCostMoney());
                content.text(ChatColor.GOLD + "着払い料金: ").text(ChatColor.WHITE + costDesc);
                content.text("\n");
            } else if (mail.getCostItem() != null) {
                content.text(ChatColor.GOLD + "着払いアイテム: ").text(ChatColor.WHITE + mailer.itemDesc(mail.getCostItem(), true));
                content.text("\n");
            }
        }

        if (mail.getAttachmentsOriginal() != null && !mail.getAttachmentsOriginal().isEmpty() && mail.getFrom().equals(mailSender)) {
            // 添付アイテムオリジナルがあり、表示先が送信者なら、元の添付アイテムを表示する。
            content.text("\n");
            content.text(ChatColor.RED).text("送信時の添付アイテム: ").text("\n");
            mail.getAttachmentsOriginal().forEach(item ->
                    content.text(ChatColor.WHITE + "  " + mailer.itemDesc(item, true) + "\n"));
        }

        ModalButtonForm b = ModalButtonForm.builder(owner)
                .title("メール #" + mail.getIndex())
                .content(content.toString());

        if (mailer.getMailManager().getTrashboxMails(mailSender).contains(mail)) {
            if (MailPermission.TRASH.can(bukkitPlayer)) {
                b.button1("ゴミ箱に戻る", this::openTrashPanel);
            } else {
                b.button1("メニューに戻る", this::openMainPanel);
            }
            b.button2("メール操作", () -> openTrashActionPanel(mail));
        } else if (mailer.getMailManager().getOutboxMails(mailSender).contains(mail)) {
            if (MailPermission.OUTBOX.can(bukkitPlayer)) {
                b.button1("送信箱に戻る", this::openOutboxPanel);
            } else {
                b.button1("メニューに戻る", this::openMainPanel);
            }
            b.button2("メール操作", () -> openOutboxActionPanel(mail));
        } else {
            if (MailPermission.INBOX.can(bukkitPlayer)) {
                b.button1("受信箱に戻る", this::openInboxPanel);
            } else {
                b.button1("メニューに戻る", this::openMainPanel);
            }
            b.button2("メール操作", () -> openInboxActionPanel(mail));
        }

        if (!mail.isRead(mailSender) && (mail.getAttachments().size() == 0 || mail.isAttachmentsCancelled())) {
            mail.setReadFlag(mailSender);
            mailer.getMailManager().saveMail(mail);
        }
        player.sendForm(b.build());
    }

    private void openAttachmentInventory(MailData mail, @Nullable Runnable close) {
        if (checkMailerLoadingWithPrompt(null))
            return;

        String openCommand = "umail attach " + mail.getIndex();
        if (mail.equals(mailer.getMailManager().getEditmodeMail(mailSender)))
            openCommand = "umail attach";  // editing mail

        if (!Bukkit.dispatchCommand(bukkitPlayer, openCommand) || !MailPermission.ATTACH_INBOXMAIL.can(bukkitPlayer) || !mailer.getMailer().getUndineConfig().isEnableAttachment())
            return;

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onClose(InventoryCloseEvent event) {
                HandlerList.unregisterAll(this);
                if (close != null) {
                    close.run();
                } else {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(owner, () -> openViewPanel(mail), 0);
                }
            }
        }, owner);
    }

    // check

    private boolean checkMailerLoadingWithPrompt(@Nullable Runnable retry) {
        if (mailer.available())
            return false;
        SimpleButtonForm form = SimpleButtonForm.builder(owner)
                .content(ChatColor.RED + "現在メールデータにアクセスできません。しばらく待ってからお試しください。");
        if (retry != null)
            form.button("リトライ", retry);
        form.button("メニューに戻る", this::openMainPanel);
        player.sendForm(form.build());
        return true;
    }

    private boolean checkAttachInboxPermission(Permissible permissible) {
        return mailer.getMailer().getUndineConfig().isEnableAttachment() && MailPermission.ATTACH.can(permissible) && MailPermission.ATTACH_INBOXMAIL.can(permissible);
    }

    private boolean checkDeniedAttachInboxPermissionPrompt(Permissible permissible, String buttonName, Runnable close) {
        if (checkAttachInboxPermission(permissible))
            return false;

        player.sendForm(SimpleButtonForm.builder(owner)
                .title("添付ボックス")
                .content(ChatColor.RED + "添付ボックスを開く権限がありません")
                .button(buttonName, close));
        return true;
    }

    private boolean checkAttachmentWorldAccess() {
        return !mailer.getMailer().getUndineConfig().getDisableWorldsToOpenAttachBox().contains(mailSender.getWorldName());
    }

    private boolean checkDeniedAttachmentWorldPrompt(String buttonName, Runnable click) {
        if (mailer.getMailer().getUndineConfig().getDisableWorldsToOpenAttachBox().contains(mailSender.getWorldName())) {
            player.sendForm(SimpleButtonForm.builder(owner)
                    .title("添付ボックス")
                    .content(ChatColor.RED + "このワールドで開くことができません。\n別のワールドに移動してから添付ボックスを開いてください。")
                    .button(buttonName, click));
            return true;
        }
        return false;
    }

    private boolean checkAttachSendMailPermission(Permissible permissible) {
        return mailer.getMailer().getUndineConfig().isEnableAttachment() && MailPermission.ATTACH.can(permissible) && MailPermission.ATTACH_SENDMAIL.can(permissible);
    }

}
