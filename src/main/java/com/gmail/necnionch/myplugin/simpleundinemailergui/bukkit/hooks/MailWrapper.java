package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.hooks;

import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.ItemMailsResult;
import com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util.MailsResult;
import com.google.common.collect.Lists;
import org.bitbucket.ucchy.undine.*;
import org.bitbucket.ucchy.undine.bridge.VaultEcoBridge;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @SuppressWarnings("deprecation")
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

    public double getSendFee(MailData mail) {
        if ( mailer.getVaultEco() == null ) return 0;
        UndineConfig config = mailer.getUndineConfig();
        if ( !config.isEnableSendFee() ) return 0;

        // 添付がないなら、着払い設定をクリアする。
        if ( mail.getAttachments().size() == 0 ) {
            mail.setCostMoney(0);
            mail.setCostItem(null);
        }

        double total = 0;
        total += mail.getTo().size() * config.getSendFee();
        if ( config.isAttachFeePerAmount() ) {
            total += getItemAmount(mail.getAttachments()) * config.getAttachFee();
        } else {
            total += mail.getAttachments().size() * config.getAttachFee();
        }
        if ( mail.getCostMoney() > 0 ) {
            total += (mail.getCostMoney() * config.getCodMoneyTax() / 100);
        } else if ( mail.getCostItem() != null ) {
            total += (mail.getCostItem().getAmount() * config.getCodItemTax());
        }
        return total;
    }

    public int getItemAmount(List<ItemStack> items) {
        int total = 0;
        for ( ItemStack item : items ) {
            if ( item != null ) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public long getGapWithSpamProtectionMilliSeconds(MailSender sender) {
        String value = sender.getStringMetadata(MailManager.SENDTIME_METAKEY);
        if ( value == null ) return -1;
        long prev = Long.parseLong(value);
        long next = prev + mailer.getUndineConfig().getMailSpamProtectionSeconds() * 1000L;
        return next - System.currentTimeMillis();
    }

    // cost util

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

    @SuppressWarnings("deprecation")
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

    @SuppressWarnings("deprecation")
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

    // read / unread

    /**
     * 受信箱の未読メール全てに既読フラグを立てます<br>
     * 添付アイテムが残っているメールは失敗に分類されます
     */
    public MailsResult setReadFlagInboxMails(MailSender mailSender) {
        if (!available())
            return MailsResult.empty();

        List<MailData> mails = mailer.getMailManager().getInboxMails(mailSender).stream()
                .filter(mail -> !mail.isRead(mailSender))
                .collect(Collectors.toList());
        List<MailData> fails = Lists.newArrayList();

        for (MailData mail : mails) {
            if (mail.getAttachments().isEmpty() || mail.isAttachmentsCancelled()) {
                mail.setReadFlag(mailSender);
                mailer.getMailManager().saveMail(mail);
            } else {
                fails.add(mail);
            }
        }

        return MailsResult.of(mails, fails);
    }

    // trash

    /**
     * 受信箱の全メールにゴミ箱フラグを立てます<br>
     * 添付アイテムが残っている、または未読メールは失敗に分類されます
     */
    public MailsResult setTrashFlagInboxMails(MailSender mailSender) {
        if (!available())
            return MailsResult.empty();

        List<MailData> mails = Lists.newArrayList(mailer.getMailManager().getInboxMails(mailSender));
        List<MailData> fails = Lists.newArrayList();

        for (MailData mail : mails) {
            if (mail.isRead(mailSender) && mail.getAttachments().isEmpty()) {
                mail.setTrashFlag(mailSender);
                mailer.getMailManager().saveMail(mail);
            } else {
                fails.add(mail);
            }
        }

        return MailsResult.of(mails, fails);
    }

    /**
     * 送信箱の全メールにゴミ箱フラグを立てます<br>
     * 添付アイテムが残っているメールは失敗に分類されます
     */
    public MailsResult setTrashFlagOutboxMails(MailSender mailSender) {
        if (!available())
            return MailsResult.empty();

        List<MailData> mails = Lists.newArrayList(mailer.getMailManager().getOutboxMails(mailSender));
        List<MailData> fails = Lists.newArrayList();

        for (MailData mail : mails) {
            if (mail.getAttachments().isEmpty()) {
                mail.setTrashFlag(mailSender);
                mailer.getMailManager().saveMail(mail);
            } else {
                fails.add(mail);
            }
        }

        return MailsResult.of(mails, fails);
    }

    public MailsResult removeTrashFlagInboxMails(MailSender mailSender) {
        if (!available())
            return MailsResult.empty();

        List<MailData> mails = mailer.getMailManager().getTrashboxMails(mailSender).stream()
                .filter(mail -> mail.isAllMail()
                        || (mail.getToTotal() != null && mail.getToTotal().contains(mailSender))
                        || mail.getTo().contains(mailSender))
                .peek(mail -> {
                    mail.removeTrashFlag(mailSender);
                    mailer.getMailManager().saveMail(mail);
                })
                .collect(Collectors.toList());

        return MailsResult.of(mails, Collections.emptyList());
    }

    public MailsResult removeTrashFlagOutboxMails(MailSender mailSender) {
        if (!available())
            return MailsResult.empty();

        List<MailData> mails = mailer.getMailManager().getTrashboxMails(mailSender).stream()
                .filter(mail -> mail.getFrom().equals(mailSender))
                .peek(mail -> {
                    mail.removeTrashFlag(mailSender);
                    mailer.getMailManager().saveMail(mail);
                })
                .collect(Collectors.toList());

        return MailsResult.of(mails, Collections.emptyList());
    }

    // attachments

    /**
     * 受信箱の添付アイテムがある全メールからアイテムを inventory に移動します<br>
     * 着払い設定があるメール、または inventory が一杯などの理由でアイテムを1個も移動できなかったメールは失敗に分類されます<br>
     * 添付アイテムが空になったメールは既読に設定されます
     */
    public ItemMailsResult takeAllAttachmentsInboxMails(MailSender mailSender, Inventory inventory) {
        if (!available())
           return ItemMailsResult.empty();

        List<MailData> mails = mailer.getMailManager().getInboxMails(mailSender).stream()
                .filter((mail) -> !mail.getAttachments().isEmpty() && !mail.isAttachmentsCancelled())
                .collect(Collectors.toList());
        List<MailData> fails = Lists.newArrayList();
        List<ItemStack> items = Lists.newArrayList();
        List<ItemStack> failItems = Lists.newArrayList();

        for (MailData mail : mails) {
            List<ItemStack> attachments = mail.getAttachments().stream()
                    .map(ItemStack::clone)
                    .collect(Collectors.toList());

            if (mail.getCostItem() != null || mail.getCostMoney() > 0) {
                fails.add(mail);
                items.addAll(attachments);
                failItems.addAll(attachments);
                continue;
            }

            boolean changed = false;
            for (ItemStack is : attachments) {
                items.add(is.clone());
                Map<Integer, ItemStack> result = inventory.addItem(is.clone());
                if (result.isEmpty()) {  // all done
                    changed = true;
                    is.setAmount(0);
                } else {
                    for (ItemStack is2 : result.values()) {
                        if (is.getAmount() != is2.getAmount()) {  // moved
                            changed = true;
                            is.setAmount(is2.getAmount());
//                        } else {  // all fail
                        }
                        failItems.add(is2.clone());
                    }
                }
            }
            if (changed) {
                mail.setOpenAttachments();
                // sort
                Inventory fakeInv = Bukkit.createInventory(null, 54);
                fakeInv.addItem(attachments.toArray(new ItemStack[0]));
                mail.setAttachments(Stream.of(fakeInv.getContents())
                        .filter(is -> is != null && !Material.AIR.equals(is.getType()))
                        .collect(Collectors.toList()));
                if (mail.getAttachments().isEmpty())
                    mail.setReadFlag(mailSender);
                mailer.getMailManager().saveMail(mail);
            } else {
                fails.add(mail);
            }
        }

        return ItemMailsResult.of(mails, fails, items, failItems);
    }

}
