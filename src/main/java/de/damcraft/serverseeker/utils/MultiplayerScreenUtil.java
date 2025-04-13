package de.damcraft.serverseeker.utils;

import de.damcraft.serverseeker.mixin.MultiplayerScreenAccessor;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public final class MultiplayerScreenUtil {
    private static final Logger LOG = LoggerFactory.getLogger(MultiplayerScreenUtil.class);
    private static final int MAX_SERVER_NAME_LENGTH = 32;
    private static final int MAX_SERVER_IP_LENGTH = 255;

    private MultiplayerScreenUtil() {} // Prevent instantiation

    /**
     * Adds a server to the server list with full configuration options
     * @param mps The MultiplayerScreen instance
     * @param info The server info to add
     * @param options Configuration options (reload list, save file, etc)
     */
    public static void addServer(MultiplayerScreen mps, ServerInfo info, ServerAddOptions options) {
        validateServerInfo(info);
        try {
            MultiplayerScreenAccessor mpsAccessor = (MultiplayerScreenAccessor) mps;
            
            mps.getServerList().add(info, false);
            
            if (options.shouldReload()) {
                mpsAccessor.getServerListWidget().setServers(mps.getServerList());
            }
            
            if (options.shouldSave()) {
                mps.getServerList().saveFile();
            }
            
            if (options.getCallback() != null) {
                options.getCallback().accept(info);
            }
        } catch (Exception e) {
            LOG.error("Failed to add server to list", e);
            throw new RuntimeException("Failed to add server", e);
        }
    }

    /**
     * Creates and adds a new server to the list
     * @param mps The MultiplayerScreen instance
     * @param name The server display name
     * @param ip The server IP address
     * @param options Configuration options
     */
    public static void addNewServer(MultiplayerScreen mps, String name, String ip, ServerAddOptions options) {
        validateServerName(name);
        validateServerIp(ip);
        
        ServerInfo info = new ServerInfo(
            sanitizeName(name),
            sanitizeIp(ip),
            ServerInfo.ServerType.OTHER
        );
        
        // Apply additional configuration from options
        if (options.getResourcePackPolicy() != null) {
            info.setResourcePackPolicy(options.getResourcePackPolicy());
        }
        
        if (options.getIcon() != null) {
            info.setFavicon(options.getIcon());
        }
        
        addServer(mps, info, options);
    }

    /**
     * Reloads the server list widget
     * @param mps The MultiplayerScreen instance
     */
    public static void reloadServerList(MultiplayerScreen mps) {
        try {
            MultiplayerScreenAccessor mpsAccessor = (MultiplayerScreenAccessor) mps;
            mpsAccessor.getServerListWidget().setServers(mps.getServerList());
        } catch (Exception e) {
            LOG.error("Failed to reload server list", e);
        }
    }

    /**
     * Saves the server list to file
     * @param mps The MultiplayerScreen instance
     * @return true if save was successful
     */
    public static boolean saveServerList(MultiplayerScreen mps) {
        try {
            mps.getServerList().saveFile();
            return true;
        } catch (Exception e) {
            LOG.error("Failed to save server list", e);
            return false;
        }
    }

    // Validation methods
    private static void validateServerInfo(ServerInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("ServerInfo cannot be null");
        }
        validateServerName(info.name);
        validateServerIp(info.address);
    }

    private static void validateServerName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Server name cannot be empty");
        }
        if (name.length() > MAX_SERVER_NAME_LENGTH) {
            throw new IllegalArgumentException("Server name too long (max " + MAX_SERVER_NAME_LENGTH + " chars)");
        }
    }

    private static void validateServerIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            throw new IllegalArgumentException("Server IP cannot be empty");
        }
        if (ip.length() > MAX_SERVER_IP_LENGTH) {
            throw new IllegalArgumentException("Server IP too long (max " + MAX_SERVER_IP_LENGTH + " chars)");
        }
    }

    // Sanitization methods
    private static String sanitizeName(String name) {
        return Text.literal(name).getString(); // Remove formatting
    }

    private static String sanitizeIp(String ip) {
        return ip.trim().toLowerCase(); // Normalize IP
    }

    /**
     * Configuration options for adding servers
     */
    public static class ServerAddOptions {
        private boolean reload = true;
        private boolean save = true;
        private ServerInfo.ResourcePackPolicy resourcePackPolicy;
        private String icon;
        private Consumer<ServerInfo> callback;

        public ServerAddOptions reload(boolean reload) {
            this.reload = reload;
            return this;
        }

        public ServerAddOptions save(boolean save) {
            this.save = save;
            return this;
        }

        public ServerAddOptions resourcePackPolicy(ServerInfo.ResourcePackPolicy policy) {
            this.resourcePackPolicy = policy;
            return this;
        }

        public ServerAddOptions icon(String icon) {
            this.icon = icon;
            return this;
        }

        public ServerAddOptions callback(Consumer<ServerInfo> callback) {
            this.callback = callback;
            return this;
        }

        // Getters
        public boolean shouldReload() { return reload; }
        public boolean shouldSave() { return save; }
        public ServerInfo.ResourcePackPolicy getResourcePackPolicy() { return resourcePackPolicy; }
        public String getIcon() { return icon; }
        public Consumer<ServerInfo> getCallback() { return callback; }

        // Predefined options
        public static ServerAddOptions defaultOptions() {
            return new ServerAddOptions();
        }

        public static ServerAddOptions silentAdd() {
            return new ServerAddOptions()
                .reload(false)
                .save(false);
        }

        public static ServerAddOptions quickAdd() {
            return new ServerAddOptions()
                .reload(true)
                .save(false);
        }
    }
}
