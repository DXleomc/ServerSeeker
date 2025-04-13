package de.damcraft.serverseeker.utils;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public final class MCVersionUtil {
    private static final Logger LOG = LoggerFactory.getLogger(MCVersionUtil.class);
    private static final Object2IntMap<String> VERSION_TO_PROTOCOL = new Object2IntOpenHashMap<>();
    private static final NavigableMap<Integer, String> PROTOCOL_TO_VERSION = new TreeMap<>();
    private static final Set<String> SUPPORTED_VERSIONS = new TreeSet<>(Comparator.reverseOrder());
    private static final int CURRENT_PROTOCOL = 768; // 1.21.3
    private static final String CURRENT_VERSION = "1.21.3";

    static {
        initializeVersionMaps();
    }

    private MCVersionUtil() {} // Prevent instantiation

    private static void initializeVersionMaps() {
        // Version to protocol mapping
        addVersion("1.21.3", 768);
        addVersion("1.21.2", 768);
        addVersion("1.21.1", 767);
        addVersion("1.21", 767);

        addVersion("1.20.6", 766);
        addVersion("1.20.5", 766);
        addVersion("1.20.4", 765);
        addVersion("1.20.3", 765);
        addVersion("1.20.2", 764);
        addVersion("1.20.1", 763);
        addVersion("1.20", 763);

        // Previous versions...
        // (Include all your existing version mappings here)
        // ...

        // Build reverse mapping and supported versions set
        VERSION_TO_PROTOCOL.forEach((version, protocol) -> {
            PROTOCOL_TO_VERSION.putIfAbsent(protocol, version); // Keep first version for each protocol
            SUPPORTED_VERSIONS.add(version);
        });
    }

    private static void addVersion(String version, int protocol) {
        VERSION_TO_PROTOCOL.put(version, protocol);
    }

    /**
     * Gets the protocol version for a given Minecraft version string
     * @param versionString Minecraft version string (e.g., "1.20.1")
     * @return Protocol version number, or -1 if version not found
     */
    public static int versionToProtocol(@Nullable String versionString) {
        if (versionString == null || versionString.isEmpty()) {
            LOG.warn("Null or empty version string provided");
            return -1;
        }

        if (!VERSION_TO_PROTOCOL.containsKey(versionString)) {
            LOG.warn("Unknown Minecraft version: {}", versionString);
            return -1;
        }

        return VERSION_TO_PROTOCOL.getInt(versionString);
    }

    /**
     * Gets the most recent version name for a given protocol number
     * @param protocol Protocol version number
     * @return Version string (e.g., "1.20.1"), or null if protocol not found
     */
    public static @Nullable String protocolToVersion(int protocol) {
        return PROTOCOL_TO_VERSION.get(protocol);
    }

    /**
     * Gets all versions that use the specified protocol
     * @param protocol Protocol version number
     * @return Set of version strings, empty set if protocol not found
     */
    public static @NotNull Set<String> getVersionsForProtocol(int protocol) {
        return VERSION_TO_PROTOCOL.object2IntEntrySet().stream()
            .filter(entry -> entry.getIntValue() == protocol)
            .map(Object2IntMap.Entry::getKey)
            .collect(Collectors.toSet());
    }

    /**
     * Gets the current (latest supported) protocol version
     * @return Latest protocol version number
     */
    public static int getCurrentProtocol() {
        return CURRENT_PROTOCOL;
    }

    /**
     * Gets the current (latest supported) Minecraft version
     * @return Latest version string
     */
    public static @NotNull String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    /**
     * Checks if a version is supported
     * @param versionString Version to check
     * @return true if version is supported
     */
    public static boolean isVersionSupported(@Nullable String versionString) {
        return versionString != null && VERSION_TO_PROTOCOL.containsKey(versionString);
    }

    /**
     * Gets all supported versions in descending order (newest first)
     * @return Sorted set of supported versions
     */
    public static @NotNull Set<String> getSupportedVersions() {
        return Collections.unmodifiableSet(SUPPORTED_VERSIONS);
    }

    /**
     * Gets the closest supported protocol for version compatibility
     * @param protocol Desired protocol version
     * @return Nearest supported protocol (may be the same, higher, or lower)
     */
    public static int getClosestProtocol(int protocol) {
        if (PROTOCOL_TO_VERSION.containsKey(protocol)) {
            return protocol;
        }

        Integer lower = PROTOCOL_TO_VERSION.lowerKey(protocol);
        Integer higher = PROTOCOL_TO_VERSION.higherKey(protocol);

        if (lower == null) return higher != null ? higher : -1;
        if (higher == null) return lower;

        return (protocol - lower) < (higher - protocol) ? lower : higher;
    }

    /**
     * Gets the display name for a version (returns the most common/release version)
     * @param protocol Protocol version number
     * @return Display version string, or "Unknown" if protocol not found
     */
    public static @NotNull String getDisplayVersion(int protocol) {
        String version = PROTOCOL_TO_VERSION.get(protocol);
        return version != null ? version : "Unknown (" + protocol + ")";
    }
}
