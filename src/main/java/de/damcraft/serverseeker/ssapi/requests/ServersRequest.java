package de.damcraft.serverseeker.ssapi.requests;

import com.google.gson.JsonArray;
import com.google.gson.annotations.SerializedName;
import de.damcraft.serverseeker.ServerSeeker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a comprehensive server search request with multiple filtering options.
 */
public class ServersRequest {
    @SerializedName("api_key")
    private final String apiKey = ServerSeeker.API_KEY;

    // Filtering parameters
    private Integer asn;
    private String countryCode;
    private Boolean cracked;
    private String description;
    private JsonArray maxPlayers;
    private Integer onlineAfter;
    private JsonArray onlinePlayers;
    private Integer protocol;
    private Boolean ignoreModded;
    private Boolean onlyBungeeSpoofable;
    private Software software;
    private Integer minUptime;
    private String version;
    private String hostname;
    private Boolean hasPlayers;
    private Boolean hasSpecificPlayer;
    private String playerName;

    /**
     * Server software types
     */
    public enum Software {
        @SerializedName("any") ANY,
        @SerializedName("bukkit") BUKKIT,
        @SerializedName("spigot") SPIGOT,
        @SerializedName("paper") PAPER,
        @SerializedName("vanilla") VANILLA,
        @SerializedName("fabric") FABRIC,
        @SerializedName("forge") FORGE,
        @SerializedName("bungeecord") BUNGEECORD,
        @SerializedName("waterfall") WATERFALL,
        @SerializedName("velocity") VELOCITY
    }

    // Builder pattern for fluent construction
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ServersRequest request = new ServersRequest();

        public Builder asn(Integer asn) {
            request.setAsn(asn);
            return this;
        }

        public Builder countryCode(String countryCode) {
            request.setCountryCode(countryCode);
            return this;
        }

        public Builder cracked(Boolean cracked) {
            request.setCracked(cracked);
            return this;
        }

        public Builder description(String description) {
            request.setDescription(description);
            return this;
        }

        public Builder maxPlayers(Integer exact) {
            request.setMaxPlayers(exact);
            return this;
        }

        public Builder maxPlayers(Integer min, Integer max) {
            request.setMaxPlayers(min, max);
            return this;
        }

        public Builder onlineAfter(Instant timestamp) {
            request.setOnlineAfter(timestamp);
            return this;
        }

        public Builder onlinePlayers(Integer exact) {
            request.setOnlinePlayers(exact);
            return this;
        }

        public Builder onlinePlayers(Integer min, Integer max) {
            request.setOnlinePlayers(min, max);
            return this;
        }

        public Builder protocol(Integer version) {
            request.setProtocolVersion(version);
            return this;
        }

        public Builder software(Software software) {
            request.setSoftware(software);
            return this;
        }

        public Builder ignoreModded(Boolean ignore) {
            request.setIgnoreModded(ignore);
            return this;
        }

        public Builder onlyBungeeSpoofable(Boolean only) {
            request.setOnlyBungeeSpoofable(only);
            return this;
        }

        public Builder minUptime(Integer hours) {
            request.setMinUptime(hours);
            return this;
        }

        public Builder version(String version) {
            request.setVersion(version);
            return this;
        }

        public Builder hostname(String hostname) {
            request.setHostname(hostname);
            return this;
        }

        public Builder hasPlayers(Boolean hasPlayers) {
            request.setHasPlayers(hasPlayers);
            return this;
        }

        public Builder hasSpecificPlayer(String playerName) {
            request.setHasSpecificPlayer(playerName);
            return this;
        }

