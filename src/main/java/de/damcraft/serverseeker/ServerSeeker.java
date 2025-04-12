package de.damcraft.serverseeker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import de.damcraft.serverseeker.commands.*;
import de.damcraft.serverseeker.country.Countries;
import de.damcraft.serverseeker.country.Country;
import de.damcraft.serverseeker.country.CountrySetting;
import de.damcraft.serverseeker.hud.HistoricPlayersHud;
import de.damcraft.serverseeker.modules.*;
import de.damcraft.serverseeker.utils.*;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.utils.SettingsWidgetFactory;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerSeeker extends MeteorAddon {
    // Constants
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("ServerSeeker", 
        Items.SPYGLASS.getDefaultStack(), 
        Text.literal("ServerSeeker").formatted(Formatting.GOLD));
    
    public static final Map<String, Country> COUNTRY_MAP = new Object2ReferenceOpenHashMap<>();
    public static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    
    // Configuration
    private static final boolean DEBUG_MODE = false;
    private static final String API_BASE_URL = "https://api.serverseeker.net/v1";
    
    // Gson instance with pretty printing for debug
    public static final Gson GSON = DEBUG_MODE ? 
        new GsonBuilder().setPrettyPrinting().create() : 
        new Gson();
    
    // API key management
    private static String apiKey = "ZzOluD4Uj0TPrRPZuE94UtBuIVjYxNMt";
    
    @Override
    public void onInitialize() {
        logStartupBanner();
        checkEnvironment();
        
        try {
            // Initialize core components
            initializeCountries();
            registerModules();
            registerHudElements();
            registerCommands();
            registerEventHandlers();
            registerCustomSettings();
            
            LOG.info("ServerSeeker initialized successfully!");
        } catch (Exception e) {
            LOG.error("Failed to initialize ServerSeeker", e);
            throw new RuntimeException("ServerSeeker initialization failed", e);
        }
    }
    
    private void logStartupBanner() {
        LOG.info("\n" +
            "  ____                            ____                _               \n" +
            " / ___|  ___ _ ____   _____ _ __ / ___|  ___ _ __ ___| | _____ _ __   \n" +
            " \\___ \\ / _ \\ '__\\ \\ / / _ \\ '__\\___ \\ / __| '__/ _ \\ |/ / _ \\ '__|  \n" +
            "  ___) |  __/ |   \\ V /  __/ |   ___) | (__| | |  __/   <  __/ |     \n" +
            " |____/ \\___|_|    \\_/ \\___|_|  |____/ \\___|_|  \\___|_|\\_\\___|_|    \n" +
            "                                                                     ");
    }
    
    private void checkEnvironment() {
        if (DEBUG_MODE) {
            LOG.warn("ServerSeeker is running in DEBUG mode");
        }
        
        String minecraftVersion = MeteorClient.VERSION.toString();
        LOG.info("Running on Minecraft version: {}", minecraftVersion);
    }
    
    private void initializeCountries() {
        long startTime = System.currentTimeMillis();
        Countries.init();
        LOG.debug("Country data loaded in {}ms", System.currentTimeMillis() - startTime);
    }
    
    private void registerModules() {
        Modules.get().addAll(
            new BungeeSpoofModule(),
            new ServerFinderModule(),
            new PlayerTrackerModule(),
            new MOTDAnalyzerModule()
        );
    }
    
    private void registerHudElements() {
        Hud.get().register(
            HistoricPlayersHud.INFO,
            new ServerInfoHud(),
            new PlayerStatsHud()
        );
    }
    
    private void registerCommands() {
        Commands.addAll(
            new ServerInfoCommand(),
            new FindServersCommand(),
            new PlayerLookupCommand(),
            new ServerStatsCommand()
        );
    }
    
    private void registerEventHandlers() {
        MeteorClient.EVENT_BUS.subscribe(HistoricPlayersUpdater.class);
        MeteorClient.EVENT_BUS.subscribe(ServerConnectionTracker.class);
    }
    
    private void registerCustomSettings() {
        SettingsWidgetFactory.registerCustomFactory(CountrySetting.class, 
            (theme) -> (table, setting) -> CountrySetting.countrySettingW(table, (CountrySetting) setting, theme));
    }
    
    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }
    
    // API Methods
    public static String getApiKey() {
        return apiKey;
    }
    
    public static void setApiKey(String newKey) {
        if (newKey != null && !newKey.trim().isEmpty()) {
            apiKey = newKey.trim();
            LOG.info("API key updated");
        } else {
            LOG.warn("Attempted to set invalid API key");
        }
    }
    
    public static String getApiUrl(String endpoint) {
        return API_BASE_URL + endpoint;
    }
    
    // Utility methods
    public static void runAsync(Runnable task) {
        EXECUTOR.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                LOG.error("Async task failed", e);
            }
        });
    }
    
    @Override
    public String getPackage() {
        return "de.damcraft.serverseeker";
    }
    
    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("DXleomc", "ServerSeeker", "main");
    }
    
    @Override
    public String getWebsite() {
        return "https://serverseeker.net";
    }
    
    @Override
    public String getCommit() {
        return Optional.ofNullable(FabricLoader.getInstance()
                .getModContainer("serverseeker")
                .orElseThrow()
                .getMetadata()
                .getCustomValue("github:sha"))
            .map(CustomValue::getAsString)
            .map(String::trim)
            .orElse("unknown");
    }
    
    @Override
    public String toString() {
        return "ServerSeeker v" + getVersion() + " by DXleomc";
    }
}
