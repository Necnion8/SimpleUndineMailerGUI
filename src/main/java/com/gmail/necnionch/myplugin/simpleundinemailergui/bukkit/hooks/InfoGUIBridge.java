package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks;

import com.gmail.necnionch.myplugin.infogui.bukkit.InfoGUI;
import com.gmail.necnionch.myplugin.infogui.bukkit.events.InfoGUIMailPanelEvent;
import com.gmail.necnionch.myplugin.infogui.bukkit.gui.PanelItem;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.MainPanel;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

public class InfoGUIBridge {
    private final MailGUIPlugin owner;
    private @Nullable GUIListener listener;

    public InfoGUIBridge(MailGUIPlugin owner) {
        this.owner = owner;
    }

    public void hook() {
        unhook();
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("LOInfoGUI")) {
                this.listener = new GUIListener();
                Bukkit.getPluginManager().registerEvents(listener, owner);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void unhook() {
        if (this.listener != null) {
            HandlerList.unregisterAll(this.listener);
            this.listener = null;
        }
    }

    private int getUnreadMailCount(Player player) {
        MailWrapper wrapper = MailGUIPlugin.getWrapper();
        if (!wrapper.available())
            return 0;
        return Optional.ofNullable(wrapper.getMailManager().getUnreadMails(MailSender.getMailSender(player)))
                .map(ArrayList::size)
                .orElse(0);
    }

    private void openGUI(Player player) {
        new com.gmail.necnionch.myplugin.infogui.bukkit.panels.MainPanel(InfoGUI.getInstance(), player).open();
    }


    public final class GUIListener implements Listener {
        @EventHandler
        public void onMainPanel(InfoGUIMailPanelEvent event) {
            int count = getUnreadMailCount(event.getPlayer());
            String name = ChatColor.AQUA + "メール一覧";
            if (count > 0)
                name += " (+" + count + ")";
            event.getSlots()[29] = PanelItem.createItem(Material.FILLED_MAP, name).setClickListener((e, p) -> {
                new MainPanel(event.getPlayer(), MainPanel.DEFAULT_UI)
                        .setFallbackPanel(() -> openGUI(event.getPlayer()))
                        .open();
            });
        }
    }

}