        public ServersRequest build() {
            return request;
        }
    }

    // Setters with validation
    public void setAsn(@Nullable Integer asn) {
        if (asn != null && asn < 0) {
            throw new IllegalArgumentException("ASN must be positive");
        }
        this.asn = asn;
    }

    public void setCountryCode(@Nullable String countryCode) {
        if (countryCode != null && countryCode.length() != 2) {
            throw new IllegalArgumentException("Country code must be 2 characters");
        }
        this.countryCode = countryCode != null ? countryCode.toUpperCase() : null;
    }

    public void setCracked(@Nullable Boolean cracked) {
        this.cracked = cracked;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public void setMaxPlayers(@NotNull Integer exact) {
        validatePlayerCount(exact);
        this.maxPlayers = new JsonArray();
        this.maxPlayers.add(exact);
        this.maxPlayers.add(exact);
    }

    public void setMaxPlayers(@NotNull Integer min, @NotNull Integer max) {
        validatePlayerCount(min);
        if (max != -1) validatePlayerCount(max);
        if (min > max && max != -1) {
            throw new IllegalArgumentException("Min players cannot be greater than max players");
        }

        this.maxPlayers = new JsonArray();
        this.maxPlayers.add(min);
        this.maxPlayers.add(max == -1 ? "inf" : max);
    }

    public void setOnlineAfter(@Nullable Integer unixTimestamp) {
        if (unixTimestamp != null && unixTimestamp < 0) {
            throw new IllegalArgumentException("Timestamp cannot be negative");
        }
        this.onlineAfter = unixTimestamp;
    }

    public void setOnlineAfter(@Nullable Instant instant) {
        this.onlineAfter = instant != null ? (int) instant.getEpochSecond() : null;
    }

    public void setOnlinePlayers(@NotNull Integer exact) {
        validatePlayerCount(exact);
        this.onlinePlayers = new JsonArray();
        this.onlinePlayers.add(exact);
        this.onlinePlayers.add(exact);
    }

    public void setOnlinePlayers(@NotNull Integer min, @NotNull Integer max) {
        validatePlayerCount(min);
        if (max != -1) validatePlayerCount(max);
        if (min > max && max != -1) {
            throw new IllegalArgumentException("Min players cannot be greater than max players");
        }

        this.onlinePlayers = new JsonArray();
        this.onlinePlayers.add(min);
        this.onlinePlayers.add(max == -1 ? "inf" : max);
    }

    public void setProtocolVersion(@Nullable Integer version) {
        this.protocol = version;
    }

    public void setSoftware(@Nullable Software software) {
        this.software = software;
    }

    public void setIgnoreModded(@Nullable Boolean ignore) {
        this.ignoreModded = ignore;
    }

    public void setOnlyBungeeSpoofable(@Nullable Boolean only) {
        this.onlyBungeeSpoofable = only;
    }

    public void setMinUptime(@Nullable Integer hours) {
        if (hours != null && hours < 0) {
            throw new IllegalArgumentException("Uptime cannot be negative");
        }
        this.minUptime = hours;
    }

    public void setVersion(@Nullable String version) {
        this.version = version;
    }

    public void setHostname(@Nullable String hostname) {
        this.hostname = hostname;
    }

    public void setHasPlayers(@Nullable Boolean hasPlayers) {
        this.hasPlayers = hasPlayers;
    }

    public void setHasSpecificPlayer(@Nullable String playerName) {
        this.playerName = playerName;
        this.hasSpecificPlayer = playerName != null;
    }

    // Validation helper
    private void validatePlayerCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Player count cannot be negative");
        }
    }

    // Getters
    public Integer getAsn() { return asn; }
    public String getCountryCode() { return countryCode; }
    public Boolean isCracked() { return cracked; }
    public String getDescription() { return description; }
    public JsonArray getMaxPlayers() { return maxPlayers; }
    public Integer getOnlineAfter() { return onlineAfter; }
    public JsonArray getOnlinePlayers() { return onlinePlayers; }
    public Integer getProtocol() { return protocol; }
    public Boolean shouldIgnoreModded() { return ignoreModded; }
    public Boolean isOnlyBungeeSpoofable() { return onlyBungeeSpoofable; }
    public Software getSoftware() { return software; }
    public Integer getMinUptime() { return minUptime; }
    public String getVersion() { return version; }
    public String getHostname() { return hostname; }
    public Boolean hasPlayers() { return hasPlayers; }
    public Boolean hasSpecificPlayer() { return hasSpecificPlayer; }
    public String getPlayerName() { return playerName; }
}
