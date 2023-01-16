package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util;

import org.bukkit.permissions.Permissible;

public enum MailPermission {
    INBOX("inbox"),
    OUTBOX("outbox"),
    TRASH("trash"),
    READ("read"),
    WRITE("write"),
    SEND("send"),
    ATTACH("attach"),
    ATTACH_INBOXMAIL("attach-inboxmail"),
    ATTACH_INFINITY("attach-infinity"),
    ATTACH_SENDMAIL("attach-sendmail")
    ;

    private final String node;
    public static final String NODE_PREFIX = "undine";

    MailPermission(String node) {
        this.node = NODE_PREFIX + "." + node;
    }

    public String getNode() {
        return node;
    }

    public boolean can(Permissible permissible) {
        return permissible.hasPermission(getNode());
    }

    public boolean cannot(Permissible permissible) {
        return !permissible.hasPermission(getNode());
    }

}
