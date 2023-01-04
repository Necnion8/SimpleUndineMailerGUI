package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util;

import com.google.common.collect.Lists;
import org.bukkit.plugin.Plugin;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.response.ModalFormResponse;
import org.geysermc.cumulus.response.result.FormResponseResult;
import org.geysermc.cumulus.response.result.InvalidFormResponseResult;
import org.geysermc.cumulus.response.result.ResultType;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ModalButtonForm implements ModalForm.Builder {
    private final ModalForm.Builder builder =  ModalForm.builder();
    private final List<Runnable> buttonActions = Lists.newArrayList();

    private ModalButtonForm(Plugin owner) {
        builder.validResultHandler(r -> {
            if (!owner.isEnabled())
                return;
            Optional.ofNullable(buttonActions.get(r.clickedButtonId())).ifPresent(Runnable::run);
        });
    }
    
    public static ModalButtonForm builder(Plugin owner) {
        return new ModalButtonForm(owner);
    }


    @Override
    public ModalButtonForm content(String s) {
        builder.content(s);
        return this;
    }

    @Override
    public ModalButtonForm button1(String s) {
        builder.button1(s);
        buttonActions.add(null);
        return this;
    }

    @Override
    public ModalButtonForm button2(String s) {
        builder.button2(s);
        buttonActions.add(null);
        return this;
    }

    public ModalButtonForm button1(String s, Runnable runnable) {
        builder.button1(s);
        buttonActions.add(runnable);
        return this;
    }

    public ModalButtonForm button2(String s, Runnable runnable) {
        builder.button2(s);
        buttonActions.add(runnable);
        return this;
    }


    @Override
    public ModalButtonForm title(String s) {
        builder.title(s);
        return this;
    }

    @Override
    public ModalButtonForm translator(BiFunction<String, String, String> biFunction, String s) {
        builder.translator(biFunction, s);
        return this;
    }

    @Override
    public ModalButtonForm translator(BiFunction<String, String, String> biFunction) {
        builder.translator(biFunction);
        return this;
    }

    @Override
    public ModalButtonForm closedResultHandler(Runnable runnable) {
        builder.closedResultHandler(runnable);
        return this;
    }

    @Override
    public ModalButtonForm closedResultHandler(Consumer<ModalForm> consumer) {
        builder.closedResultHandler(consumer);
        return this;
    }

    @Override
    public ModalButtonForm invalidResultHandler(Runnable runnable) {
        builder.invalidResultHandler(runnable);
        return this;
    }

    @Override
    public ModalButtonForm invalidResultHandler(Consumer<InvalidFormResponseResult<ModalFormResponse>> consumer) {
        builder.invalidResultHandler(consumer);
        return this;
    }

    @Override
    public ModalButtonForm invalidResultHandler(BiConsumer<ModalForm, InvalidFormResponseResult<ModalFormResponse>> biConsumer) {
        builder.invalidResultHandler(biConsumer);
        return this;
    }

    @Override
    public ModalButtonForm closedOrInvalidResultHandler(Runnable runnable) {
        builder.closedOrInvalidResultHandler(runnable);
        return this;
    }

    @Override
    public ModalButtonForm closedOrInvalidResultHandler(Consumer<FormResponseResult<ModalFormResponse>> consumer) {
        builder.closedOrInvalidResultHandler(consumer);
        return this;
    }

    @Override
    public ModalButtonForm closedOrInvalidResultHandler(BiConsumer<ModalForm, FormResponseResult<ModalFormResponse>> biConsumer) {
        builder.closedOrInvalidResultHandler(biConsumer);
        return this;
    }

    @Override
    public ModalButtonForm validResultHandler(Consumer<ModalFormResponse> consumer) {
        builder.validResultHandler(consumer);
        return this;
    }

    @Override
    public ModalButtonForm validResultHandler(BiConsumer<ModalForm, ModalFormResponse> biConsumer) {
        builder.validResultHandler(biConsumer);
        return this;
    }

    @Override
    public ModalButtonForm resultHandler(BiConsumer<ModalForm, FormResponseResult<ModalFormResponse>> biConsumer) {
        builder.resultHandler(biConsumer);
        return this;
    }

    @Override
    public ModalButtonForm resultHandler(BiConsumer<ModalForm, FormResponseResult<ModalFormResponse>> biConsumer, ResultType... resultTypes) {
        builder.resultHandler(biConsumer, resultTypes);
        return this;
    }

    @Override
    public ModalForm build() {
        return builder.build();
    }

}
