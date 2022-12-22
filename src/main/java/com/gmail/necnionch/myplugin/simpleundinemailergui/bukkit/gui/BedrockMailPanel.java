package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks.MailWrapper;
import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BedrockMailPanel {

    private final FloodgatePlayer player;
    private final MailSender mailSender;
    private final MailWrapper mail;

    public BedrockMailPanel(FloodgatePlayer player, MailSender mailSender, @Nullable MainPanel.UIType ui) {
        this.player = player;
        this.mailSender = mailSender;
        this.mail = MailGUIPlugin.getWrapper();

        if (ui == null) {
            player.sendForm(createMainPanel());
        } else {

        }
    }

    public static BedrockMailPanel open(FloodgatePlayer player, MailSender mailSender, @Nullable MainPanel.UIType ui) {
        return new BedrockMailPanel(player, mailSender, ui);
    }

    private Form createMainPanel() {
        List<MailData> mails = mail.getMailManager().getInboxMails(mailSender);

        long unread = mails.stream()
                .filter(mail -> !mail.isRead(mailSender))
                .count();

        return SimpleForm.builder()
                .button("受信箱")
                .button("ゴミ箱")
                .build();

    }

}
