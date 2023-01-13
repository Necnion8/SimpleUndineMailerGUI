package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util;

import org.bitbucket.ucchy.undine.MailData;

import java.util.Collections;
import java.util.List;

public class MailsResult {
    private final List<MailData> allMails;
    private final List<MailData> fails;

    public MailsResult(List<MailData> allMails, List<MailData> fails) {
        this.allMails = allMails;
        this.fails = fails;
    }

    public static MailsResult empty() {
        return new MailsResult(Collections.emptyList(), Collections.emptyList());
    }

    public static MailsResult of(List<MailData> allMails, List<MailData> fails) {
        return new MailsResult(Collections.unmodifiableList(allMails), Collections.unmodifiableList(fails));
    }

    public List<MailData> getAll() {
        return allMails;
    }

    public List<MailData> getFails() {
        return fails;
    }

}
