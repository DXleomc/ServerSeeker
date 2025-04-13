package de.damcraft.serverseeker.gui;

import de.damcraft.serverseeker.ServerSeeker;
import de.damcraft.serverseeker.ssapi.requests.ServerInfoRequest;
import de.damcraft.serverseeker.ssapi.responses.ServerInfoResponse;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.systems.accounts.types.CrackedAccount;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.Util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import static de.damcraft.serverseeker.ServerSeeker.LOG;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class GetInfoScreen extends WindowScreen {
    private final MultiplayerServerListWidget.Entry entry;
    private long latency = -1;
    private String serverVersion = "Unknown";

    public GetInfoScreen(MultiplayerScreen multiplayerScreen, MultiplayerServerListWidget.Entry entry) {
        super(GuiThemes.get(), "Get players");
        this.parent = multiplayerScreen;
        this.entry = entry;
    }

    @Override
    public void initWidgets() {
        if (entry == null || !(entry instanceof MultiplayerServerListWidget.ServerEntry)) {
            add(theme.label("No server selected"));
            return;
        }

        ServerInfo serverInfo = ((MultiplayerServerListWidget.ServerEntry) entry).getServer();
        String address = resolveIp(serverInfo.address);
        if (address == null) {
            add(theme.label("Invalid or unsupported address."));
            return;
        }

        pingServer(serverInfo);
        add(theme.label("Loading..."));
        fetchServerInfo(address);
    }

    private String resolveIp(String address) {
        if (address.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?::[0-9]{1,5})?$")) return address;

        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            return inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private void pingServer(ServerInfo serverInfo) {
        serverInfo.pinged = false;
        mc.getCurrentServerEntry().label = serverInfo.label;
        mc.getCurrentServerEntry().address = serverInfo.address;
        mc.getCurrentServerEntry().icon = serverInfo.getIcon();
        mc.getCurrentServerEntry().ping = -2;

        new Thread(() -> {
            try {
                mc.getServerList().ping(mc.getCurrentServerEntry());
                latency = mc.getCurrentServerEntry().ping;
            } catch (Exception ignored) {}
        }).start();
    }

    private void fetchServerInfo(String address) {
        String[] parts = address.split(":");
        String ip = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;

        ServerInfoRequest request = new ServerInfoRequest(ServerSeeker.API_KEY, ip, port);

        MeteorExecutor.execute(() -> {
            ServerInfoResponse response = Http.post("https://api.serverseeker.net/server_info")
                .exceptionHandler(e -> LOG.error("Could not post to 'server_info': ", e))
                .bodyJson(request)
                .sendJson(ServerInfoResponse.class);

            MinecraftClient.getInstance().execute(() -> {
                clear();
                if (response == null) {
                    add(theme.label("Network error")).expandX();
                    return;
                }

                if (response.isError()) {
                    add(theme.label(response.error())).expandX();
                    return;
                }

                if (latency != -1) {
                    add(theme.label("Server latency: " + latency + " ms")).expandX();
                }

                serverVersion = response.version() != null ? response.version() : "Unknown";
                fetchServerLocation(ip);
                loadPlayers(response);
            });
        });
    }

    private void fetchServerLocation(String ip) {
        MeteorExecutor.execute(() -> {
            String locationUrl = "http://ip-api.com/json/" + ip;
            String locationResponse = Http.get(locationUrl).sendString();

            MinecraftClient.getInstance().execute(() -> {
                if (locationResponse != null) {
                    try {
                        // Parse JSON response to extract country and region
                        String country = locationResponse.split("\"country\":\"")[1].split("\"")[0];
                        String region = locationResponse.split("\"regionName\":\"")[1].split("\"")[0];

                        add(theme.label("Server Location: " + country + ", " + region)).expandX();
                    } catch (Exception e) {
                        add(theme.label("Failed to fetch server location")).expandX();
                    }
                }
            });
        });
    }

    private void loadPlayers(ServerInfoResponse response) {
        List<ServerInfoResponse.Player> players = response.players();

        if (players.isEmpty()) {
            add(theme.label("No records of players found.")).expandX();
            return;
        }

        if (Boolean.FALSE.equals(response.cracked())) {
            add(theme.label("Attention: The server is NOT cracked!")).expandX();
        }

        add(theme.label("Server Version: " + serverVersion)).expandX();
        add(theme.label("Found " + players.size() + " players:"));

        WTable table = add(theme.table()).widget();
        table.add(theme.label("Name"));
        table.add(theme.label("Last seen"));
        table.add(theme.label("UUID"));
        table.add(theme.label("Actions"));
        table.row();

        for (ServerInfoResponse.Player player : players) {
            String lastSeenStr = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .format(Instant.ofEpochSecond(player.lastSeen()).atZone(ZoneId.systemDefault()).toLocalDateTime());

            table.add(theme.label(player.name()));
            table.add(theme.label(lastSeenStr));
            table.add(theme.label(player.uuid()));

            WButton loginBtn = theme.button("Login");
            loginBtn.action = () -> {
                loginCracked(player.name());
                close();
            };

            WButton uuidBtn = theme.button("Copy UUID");
            uuidBtn.action = () -> {
                mc.keyboard.setClipboard(player.uuid());
            };

            WButton profileBtn = theme.button("NameMC");
            profileBtn.action = () -> {
                Util.getOperatingSystem().open("https://namemc.com/profile/" + player.uuid());
            };

            table.add(theme.horizontalList(loginBtn, uuidBtn, profileBtn)).expandCellX();
            table.row();
        }
    }

    private void loginCracked(String name) {
        for (Account<?> account : Accounts.get()) {
            if (account instanceof CrackedAccount && account.getUsername().equals(name)) {
                account.login();
                return;
            }
        }
        CrackedAccount account = new CrackedAccount(name);
        account.login();
        Accounts.get().add(account);
    }
                    }
