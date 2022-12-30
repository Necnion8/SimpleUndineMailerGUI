package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks.MailWrapper;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.StrGen;
import com.google.common.collect.Lists;
import net.md_5.bungee.api.ChatColor;
import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.Utility;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BedrockMailPanel {

    private final FloodgatePlayer player;
    private final MailSender mailSender;
    private final MailWrapper mailer;

    public BedrockMailPanel(FloodgatePlayer player, MailSender mailSender, @Nullable MainPanel.UIType ui) {
        this.player = player;
        this.mailSender = mailSender;
        this.mailer = MailGUIPlugin.getWrapper();

        if (MainPanel.UIType.INBOX.equals(ui)) {
            player.sendForm(createInboxPanel());
        } else if (MainPanel.UIType.TRASH_BOX.equals(ui)) {
            player.sendForm(createTrashPanel());
        } else {
            player.sendForm(createMainPanel());
        }
    }

    public static BedrockMailPanel open(FloodgatePlayer player, MailSender mailSender, @Nullable MainPanel.UIType ui) {
        return new BedrockMailPanel(player, mailSender, ui);
    }

    private Form createMainPanel() {
        List<MailData> mails = mailer.getMailManager().getInboxMails(mailSender);
        List<MailData> trash = mailer.getMailManager().getTrashboxMails(mailSender);

        long unread = mails.stream()
                .filter(mail -> !mail.isRead(mailSender))
                .count();

        return SimpleForm.builder()
                .title("メールメニュー")
                .button(StrGen.builder()
                        .text("受信箱 (" + mails.size() + ")")
                        .text(StrGen.builder(() -> unread > 0)
                                .text(ChatColor.DARK_RED).text(ChatColor.BOLD).text("\n(未読 " + unread + "通)"))
                        .toString())
                .button("ゴミ箱 (" + trash.size() + ")")
                .validResultHandler(res -> {
                    if (!MailGUIPlugin.isEnabledPlugin())
                        return;

                    int idx = res.clickedButtonId();
                    if (idx == 0) {
                        player.sendForm(createInboxPanel());
                    } else if (idx == 1) {
                        player.sendForm(createTrashPanel());
                    }
                })
                .build();
    }

    private Form createInboxPanel() {
        List<MailData> mails = Lists.newArrayList(mailer.getMailManager().getInboxMails(mailSender));

        SimpleForm.Builder b = SimpleForm.builder()
                .title("受信箱  " + mails.size() + "件")
                .button("メニューに戻る")
                .button("メール管理");

        for (MailData mail : mails) {
            boolean unread = !mail.isRead(mailSender);

            b.button(StrGen.builder()
                    .join(() -> unread, "***  ")
                    .text("#" + mail.getIndex())
                            .text("  送信日時: " + mailer.formatDate(mail.getDate()))
                    .join(() -> !mail.getAttachments().isEmpty(), "  (送付: " + mail.getAttachments().size() + ")")
                    .join(() -> unread, "  ***")
                    .text("\n")
                    .text(mail.getFrom().getName())
                    .text(" : " + mail.getMessage().get(0))
                    .toString()
            );
        }

        return b.validResultHandler(res -> {
            if (!MailGUIPlugin.isEnabledPlugin())
                return;

            int idx = res.clickedButtonId();
            if (idx == 0) {
                player.sendForm(createMainPanel());
            } else if (idx == 1) {
                player.sendForm(createInboxManagePanel());
            } else {
                MailData mail;
                try {
                    mail = mails.get(idx - 2);
                } catch (IndexOutOfBoundsException e) {
                    player.sendForm(createInboxManagePanel());
                    return;
                }
                player.sendForm(createViewPanel(mail));
            }
        }).build();
    }

    private Form createInboxManagePanel() {
        return SimpleForm.builder().build();
    }

    private Form createViewPanel(MailData mail) {
        String toName = mailer.joinToAndGroup(mail);
        ModalForm.Builder b = ModalForm.builder()
                .title("メール #" + mail.getIndex())
                .content(StrGen.builder()
                        .text(ChatColor.RED).text("送信者: ").text(ChatColor.WHITE).text(mail.getFrom().getName() + "  ")
                        .text(ChatColor.RED).text("宛先: ").text(ChatColor.WHITE).text(toName)
                        .text("\n")
                        .text(ChatColor.RED).text("送信日時: ").text(ChatColor.WHITE).text(mailer.formatDate(mail.getDate()))
                        .text("\n")
                        .text(ChatColor.RED).text("メッセージ:\n")
                        .text(mail.getMessage().stream()
                                .map(m -> ChatColor.WHITE + "  " + Utility.replaceColorCode(m))
                                .collect(Collectors.joining("\n")))
                        .text("\n")
                        .join(() -> !mail.getAttachments().isEmpty(), () -> {
                            StrGen sg = StrGen.builder().text(ChatColor.RED).text("送付アイテム: ");

                            if (!mail.isEditmode() && mail.isAttachmentsCancelled() && !mail.getFrom().equals(mailSender)) {
                                // show cancelled
                                if (mail.isAttachmentsRefused()) {
                                    sg.text(ChatColor.DARK_AQUA).text("[受取拒否済み]");
                                    Optional.ofNullable(mail.getAttachmentsRefusedReason()).ifPresent(reason ->
                                        sg.text(ChatColor.WHITE + "\n  " + reason));
                                } else {
                                    sg.text(ChatColor.DARK_RED).text("[送付キャンセル]");
                                }
                            }

                            sg.text("\n");
                            if (mail.getCostMoney() > 0) {
                                String costDesc = Optional.ofNullable(mailer.getMailer().getVaultEco())
                                        .map(eco -> eco.format(mail.getCostMoney()))
                                        .orElse(mail.getCostMoney() + "");
                                sg.text(ChatColor.GOLD + "  着払い料金: ").text(ChatColor.WHITE + costDesc);
                                sg.text("\n");
                            } else if (mail.getCostItem() != null) {
                                sg.text(ChatColor.GOLD + "  着払いアイテム: ").text(ChatColor.WHITE + mailer.itemDesc(mail.getCostItem(), true));
                                sg.text("\n");
                            }

                            mail.getAttachments().forEach(item ->
                                    sg.text(ChatColor.WHITE + "  " + mailer.itemDesc(item, true) + "\n"));

                            return sg;
                        })
                        .toString());

        if (mailer.getMailManager().getInboxMails(mailSender).contains(mail)) {
            b.button1("受信箱に戻る");
            b.button2("送付アイテム");  // -> 開くor取引を確定し開くor受け取りの拒否
            b.validResultHandler(res -> {
                if (!MailGUIPlugin.isEnabledPlugin())
                    return;

                int idx = res.clickedButtonId();
                if (idx == 0) {
                    player.sendForm(createInboxPanel());
                } else if (idx == 1) {
                    // open attachments box
                }
            });
        } else if (mailer.getMailManager().getTrashboxMails(mailSender).contains(mail)) {
            b.button1("受信箱に戻る");
            b.button2("送付ボックスを開く");
            b.validResultHandler(res -> {
                if (!MailGUIPlugin.isEnabledPlugin())
                    return;

                int idx = res.clickedButtonId();
                if (idx == 0) {
                    player.sendForm(createInboxPanel());
                } else if (idx == 1) {
                    // open attachments box
                }
            });
        }
        return b.build();
    }

    private Form createTrashPanel() {
        List<MailData> mails = mailer.getMailManager().getTrashboxMails(mailSender);

        SimpleForm.Builder b = SimpleForm.builder()
                .title("ゴミ箱  " + mails.size() + "件")
                .button("メニューに戻る")
                .button("メール管理");

        for (MailData mail : mails) {
            b.button(StrGen.builder()
                            .text("#" + mail.getIndex())
                            .text("  送信日時: " + mailer.formatDate(mail.getDate()))
                            .text("\n")
                            .text(mail.getFrom().getName())
                            .text(" : " + mail.getMessage().get(0))
                            .toString()
            );
        }

        return b.validResultHandler(res -> {
            if (!MailGUIPlugin.isEnabledPlugin())
                return;

            int idx = res.clickedButtonId();
            if (idx == 0) {
                player.sendForm(createMainPanel());
            } else if (idx == 1) {
                player.sendForm(createTrashPanel());
            } else {
                MailData mail;
                try {
                    mail = mails.get(idx - 2);
                } catch (IndexOutOfBoundsException e) {
                    player.sendForm(createTrashManagePanel());
                    return;
                }
                player.sendForm(createViewPanel(mail));
            }
        }).build();
    }

    private Form createTrashManagePanel() {
        return SimpleForm.builder().build();
    }

}
