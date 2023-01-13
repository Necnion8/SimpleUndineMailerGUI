package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util;

import org.bitbucket.ucchy.undine.MailData;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class ItemMailsResult extends MailsResult {
    private final List<ItemStack> allItems;
    private final List<ItemStack> failItems;

    public ItemMailsResult(List<MailData> allMails, List<MailData> fails, List<ItemStack> allItems, List<ItemStack> failItems) {
        super(allMails, fails);
        this.allItems = allItems;
        this.failItems = failItems;
    }

    public static ItemMailsResult empty() {
        return new ItemMailsResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public static ItemMailsResult of(List<MailData> allMails, List<MailData> fails, List<ItemStack> allItems, List<ItemStack> failItems) {
        return new ItemMailsResult(
                Collections.unmodifiableList(allMails),
                Collections.unmodifiableList(fails),
                Collections.unmodifiableList(allItems),
                Collections.unmodifiableList(failItems)
        );
    }

    public List<ItemStack> getAllItems() {
        return allItems;
    }

    public List<ItemStack> getFailItems() {
        return failItems;
    }

    public int totalItemCount() {
        return allItems.stream().mapToInt(ItemStack::getAmount).sum();
    }

    public int failItemCount() {
        return failItems.stream().mapToInt(ItemStack::getAmount).sum();
    }

}
