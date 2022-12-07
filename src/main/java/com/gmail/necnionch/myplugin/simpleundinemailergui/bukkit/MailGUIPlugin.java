package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class MailGUIPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Panel.OWNER = this;
        Optional.ofNullable(getCommand("undinemailergui")).ifPresent(cmd ->
                cmd.setExecutor(new GUICommand(this)));

    }

    @Override
    public void onDisable() {
        Panel.destroyAll();
    }

}
