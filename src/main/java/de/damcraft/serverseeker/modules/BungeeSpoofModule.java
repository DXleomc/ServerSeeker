package de.damcraft.serverseeker.modules;

import de.damcraft.serverseeker.ServerSeeker;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class BungeeSpoofModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAdvanced = settings.createGroup("Advanced");
    private final SettingGroup sgSecurity = settings.createGroup("Security");

    // General Settings
    public final Setting<String> spoofedAddress = sgGeneral.add(new StringSetting.Builder()
        .name("spoofed-address")
        .description("The IP address that will be sent to the server")
        .defaultValue("127.0.0.1")
        .filter(this::isValidIpAddress)
        .onChanged(this::validateAddress)
        .build()
    );

    public final Setting<Boolean> randomizeAddress = sgGeneral.add(new BoolSetting.Builder()
        .name("randomize-address")
        .description("Randomizes the last octet of IPv4 addresses for each connection")
        .defaultValue(false)
        .visible(() -> isIPv4(spoofedAddress.get()))
        .build()
    );

    // Advanced Settings
    public final Setting<Boolean> spoofPort = sgAdvanced.add(new BoolSetting.Builder()
        .name("spoof-port")
        .description("Whether to spoof the connection port")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> spoofedPort = sgAdvanced.add(new IntSetting.Builder()
        .name("spoofed-port")
        .description("The port that will be sent to the server")
        .defaultValue(25565)
        .range(1, 65535)
        .visible(spoofPort::get)
        .build()
    );

    public final Setting<Boolean> spoofHostname = sgAdvanced.add(new BoolSetting.Builder()
        .name("spoof-hostname")
        .description("Whether to spoof the hostname during connection")
        .defaultValue(true)
        .build()
    );

    // Security Settings
    public final Setting<List<String>> whitelistedServers = sgSecurity.add(new StringListSetting.Builder()
        .name("whitelisted-servers")
        .description("Only spoof on these servers (ip:port format)")
        .defaultValue(List.of())
        .filter((text, c) -> (text + c).matches("^[0-9a-zA-Z\\\\.:\\-_]{0,255}$"))
        .build()
    );

    public final Setting<Boolean> enableWarning = sgSecurity.add(new BoolSetting.Builder()
        .name("show-warning")
        .description("Shows a warning when joining non-whitelisted servers")
        .defaultValue(true)
        .build()
    );

    public BungeeSpoofModule() {
        super(ServerSeeker.CATEGORY, "bungee-spoof", "Allows you to bypass IP-based restrictions on certain proxy configurations. Use with caution!");
    }

    @EventHandler
    private void onHandshake(PacketEvent.Send event) {
        if (!(event.packet instanceof HandshakeC2SPacket packet)) return;
        if (packet.getIntent() != ConnectionIntent.LOGIN) return;

        // Check if server is whitelisted
        String currentServer = getCurrentServerAddress();
        if (!whitelistedServers.get().isEmpty() && !whitelistedServers.get().contains(currentServer)) {
            if (enableWarning.get()) {
                warning("Not spoofing connection to non-whitelisted server: %s", currentServer);
            }
            return;
        }

        // Modify the handshake packet
        String address = getSpoofedAddress();
        int port = spoofPort.get() ? spoofedPort.get() : packet.getPort();

        event.packet = new HandshakeC2SPacket(
            spoofHostname.get() ? address : packet.getAddress(),
            port,
            packet.getIntent()
        );
    }

    private String getSpoofedAddress() {
        String address = spoofedAddress.get();
        if (randomizeAddress.get() && isIPv4(address)) {
            String[] octets = address.split("\\.");
            if (octets.length == 4) {
                octets[3] = String.valueOf((int) (Math.random() * 254) + 1);
                return String.join(".", octets);
            }
        }
        return address;
    }

    private String getCurrentServerAddress() {
        if (mc.getCurrentServerEntry() != null) {
            return mc.getCurrentServerEntry().address;
        }
        return "unknown";
    }

    private boolean isValidIpAddress(String text, char c) {
        String test = text + c;
        return test.matches("^[0-9a-f.:]{0,45}$") || 
               test.matches("^[0-9a-zA-Z\\-._]{0,255}$");
    }

    private boolean isIPv4(String ip) {
        return ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
    }

    private void validateAddress(String address) {
        try {
            InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            error("Invalid IP address format: %s", address);
        }
    }

    @Override
    public String getInfoString() {
        return whitelistedServers.get().isEmpty() ? "Unrestricted" : 
               "Whitelist: " + whitelistedServers.get().size();
    }
}
