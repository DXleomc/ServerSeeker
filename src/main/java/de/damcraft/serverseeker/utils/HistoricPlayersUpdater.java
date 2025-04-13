package de.damcraft.serverseeker.utils;

import de.damcraft.serverseeker.ServerSeeker;
import de.damcraft.serverseeker.hud.HistoricPlayersHud;
import de.damcraft.serverseeker.ssapi.requests.ServerInfoRequest;
import de.damcraft.serverseeker.ssapi.responses.ServerInfoResponse;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class HistoricPlayersUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(HistoricPlayersUpdater.class);
    private static final String API_ENDPOINT = "https://api.serverseeker.net/server_info";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final int REQUEST_TIMEOUT_MS = 5000;

    private HistoricPlayersUpdater() {} // Prevent instantiation

    @EventHandler(priority = EventPriority.HIGH)
    private static void onGameJoinEvent(GameJoinedEvent event) {
        EXECUTOR.submit(HistoricPlayersUpdater::updatePlayersData);
    }

    public static CompletableFuture<Void> update() {
        return CompletableFuture.runAsync(HistoricPlayersUpdater::updatePlayersData, EXECUTOR);
    }

    private static void updatePlayersData() {
        try {
            List<HistoricPlayersHud> activeHuds = getActiveHuds();
            if (activeHuds.isEmpty()) return;

            ServerAddress serverAddress = resolveCurrentServerAddress();
            if (serverAddress == null) return;

            fetchAndUpdateServerInfo(activeHuds, serverAddress);
        } catch (Exception e) {
            LOG.error("Failed to update historic players data", e);
        }
    }

    @NotNull
    private static List<HistoricPlayersHud> getActiveHuds() {
        return Hud.get().stream()
            .filter(HudElement::isActive)
            .filter(HistoricPlayersHud.class::isInstance)
            .map(HistoricPlayersHud.class::cast)
            .collect(Collectors.toList());
    }

    private static ServerAddress resolveCurrentServerAddress() {
        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        if (networkHandler == null) {
            LOG.debug("No network handler available");
            return null;
        }

        // Try to get address from current server entry first
        ServerInfo currentServer = mc.getCurrentServerEntry();
        if (currentServer != null) {
            return ServerAddress.fromString(currentServer.address);
        }

        // Fallback to connection address parsing
        InetSocketAddress address = (InetSocketAddress) networkHandler.getConnection().getAddress();
        return new ServerAddress(address.getHostString(), address.getPort());
    }

    private static void fetchAndUpdateServerInfo(List<HistoricPlayersHud> huds, ServerAddress serverAddress) {
        ServerInfoRequest request = new ServerInfoRequest(
            ServerSeeker.API_KEY,
            serverAddress.host(),
            serverAddress.port()
        );

        ServerInfoResponse response = Http.post(API_ENDPOINT)
            .timeout(REQUEST_TIMEOUT_MS)
            .exceptionHandler(e -> LOG.error("Failed to fetch server info", e))
            .bodyJson(request)
            .sendJson(ServerInfoResponse.class);

        if (response == null || response.isError()) {
            LOG.warn("Failed to get server info: {}", response == null ? "null" : response.error());
            return;
        }

        updateHuds(huds, response);
    }

    private static void updateHuds(List<HistoricPlayersHud> huds, ServerInfoResponse response) {
        List<ServerInfoResponse.Player> players = Objects.requireNonNullElse(response.players(), List.of());
        Boolean isCracked = response.cracked();

        mc.execute(() -> {
            for (HistoricPlayersHud hud : huds) {
                hud.players = players;
                hud.isCracked = isCracked != null && isCracked;
                hud.update();
            }
        });
    }

    private record ServerAddress(String host, int port) {
        public static ServerAddress fromString(String address) {
            String[] parts = address.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;
            return new ServerAddress(host, port);
        }
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
    }
}
