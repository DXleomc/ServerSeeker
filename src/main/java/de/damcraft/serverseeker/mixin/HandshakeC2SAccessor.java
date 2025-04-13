package de.damcraft.serverseeker.mixin;

import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Enhanced mixin for accessing and modifying HandshakeC2SPacket internals.
 * Provides full control over handshake packet fields for connection manipulation.
 */
@Mixin(HandshakeC2SPacket.class)
public interface HandshakeC2SAccessor {
    /**
     * Gets the connection address (hostname/IP)
     */
    @Accessor("address")
    String getAddress();

    /**
     * Sets the connection address (hostname/IP)
     * @param address New address to use (max 255 chars)
     */
    @Mutable
    @Accessor("address")
    void setAddress(String address);

    /**
     * Gets the connection port number
     */
    @Accessor("port")
    int getPort();

    /**
     * Sets the connection port number
     * @param port Valid port number (1-65535)
     */
    @Mutable
    @Accessor("port")
    void setPort(int port);

    /**
     * Gets the connection intent/state
     */
    @Accessor("intendedState")
    ConnectionIntent getConnectionIntent();

    /**
     * Sets the connection intent/state
     */
    @Mutable
    @Accessor("intendedState")
    void setConnectionIntent(ConnectionIntent intent);

    /**
     * Creates a new handshake packet instance
     */
    @Invoker("<init>")
    static HandshakeC2SPacket create(String address, int port, ConnectionIntent intent) {
        throw new AssertionError();
    }
}
