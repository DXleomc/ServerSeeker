package de.damcraft.serverseeker.ssapi.responses;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Represents a response containing multiple server entries from a search query.
 */
public class ServersResponse {
    @Nullable 
    private final String error;
    
    @SerializedName("total_results")
    private final Integer totalResults;
    
    @SerializedName("page")
    private final Integer currentPage;
    
    @SerializedName("total_pages")
    private final Integer totalPages;
    
    @Nullable
    private final List<Server> data;

    /**
     * Checks if the response contains an error.
     */
    public boolean isError() {
        return error != null;
    }

    /**
     * Represents detailed information about a Minecraft server in the results.
     */
    public static final class Server {
        @SerializedName("ip")
        private final String ipAddress;
        
        @SerializedName("port")
        private final Integer port;
        
        @Nullable
        private final Boolean cracked;
        
        @Nullable
        private final String description;
        
        @SerializedName("last_seen")
        private final Integer lastSeen;
        
        @SerializedName("max_players")
        private final Integer maxPlayers;
        
        @SerializedName("online_players")
        private final Integer onlinePlayers;
        
        @Nullable
        private final Integer protocol;
        
        @Nullable
        private final String version;
        
        @SerializedName("hostname")
        @Nullable
        private final String hostname;
        
        @SerializedName("country")
        @Nullable
        private final String countryCode;
        
        @SerializedName("software")
        @Nullable
        private final SoftwareType software;
        
        @SerializedName("uptime")
        @Nullable
        private final Integer uptimeHours;
        
        @SerializedName("asn")
        @Nullable
        private final Integer asn;
        
        @SerializedName("plugins")
        @Nullable
        private final List<String> plugins;

        // Server types enumeration
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
         * Gets the server address in ip:port format.
         */
        public String getAddress() {
            return ipAddress + ":" + port;
        }

        /**
         * Gets the last seen time as a formatted string.
         */
        public String getLastSeenFormatted() {
            return lastSeen != null ? 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(lastSeen)) : 
                "Unknown";
        }

        /**
         * Gets the player count as "online/max" string.
         */
        public String getPlayerCount() {
            return (onlinePlayers != null ? onlinePlayers : "?") + 
                   "/" + 
                   (maxPlayers != null ? maxPlayers : "?");
        }

        /**
         * Checks if the server is currently online.
         */
        public boolean isOnline() {
            return onlinePlayers != null;
        }

        // Getters for all fields...
        public String getIpAddress() { return ipAddress; }
        public Integer getPort() { return port; }
        public Boolean isCracked() { return cracked; }
        public String getDescription() { return description; }
        public Integer getLastSeen() { return lastSeen; }
        public Integer getMaxPlayers() { return maxPlayers; }
        public Integer getOnlinePlayers() { return onlinePlayers; }
        public Integer getProtocol() { return protocol; }
        public String getVersion() { return version; }
        public String getHostname() { return hostname; }
        public String getCountryCode() { return countryCode; }
        public SoftwareType getSoftware() { return software; }
        public Integer getUptimeHours() { return uptimeHours; }
        public Integer getAsn() { return asn; }
        public List<String> getPlugins() { return plugins; }
    }

    /**
     * Builder for ServersResponse.
     */
    public static final class Builder {
        private String error;
        private Integer totalResults;
        private Integer currentPage;
        private Integer totalPages;
        private List<Server> data;

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder totalResults(Integer totalResults) {
            this.totalResults = totalResults;
            return this;
        }

        public Builder currentPage(Integer currentPage) {
            this.currentPage = currentPage;
            return this;
        }

        public Builder totalPages(Integer totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder data(List<Server> data) {
            this.data = data;
            return this;
        }

        public ServersResponse build() {
            return new ServersResponse(this);
        }
    }

    private ServersResponse(Builder builder) {
        this.error = builder.error;
        this.totalResults = builder.totalResults;
        this.currentPage = builder.currentPage;
        this.totalPages = builder.totalPages;
        this.data = builder.data;
    }

    // Getters
    public String getError() { return error; }
    public Integer getTotalResults() { return totalResults; }
    public Integer getCurrentPage() { return currentPage; }
    public Integer getTotalPages() { return totalPages; }
    public List<Server> getData() { return data; }

    /**
     * Creates a new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }
}
