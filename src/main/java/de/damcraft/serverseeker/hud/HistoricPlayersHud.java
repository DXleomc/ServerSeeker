package de.damcraft.serverseeker.hud;

import de.damcraft.serverseeker.ServerSeeker;
import de.damcraft.serverseeker.ssapi.responses.ServerInfoResponse;
import de.damcraft.serverseeker.utils.HistoricPlayersUpdater;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.network.PlayerListEntry;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class HistoricPlayersHud extends HudElement {
    public static final HudElementInfo<HistoricPlayersHud> INFO = new HudElementInfo<>(Hud.GROUP, "historic-players", "Displays current and historic players with join times.", HistoricPlayersHud::new);

    // Data
    private final Map<UUID, ServerInfoResponse.Player> historicPlayers = new ConcurrentHashMap<>();
    private volatile boolean isCracked = false;
    private volatile long lastUpdate = 0;

    // Settings groups
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAppearance = settings.createGroup("Appearance");
    private final SettingGroup sgFormatting = settings.createGroup("Formatting");

    // General Settings
    private final Setting<Boolean> showCurrentPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("show-current-players")
        .description("Show players currently online")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showHistoricPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("show-historic-players")
        .description("Show players who were previously on the server")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> limit = sgGeneral.add(new IntSetting.Builder()
        .name("limit")
        .description("Max players to display")
        .defaultValue(10)
        .min(1)
        .max(50)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Boolean> autoUpdate = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-update")
        .description("Automatically update player data")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> updateInterval = sgGeneral.add(new IntSetting.Builder()
        .name("update-interval")
        .description("Seconds between updates")
        .defaultValue(60)
        .min(10)
        .visible(autoUpdate::get)
        .build()
    );

    // Appearance Settings
    private final Setting<Alignment> alignment = sgAppearance.add(new EnumSetting.Builder<Alignment>()
        .name("alignment")
        .description("Text alignment")
        .defaultValue(Alignment.Auto)
        .build()
    );

    private final Setting<Boolean> showHeader = sgAppearance.add(new BoolSetting.Builder()
        .name("show-header")
        .description("Show the 'Players' header")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showCrackedStatus = sgAppearance.add(new BoolSetting.Builder()
        .name("show-cracked-status")
        .description("Show server cracked status")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showMoreIndicator = sgAppearance.add(new BoolSetting.Builder()
        .name("show-more-indicator")
        .description("Show '...and X more' when limited")
        .defaultValue(true)
        .build()
    );

    // Formatting Settings
    private final Setting<Format> timeFormat = sgFormatting.add(new EnumSetting.Builder<Format>()
        .name("time-format")
        .description("How to display last seen times")
        .defaultValue(Format.RELATIVE)
        .build()
    );

    private final Setting<String> customFormat = sgFormatting.add(new StringSetting.Builder()
        .name("custom-format")
        .description("Custom datetime format (when time format is Custom)")
        .defaultValue("MMM dd HH:mm")
        .visible(() -> timeFormat.get() == Format.CUSTOM)
        .build()
    );

    private final Setting<SettingColor> currentColor = sgFormatting.add(new ColorSetting.Builder()
        .name("current-color")
        .description("Color for current players")
        .defaultValue(new SettingColor(0, 255, 0))
        .build()
    );

    private final Setting<SettingColor> historicColor = sgFormatting.add(new ColorSetting.Builder()
        .name("historic-color")
        .description("Color for historic players")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> timeColor = sgFormatting.add(new ColorSetting.Builder()
        .name("time-color")
        .description("Color for time text")
        .defaultValue(new SettingColor(175, 175, 175))
        .build()
    );

    private final Setting<SettingColor> crackedColor = sgFormatting.add(new ColorSetting.Builder()
        .name("cracked-color")
        .description("Color for cracked status")
        .defaultValue(new SettingColor(255, 100, 100))
        .build()
    );

    public HistoricPlayersHud() {
        super(INFO);
        ServerSeeker.EXECUTOR.submit(this::updateData);
    }

    public void updatePlayers(List<ServerInfoResponse.Player> players, boolean cracked) {
        historicPlayers.clear();
        players.forEach(p -> historicPlayers.put(UUID.fromString(p.uuid()), p));
        this.isCracked = cracked;
        this.lastUpdate = System.currentTimeMillis();
    }

    @Override
    public void tick(HudRenderer renderer) {
        if (autoUpdate.get() && System.currentTimeMillis() - lastUpdate > updateInterval.get() * 1000L) {
            updateData();
        }
    }

    private void updateData() {
        HistoricPlayersUpdater.update().thenAccept(updated -> {
            if (updated) lastUpdate = System.currentTimeMillis();
        });
    }

    @Override
    public void render(HudRenderer renderer) {
        super.render(renderer);

        List<PlayerInfo> players = collectPlayers();
        if (players.isEmpty() && !showHeader.get()) return;

        double width = 0;
        double height = 0;
        int line = 0;

        // Render header
        if (showHeader.get()) {
            String header = "Players" + (isCracked && showCrackedStatus.get() ? " (Cracked)" : "");
            width = renderer.textWidth(header);
            renderer.text(header, x + alignX(width, alignment.get()), y, GuiThemes.get().textColor(), true);
            height += renderer.textHeight();
            line++;
        }

        // Render players
        int moreCount = Math.max(0, players.size() - limit.get());
        int renderCount = Math.min(limit.get(), players.size());

        for (int i = 0; i < renderCount; i++) {
            PlayerInfo player = players.get(i);
            String name = player.name();
            String timeText = formatTime(player.lastSeen());

            double nameWidth = renderer.textWidth(name);
            double timeWidth = renderer.textWidth(timeText);
            double totalWidth = nameWidth + timeWidth + 3; // +3 for space between

            width = Math.max(width, totalWidth);
            double xOffset = alignX(totalWidth, alignment.get());

            renderer.text(name, x + xOffset, y + height, player.isCurrent() ? currentColor.get() : historicColor.get(), true);
            renderer.text(" " + timeText, x + xOffset + nameWidth, y + height, timeColor.get(), true);
            height += renderer.textHeight();
            line++;
        }

        // Render "more" indicator
        if (moreCount > 0 && showMoreIndicator.get()) {
            String moreText = "... and " + moreCount + " more";
            double moreWidth = renderer.textWidth(moreText);
            width = Math.max(width, moreWidth);
            renderer.text(moreText, x + alignX(moreWidth, alignment.get()), y + height, GuiThemes.get().textColor(), true);
            height += renderer.textHeight();
        }

        // Render cracked status
        if (isCracked && showCrackedStatus.get() && line == 0) {
            String crackedText = "Cracked Server";
            width = renderer.textWidth(crackedText);
            renderer.text(crackedText, x + alignX(width, alignment.get()), y + height, crackedColor.get(), true);
            height += renderer.textHeight();
        }

        box.setSize(width, height);
    }

    private List<PlayerInfo> collectPlayers() {
        List<PlayerInfo> players = new ArrayList<>();

        // Add current players
        if (showCurrentPlayers.get() && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().getPlayerList().forEach(entry -> {
                String name = Names.get(entry.getProfile());
                players.add(new PlayerInfo(name, System.currentTimeMillis() / 1000, true));
                historicPlayers.remove(entry.getProfile().getId());
            });
        }

        // Add historic players
        if (showHistoricPlayers.get()) {
            historicPlayers.values().stream()
                .sorted(Comparator.comparing(ServerInfoResponse.Player::lastSeen).reversed())
                .forEach(p -> players.add(new PlayerInfo(p.name(), p.lastSeen(), false)));
        }

        return players;
    }

    private String formatTime(long lastSeen) {
        Instant instant = Instant.ofEpochSecond(lastSeen);
        Duration duration = Duration.between(instant, Instant.now());

        switch (timeFormat.get()) {
            case RELATIVE_SHORT: return formatRelativeTimeShort(duration);
            case RELATIVE_LONG: return formatRelativeTimeLong(duration);
            case ABSOLUTE: return DateTimeFormatter.ofPattern("MMM dd HH:mm").format(instant.atZone(ZoneId.systemDefault()));
            case CUSTOM: return DateTimeFormatter.ofPattern(customFormat.get()).format(instant.atZone(ZoneId.systemDefault()));
            case RELATIVE:
            default: return formatRelativeTime(duration);
        }
    }

    private String formatRelativeTime(Duration duration) {
        if (duration.toDays() > 365) return (duration.toDays() / 365) + "y";
        if (duration.toDays() > 30) return (duration.toDays() / 30) + "mo";
        if (duration.toDays() > 0) return duration.toDays() + "d";
        if (duration.toHours() > 0) return duration.toHours() + "h";
        if (duration.toMinutes() > 0) return duration.toMinutes() + "m";
        return duration.getSeconds() + "s";
    }

    private String formatRelativeTimeShort(Duration duration) {
        if (duration.toDays() > 0) return "<" + (duration.toDays() + 1) + "d";
        if (duration.toHours() > 0) return "<" + (duration.toHours() + 1) + "h";
        if (duration.toMinutes() > 0) return "<" + (duration.toMinutes() + 1) + "m";
        return "<1m";
    }

    private String formatRelativeTimeLong(Duration duration) {
        if (duration.toDays() > 365) return (duration.toDays() / 365) + " years ago";
        if (duration.toDays() > 30) return (duration.toDays() / 30) + " months ago";
        if (duration.toDays() > 0) return duration.toDays() + " days ago";
        if (duration.toHours() > 0) return duration.toHours() + " hours ago";
        if (duration.toMinutes() > 0) return duration.toMinutes() + " minutes ago";
        return "just now";
    }

    private double alignX(double width, Alignment alignment) {
        return switch (alignment) {
            case Center -> (box.width - width) / 2;
            case Right -> box.width - width;
            case Auto, Left -> 0;
        };
    }

    private record PlayerInfo(String name, long lastSeen, boolean isCurrent) {}

    public enum Format {
        RELATIVE("Relative (1h 30m)"),
        RELATIVE_SHORT("Relative Short (<2h)"),
        RELATIVE_LONG("Relative Long (2 hours ago)"),
        ABSOLUTE("Absolute (Jan 01 15:30)"),
        CUSTOM("Custom Format");

        private final String title;

        Format(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
