package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util;

import net.md_5.bungee.api.ChatColor;

import java.util.function.Supplier;

public class StrGen {

    private final StringBuilder builder;
    private final Supplier<Boolean> check;

    private StrGen() {
        this.builder = new StringBuilder();
        this.check = () -> true;
    }

    private StrGen(Supplier<Boolean> check) {
        this.builder = new StringBuilder();
        this.check = check;
    }

    public static StrGen builder() {
        return new StrGen();
    }

    public static StrGen builder(Supplier<Boolean> check) {
        return new StrGen(check);
    }

    public String toString() {
        if (check.get())
            return builder.toString();
        return "";
    }


    public StrGen text(String text) {
        builder.append(text);
        return this;
    }

    public StrGen text(ChatColor color) {
        builder.append(color);
        return this;
    }

    public StrGen text(StrGen child) {
        builder.append(child);
        return this;
    }


    public StrGen join(Supplier<Boolean> check, String text) {
        if (check.get()) {
            builder.append(text);
        }
        return this;
    }

    public StrGen join(Supplier<Boolean> check, ChatColor color) {
        if (check.get()) {
            builder.append(color);
        }
        return this;
    }

    public StrGen join(Supplier<Boolean> check, Supplier<StrGen> child) {
        if (check.get())
            builder.append(child.get());
        return this;
    }


}
