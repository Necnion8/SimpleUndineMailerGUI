package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks;

import org.bitbucket.ucchy.undine.*;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MailWrapper {

    private final UndineMailer mailer;

    public MailWrapper() {
        mailer = UndineMailer.getInstance();
        if (!mailer.isEnabled())
            throw new IllegalStateException("UndineMailer is disabled!");
    }

    public UndineMailer getMailer() {
        return mailer;
    }

    public MailManager getMailManager() {
        return mailer.getMailManager();
    }

    public boolean available() {
        return mailer.getMailManager().isLoaded();
    }

    public String summarySubstring(String line) {
        line = Utility.removeColorCode(line);
        if (line.length() > 47)
            return line.substring(0, 45) + "..";
        return line;
    }

    public String formatDate(Date date) {
        return (new SimpleDateFormat(Messages.get("DateFormat"))).format(date);
    }

    public String joinToAndGroup(MailData mail) {
        List<String> names = mail.getTo()
                .stream()
                .map(MailSender::getName)
                .collect(Collectors.toList());
        names.addAll(mail.getToGroups());
        return String.join(", ", names);
    }

    public String itemDesc(ItemStack item, boolean forDescription) {
        if (item == null) {
            return "null";
        } else {
            String desc = item.getDurability() == 0 ? item.getType().toString() : item.getType() + ":" + item.getDurability();
            if (item.getAmount() == 1) {
                return desc;
            } else {
                return forDescription ? desc + " * " + item.getAmount() : desc + " " + item.getAmount();
            }
        }
    }


}
