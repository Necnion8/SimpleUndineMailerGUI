package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.geyser;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.MainPanel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks.MailWrapper;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.MailPermission;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.ModalButtonForm;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.SimpleButtonForm;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.StrGen;
import com.google.common.collect.Lists;
import net.md_5.bungee.api.ChatColor;
import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.Utility;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderConsole;
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
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
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

        if (MainPanel.UIType.INBOX.equals(ui)) {
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
                    .join(() -> !mail.getAttachments().isEmpty(), "  (送付: " + mail.getAttachments().size() + ")")
                    .join(() -> unread, "  ***")
                    .text("\n")
                    .text(mail.getFrom().getName())
                    .text(" : ").text(mailer.formatContentSummary(mail))
                    .toString(),
                    () -> openViewPanel(mail));
        }

        player.sendForm(b.build());
    }

    private void openInboxManagePanel() {
        if (checkMailerLoadingWithPrompt(this::openInboxManagePanel))
            return;

        Function<MailData, Boolean> isRead = (mail) -> !mail.isRead(mailSender) && (mail.getAttachments().isEmpty() || mail.isAttachmentsCancelled());
        Function<MailData, Boolean> isTrash = (mail) -> mail.getAttachments().isEmpty();
        Function<MailData, Boolean> isAttach = (mail) -> !mail.getAttachments().isEmpty() && !mail.isAttachmentsCancelled() && mail.getCostItem() == null && mail.getCostMoney() <= 0;

        int readMails = 0;
        int trashMails = 0;
        int attachItems = 0;

        for (MailData mail : mailer.getMailManager().getInboxMails(mailSender)) {
            if (isRead.apply(mail))
                readMails++;
            if (isTrash.apply(mail))
                trashMails++;
            if (isAttach.apply(mail))
                attachItems += mail.getAttachments().size();
        }

        SimpleButtonForm b = SimpleButtonForm.builder(owner).title("受信メール管理");
        b.button("受信メールを全て既読にする\n未読メール: " + readMails + "通", () -> {
            if (checkMailerLoadingWithPrompt(this::openInboxManagePanel))
                return;

            int mails = mailer.setReadFlagMails(mailSender).size();
            player.sendForm(SimpleButtonForm.builder(owner)
                    .title("受信メール管理")
                    .content("受信メール " + mails + "通 を既読にしました")
                    .button("メール管理", this::openInboxManagePanel)
                    .build());
        });

        if (MailPermission.TRASH.can(bukkitPlayer)) {
            b.button("既読メールを全てゴミ箱に移動する\n既読メール: " + trashMails + "通", () -> {
                if (checkMailerLoadingWithPrompt(this::openInboxManagePanel))
                    return;

                SimpleButtonForm form = SimpleButtonForm.builder(owner).title("受信メール管理");

                if (!MailPermission.TRASH.can(bukkitPlayer)) {
                    form.content(ChatColor.RED + "ゴミ箱に移動する権限がありません");
                    form.button("メール管理", this::openInboxManagePanel);
                    return;
                }

                int mails = mailer.setTrashFlagMails(mailSender).size();
                form.content("既読メール " + mails + "通 をゴミ箱に移動しました");
                form.button("メール管理", this::openInboxManagePanel);
                player.sendForm(form.build());
            });
        }

        if (checkAttachInboxPermission(bukkitPlayer)) {
            b.button("添付アイテムを全て受け取る\n添付アイテム: " + attachItems + "個", () -> {
                if (checkMailerLoadingWithPrompt(this::openInboxManagePanel))
                    return;

                SimpleButtonForm form = SimpleButtonForm.builder(owner).title("受信メール管理");

                if (!checkAttachInboxPermission(bukkitPlayer)) {
                    form.content(ChatColor.RED + "送付ボックスを開く権限がありません");
                    form.button("メール管理", this::openInboxManagePanel);
                }

                MailWrapper.TakeAttachmentsResult result = mailer.takeAllMailAttachments(mailSender, bukkitPlayer);
                int total = result.getAll().size();
                int fail = result.getFails().size();

                if (fail <= 0) {
                    form.content("送付されたアイテムを全て受け取りました");
                } else if (total == fail) {
                    form.content(ChatColor.RED + "手持ちに空きがありません");
                } else {
                    form.content("送付されたアイテムを受け取りました\n" + ChatColor.YELLOW + "手持ちが一杯になったため、" + fail + "通のメールを開けませんでした");
                }
                form.button("メール管理", this::openInboxManagePanel);
                player.sendForm(form.build());
            });
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
                            .join(() -> !mail.getAttachments().isEmpty(), "  (送付: " + mail.getAttachments().size() + ")")
                            .text("\n")
                            .text(mail.getFrom().getName())
                            .text(" : ").text(mailer.formatContentSummary(mail))
                            .toString(),
                    () -> openViewPanel(mail));
        }

        player.sendForm(b.build());
    }

    private void openOutboxManagePanel() {
        player.sendForm(SimpleButtonForm.builder(owner).build());
    }

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

    private void openTrashPanel() {
        if (checkMailerLoadingWithPrompt(this::openTrashPanel))
            return;

        List<MailData> mails = mailer.getMailManager().getTrashboxMails(mailSender);

        SimpleButtonForm b = SimpleButtonForm.builder(owner)
                .title("ゴミ箱  " + mails.size() + "件")
                .button("メニューに戻る", this::openMainPanel)
                .button("メール管理", this::openTrashPanel);

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

    private void openTrashActionPanel(MailData mail) {
        if (checkMailerLoadingWithPrompt(() -> openTrashActionPanel(mail)))
            return;

        String panelTitle = "メール #" + mail.getIndex();
        SimpleButtonForm b = SimpleButtonForm.builder(JavaPlugin.getProvidingPlugin(MailGUIPlugin.class))
                .title(panelTitle)
                .button("メール画面に戻る", () -> openViewPanel(mail));

        if (mail.isEditmode() || !mail.isRelatedWith(mailSender) || !mail.isSetTrash(mailSender)) {
            player.sendForm(b.build());
            return;
        }

        b.button("ゴミ箱から戻す", () -> {
            SimpleButtonForm form = SimpleButtonForm.builder(owner)
                    .title(panelTitle);
            if (checkMailerLoadingWithPrompt(null))
                return;

            if (!mail.isRelatedWith(mailSender)) {
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
                    if (MailPermission.TRASH.can(bukkitPlayer))
                        form.button("ゴミ箱に戻る", this::openTrashPanel);
                    if (MailPermission.INBOX.can(bukkitPlayer))
                        form.button("受信箱に戻る", this::openInboxPanel);
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
            b.content(ChatColor.RED + "送付ボックスを開く権限がありません");

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
                        if (checkMailerLoadingWithPrompt(null))
                            return;

                        if (!checkAttachInboxPermission(bukkitPlayer)) {
                            player.sendForm(SimpleButtonForm.builder(owner)
                                    .title(panelTitle)
                                    .content(ChatColor.RED + "送付ボックスを開く権限がありません")
                                    .button("メール画面に戻る", () -> openViewPanel(mail))
                                    .build());
                        } else if (mailer.tryAcceptCostMoney(mailSender, mail)) {
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
                        if (checkMailerLoadingWithPrompt(null))
                            return;

                        if (!checkAttachInboxPermission(bukkitPlayer)) {
                            player.sendForm(SimpleButtonForm.builder(owner)
                                    .title(panelTitle)
                                    .content(ChatColor.RED + "送付ボックスを開く権限がありません")
                                    .button("メール画面に戻る", () -> openViewPanel(mail))
                                    .build());
                        } else if (mailer.tryAcceptCostItem(bukkitPlayer, mailSender, mail)) {
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
            b.button("送付ボックスを開く", () -> {
                if (!checkAttachInboxPermission(bukkitPlayer)) {
                    player.sendForm(SimpleButtonForm.builder(owner)
                            .title(panelTitle)
                            .content(ChatColor.RED + "送付ボックスを開く権限がありません")
                            .button("メール画面に戻る", () -> openViewPanel(mail))
                            .build());
                } else {
                    openAttachmentInventory(mail, null);
                }
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
                    b.content(ChatColor.RED + "送付ボックスを開く権限がありません");
                } else {
                    b.button("送付ボックスを開く", () -> {
                        if (checkMailerLoadingWithPrompt(null))
                            return;

                        if (!checkAttachInboxPermission(bukkitPlayer)) {
                            player.sendForm(SimpleButtonForm.builder(owner)
                                    .title(panelTitle)
                                    .content(ChatColor.RED + "送付ボックスを開く権限がありません")
                                    .button("メール画面に戻る", () -> openViewPanel(mail))
                                    .build());
                        } else {
                            openAttachmentInventory(mail, null);
                        }
                    });
                }

            } else if (!mail.isAttachmentsOpened()) {
                if (!checkAttachInboxPermission(bukkitPlayer)) {
                    b.content(ChatColor.RED + "送付ボックスを開く権限がありません");
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
                        } else if (!checkAttachInboxPermission(bukkitPlayer)) {
                            player.sendForm(SimpleButtonForm.builder(owner)
                                    .title(panelTitle)
                                    .content(ChatColor.RED + "送付ボックスを開く権限がありません")
                                    .button("メール画面に戻る", () -> openViewPanel(mail))
                                    .build());
                        } else {
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
                    if (MailPermission.TRASH.can(bukkitPlayer))
                        form.button("ゴミ箱に戻る", this::openTrashPanel);
                    if (MailPermission.OUTBOX.can(bukkitPlayer))
                        form.button("送信箱に戻る", this::openTrashPanel);
                    mail.setTrashFlag(mailSender);
                    mailer.getMailManager().saveMail(mail);
                }
                player.sendForm(form.build());
            });
        }
        player.sendForm(b.build());
    }

    private void openAttachmentInventory(MailData mail, @Nullable Runnable close) {
        if (checkMailerLoadingWithPrompt(null))
            return;

        if (!Bukkit.dispatchCommand(bukkitPlayer, "umail attach " + mail.getIndex()) || !MailPermission.ATTACH_INBOXMAIL.can(bukkitPlayer) || !mailer.getMailer().getUndineConfig().isEnableAttachment())
            return;
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onClose(InventoryCloseEvent event) {
                HandlerList.unregisterAll(this);
                if (close != null) {
                    close.run();
                } else {
                    openViewPanel(mail);
                }
            }
        }, owner);
    }

    private boolean checkAttachInboxPermission(Permissible permissible) {
        return mailer.getMailer().getUndineConfig().isEnableAttachment() && MailPermission.ATTACH.can(permissible) && MailPermission.ATTACH_INBOXMAIL.can(permissible);
    }

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

}
