package de.damcraft.serverseeker.gui;

import com.google.common.net.HostAndPort;
import de.damcraft.serverseeker.ServerSeeker;
import de.damcraft.serverseeker.country.Country;
import de.damcraft.serverseeker.country.CountrySetting;
import de.damcraft.serverseeker.ssapi.requests.ServersRequest;
import de.damcraft.serverseeker.ssapi.responses.ServersResponse;
import de.damcraft.serverseeker.utils.*;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.*;
import meteordevelopment.meteorclient.gui.widgets.pressable.*;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static de.damcraft.serverseeker.ServerSeeker.LOG;

public class FindNewServersScreen extends WindowScreen {
    // Saved settings
    public static NbtCompound savedSettings;
    
    // UI state
    private WButton findButton;
    private WButton stopButton;
    private WProgressBar progressBar;
    private WLabel statusLabel;
    private WTable resultsTable;
    
    // Search state
    private boolean searchActive;
    private String searchError;
    private List<ServersResponse.Server> foundServers;
    private AtomicInteger currentPage = new AtomicInteger(1);
    private int totalPages = 1;
    private CompletableFuture<Void> searchFuture;
    
    // Constants
    private static final int RESULTS_PER_PAGE = 10;
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("MMM dd HH:mm").withZone(ZoneId.systemDefault());

    // Enums
    public enum Cracked { Any, Yes, No }
    public enum Version { Current, Any, Protocol, VersionString }
    public enum NumRangeType { Any, Equals, AtLeast, AtMost, Between }
    public enum GeoSearchType { None, ASN, Country }
    public enum SortBy { Players, LastSeen, MaxPlayers, Random }

    // Settings
    private final Settings settings = new Settings();
    private final SettingGroup sgGeneral = settings.createGroup("General");
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgVersion = settings.createGroup("Version");
    private final SettingGroup sgLocation = settings.createGroup("Location");
    private final SettingGroup sgAdvanced = settings.createGroup("Advanced");
    
    // General Settings
    private final Setting<Cracked> crackedSetting = sgGeneral.add(new EnumSetting.Builder<Cracked>()
        .name("cracked")
        .description("Server cracked status")
        .defaultValue(Cracked.Any)
        .build()
    );

    private final Setting<String> descriptionSetting = sgGeneral.add(new StringSetting.Builder()
        .name("motd")
        .description("MOTD contains (leave empty for any)")
        .defaultValue("")
        .build()
    );

    private final Setting<ServersRequest.Software> softwareSetting = sgGeneral.add(new EnumSetting.Builder<ServersRequest.Software>()
        .name("software")
        .description("Server software type")
        .defaultValue(ServersRequest.Software.Any)
        .build()
    );

    private final Setting<Boolean> onlineOnlySetting = sgGeneral.add(new BoolSetting.Builder()
        .name("online-only")
        .description("Only show currently online servers")
        .defaultValue(true)
        .build()
    );

    // Players Settings
    private final Setting<NumRangeType> onlinePlayersNumTypeSetting = sgPlayers.add(new EnumSetting.Builder<NumRangeType>()
        .name("online-players-range")
        .description("Online players range type")
        .defaultValue(NumRangeType.Any)
        .build()
    );

    private final Setting<Integer> equalsOnlinePlayersSetting = sgPlayers.add(new IntSetting.Builder()
        .name("online-players")
        .description("Exact online players count")
        .defaultValue(2)
        .min(0)
        .visible(() -> onlinePlayersNumTypeSetting.get() == NumRangeType.Equals)
        .build()
    );

    private final Setting<Integer> atLeastOnlinePlayersSetting = sgPlayers.add(new IntSetting.Builder()
        .name("min-online-players")
        .description("Minimum online players")
        .defaultValue(1)
        .min(0)
        .visible(() -> onlinePlayersNumTypeSetting.get() == NumRangeType.AtLeast || 
                      onlinePlayersNumTypeSetting.get() == NumRangeType.Between)
        .build()
    );

    private final Setting<Integer> atMostOnlinePlayersSetting = sgPlayers.add(new IntSetting.Builder()
        .name("max-online-players")
        .description("Maximum online players")
        .defaultValue(20)
        .min(0)
        .visible(() -> onlinePlayersNumTypeSetting.get() == NumRangeType.AtMost || 
                      onlinePlayersNumTypeSetting.get() == NumRangeType.Between)
        .build()
    );

    private final Setting<NumRangeType> maxPlayersNumTypeSetting = sgPlayers.add(new EnumSetting.Builder<NumRangeType>()
        .name("max-players-range")
        .description("Max players range type")
        .defaultValue(NumRangeType.Any)
        .build()
    );

