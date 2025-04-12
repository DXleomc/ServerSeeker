package de.damcraft.serverseeker.commands;

import com.google.common.net.HostAndPort;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import de.damcraft.serverseeker.ServerSeeker;
import de.damcraft.serverseeker.ssapi.requests.ServerInfoRequest;
import de.damcraft.serverseeker.ssapi.responses.ServerInfoResponse;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static de.damcraft.serverseeker.ServerSeeker.LOG;

public class ServerInfoCommand extends Command {
    private static final SimpleCommandExceptionType SINGLEPLAYER_EXCEPTION = 
        new SimpleCommandExceptionType(new LiteralMessage("Cannot run command in singleplayer."));
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
    private static final String API_ENDPOINT = "https://api.serverseeker.net/server_info";

    public ServerInfoCommand() {
        super("server-info", "Displays detailed information about the current server", "si");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (mc.getCurrentServerEntry() == null) {
                throw SINGLEPLAYER_EXCEPTION.create();
            }

            HostAndPort hap = HostAndPort.fromString(mc.getCurrentServerEntry().address);
            ServerInfoRequest request = new ServerInfoRequest(ServerSeeker.API_KEY, hap.getHost(), hap.getPort());

            info("Fetching server info...", Formatting.GRAY);

            MeteorExecutor.execute(() -> {
                try {
                    ServerInfoResponse response = Http.post(API_ENDPOINT)
                        .exceptionHandler(e -> {
                            LOG.error("Failed to fetch server info: ", e);
                            mc.execute(() -> error("Failed to connect to ServerSeeker API"));
                        })
                        .timeout(5000, TimeUnit.MILLISECONDS)
                        .bodyJson(request)
                        .sendJson(ServerInfoResponse.class);

                    if (response == null) return;

                    mc.execute(() -> handleResponse(response));
                } catch (Exception e) {
                    LOG.error("Unexpected error in ServerInfoCommand: ", e);
                    mc.execute(() -> error("An unexpected error occurred"));
                }
            });

            return SINGLE_SUCCESS;
        });
    }

    private void handleResponse(ServerInfoResponse response) {
        if (response.isError()) {
            error(response.error());
            return;
        }

        // Format server description
        String description = formatDescription(response.description());
        
        // Format last seen time with relative time
        String lastSeen = formatLastSeen(response.lastSeen());
        
        // Build the info message
        Text infoMessage = buildInfoMessage(response, description, lastSeen);
        
        // Send the formatted message
        info(infoMessage);
    }

    private String formatDescription(String rawDescription) {
        if (rawDescription == null) return "No description available";
        
        return rawDescription
            .replace("\n", " ")
            .replace("§r", "")
            .replace("§", "&")
            .trim();
    }

    private String formatLastSeen(int lastSeenTimestamp) {
        Instant lastSeenInstant = Instant.ofEpochSecond(lastSeenTimestamp);
        String formattedDate = DATE_FORMATTER.format(lastSeenInstant);
        
        long secondsAgo = Instant.now().getEpochSecond() - lastSeenTimestamp;
        String relativeTime;
        
        if (secondsAgo < 60) {
            relativeTime = secondsAgo + " seconds ago";
        } else if (secondsAgo < 3600) {
            relativeTime = TimeUnit.SECONDS.toMinutes(secondsAgo) + " minutes ago";
        } else if (secondsAgo < 86400) {
            relativeTime = TimeUnit.SECONDS.toHours(secondsAgo) + " hours ago";
        } else {
            relativeTime = TimeUnit.SECONDS.toDays(secondsAgo) + " days ago";
        }
        
        return formattedDate + " (" + relativeTime + ")";
    }

    private Text buildInfoMessage(ServerInfoResponse response, String description, String lastSeen) {
        Text.Builder builder = Text.literal("")
            .append(Text.literal("=== Server Information ===\n").formatted(Formatting.GOLD, Formatting.BOLD))
            .append(buildLine("Address", response.host() + ":" + response.port()))
            .append(buildLine("Status", response.online() ? "Online" : "Offline"))
            .append(buildLine("Cracked", response.cracked() != null ? response.cracked().toString() : "Unknown"))
            .append(buildLine("Description", description))
            .append(buildLine("Players", response.onlinePlayers() + "/" + response.maxPlayers()))
            .append(buildLine("Version", response.version() + " (Protocol " + response.protocol() + ")"))
            .append(buildLine("Last Scanned", lastSeen));
        
        if (!response.players().isEmpty()) {
            builder.append(Text.literal("\n=== Recent Players ===\n").formatted(Formatting.GOLD, Formatting.BOLD));
            
            // Limit to top 10 players and sort by last seen
            List<ServerInfoResponse.Player> recentPlayers = response.players().stream()
                .sorted((p1, p2) -> Long.compare(p2.lastSeen(), p1.lastSeen()))
                .limit(10)
                .collect(Collectors.toList());
            
            for (ServerInfoResponse.Player player : recentPlayers) {
                String playerLastSeen = formatLastSeen(player.lastSeen());
                builder.append(buildLine(player.name(), playerLastSeen));
            }
        } else {
            builder.append(Text.literal("\nNo player history available").formatted(Formatting.GRAY));
        }
        
        return builder.build();
    }

    private Text buildLine(String label, String value) {
        return Text.literal("")
            .append(Text.literal(label + ": ").formatted(Formatting.GRAY))
            .append(Text.literal(value).formatted(Formatting.WHITE))
            .append(Text.literal("\n"));
    }
}
