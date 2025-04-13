package de.damcraft.serverseeker.gui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import de.damcraft.serverseeker.SmallHttp;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class InstallMeteorScreen extends Screen {
    private static final Logger LOGGER = Logger.getLogger("InstallMeteorScreen");
    private String statusMessage = "";

    public InstallMeteorScreen() {
        super(Text.of("Meteor Client is not installed!"));
    }

    @Override
    protected void init() {
        super.init();
        updateUI();
    }

    private void updateUI() {
        clearChildren();

        addDrawableChild(ButtonWidget.builder(Text.of("Automatically install Meteor (§arecommended§r)"), (button) -> {
            button.active = false;
            statusMessage = "Downloading Meteor Client...";
            updateUI();

            CompletableFuture.runAsync(() -> {
                install();
                button.active = true;
            });
        }).dimensions(width / 2 - 150, height / 4 + 100, 300, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.of("Manual installation"), (button) ->
            Util.getOperatingSystem().open("https://meteorclient.com/faq/installation")
        ).dimensions(width / 2 - 150, height / 4 + 130, 145, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.of("Open mods folder"), (button) -> {
            Path modsFolder = FabricLoader.getInstance().getGameDir().resolve("mods");
            Util.getOperatingSystem().open(modsFolder.toUri());
        }).dimensions(width / 2 + 5, height / 4 + 130, 145, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("menu.quit"), (button) ->
            this.client.scheduleStop()
        ).dimensions(width / 2 - 75, height / 4 + 160, 150, 20).build());

        if (!statusMessage.isEmpty()) {
            addDrawableChild(new TextWidget(width / 2 - 140, height / 4 + 70, 280, 20, Text.of(statusMessage), textRenderer));
        }
    }

    private void install() {
        String result = SmallHttp.get("https://meteorclient.com/api/stats");
        if (result == null) {
            setStatus("Failed to fetch version info. Try manual installation.");
            return;
        }

        JsonObject json = new Gson().fromJson(result, JsonObject.class);
        String currentVersion = SharedConstants.getGameVersion().getName();
        String stableVersion = json.get("mc_version").getAsString();
        String devBuildVersion = json.get("dev_build_mc_version").getAsString();

        String url;
        if (currentVersion.equals(stableVersion)) {
            url = "https://meteorclient.com/api/download";
        } else if (currentVersion.equals(devBuildVersion)) {
            url = "https://meteorclient.com/api/download?devBuild=latest";
        } else {
            setStatus("No compatible Meteor version found for your Minecraft version.");
            return;
        }

        HttpResponse<InputStream> file = SmallHttp.download(url);
        if (file == null) {
            setStatus("Failed to download Meteor Client.");
            return;
        }

        String filename = file.headers().firstValue("Content-Disposition")
            .map(header -> header.replaceAll(".*filename=\"?([^\"]+)\"?", "$1"))
            .orElse("meteor-client.jar");

        Path modsFolder = FabricLoader.getInstance().getGameDir().resolve("mods");
        if (!Files.exists(modsFolder)) {
            setStatus("Mods folder not found.");
            return;
        }

        Path filePath = modsFolder.resolve(filename);
        if (Files.exists(filePath)) {
            setStatus("Meteor Client already exists in mods folder.");
            return;
        }

        try {
            Files.copy(file.body(), filePath);
            setStatus("Meteor Client installed successfully. Please restart the game.");
        } catch (IOException e) {
            LOGGER.warning("Error saving Meteor jar: " + e);
            setStatus("Failed to save Meteor Client. Check file permissions.");
        }
    }

    private void setStatus(String message) {
        this.statusMessage = message;
        LOGGER.info(message);
        client.execute(this::updateUI);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 4 - 20, 0xFFFFFF);
    }
}
