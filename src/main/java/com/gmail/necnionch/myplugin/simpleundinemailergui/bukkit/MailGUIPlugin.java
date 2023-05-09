package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.commands.GUICommand;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.MainPanel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.geyser.BedrockMailPanel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks.InfoGUIBridge;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks.MailWrapper;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.MailPermission;
import net.md_5.bungee.api.ChatColor;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.entity.Player;
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
    private InfoGUIBridge infoGUI = new InfoGUIBridge(this);

    @Override
    public void onEnable() {
        enabled = true;
        Panel.OWNER = this;
        Optional.ofNullable(getCommand("undinemailergui")).ifPresent(cmd ->
                cmd.setExecutor(new GUICommand(this)));

        mailWrapper = new MailWrapper();
        enabledFloodgate = getServer().getPluginManager().isPluginEnabled("floodgate");
        infoGUI.hook();
    }

    @Override
    public void onDisable() {
        enabled = false;
        Panel.destroyAll();
        infoGUI.unhook();
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


    public void openGUI(Player player, MainPanel.UIType ui) {
        if (enabledFloodgate) {
            FloodgatePlayer fPlayer = getFloodgatePlayer(player.getUniqueId());
            if (fPlayer != null) {
                BedrockMailPanel.open(fPlayer, MailSender.getMailSender(player), ui);
                return;
            }
        }

        if (MailPermission.READ.cannot(player)) {
            player.sendMessage(ChatColor.RED + "メールを見る権限がありません");
        } else if (ui != null && ui.can(player)) {
            new MainPanel(player, ui).open();
        } else {
            new MainPanel(player, MainPanel.DEFAULT_UI).open();
        }
    }

    public void openGUI(Player player) {
        openGUI(player, MainPanel.DEFAULT_UI);
    }

}
