package de.damcraft.serverseeker.ssapi.responses;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Represents detailed information about a Minecraft server.
 * 
 * @param error Error message if request failed
 * @param cracked Whether the server allows cracked (offline-mode) accounts
 * @param description Server MOTD/description
 * @param lastSeen Unix timestamp when server was last seen online
 * @param maxPlayers Maximum player capacity
 * @param onlinePlayers Current online player count
 * @param protocol Server protocol version
 * @param version Minecraft version string
 * @param players List of recent players
 * @param hostname Server hostname if available
 * @param port Server port number
 * @param favicon Base64 encoded server icon
 * @param software Server software type
 * @param country Country code where server is hosted
 * @param uptime Estimated server uptime in hours
 * @param plugins List of plugins if detectable
 */
public record ServerInfoResponse(
    @Nullable String error,
    @Nullable Boolean cracked,
    @Nullable String description,
    @SerializedName("last_seen") @Nullable Integer lastSeen,
    @SerializedName("max_players") @Nullable Integer maxPlayers,
    @SerializedName("online_players") @Nullable Integer onlinePlayers,
    @Nullable Integer protocol,
    @Nullable String version,
    @Nullable List<Player> players,
    @Nullable String hostname,
    @Nullable Integer port,
    @Nullable String favicon,
    @Nullable SoftwareType software,
    @Nullable String country,
    @Nullable Integer uptime,
    @Nullable List<String> plugins
) {
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * Checks if the response contains an error
     */
    public boolean isError() {
        return error != null;
    }

    /**
     * Gets the server address (hostname:port)
     */
    public @Nullable String getAddress() {
        if (hostname == null) return null;
        return port != null ? hostname + ":" + port : hostname;
    }

    /**
     * Gets the last seen time as formatted string
     */
    public @Nullable String getLastSeenFormatted() {
        return lastSeen != null ? 
            DATE_FORMATTER.format(Instant.ofEpochSecond(lastSeen)) : 
            null;
    }

    /**
     * Gets the player count as string (online/max)
     */
    public String getPlayerCount() {
        return (onlinePlayers != null ? onlinePlayers : "?") + 
               "/" + 
               (maxPlayers != null ? maxPlayers : "?");
    }

    /**
     * Gets the server status (online/offline)
     */
    public boolean isOnline() {
        return onlinePlayers != null;
    }

    /**
     * Represents a player seen on the server
     */
    public record Player(
        @Nullable String name,
        @Nullable String uuid,
        @SerializedName("last_seen") @Nullable Integer lastSeen,
        @Nullable Integer playtimeMinutes,
        @Nullable String joinAddress
    ) {
        /**
         * Gets last seen time as formatted string
         */
        public @Nullable String getLastSeenFormatted() {
            return lastSeen != null ? 
                DATE_FORMATTER.format(Instant.ofEpochSecond(lastSeen)) : 
                null;
        }
    }

    /**
     * Server software types
     */
    public enum SoftwareType {
        @SerializedName("vanilla") VANILLA,
        @SerializedName("bukkit") BUKKIT,
        @SerializedName("spigot") SPIGOT,
        @SerializedName("paper") PAPER,
        @SerializedName("fabric") FABRIC,
        @SerializedName("forge") FORGE,
        @SerializedName("bungeecord") BUNGEECORD,
        @SerializedName("waterfall") WATERFALL,
        @SerializedName("velocity") VELOCITY,
        @SerializedName("unknown") UNKNOWN
    }

    /**
     * Builder for ServerInfoResponse
     */
    public static class Builder {
        private String error;
        private Boolean cracked;
        private String description;
        private Integer lastSeen;
        private Integer maxPlayers;
        private Integer onlinePlayers;
        private Integer protocol;
        private String version;
        private List<Player> players;
        private String hostname;
        private Integer port;
        private String favicon;
        private SoftwareType software;
        private String country;
        private Integer uptime;
        private List<String> plugins;

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder cracked(Boolean cracked) {
            this.cracked = cracked;
            return this;
        }

        // Additional builder methods for all fields...

        public ServerInfoResponse build() {
            return new ServerInfoResponse(
                error, cracked, description, lastSeen, maxPlayers,
                onlinePlayers, protocol, version, players, hostname,
                port, favicon, software, country, uptime, plugins
            );
        }
    }

    /**
     * Creates a builder for ServerInfoResponse
     */
    public static Builder builder() {
        return new Builder();
    }
}
