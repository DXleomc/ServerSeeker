package de.damcraft.serverseeker.ssapi.requests;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a request for server information from the ServerSeeker API.
 * 
 * @param apiKey The API key for authentication
 * @param ip The IP address or hostname of the server
 * @param port The port number of the server
 */
public record ServerInfoRequest(
    @NotNull @SerializedName("api_key") String apiKey,
    @NotNull String ip,
    int port
) {
    /**
     * Validates the request parameters before construction.
     *
     * @throws IllegalArgumentException if any parameter is invalid
     * @throws NullPointerException if apiKey or ip is null
     */
    public ServerInfoRequest {
        Objects.requireNonNull(apiKey, "API key cannot be null");
        Objects.requireNonNull(ip, "IP address cannot be null");
        
        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be blank");
        }
        
        if (ip.isBlank()) {
            throw new IllegalArgumentException("IP address cannot be blank");
        }
        
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        
        // Normalize the IP/hostname by removing any protocol prefixes
        ip = ip.replaceAll("^(https?://)?", "").split("/")[0];
    }

    /**
     * Creates a new ServerInfoRequest with default port (25565).
     *
     * @param apiKey The API key for authentication
     * @param ip The IP address or hostname of the server
     * @return A new ServerInfoRequest with default port
     */
    public static ServerInfoRequest withDefaultPort(@NotNull String apiKey, @NotNull String ip) {
        return new ServerInfoRequest(apiKey, ip, 25565);
    }

    /**
     * Gets the server address in standard format (ip:port).
     *
     * @return The formatted server address
     */
    public String getServerAddress() {
        return port == 25565 ? ip : ip + ":" + port;
    }

    /**
     * Checks if this request is valid for sending to the API.
     *
     * @return true if the request is valid
     */
    public boolean isValid() {
        return !apiKey.isBlank() && !ip.isBlank() && port >= 1 && port <= 65535;
    }
}