    private final Setting<Integer> equalsMaxPlayersSetting = sgPlayers.add(new IntSetting.Builder()
        .name("max-players")
        .description("Exact max players count")
        .defaultValue(20)
        .min(0)
        .visible(() -> maxPlayersNumTypeSetting.get() == NumRangeType.Equals)
        .build()
    );

    private final Setting<Integer> atLeastMaxPlayersSetting = sgPlayers.add(new IntSetting.Builder()
        .name("min-max-players")
        .description("Minimum max players")
        .defaultValue(10)
        .min(0)
        .visible(() -> maxPlayersNumTypeSetting.get() == NumRangeType.AtLeast || 
                      maxPlayersNumTypeSetting.get() == NumRangeType.Between)
        .build()
    );

    private final Setting<Integer> atMostMaxPlayersSetting = sgPlayers.add(new IntSetting.Builder()
        .name("max-max-players")
        .description("Maximum max players")
        .defaultValue(100)
        .min(0)
        .visible(() -> maxPlayersNumTypeSetting.get() == NumRangeType.AtMost || 
                      maxPlayersNumTypeSetting.get() == NumRangeType.Between)
        .build()
    );

    // Version Settings
    private final Setting<Version> versionSetting = sgVersion.add(new EnumSetting.Builder<Version>()
        .name("version")
        .description("Version matching mode")
        .defaultValue(Version.Current)
        .build()
    );

    private final Setting<Integer> protocolVersionSetting = sgVersion.add(new IntSetting.Builder()
        .name("protocol")
        .description("Protocol version number")
        .defaultValue(SharedConstants.getProtocolVersion())
        .visible(() -> versionSetting.get() == Version.Protocol)
        .min(0)
        .build()
    );

    private final Setting<String> versionStringSetting = sgVersion.add(new StringSetting.Builder()
        .name("version-string")
        .description("Minecraft version (e.g. 1.19.3)")
        .defaultValue(MCVersionUtil.getCurrentVersion())
        .visible(() -> versionSetting.get() == Version.VersionString)
        .build()
    );

    // Location Settings
    private final Setting<GeoSearchType> geoSearchTypeSetting = sgLocation.add(new EnumSetting.Builder<GeoSearchType>()
        .name("geo-search-type")
        .description("Geographic search type")
        .defaultValue(GeoSearchType.Country)
        .build()
    );

    private final Setting<Integer> asnNumberSetting = sgLocation.add(new IntSetting.Builder()
        .name("asn")
        .description("Autonomous System Number")
        .defaultValue(24940)
        .visible(() -> geoSearchTypeSetting.get() == GeoSearchType.ASN)
        .build()
    );

    private final Setting<Country> countrySetting = sgLocation.add(new CountrySetting.Builder()
        .name("country")
        .description("Server country")
        .defaultValue(ServerSeeker.COUNTRY_MAP.get("UN"))
        .visible(() -> geoSearchTypeSetting.get() == GeoSearchType.Country)
        .build()
    );

