package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks;

import com.google.common.collect.Lists;
import org.bitbucket.ucchy.undine.*;
import org.bitbucket.ucchy.undine.bridge.VaultEcoBridge;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MailWrapper {

    private final UndineMailer mailer;

    public MailWrapper() {
        mailer = UndineMailer.getInstance();
        if (!mailer.isEnabled())
            throw new IllegalStateException("UndineMailer is disabled!");
    }

    public UndineMailer getMailer() {
        return mailer;
    }

    public MailManager getMailManager() {
        return mailer.getMailManager();
    }

    public boolean available() {
        return mailer.isEnabled() && mailer.getMailManager().isLoaded();
    }

    public String summarySubstring(String line) {
        line = Utility.removeColorCode(line);
        if (line.length() > 47)
            return line.substring(0, 45) + "..";
        return line;
    }

    public String formatDate(Date date) {
        return (new SimpleDateFormat(Messages.get("DateFormat"))).format(date);
    }

    public String formatDateOrReadable(Date date) {
        Date now = new Date();
        if (now.getTime() - date.getTime() < 60 * 1000) {
            return "たった今";
        } else if (now.getTime() - date.getTime() < 60 * 60 * 1000) {
            long minutes = (now.getTime() - date.getTime()) / (60 * 1000);
            return minutes + "分前";
        } else if (now.getTime() - date.getTime() < 24 * 60 * 60 * 1000) {
            long minutes = (now.getTime() - date.getTime()) / (60 * 60 * 1000);
            return minutes + "時間前";
        } else {
            Calendar now2 = Calendar.getInstance();
            now2.setTime(now);
            Calendar date2 = Calendar.getInstance();
            date2.setTime(date);
            int days = now2.get(Calendar.DAY_OF_YEAR) - date2.get(Calendar.DAY_OF_YEAR);
            if (now2.get(Calendar.YEAR) == date2.get(Calendar.YEAR) && days <= 7)
                return days + "日前";
        }
        return (new SimpleDateFormat(Messages.get("DateFormat"))).format(date);
    }

    public String formatContentSummary(MailData mail) {
        String content;
        if (mail.getMessage().stream().anyMatch(((Predicate<String>) String::isEmpty).negate())) {
            content = mail.getMessage().get(0);
        } else if (mail.isAttachmentsRefused()) {
            content = "受信者により添付が受取拒否されました";
        } else if (mail.isAttachmentsCancelled()) {
            content = "送信者により添付がキャンセルされました";
        } else if (!mail.getAttachments().isEmpty()) {
            content = itemDesc(mail.getAttachments().get(0), true);
            if (mail.getAttachments().size() > 1)
                content += " ...他 " + (mail.getAttachments().size() - 1) + "個";
        } else {
            content = "";
        }
        return content;
    }

    public String joinToAndGroup(MailData mail) {
        List<String> names = mail.getTo()
                .stream()
                .map(MailSender::getName)
                .collect(Collectors.toList());
        names.addAll(mail.getToGroups());
        return String.join(", ", names);
    }

    public String itemDesc(ItemStack item, boolean forDescription) {
        if (item == null) {
            return "null";
        } else {
            String desc = item.getDurability() == 0 ? item.getType().toString() : item.getType() + ":" + item.getDurability();
            if (item.getAmount() == 1) {
                return desc;
            } else {
                return forDescription ? desc + " * " + item.getAmount() : desc + " " + item.getAmount();
            }
        }
    }

    public String formatCostMoney(double cost) {
        return Optional.ofNullable(mailer.getVaultEco())
                .map(eco -> eco.format(cost))
                .orElse(cost + "");
    }

    public boolean checkCostMoney(MailSender ms, MailData mail) {
        VaultEcoBridge eco = mailer.getVaultEco();
        if (eco == null)
            return false;
        double fee = mail.getCostMoney();
        return eco.has(ms.getOfflinePlayer(), fee);
    }

    public boolean tryAcceptCostMoney(MailSender ms, MailData mail) {
        VaultEcoBridge eco = mailer.getVaultEco();
        if (eco == null)
            return false;
        double fee = mail.getCostMoney();

        OfflinePlayer from = mail.getFrom().getOfflinePlayer();
        double preTo = eco.getBalance(ms.getOfflinePlayer());
        double preFrom = eco.getBalance(from);

        if (!eco.has(ms.getOfflinePlayer(), fee))
            return false;
        if (!eco.withdrawPlayer(ms.getOfflinePlayer(), fee))
            return false;

        boolean depositResult = eco.depositPlayer(from, fee);

        // https://github.com/ucchyocean/UndineMailer/blob/862e4566b876852cf8ed77997cb4da11c46fa0c2/src/main/java/org/bitbucket/ucchy/undine/command/UndineAttachCommand.java#L423
        if ( !depositResult || ( mailer.getUndineConfig().getDepositErrorOnUnmatch()
                && ((preFrom + fee) > eco.getBalance(from)) ) ) {
            // 返金
            eco.setPlayer(ms.getOfflinePlayer(), preTo);
            eco.setPlayer(from, preFrom);
            return false;
        }

        // 着払いを0に設定して保存
        mail.setCostMoney(0);
        mailer.getMailManager().saveMail(mail);
        return true;
    }

    private boolean hasItem(Player player, ItemStack item) {
        int total = 0;
        for ( ItemStack i : player.getInventory().getContents() ) {
            if ( i != null && i.getType() == item.getType()
                    && i.getDurability() == item.getDurability() ) {
                total += i.getAmount();
                if ( total >= item.getAmount() ) return true;
            }
        }
        return false;
    }

    private boolean consumeItem(Player player, ItemStack item) {
        Inventory inv = player.getInventory();
        int remain = item.getAmount();
        for ( int index=0; index<inv.getSize(); index++ ) {
            ItemStack i = inv.getItem(index);
            if ( i == null || i.getType() != item.getType()
                    || i.getDurability() != item.getDurability() ) {
                continue;
            }

            if ( i.getAmount() >= remain ) {
                if ( i.getAmount() == remain ) {
                    inv.clear(index);
                } else {
                    i.setAmount(i.getAmount() - remain);
                    inv.setItem(index, i);
                }
                remain = 0;
                break;
            } else {
                remain -= i.getAmount();
                inv.clear(index);
            }
        }
        player.updateInventory();
        return (remain <= 0);
    }

    public boolean checkCostItem(Player player, MailSender ms, MailData mail) {
        ItemStack fee = mail.getCostItem();
        return hasItem(player, fee);
    }

    public boolean tryAcceptCostItem(Player player, MailSender ms, MailData mail) {
        ItemStack fee = mail.getCostItem();
        if (!hasItem(player, fee))
            return false;
        consumeItem(player, fee);

        // メールの送信元に送金
        MailData reply = new MailData();
        reply.setTo(0, mail.getFrom());
        reply.setFrom(ms);
        reply.setMessage(0, Messages.get(
                "BoxOpenCostItemSenderResult",
                new String[]{"%to", "%material", "%amount"},
                new String[]{ms.getName(), fee.getType().toString(), fee.getAmount() + ""}));

        int stackSize = fee.getType().getMaxStackSize();
        while ( fee.getAmount() > stackSize ) {
            reply.addAttachment(new ItemStack(fee.getType(), stackSize));
            fee.setAmount(fee.getAmount() - stackSize);
        }
        reply.addAttachment(fee);

        mailer.getMailManager().sendNewMail(reply);

        // 着払いをnullに設定して保存
        mail.setCostItem(null);
        mailer.getMailManager().saveMail(mail);
        return true;
    }

    public List<MailData> setReadFlagMails(MailSender mailSender) {
        if (!available())
            return Collections.emptyList();

        List<MailData> mails = mailer.getMailManager().getInboxMails(mailSender).stream()
                .filter(mail -> !mail.isRead(mailSender) && (mail.getAttachments().isEmpty() || mail.isAttachmentsCancelled()))
                .collect(Collectors.toList());

        mails.forEach(mail -> {
            mail.setReadFlag(mailSender);
            mailer.getMailManager().saveMail(mail);
        });

        return mails;
    }

    public List<MailData> setTrashFlagMails(MailSender mailSender) {
        if (!available())
            return Collections.emptyList();

        List<MailData> mails = mailer.getMailManager().getInboxMails(mailSender).stream()
                .filter((mail) -> mail.getAttachments().isEmpty())
                .collect(Collectors.toList());

        mails.forEach(mail -> {
            mail.setTrashFlag(mailSender);
            mailer.getMailManager().saveMail(mail);
        });

        return mails;
    }

    public TakeAttachmentsResult takeAllMailAttachments(MailSender mailSender, Player player) {
        if (!available())
           return new TakeAttachmentsResult(Collections.emptyList(), Collections.emptyList());

        List<MailData> mails = mailer.getMailManager().getInboxMails(mailSender).stream()
                .filter((mail) -> !mail.getAttachments().isEmpty() && !mail.isAttachmentsCancelled() && mail.getCostItem() == null && mail.getCostMoney() <= 0)
                .collect(Collectors.toList());
        List<MailData> fails = Lists.newArrayList();

        PlayerInventory inv = player.getInventory();
        for (MailData mail : mails) {
            boolean changed = false;
            for (ItemStack is : Lists.newArrayList(mail.getAttachments())) {
                Map<Integer, ItemStack> result = inv.addItem(is);
                if (result.isEmpty()) {
                    changed = true;
                    is.setAmount(0);
                } else {
                    for (ItemStack is2 : result.values()) {
                        if (is.getAmount() != is2.getAmount()) {
                            changed = true;
                            is.setAmount(is.getAmount() - is2.getAmount());
                        } else {
                            fails.add(mail);
                        }
                    }
                }
            }
            if (changed) {
                mail.setOpenAttachments();
                // sort
                Inventory fakeInv = Bukkit.createInventory(null, 54);
                mail.getAttachments().forEach(fakeInv::addItem);
                mail.setAttachments(Lists.newArrayList(fakeInv.getContents()));
                mailer.getMailManager().saveMail(mail);
            }
        }

        return new TakeAttachmentsResult(
                Collections.unmodifiableList(mails),
                Collections.unmodifiableList(fails)
        );
    }


    public static final class TakeAttachmentsResult {

        private final List<MailData> allMails;
        private final List<MailData> fails;

        public TakeAttachmentsResult(List<MailData> allMails, List<MailData> fails) {
            this.allMails = allMails;
            this.fails = fails;
        }

        public List<MailData> getAll() {
            return allMails;
        }

        public List<MailData> getFails() {
            return fails;
        }

    }


}
