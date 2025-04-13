package de.damcraft.serverseeker.ssapi.requests;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import de.damcraft.serverseeker.ServerSeeker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a request to locate a player by name or UUID across servers.
 */
public class WhereisRequest {
    @SerializedName("api_key")
    private final String apiKey = ServerSeeker.API_KEY;

    private SearchType searchType;
    private String searchValue;
    private Boolean includeHistoric;
    private Integer lastSeenWithin;
    private Boolean showOffline;
    private Integer limit;

    /**
     * Player search type enumeration
     */
    public enum SearchType {
        @SerializedName("name") NAME,
        @SerializedName("uuid") UUID
    }

    /**
     * Creates a new WhereisRequest builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final WhereisRequest request = new WhereisRequest();

        public Builder byName(@NotNull String name) {
            request.setName(name);
            return this;
        }

        public Builder byUuid(@NotNull String uuid) {
            request.setUuid(uuid);
            return this;
        }

        public Builder byUuid(@NotNull UUID uuid) {
            request.setUuid(uuid.toString());
            return this;
        }

        public Builder includeHistoric(boolean include) {
            request.setIncludeHistoric(include);
            return this;
        }

        public Builder lastSeenWithin(int hours) {
            request.setLastSeenWithin(hours);
            return this;
        }

        public Builder showOffline(boolean show) {
            request.setShowOffline(show);
            return this;
        }

        public Builder limit(int limit) {
            request.setLimit(limit);
            return this;
        }

        public WhereisRequest build() {
            return request;
        }
    }

    public WhereisRequest() {
        // Default configuration
        this.includeHistoric = true;
        this.showOffline = false;
        this.limit = 50;
    }

    /**
     * Sets the player name to search for
     * @param name Minecraft player name (1-16 characters)
     */
    public void setName(@NotNull String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be null or empty");
        }
        if (name.length() > 16) {
            throw new IllegalArgumentException("Player name cannot exceed 16 characters");
        }
        this.searchType = SearchType.NAME;
        this.searchValue = name.trim();
    }

    /**
     * Sets the player UUID to search for
     * @param uuid Minecraft player UUID (with or without hyphens)
     */
    public void setUuid(@NotNull String uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        String cleaned = uuid.replace("-", "");
        if (cleaned.length() != 32) {
            throw new IllegalArgumentException("Invalid UUID format");
        }
        try {
            // Validate it's a proper UUID
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format", e);
        }
        this.searchType = SearchType.UUID;
        this.searchValue = cleaned;
    }

    public void setIncludeHistoric(Boolean include) {
        this.includeHistoric = include;
    }

    /**
     * Sets the maximum age of results in hours
     * @param hours Number of hours (1-720)
     */
    public void setLastSeenWithin(Integer hours) {
        if (hours != null && (hours < 1 || hours > 720)) {
            throw new IllegalArgumentException("Last seen within must be between 1 and 720 hours");
        }
        this.lastSeenWithin = hours;
    }

    public void setShowOffline(Boolean show) {
        this.showOffline = show;
    }

    /**
     * Sets the maximum number of results to return
     * @param limit Number of results (1-1000)
     */
    public void setLimit(Integer limit) {
        if (limit != null && (limit < 1 || limit > 1000)) {
            throw new IllegalArgumentException("Limit must be between 1 and 1000");
        }
        this.limit = limit;
    }

    /**
     * Converts the request to JSON format
     * @return JSON string representation
     */
    public String toJson() {
        if (searchType == null || searchValue == null) {
            throw new IllegalStateException("Search criteria not set");
        }

        JsonObject jo = new JsonObject();
        jo.addProperty("api_key", apiKey);
        jo.addProperty(searchType.name().toLowerCase(), searchValue);

        if (includeHistoric != null) {
            jo.addProperty("include_historic", includeHistoric);
        }
        if (lastSeenWithin != null) {
            jo.addProperty("last_seen_within", lastSeenWithin);
        }
        if (showOffline != null) {
            jo.addProperty("show_offline", showOffline);
        }
        if (limit != null) {
            jo.addProperty("limit", limit);
        }

        return jo.toString();
    }

    // Getters
    public SearchType getSearchType() { return searchType; }
    public String getSearchValue() { return searchValue; }
    public Boolean getIncludeHistoric() { return includeHistoric; }
    public Integer getLastSeenWithin() { return lastSeenWithin; }
    public Boolean getShowOffline() { return showOffline; }
    public Integer getLimit() { return limit; }
}
