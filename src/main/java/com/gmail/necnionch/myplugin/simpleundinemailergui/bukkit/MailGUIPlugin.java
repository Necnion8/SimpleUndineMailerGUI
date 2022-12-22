package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks.MailWrapper;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class MailGUIPlugin extends JavaPlugin {

    private static boolean enabled;
    private MailWrapper mailWrapper;
    private boolean enabledFloodgate;

    @Override
    public void onEnable() {
        enabled = true;
        Panel.OWNER = this;
        Optional.ofNullable(getCommand("undinemailergui")).ifPresent(cmd ->
                cmd.setExecutor(new GUICommand(this)));

        mailWrapper = new MailWrapper();
        enabledFloodgate = getServer().getPluginManager().isPluginEnabled("floodgate");
    }

    @Override
    public void onDisable() {
        enabled = false;
        Panel.destroyAll();
    }

    public static MailWrapper getWrapper() {
        return Objects.requireNonNull(
                getPlugin(MailGUIPlugin.class).mailWrapper,
                "MailWrapper not initialized!"
        );
    }

    public static boolean isEnabledPlugin() {
        return enabled;
    }

    public boolean isEnabledFloodgate() {
        return enabledFloodgate;
    }

    public @Nullable FloodgatePlayer getFloodgatePlayer(UUID playerId) {
        if (enabledFloodgate) {
            return FloodgateApi.getInstance().getPlayer(playerId);
        }
        return null;
    }

}
