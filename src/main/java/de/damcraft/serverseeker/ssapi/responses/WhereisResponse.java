package de.damcraft.serverseeker.ssapi.responses;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Represents a response containing player location information across servers.
 */
public class WhereisResponse {
    @Nullable 
    private final String error;
    
    @SerializedName("total_results")
    private final Integer totalResults;
    
    @SerializedName("current_page")
    private final Integer currentPage;
    
    @SerializedName("total_pages")
    private final Integer totalPages;
    
    @Nullable
    private final List<Record> data;

    /**
     * Checks if the response contains an error.
     */
    public boolean isError() {
        return error != null;
    }

    /**
     * Represents a sighting record of a player on a server.
     */
    public static final class Record {
        @SerializedName("server_ip")
        private final String serverIp;
        
        @SerializedName("server_port")
        private final Integer serverPort;
        
        @SerializedName("server_hostname")
        @Nullable
        private final String serverHostname;
        
        @SerializedName("uuid")
        private final String playerUuid;
        
        @SerializedName("name")
        private final String playerName;
        
        @SerializedName("last_seen")
        private final Integer lastSeen;
        
        @SerializedName("playtime_minutes")
        @Nullable
        private final Integer playtimeMinutes;
        
        @SerializedName("join_method")
        @Nullable
        private final JoinMethod joinMethod;
        
        @SerializedName("country")
        @Nullable
        private final String countryCode;
        
        @SerializedName("server_version")
        @Nullable
        private final String serverVersion;

        /**
         * Player join method types.
         */
        public enum JoinMethod {
            @SerializedName("direct") DIRECT,
            @SerializedName("bungee") BUNGEE,
            @SerializedName("proxy") PROXY,
            @SerializedName("unknown") UNKNOWN
        }

        /**
         * Gets the server address in ip:port format.
         */
        public String getServerAddress() {
            return serverIp + ":" + serverPort;
        }

        /**
         * Gets the last seen time as a formatted string.
         */
        public String getLastSeenFormatted() {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochSecond(lastSeen));
        }

        /**
         * Gets playtime as hours and minutes string.
         */
        public String getPlaytimeFormatted() {
            if (playtimeMinutes == null) return "Unknown";
            int hours = playtimeMinutes / 60;
            int minutes = playtimeMinutes % 60;
            return String.format("%dh %dm", hours, minutes);
        }

        // Getters for all fields...
        public String getServerIp() { return serverIp; }
        public Integer getServerPort() { return serverPort; }
        public String getServerHostname() { return serverHostname; }
        public String getPlayerUuid() { return playerUuid; }
        public String getPlayerName() { return playerName; }
        public Integer getLastSeen() { return lastSeen; }
        public Integer getPlaytimeMinutes() { return playtimeMinutes; }
        public JoinMethod getJoinMethod() { return joinMethod; }
        public String getCountryCode() { return countryCode; }
        public String getServerVersion() { return serverVersion; }
    }

    /**
     * Builder for WhereisResponse.
     */
    public static final class Builder {
        private String error;
        private Integer totalResults;
        private Integer currentPage;
        private Integer totalPages;
        private List<Record> data;

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

        public Builder data(List<Record> data) {
            this.data = data;
            return this;
        }

        public WhereisResponse build() {
            return new WhereisResponse(this);
        }
    }

    private WhereisResponse(Builder builder) {
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
    public List<Record> getData() { return data; }

    /**
     * Creates a new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the most recent sighting of the player.
     */
    @Nullable
    public Record getMostRecentSighting() {
        if (data == null || data.isEmpty()) return null;
        
        Record mostRecent = data.get(0);
        for (Record record : data) {
            if (record.getLastSeen() > mostRecent.getLastSeen()) {
                mostRecent = record;
            }
        }
        return mostRecent;
    }
}
