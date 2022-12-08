package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks;

import org.bitbucket.ucchy.undine.MailManager;
import org.bitbucket.ucchy.undine.UndineMailer;

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

}
