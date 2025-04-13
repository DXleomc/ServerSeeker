package de.damcraft.serverseeker.mixin;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Enhanced accessor mixin for MultiplayerScreen providing additional functionality
 * for server list manipulation and UI interaction.
 */
@Mixin(MultiplayerScreen.class)
public interface MultiplayerScreenAccessor {
    /**
     * Gets the server list widget instance
     */
    @Accessor("serverListWidget")
    MultiplayerServerListWidget getServerListWidget();

    /**
     * Gets the server list (saved servers) instance
     */
    @Accessor("serverList")
    ServerList getServerList();

    /**
     * Sets the currently selected server entry
     */
    @Accessor("selectedEntry")
    void setSelectedEntry(MultiplayerServerListWidget.Entry entry);

    /**
     * Gets the currently selected server entry
     */
    @Accessor("selectedEntry")
    MultiplayerServerListWidget.Entry getSelectedEntry();

    /**
     * Invokes the connect method to join a server
     */
    @Invoker("connect")
    void invokeConnect(ServerInfo entry);

    /**
     * Invokes the refresh method to reload server pings
     */
    @Invoker("refresh")
    void invokeRefresh();

    /**
     * Invokes the saveList method to persist server list changes
     */
    @Invoker("saveList")
    void invokeSaveList();

    /**
     * Invokes the setScreen method to change the current screen
     */
    @Invoker("setScreen")
    void invokeSetScreen(net.minecraft.client.gui.screen.Screen screen);
}
