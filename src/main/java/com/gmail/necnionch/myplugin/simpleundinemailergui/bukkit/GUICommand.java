package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.MainPanel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
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

            if (ui != null) {
                new MainPanel(((Player) sender), ui).open();
            } else {
                new MainPanel(((Player) sender)).open();
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of(MainPanel.UIType.values())
                    .map(MainPanel.UIType::getCommandName)
                    .filter(s -> args[0].regionMatches(true, 0, s, 0, args[0].length()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}