    // Advanced Settings
    private final Setting<Boolean> ignoreModded = sgAdvanced.add(new BoolSetting.Builder()
        .name("ignore-modded")
        .description("Exclude modded servers")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyBungeeSpoofable = sgAdvanced.add(new BoolSetting.Builder()
        .name("only-bungee-spoofable")
        .description("Only BungeeSpoof-compatible servers")
        .defaultValue(false)
        .build()
    );

    private final Setting<SortBy> sortBySetting = sgAdvanced.add(new EnumSetting.Builder<SortBy>()
        .name("sort-by")
        .description("Result sorting method")
        .defaultValue(SortBy.Players)
        .build()
    );

    private final Setting<Boolean> sortDescending = sgAdvanced.add(new BoolSetting.Builder()
        .name("sort-descending")
        .description("Sort in descending order")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> minUptime = sgAdvanced.add(new IntSetting.Builder()
        .name("min-uptime")
        .description("Minimum server uptime in hours")
        .defaultValue(0)
        .min(0)
        .build()
    );

    private final MultiplayerScreen multiplayerScreen;

    public FindNewServersScreen(MultiplayerScreen multiplayerScreen) {
        super(GuiThemes.get(), "Server Finder");
        this.multiplayerScreen = multiplayerScreen;
    }

    @Override
    public void initWidgets() {
        loadSettings();
        onClosed(this::saveSettings);

        // Settings panel
        WVerticalList settingsList = add(theme.verticalList()).expandX().widget();
        settingsList.add(theme.settings(settings)).expandX();

        // Action buttons
        WHorizontalList buttonList = add(theme.horizontalList()).expandX().widget();
        findButton = buttonList.add(theme.button("Search")).expandX().widget();
        findButton.action = this::startSearch;

        stopButton = buttonList.add(theme.button("Stop")).expandX().widget();
        stopButton.action = this::stopSearch;
        stopButton.visible = false;

        buttonList.add(theme.button("Reset")).expandX().widget().action = this::resetSettings;

        // Status bar
        progressBar = add(theme.progressBar(0)).expandX().widget();
        statusLabel = add(theme.label("Ready")).expandX().widget();

        // Results table
        resultsTable = add(theme.table()).expandX().widget();
        setupResultsHeader();
    }

    private void setupResultsHeader() {
        resultsTable.clear();
        resultsTable.add(theme.label("Server"));
        resultsTable.add(theme.label("Version"));
        resultsTable.add(theme.label("Players"));
        resultsTable.add(theme.label("Last Seen"));
        resultsTable.add(theme.label("Actions")).expandCellX();
        resultsTable.row();
        resultsTable.add(theme.horizontalSeparator()).expandX();
        resultsTable.row();
    }

    private void startSearch() {
        if (searchActive) return;

        ServersRequest request = buildSearchRequest();
        if (request == null) return;

        clearResults();
        searchActive = true;
        findButton.visible = false;
        stopButton.visible = true;
        statusLabel.set("Searching...");
        progressBar.progress = 0;

        searchFuture = CompletableFuture.runAsync(() -> {
            ServersResponse response = Http.post("https://api.serverseeker.net/servers")
                .timeout(10000)
                .exceptionHandler(e -> {
                    LOG.error("Search failed", e);
                    searchError = "Network error: " + e.getMessage();
                })
                .bodyJson(request)
                .sendJson(ServersResponse.class);

            MinecraftClient.getInstance().execute(() -> handleSearchResponse(response));
        });
    }

    private ServersRequest buildSearchRequest() {
        ServersRequest request = new ServersRequest();

        // Set player count filters
        setPlayerCountFilters(request);

        // Set version filters
        if (!setVersionFilters(request)) return null;

        // Set location filters
        setLocationFilters(request);

        // Set other filters
        request.setCracked(crackedSetting.get() == Cracked.Any ? null : crackedSetting.get() == Cracked.Yes);
        request.setDescription(descriptionSetting.get());
        request.setSoftware(softwareSetting.get());
        request.setOnlineOnly(onlineOnlySetting.get());
        request.setIgnoreModded(ignoreModded.get());
        request.setOnlyBungeeSpoofable(onlyBungeeSpoofable.get());
        request.setMinUptime(minUptime.get());
        request.setSort(sortBySetting.get().name().toLowerCase(), sortDescending.get());

        return request;
    }

    private void setPlayerCountFilters(ServersRequest request) {
        // Online players
        switch (onlinePlayersNumTypeSetting.get()) {
            case AtLeast -> request.setOnlinePlayers(atLeastOnlinePlayersSetting.get(), -1);
            case AtMost -> request.setOnlinePlayers(0, atMostOnlinePlayersSetting.get());
            case Between -> request.setOnlinePlayers(
                atLeastOnlinePlayersSetting.get(), 
                atMostOnlinePlayersSetting.get()
            );
            case Equals -> request.setOnlinePlayers(equalsOnlinePlayersSetting.get());
        }

        // Max players
        switch (maxPlayersNumTypeSetting.get()) {
            case AtLeast -> request.setMaxPlayers(atLeastMaxPlayersSetting.get(), -1);
            case AtMost -> request.setMaxPlayers(0, atMostMaxPlayersSetting.get());
            case Between -> request.setMaxPlayers(
                atLeastMaxPlayersSetting.get(), 
                atMostMaxPlayersSetting.get()
            );
            case Equals -> request.setMaxPlayers(equalsMaxPlayersSetting.get());
        }
    }

    private boolean setVersionFilters(ServersRequest request) {
        switch (versionSetting.get()) {
            case Protocol -> request.setProtocolVersion(protocolVersionSetting.get());
            case VersionString -> {
                int protocol = MCVersionUtil.versionToProtocol(versionStringSetting.get());
                if (protocol == -1) {
                    searchError = "Invalid version string";
                    return false;
                }
                request.setProtocolVersion(protocol);
            }
            case Current -> request.setProtocolVersion(SharedConstants.getProtocolVersion());
        }
        return true;
    }

    private void setLocationFilters(ServersRequest request) {
        switch (geoSearchTypeSetting.get()) {
            case ASN -> request.setAsn(asnNumberSetting.get());
            case Country -> {
                if (!countrySetting.get().name.equalsIgnoreCase("Any")) {
                    request.setCountryCode(countrySetting.get().code);
                }
            }
        }
    }

    private void stopSearch() {
        if (searchFuture != null && !searchFuture.isDone()) {
            searchFuture.cancel(true);
        }
        searchActive = false;
        findButton.visible = true;
        stopButton.visible = false;
        statusLabel.set("Search stopped");
        progressBar.progress = 1;
    }

    private void handleSearchResponse(ServersResponse response) {
        searchActive = false;
        findButton.visible = true;
        stopButton.visible = false;
        progressBar.progress = 1;

        if (response == null || response.isError()) {
            statusLabel.set(searchError != null ? searchError : 
                response != null ? response.error : "Unknown error");
            return;
        }

        this.foundServers = response.data;
        this.totalPages = (int) Math.ceil((double) foundServers.size() / RESULTS_PER_PAGE);
        this.currentPage.set(1);

        if (foundServers.isEmpty()) {
            statusLabel.set("No servers found");
            resultsTable.add(theme.label("No servers matching your criteria")).expandX();
            return;
        }

        statusLabel.set(String.format("Found %d servers", foundServers.size()));
        displayPage(1);
    }

    private void displayPage(int page) {
        setupResultsHeader();
        int startIdx = (page - 1) * RESULTS_PER_PAGE;
        int endIdx = Math.min(startIdx + RESULTS_PER_PAGE, foundServers.size());

        for (int i = startIdx; i < endIdx; i++) {
            ServersResponse.Server server = foundServers.get(i);
            addServerToTable(server);
        }

        // Pagination controls
        if (totalPages > 1) {
            resultsTable.add(theme.horizontalSeparator()).expandX();
            resultsTable.row();

            WHorizontalList pagination = resultsTable.add(theme.horizontalList()).expandX().widget();
            
            WButton prevButton = pagination.add(theme.button("Previous")).widget();
            prevButton.action = () -> {
                if (currentPage.get() > 1) {
                    displayPage(currentPage.decrementAndGet());
                }
            };
            prevButton.visible = currentPage.get() > 1;

            pagination.add(theme.label(String.format("Page %d/%d", currentPage.get(), totalPages))).center();

            WButton nextButton = pagination.add(theme.button("Next")).widget();
            nextButton.action = () -> {
                if (currentPage.get() < totalPages) {
                    displayPage(currentPage.incrementAndGet());
                }
            };
            nextButton.visible = currentPage.get() < totalPages;
        }
    }

    private void addServerToTable(ServersResponse.Server server) {
        // Server address
        resultsTable.add(theme.label(server.server));

        // Version
        resultsTable.add(theme.label(server.version != null ? server.version : "Unknown"));

        // Players
        String playersText = server.onlinePlayers != null && server.maxPlayers != null ?
            String.format("%d/%d", server.onlinePlayers, server.maxPlayers) : "?/?";
        resultsTable.add(theme.label(playersText));

        // Last seen
        String lastSeen = server.lastSeen != null ? 
            TIME_FORMATTER.format(Instant.ofEpochSecond(server.lastSeen)) : "Unknown";
        resultsTable.add(theme.label(lastSeen));

        // Action buttons
        WHorizontalList actions = resultsTable.add(theme.horizontalList()).widget();
        
        WButton addButton = actions.add(theme.button("Add")).widget();
        addButton.action = () -> addServer(server.server);

        WButton joinButton = actions.add(theme.button("Join")).widget();
        joinButton.action = () -> joinServer(server.server);

        WButton infoButton = actions.add(theme.button("Info")).widget();
        infoButton.action = () -> showServerInfo(server.server);

        resultsTable.row();
    }

    private void addServer(String address) {
        ServerInfo info = new ServerInfo("ServerSeeker " + address, address, ServerInfo.ServerType.OTHER);
        MultiplayerScreenUtil.addInfoToServerList(multiplayerScreen, info);
        MultiplayerScreenUtil.saveList(multiplayerScreen);
    }

    private void joinServer(String address) {
        HostAndPort hap = HostAndPort.fromString(address);
        ConnectScreen.connect(
            new TitleScreen(), 
            MinecraftClient.getInstance(), 
            new ServerAddress(hap.getHost(), hap.getPort()), 
            new ServerInfo("temp", hap.toString(), ServerInfo.ServerType.OTHER),
            false,
            null
        );
    }

    private void showServerInfo(String address) {
        client.setScreen(new ServerInfoScreen(address));
    }

    private void clearResults() {
        foundServers = null;
        currentPage.set(1);
        totalPages = 1;
        setupResultsHeader();
    }

    public void saveSettings() {
        savedSettings = settings.toTag();
    }

    public void loadSettings() {
        if (savedSettings != null) {
            settings.fromTag(savedSettings);
        }
    }

    public void resetSettings() {
        settings.forEach(Setting::reset);
        saveSettings();
    }

    @Override
    protected void onClosed() {
        stopSearch();
        ServerSeeker.COUNTRY_MAP.values().forEach(Country::dispose);
    }
}
