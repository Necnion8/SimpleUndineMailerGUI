package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.commands;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.MainPanel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.geyser.BedrockMailPanel;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.MailPermission;
import net.md_5.bungee.api.ChatColor;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GUICommand implements TabExecutor {

    private final MailGUIPlugin plugin;

    public GUICommand(MailGUIPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player) {
            MainPanel.UIType ui = null;
            if (args.length >= 1)
                ui = MainPanel.UITypeNames.get(args[0].toLowerCase(Locale.ROOT));

            Player player = (Player) sender;
            if (plugin.isEnabledFloodgate() && openForBedrock(player, ui)) {
                return true;
            }

            if (ui == null)
                ui = MainPanel.DEFAULT_UI;

            if (ui.can(sender) && MailPermission.READ.can(sender)) {
                new MainPanel(player, ui).open();
            } else {
                sender.sendMessage(ChatColor.RED + "メールを見る権限がありません");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of(MainPanel.UIType.values())
                    .filter(type -> type.can(sender))
                    .map(MainPanel.UIType::getCommandName)
                    .filter(s -> args[0].regionMatches(true, 0, s, 0, args[0].length()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }


    private boolean openForBedrock(Player player, MainPanel.UIType ui) {
        FloodgatePlayer fPlayer = plugin.getFloodgatePlayer(player.getUniqueId());
        if (fPlayer != null) {
            BedrockMailPanel.open(fPlayer, MailSender.getMailSender(player), ui);
            return true;
        }
        return false;
    }

}
