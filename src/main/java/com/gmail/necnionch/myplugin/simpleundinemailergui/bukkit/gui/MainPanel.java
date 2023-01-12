package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui.*;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.MailPermission;
import com.google.common.collect.ImmutableMap;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainPanel extends Panel {
    public static final UIType DEFAULT_UI = UIType.INBOX;
    private final NewMailUI newMail;
    private final InboxUI inbox;
    private final OutboxUI outbox;
    private final TrashBoxUI trashBox;
    private final GroupsUI groups;
    private MailUI currentUI;

    public MainPanel(Player player) {
        this(player, DEFAULT_UI);
    }

    public MainPanel(Player player, UIType type) {
        super(player, 54, "", new ItemStack(Material.AIR));
        this.newMail = new NewMailUI(player, this);
        this.inbox = new InboxUI(player, this);
        this.outbox = new OutboxUI(player, this);
        this.trashBox = new TrashBoxUI(player, this);
        this.groups = new GroupsUI(player, this);

        switch (type) {
            case NEW_MAIL:
                this.currentUI = newMail;
                break;
            case OUTBOX:
                this.currentUI = outbox;
                break;
            case GROUPS:
                this.currentUI = groups;
                break;
            case TRASH_BOX:
                this.currentUI = trashBox;
                break;
            case INBOX:
                this.currentUI = inbox;
                break;
            default:
                throw new IllegalArgumentException("Unknown UIType: " + type.name());
        }
    }

    @Override
    public PanelItem[] build() {
        PanelItem[] slots = new PanelItem[getSize()];

        if (!MailGUIPlugin.getWrapper().available()) {
            slots[22] = PanelItem.createItem(Material.BARRIER, ChatColor.RED + "現在メールプラグインを利用できません");
            return slots;
        }

        slots[0] = createUIItem(newMail);
        slots[2] = createUIItem(inbox);
        slots[3] = createUIItem(outbox);
        slots[4] = createUIItem(trashBox);
        slots[5] = createUIItem(groups);

        slots[7] = new PanelItem((p) -> PanelItem.createItem(
                Material.LIGHT_BLUE_DYE,
                ((currentUI.getMaxPage() <= 0) ? ChatColor.GRAY : ChatColor.AQUA) + "前のページへ"
        ).getItemStack(), (e, p) -> currentUI.onBackButton());

        slots[8] = new PanelItem((p) -> PanelItem.createItem(
                Material.ROSE_RED,
                ((currentUI.getMaxPage() <= 0) ? ChatColor.GRAY : ChatColor.RED) + "次のページへ"
        ).getItemStack(), (e, p) -> currentUI.onNextButton());

        for (int i = 0; i < 9; i++) {
            slots[9 + i] = PanelItem.createItem(Material.STONE_BUTTON, ChatColor.RESET.toString());
        }

        PanelItem[] body = new PanelItem[4 * 9];
        currentUI.build(body);
        System.arraycopy(body, 0, slots, 2 * 9, body.length);

        return slots;
    }

    private PanelItem createUIItem(MailUI ui) {
        return new PanelItem(
                (p) -> Optional.ofNullable(ui.getIcon())
                        .orElseGet(() -> PanelItem.createBlankItem().getItemStack()),
                (e, p) -> changeUI(ui)
        );
    }

    public void changeUI(MailUI ui) {
        this.currentUI = ui;
        this.update();
    }

    @Override
    public String getTitle() {
        String title = ChatColor.DARK_PURPLE + "メール";

        String uiTitle = currentUI.getTitle();
        if (uiTitle != null)
            title += ChatColor.GRAY + " - " + ChatColor.DARK_AQUA + uiTitle;

        if (currentUI.getMaxPage() > 0)
            title += ChatColor.DARK_GRAY + " [" + ChatColor.BOLD + currentUI.getCurrentPage() + ChatColor.DARK_GRAY + "/" + ChatColor.BOLD + currentUI.getMaxPage() + ChatColor.DARK_GRAY + "]";

        return title;
    }


    public static final ImmutableMap<String, UIType> UITypeNames = ImmutableMap.copyOf(Stream.of(UIType.values())
            .collect(Collectors.toMap(UIType::getCommandName, t -> t)));

    public enum UIType {
        NEW_MAIL("write", MailPermission.WRITE),
        INBOX("inbox", MailPermission.INBOX),
        OUTBOX("outbox", MailPermission.OUTBOX),
        TRASH_BOX("trash", MailPermission.TRASH),
        GROUPS("group", null);

        private final String commandName;
        private final MailPermission permission;

        UIType(String commandName, @Nullable MailPermission permission) {
            this.commandName = commandName;
            this.permission = permission;
        }

        public String getCommandName() {
            return commandName;
        }

        public @Nullable MailPermission getPermission() {
            return permission;
        }

        public boolean can(Permissible permissible) {
            return permission == null || permission.can(permissible);
        }

    }

}
