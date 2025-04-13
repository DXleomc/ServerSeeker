package de.damcraft.serverseeker.gui;

import de.damcraft.serverseeker.utils.MultiplayerScreenUtil;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class ServerSeekerScreen extends WindowScreen {
    private final MultiplayerScreen multiplayerScreen;

    public ServerSeekerScreen(MultiplayerScreen multiplayerScreen) {
        super(GuiThemes.get(), "ServerSeeker Dashboard");
        this.multiplayerScreen = multiplayerScreen;
    }

    @Override
    public void initWidgets() {
        WVerticalList layout = add(theme.verticalList()).expandX().widget();

        WHorizontalList mainButtons = layout.add(theme.horizontalList()).expandX().widget();

        WButton newServersButton = mainButtons.add(theme.button("Find New Servers")).expandX().widget();
        WButton findPlayersButton = mainButtons.add(theme.button("Search Players")).expandX().widget();
        WButton cleanUpServersButton = mainButtons.add(theme.button("Clean Up")).expandX().widget();

        newServersButton.action = () -> {
            if (client != null) client.setScreen(new FindNewServersScreen(multiplayerScreen));
        };

        findPlayersButton.action = () -> {
            if (client != null) client.setScreen(new FindPlayerScreen(multiplayerScreen));
        };

        cleanUpServersButton.action = this::showCleanupConfirmation;

        WHorizontalList secondaryButtons = layout.add(theme.horizontalList()).expandX().widget();

        WButton exportButton = secondaryButtons.add(theme.button("Export Servers")).expandX().widget();
        WButton importButton = secondaryButtons.add(theme.button("Import Servers")).expandX().widget();
        WButton helpButton = secondaryButtons.add(theme.button("Help")).expandX().widget();

        exportButton.action = this::exportServers;
        importButton.action = this::importServers;
        helpButton.action = this::showHelpDialog;
    }

    private void showCleanupConfirmation() {
        clear();
        add(theme.label("Are you sure you want to clean up your server list?"));
        add(theme.label("This will remove all servers starting with \"ServerSeeker\"."));

        WHorizontalList buttonList = add(theme.horizontalList()).expandX().widget();
        WButton back = buttonList.add(theme.button("Back")).expandX().widget();
        back.action = this::reload;

        WButton confirm = buttonList.add(theme.button("Confirm")).expandX().widget();
        confirm.action = this::cleanUpServers;
    }

    private boolean hasAnyServers() {
        if (client == null) return false;
        return multiplayerScreen.getServerList().stream().anyMatch(server -> server.name.startsWith("ServerSeeker"));
    }

    public void cleanUpServers() {
        if (client == null) return;

        multiplayerScreen.getServerList().removeIf(server -> server.name.startsWith("ServerSeeker"));
        MultiplayerScreenUtil.saveList(multiplayerScreen);
        MultiplayerScreenUtil.reloadServerList(multiplayerScreen);

        client.setScreen(multiplayerScreen);
    }

    private void exportServers() {
        if (client == null) return;

        Path path = client.runDirectory.toPath().resolve("serverseeker_export.txt");
        try {
            Files.write(path,
                multiplayerScreen.getServerList().stream()
                    .map(server -> server.name + " | " + server.address)
                    .collect(Collectors.toList()));
            add(theme.label("Exported to: " + path.getFileName()));
        } catch (IOException e) {
            add(theme.label("Export failed: " + e.getMessage()));
        }
    }

    private void importServers() {
        if (client == null) return;

        Path path = client.runDirectory.toPath().resolve("serverseeker_export.txt");
        if (!Files.exists(path)) {
            add(theme.label("No export file found at: " + path.getFileName()));
            return;
        }

        try {
            Files.lines(path).forEach(line -> {
                String[] parts = line.split(" \\| ");
                if (parts.length == 2) {
                    MultiplayerScreenUtil.addServer(parts[0], parts[1], multiplayerScreen);
                }
            });
            MultiplayerScreenUtil.saveList(multiplayerScreen);
            MultiplayerScreenUtil.reloadServerList(multiplayerScreen);
            add(theme.label("Imported servers from file."));
        } catch (IOException e) {
            add(theme.label("Import failed: " + e.getMessage()));
        }
    }

    private void showHelpDialog() {
        clear();
        add(theme.label("ServerSeeker Help"));
        add(theme.label("- Find New Servers: Search for and add new public servers."));
        add(theme.label("- Search Players: Look for known players on public servers."));
        add(theme.label("- Clean Up: Remove servers added by ServerSeeker."));
        add(theme.label("- Export/Import: Save and load your servers."));
        add(theme.label("Export file: serverseeker_export.txt in your Minecraft folder."));

        WButton back = add(theme.button("Back")).expandX().widget();
        back.action = this::reload;
    }
}
