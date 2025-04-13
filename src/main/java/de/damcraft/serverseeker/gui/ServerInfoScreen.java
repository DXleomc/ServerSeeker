package de.damcraft.serverseeker.gui;

import com.google.common.net.HostAndPort;
import de.damcraft.serverseeker.ServerSeeker;
import de.damcraft.serverseeker.ssapi.requests.ServerInfoRequest;
import de.damcraft.serverseeker.ssapi.responses.ServerInfoResponse;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WTooltip;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.Clipboard;
import net.minecraft.text.Text;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.List;

import static de.damcraft.serverseeker.ServerSeeker.LOG;

public class ServerInfoScreen extends WindowScreen {
    private final String serverIp;

    public ServerInfoScreen(String serverIp) {
        super(GuiThemes.get(), "Server Info: " + serverIp);
        this.serverIp = serverIp;
    }

    @Override
    public void initWidgets() {
        add(theme.label("Fetching server info..."));

        HostAndPort hap = HostAndPort.fromString(serverIp);
        ServerInfoRequest request = new ServerInfoRequest(ServerSeeker.API_KEY, hap.getHost(), hap.getPort());

        MeteorExecutor.execute(() -> {
            ServerInfoResponse response = Http.post("https://api.serverseeker.net/server_info")
                .exceptionHandler(e -> LOG.error("Could not post to 'server_info': ", e))
                .bodyJson(request)
                .sendJson(ServerInfoResponse.class);

            this.client.execute(() -> {
                clear();

                if (response == null) {
                    add(theme.label("Network error")).expandX();
                    return;
                }

                if (response.isError()) {
                    add(theme.label(response.error())).expandX();
                    return;
                }

                load(response, hap);
            });
        });
    }

    private void load(ServerInfoResponse response, HostAndPort hap) {
        Boolean cracked = response.cracked();
        String description = response.description();
        int onlinePlayers = response.onlinePlayers();
        int maxPlayers = response.maxPlayers();
        int protocol = response.protocol();
        int lastSeen = response.lastSeen();
        String version = response.version();
        List<ServerInfoResponse.Player> players = response.players();

        WTable dataTable = add(theme.table()).expandX().widget();

        dataTable.add(theme.label("Cracked: "));
        dataTable.add(theme.label(cracked == null ? "Unknown" : cracked.toString())).tooltip("Indicates if the server supports cracked accounts");
        dataTable.row();

        dataTable.add(theme.label("Description: "));
        String cleanDesc = description.replace("\n", "\\n").replace("§r", "");
        dataTable.add(theme.label(cleanDesc.length() > 60 ? cleanDesc.substring(0, 60) + "..." : cleanDesc)).tooltip(cleanDesc);
        dataTable.row();

        dataTable.add(theme.label("Online Players: "));
        dataTable.add(theme.label(onlinePlayers + "/" + maxPlayers)).tooltip("Reported by last scan");
        dataTable.row();

        dataTable.add(theme.label("Version: "));
        dataTable.add(theme.label(version + " (" + protocol + ")")).tooltip("Reported Minecraft version & protocol");
        dataTable.row();

        dataTable.add(theme.label("Last Seen: "));
        String lastSeenFormatted = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .format(Instant.ofEpochSecond(lastSeen).atZone(ZoneId.systemDefault()).toLocalDateTime());
        dataTable.add(theme.label(lastSeenFormatted));
        dataTable.row();

        // Copy IP
        WButton copyIpButton = theme.button("Copy IP");
        copyIpButton.action = () -> MinecraftClient.getInstance().keyboard.setClipboard(serverIp);
        add(copyIpButton).expandX();

        // Ping Button
        WButton pingButton = theme.button("Ping Server");
        pingButton.action = () -> {
            long start = System.currentTimeMillis();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(hap.getHost(), hap.getPort()), 1000);
                long ping = System.currentTimeMillis() - start;
                add(theme.label("Ping: " + ping + " ms"));
            } catch (Exception e) {
                add(theme.label("Ping failed: " + e.getMessage()));
            }
        };
        add(pingButton).expandX();

        // Join Button
        WButton joinServerButton = theme.button("Join this Server");
        joinServerButton.action = () -> ConnectScreen.connect(new TitleScreen(), MinecraftClient.getInstance(),
            new ServerAddress(hap.getHost(), hap.getPort()),
            new ServerInfo("ServerSeeker - " + serverIp, hap.toString(), ServerInfo.ServerType.OTHER),
            false, null);
        add(joinServerButton).expandX();

        // Player Table
        if (!players.isEmpty()) {
            players.sort(Comparator.comparing(ServerInfoResponse.Player::lastSeen).reversed());

            WTable playersTable = add(theme.table()).expandX().widget();

            playersTable.row();
            playersTable.add(theme.label("Players (" + players.size() + ")")).expandX();
            playersTable.row();
            playersTable.add(theme.horizontalSeparator()).expandX();
            playersTable.row();

            playersTable.add(theme.label("Name")).expandX();
            playersTable.add(theme.label("Last Seen")).expandX();
            playersTable.row();

            for (ServerInfoResponse.Player player : players) {
                String name = player.name();
                String seen = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    .format(Instant.ofEpochSecond(player.lastSeen()).atZone(ZoneId.systemDefault()).toLocalDateTime());

                WButton playerNameButton = theme.button(name);
                playerNameButton.action = () -> MinecraftClient.getInstance().setScreen(new FindPlayerScreen(null)); // Replace null if multiplayer screen ref exists

                playersTable.add(playerNameButton).expandX();
                playersTable.add(theme.label(seen)).expandX();
                playersTable.row();
            }
        }
    }
                    }
