package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks.MailWrapper;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;

public final class MailGUIPlugin extends JavaPlugin {

    private MailWrapper mailWrapper;

    @Override
    public void onEnable() {
        Panel.OWNER = this;
        Optional.ofNullable(getCommand("undinemailergui")).ifPresent(cmd ->
                cmd.setExecutor(new GUICommand(this)));

        mailWrapper = new MailWrapper();
    }

    @Override
    public void onDisable() {
        Panel.destroyAll();
    }

    public static MailWrapper getWrapper() {
        return Objects.requireNonNull(
                getPlugin(MailGUIPlugin.class).mailWrapper,
                "MailWrapper not initialized!"
        );
    }

}
