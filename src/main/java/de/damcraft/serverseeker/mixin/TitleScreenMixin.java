package de.damcraft.serverseeker.mixin;

import de.damcraft.serverseeker.ServerSeeker;
import de.damcraft.serverseeker.gui.InstallMeteorScreen;
import de.damcraft.serverseeker.utils.UpdateChecker;
import meteordevelopment.meteorclient.utils.render.prompts.OkPrompt;
import meteordevelopment.meteorclient.utils.render.prompts.YesNoPrompt;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Unique private static boolean firstLoad = true;
    @Unique private static Instant lastUpdateCheck = Instant.EPOCH;
    @Unique private static final Duration UPDATE_CHECK_INTERVAL = Duration.ofDays(1);

    @Inject(at = @At("HEAD"), method = "init()V")
    private void onInit(CallbackInfo info) {
        checkMeteorInstallation(info);
    }

    @Inject(at = @At("TAIL"), method = "render")
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (firstLoad) {
            firstLoad = false;
            checkForUpdates();
        }
    }

    @Unique
    private void checkMeteorInstallation(CallbackInfo info) {
        if (!FabricLoader.getInstance().isModLoaded("meteor-client")) {
            info.cancel();
            MinecraftClient.getInstance().setScreen(new InstallMeteorScreen(() -> {
                if (FabricLoader.getInstance().isModLoaded("meteor-client")) {
                    MinecraftClient.getInstance().setScreen(new TitleScreen());
                }
            }));
        }
    }

    @Unique
    private void checkForUpdates() {
        Instant now = Instant.now();
        if (lastUpdateCheck.plus(UPDATE_CHECK_INTERVAL).isAfter(now)) return;

        lastUpdateCheck = now;
        CompletableFuture.runAsync(() -> {
            UpdateChecker.UpdateInfo updateInfo = UpdateChecker.checkForUpdates();
            if (updateInfo.isUpdateAvailable()) {
                MinecraftClient.getInstance().execute(() -> {
                    YesNoPrompt.create()
                        .title(Text.literal("ServerSeeker Update Available"))
                        .message(Text.literal(String.format(
                            "Version %s is available!\n\nCurrent: %s\n\nChangelog:\n%s",
                            updateInfo.latestVersion(),
                            ServerSeeker.VERSION,
                            updateInfo.changelog()
                        )))
                        .messageWidth(400)
                        .onYes(() -> Util.getOperatingSystem().open(updateInfo.downloadUrl()))
                        .id("serverseeker-update-prompt")
                        .show();
                });
            } else if (updateInfo.isError()) {
                MinecraftClient.getInstance().execute(() -> {
                    OkPrompt.create()
                        .title(Text.literal("Update Check Failed"))
                        .message(Text.literal("Couldn't check for updates. Please check manually later."))
                        .id("serverseeker-update-error")
                        .show();
                });
            }
        }).exceptionally(e -> {
            ServerSeeker.LOG.error("Update check failed", e);
            return null;
        });
    }

    @Unique
    private void showFirstTimeWelcome() {
        if (isFirstTimeUser()) {
            OkPrompt.create()
                .title(Text.literal("Welcome to ServerSeeker!"))
                .message(Text.literal(
                    "Thank you for installing ServerSeeker!\n\n" +
                    "Press F1 to view keybinds and access the main GUI through Meteor Client's module system."
                ))
                .id("serverseeker-welcome")
                .show();
        }
    }

    @Unique
    private boolean isFirstTimeUser() {
        // Check if this is the first run after installation
        return LocalDate.now(ZoneId.systemDefault())
            .equals(ServerSeeker.getInstallDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
    }
}
