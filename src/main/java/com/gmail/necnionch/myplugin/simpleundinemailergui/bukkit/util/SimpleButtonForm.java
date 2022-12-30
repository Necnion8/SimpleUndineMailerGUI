package com.gmail.necnionch.myplugin.simpleundinemailergui.bukkit.util;

import com.google.common.collect.Lists;
import org.bukkit.plugin.Plugin;
import org.geysermc.cumulus.component.ButtonComponent;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.response.result.FormResponseResult;
import org.geysermc.cumulus.response.result.InvalidFormResponseResult;
import org.geysermc.cumulus.response.result.ResultType;
import org.geysermc.cumulus.util.FormImage;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class SimpleButtonForm implements SimpleForm.Builder {
    private final SimpleForm.Builder builder = SimpleForm.builder();
    private final List<Runnable> buttonActions = Lists.newArrayList();

    private SimpleButtonForm(Plugin owner) {
        builder.validResultHandler(r -> {
            if (!owner.isEnabled())
                return;
            Optional.ofNullable(buttonActions.get(r.clickedButtonId())).ifPresent(Runnable::run);
        });
    }
    
    public static SimpleButtonForm builder(Plugin owner) {
        return new SimpleButtonForm(owner);
    }

    @Override
    public SimpleButtonForm content(String s) {
        builder.content(s);
        return this;
    }

    @Override
    public SimpleButtonForm button(ButtonComponent buttonComponent) {
        builder.button(buttonComponent);
        buttonActions.add(null);
        return this;
    }

    @Override
    public SimpleButtonForm button(String s, FormImage.Type type, String s1) {
        builder.button(s, type, s1);
        buttonActions.add(null);
        return this;
    }

    @Override
    public SimpleButtonForm button(String s, FormImage formImage) {
        builder.button(s, formImage);
        buttonActions.add(null);
        return this;
    }

    @Override
    public SimpleButtonForm button(String s) {
        builder.button(s);
        buttonActions.add(null);
        return this;
    }

    public SimpleButtonForm button(ButtonComponent buttonComponent, Runnable click) {
        builder.button(buttonComponent);
        buttonActions.add(click);
        return this;
    }

    public SimpleButtonForm button(String s, FormImage.Type type, String s1, Runnable click) {
        builder.button(s, type, s1);
        buttonActions.add(click);
        return this;
    }

    public SimpleButtonForm button(String s, FormImage formImage, Runnable click) {
        builder.button(s, formImage);
        buttonActions.add(click);
        return this;
    }

    public SimpleButtonForm button(String s, Runnable click) {
        builder.button(s);
        buttonActions.add(click);
        return this;
    }

    @Override
    public SimpleButtonForm optionalButton(String s, FormImage.Type type, String s1, boolean b) {
        builder.optionalButton(s, type, s1, b);
        buttonActions.add(null);
        return this;
    }

    @Override
    public SimpleButtonForm optionalButton(String s, FormImage formImage, boolean b) {
        builder.optionalButton(s, formImage, b);
        buttonActions.add(null);
        return this;
    }

    @Override
    public SimpleButtonForm optionalButton(String s, boolean b) {
        builder.optionalButton(s, b);
        buttonActions.add(null);
        return this;
    }

    @Override
    public SimpleButtonForm title(String s) {
        builder.title(s);
        return this;
    }

    @Override
    public SimpleButtonForm translator(BiFunction<String, String, String> biFunction, String s) {
        builder.translator(biFunction, s);
        return this;
    }

    @Override
    public SimpleButtonForm translator(BiFunction<String, String, String> biFunction) {
        builder.translator(biFunction);
        return this;
    }

    @Override
    public SimpleButtonForm closedResultHandler(Runnable runnable) {
        builder.closedResultHandler(runnable);
        return this;
    }

    @Override
    public SimpleButtonForm closedResultHandler(Consumer<SimpleForm> consumer) {
        builder.closedResultHandler(consumer);
        return this;
    }

    @Override
    public SimpleButtonForm invalidResultHandler(Runnable runnable) {
        builder.invalidResultHandler(runnable);
        return this;
    }

    @Override
    public SimpleButtonForm invalidResultHandler(Consumer<InvalidFormResponseResult<SimpleFormResponse>> consumer) {
        builder.invalidResultHandler(consumer);
        return this;
    }

    @Override
    public SimpleButtonForm invalidResultHandler(BiConsumer<SimpleForm, InvalidFormResponseResult<SimpleFormResponse>> biConsumer) {
        builder.invalidResultHandler(biConsumer);
        return this;
    }

    @Override
    public SimpleButtonForm closedOrInvalidResultHandler(Runnable runnable) {
        builder.closedOrInvalidResultHandler(runnable);
        return this;
    }

    @Override
    public SimpleButtonForm closedOrInvalidResultHandler(Consumer<FormResponseResult<SimpleFormResponse>> consumer) {
        builder.closedOrInvalidResultHandler(consumer);
        return this;
    }

    @Override
    public SimpleButtonForm closedOrInvalidResultHandler(BiConsumer<SimpleForm, FormResponseResult<SimpleFormResponse>> biConsumer) {
        builder.closedOrInvalidResultHandler(biConsumer);
        return this;
    }

    @Override
    public SimpleButtonForm validResultHandler(Consumer<SimpleFormResponse> consumer) {
        builder.validResultHandler(consumer);
        return this;
    }

    @Override
    public SimpleButtonForm validResultHandler(BiConsumer<SimpleForm, SimpleFormResponse> biConsumer) {
        builder.validResultHandler(biConsumer);
        return this;
    }

    @Override
    public SimpleButtonForm resultHandler(BiConsumer<SimpleForm, FormResponseResult<SimpleFormResponse>> biConsumer) {
        builder.resultHandler(biConsumer);
        return this;
    }

    @Override
    public SimpleButtonForm resultHandler(BiConsumer<SimpleForm, FormResponseResult<SimpleFormResponse>> biConsumer, ResultType... resultTypes) {
        builder.resultHandler(biConsumer, resultTypes);
        return this;
    }

    @Override
    public SimpleForm build() {
        return builder.build();
    }

}
