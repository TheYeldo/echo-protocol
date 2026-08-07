package dev.yeldos.echoprotocol.room;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.memory.PlayerMemoryState;
import dev.yeldos.echoprotocol.profile.ObservationMetric;
import dev.yeldos.echoprotocol.stage.FamiliarLocation;
import dev.yeldos.echoprotocol.stage.FamiliarLocationType;
import dev.yeldos.echoprotocol.stage.StageManager;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RoomMemoryManager {
    private final StageManager stageManager;
    private final RoomTransitionTracker transitions = new RoomTransitionTracker();
    private final Map<UUID, Long> lastUpdateTicks = new HashMap<>();

    public RoomMemoryManager(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    public void tick(MinecraftServer server, EchoConfig config, long tick) {
        if (!config.roomMemoryEnabled()) {
            return;
        }
        long interval = (long) config.roomMemoryUpdateIntervalSeconds() * 20L;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator() || tick - lastUpdateTicks.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2) < interval) {
                continue;
            }
            lastUpdateTicks.put(player.getUuid(), tick);
            observePeriodic(player, config, tick);
        }
    }

    public RoomMemoryNode observeInteraction(ServerPlayerEntity player, FamiliarLocationType familiarType,
                                              BlockPos position, EchoConfig config, long tick) {
        if (!config.roomMemoryEnabled() || !player.getEntityWorld().isChunkLoaded(position)) {
            return null;
        }
        RoomMemoryType roomType = RoomMemoryType.fromFamiliarLocation(familiarType);
        Probe probe = probe(player.getEntityWorld(), position, roomType, config);
        RoomObservation observation = new RoomObservation(dimension(player), position, probe.type(),
                Math.max(0.62F, probe.confidence()), tick, familiarId(familiarType, position),
                List.of(position), RoomObservationSource.EXPLICIT_INTERACTION);
        return store(player, observation, familiarType == FamiliarLocationType.DOORWAY ? position : null, config, tick);
    }

    private void observePeriodic(ServerPlayerEntity player, EchoConfig config, long tick) {
        FamiliarLocation familiar = stageManager.familiarLocations(player).stream()
                .filter(location -> location.dimension().equals(dimension(player)))
                .filter(location -> player.getEntityWorld().isChunkLoaded(location.pos()))
                .filter(location -> location.pos().getSquaredDistance(player.getBlockPos())
                        <= config.roomMemoryProbeRadius() * config.roomMemoryProbeRadius())
                .min(Comparator.comparingDouble(location -> location.pos().getSquaredDistance(player.getBlockPos())))
                .orElse(null);
        RoomMemoryType suggested = familiar == null
                ? RoomMemoryType.IDLE : RoomMemoryType.fromFamiliarLocation(familiar.type());
        BlockPos center = familiar == null ? player.getBlockPos() : familiar.pos();
        Probe probe = probe(player.getEntityWorld(), center, suggested, config);
        if (familiar == null && probe.confidence() < config.roomMemoryMinimumConfidence()) {
            return;
        }
        RoomObservationSource source = familiar == null
                ? RoomObservationSource.LOCAL_PROBE : RoomObservationSource.FAMILIAR_LOCATION;
        RoomObservation observation = new RoomObservation(dimension(player), center, probe.type(), probe.confidence(),
                tick, familiar == null ? "" : familiarId(familiar.type(), familiar.pos()),
                familiar == null ? List.of() : List.of(familiar.pos()), source);
        store(player, observation, null, config, tick);
    }

    private RoomMemoryNode store(ServerPlayerEntity player, RoomObservation observation, BlockPos transitionPosition,
                                 EchoConfig config, long tick) {
        PlayerMemoryState memory = stageManager.memory(player.getUuid());
        RoomMemoryNode previous = memory.roomGraph().nearest(observation.dimension(), observation.center(), 6.0D)
                .orElse(null);
        boolean rechecked = previous != null && previous.canMerge(observation)
                && tick - previous.lastVisitedTick() >= 2L * 60L * 20L;
        RoomMemoryNode node = memory.roomGraph().observe(observation, config.roomMemoryMaximumNodes(),
                config.roomMemoryMaximumEdges());
        transitions.observe(player.getUuid(), memory.roomGraph(), node, transitionPosition, tick,
                config.roomMemoryMaximumEdges());
        if (rechecked && config.observationProfileEnabled()) {
            memory.recordObservation(ObservationMetric.ROOM_RECHECKED, 1.0F, null);
        }
        return node;
    }

    static Probe probe(ServerWorld world, BlockPos center, RoomMemoryType suggested, EchoConfig config) {
        int radius = config.roomMemoryProbeRadius();
        int maximumChecks = config.roomMemoryMaximumBlockChecks();
        int checks = 0;
        int solid = 0;
        int ceiling = 0;
        int openSky = 0;
        List<BlockPos> offsets = boundedOffsets(radius, maximumChecks);
        for (BlockPos offset : offsets) {
            BlockPos position = center.add(offset);
            if (!world.isChunkLoaded(position)) {
                continue;
            }
            BlockState state = world.getBlockState(position);
            checks++;
            if (!state.getCollisionShape(world, position).isEmpty()) {
                solid++;
            }
            if (offset.getY() >= 2 && !state.getCollisionShape(world, position).isEmpty()) {
                ceiling++;
            }
            if (offset.getY() == 2 && world.isSkyVisible(position)) {
                openSky++;
            }
            if (checks >= maximumChecks) {
                break;
            }
        }
        float solidRatio = checks == 0 ? 0.0F : solid / (float) checks;
        boolean underground = !world.isSkyVisible(center.up(2));
        RoomMemoryType type = suggested;
        if (suggested == RoomMemoryType.IDLE || suggested == RoomMemoryType.UNKNOWN) {
            if (underground) {
                type = RoomMemoryType.UNDERGROUND;
            } else if (ceiling >= 4 && openSky == 0) {
                type = RoomMemoryType.SHELTER;
            } else {
                type = RoomMemoryType.UNKNOWN;
            }
        }
        float confidence = (suggested == RoomMemoryType.UNKNOWN || suggested == RoomMemoryType.IDLE ? 0.18F : 0.48F)
                + Math.min(0.25F, solidRatio * 0.75F) + (ceiling >= 4 ? 0.12F : 0.0F)
                + (underground ? 0.08F : 0.0F);
        return new Probe(type, Math.max(0.0F, Math.min(1.0F, confidence)), checks);
    }

    static List<BlockPos> boundedOffsets(int radius, int maximum) {
        List<BlockPos> result = new ArrayList<>(maximum);
        for (int distance = 0; distance <= radius && result.size() < maximum; distance++) {
            for (int y = -1; y <= 3 && result.size() < maximum; y++) {
                for (int x = -distance; x <= distance && result.size() < maximum; x++) {
                    for (int z = -distance; z <= distance && result.size() < maximum; z++) {
                        if (distance > 0 && Math.max(Math.abs(x), Math.abs(z)) != distance) {
                            continue;
                        }
                        result.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    public RoomMemoryNode currentRoom(ServerPlayerEntity player, double maximumDistance) {
        return stageManager.memory(player.getUuid()).roomGraph()
                .nearest(dimension(player), player.getBlockPos(), maximumDistance).orElse(null);
    }

    public void clearSession(UUID playerUuid) {
        transitions.clear(playerUuid);
        lastUpdateTicks.remove(playerUuid);
    }

    public void clearAll() {
        transitions.clearAll();
        lastUpdateTicks.clear();
    }

    private static String dimension(ServerPlayerEntity player) {
        return player.getEntityWorld().getRegistryKey().getValue().toString();
    }

    private static String familiarId(FamiliarLocationType type, BlockPos position) {
        return type.name() + ":" + position.getX() + ":" + position.getY() + ":" + position.getZ();
    }

    public record Probe(RoomMemoryType type, float confidence, int blockChecks) {
    }
}
