package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.MailGUIPlugin;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.gui.ui.*;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.MailPermission;
import com.google.common.collect.ImmutableMap;
import org.bitbucket.ucchy.undine.MailData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainPanel extends Panel {
    public static final UIType DEFAULT_UI = null;
    private final UI mainPanel;
    private final NewMailUI newMail;
    private final InboxUI inbox;
    private final OutboxUI outbox;
    private final TrashBoxUI trashBox;
    private MailUI currentUI;

    public MainPanel(Player player) {
        this(player, DEFAULT_UI);
    }

    public MainPanel(Player player, @Nullable UIType type) {
        super(player, MailUI.SLOT_SIZE, "", new ItemStack(Material.AIR));
        this.mainPanel = new UI(player, this);
        this.newMail = new NewMailUI(player, this);
        this.inbox = new InboxUI(player, this);
        this.outbox = new OutboxUI(player, this);
        this.trashBox = new TrashBoxUI(player, this);

        if (type == null) {
            this.currentUI = mainPanel;
        } else {
            switch (type) {
                case NEW_MAIL:
                    this.currentUI = newMail;
                    break;
                case OUTBOX:
                    this.currentUI = outbox;
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
    }

    @Override
    public PanelItem[] build() {
        PanelItem[] slots = new PanelItem[getSize()];

        if (!MailGUIPlugin.getWrapper().available()) {
            slots[22] = PanelItem.createItem(Material.BARRIER, ChatColor.RED + "現在メールプラグインを利用できません");
            return slots;
        }

        slots[0] = PanelItem.createItem(Material.OAK_DOOR, ChatColor.RED + "メインメニューに戻る")
                .setClickListener((e, p) -> {
                    currentUI = mainPanel;
                    update();
                });

        currentUI.build(slots);
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
        ui.resetPageIndex();
        this.update();
    }

    @Override
    public String getTitle() {
        String title = ChatColor.DARK_PURPLE + "メール";

        String uiTitle = currentUI.getTitle();
        if (uiTitle != null)
            title += ChatColor.GRAY + " - " + ChatColor.DARK_AQUA + uiTitle;

        int maxPageIndex = currentUI.getMaxPageIndex();
        int pageIndex = currentUI.getPageIndex();
        if (maxPageIndex >= 0) {
            title += ChatColor.DARK_GRAY + " [" + ChatColor.BOLD + (pageIndex+1) + ChatColor.DARK_GRAY + "/" + ChatColor.BOLD + (maxPageIndex+1) + ChatColor.DARK_GRAY + "]";
        }

        return title;
    }


    public static final ImmutableMap<String, UIType> UITypeNames = ImmutableMap.copyOf(Stream.of(UIType.values())
            .collect(Collectors.toMap(UIType::getCommandName, t -> t)));

    public enum UIType {
        NEW_MAIL("write", MailPermission.WRITE),
        INBOX("inbox", MailPermission.INBOX),
        OUTBOX("outbox", MailPermission.OUTBOX),
        TRASH_BOX("trash", MailPermission.TRASH)
        ;

        private final String commandName;
        private final MailPermission permission;

        UIType(String commandName, @Nullable MailPermission permission) {
            this.commandName = commandName;
            this.permission = permission;
        }

        public String getCommandName() {
            return commandName;
        }

        public MailPermission getPermission() {
            return permission;
        }

        public boolean can(Permissible permissible) {
            return permission == null || permission.can(permissible);
        }

    }

    public class UI extends MailUI {

        private @Nullable List<MailData> mails;

        public UI(Player player, Panel parent) {
            super(player, parent);
        }

        @Override
        public @Nullable ItemStack getIcon() {
            return null;
        }

        @Override
        public @Nullable String getTitle() {
            return null;
        }

        @Override
        public @Nullable List<MailData> getMails() {
            return mails;
        }

        @Override
        public void build(PanelItem[] slots) {
            loadMails();
            super.build(slots);
            slots[0] = createUIItem(newMail);
            slots[1] = createUIItem(inbox);
            slots[2] = createUIItem(outbox);
            slots[3] = createUIItem(trashBox);
        }

        private void loadMails() {
            mails = mailer.getMailManager().getUnreadMails(sender);
        }

        @Override
        public PanelItem[] createMailMenuItems(MailData mail) {
            return new PanelItem[0];
        }

    }

}
