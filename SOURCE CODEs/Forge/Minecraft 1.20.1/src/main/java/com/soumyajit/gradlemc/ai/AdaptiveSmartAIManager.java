package com.soumyajit.gradlemc.ai;

import com.mojang.brigadier.Command;
import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.network.GradleMCNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AdaptiveSmartAIManager {
    private static final int SAMPLE_INTERVAL_TICKS = 40;
    private static final int DARK_LIGHT_THRESHOLD = 7;
    private static final int SAFE_EVENT_MIN_SCORE = 30;
    private static final int HIGH_EVENT_MIN_SCORE = 60;
    private static final int EXTREME_EVENT_MIN_SCORE = 85;
    private static final int MAX_TRACKED_SIGNAL_TICKS = 24_000;
    private static final Map<UUID, PlayerState> STATES = new HashMap<>();

    private AdaptiveSmartAIManager() {
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!GradleMCConfig.ENABLE_ADAPTIVE_SMART_AI.get()) {
            if (!STATES.isEmpty()) {
                STATES.clear();
            }
            return;
        }
        long tick = event.getServer().getTickCount();
        if (tick % SAMPLE_INTERVAL_TICKS != 0) {
            return;
        }
        Set<UUID> activePlayers = event.getServer().getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID)
                .collect(Collectors.toSet());
        STATES.keySet().retainAll(activePlayers);
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.isSpectator()) {
                continue;
            }
            state(player).sample(player, tick);
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player.getUUID());
        }
    }

    public static void onLivingHurt(LivingHurtEvent event) {
        if (!GradleMCConfig.ENABLE_ADAPTIVE_SMART_AI.get() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        state(player).recordDamage(Math.max(1, Math.round(event.getAmount())));
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!GradleMCConfig.ENABLE_ADAPTIVE_SMART_AI.get()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerState state = state(player);
            state.recentDeaths = Math.min(80, state.recentDeaths + 1);
            if (GradleMCConfig.REDUCE_INTENSITY_AFTER_DEATH.get()) {
                state.threatScore = Math.max(0, state.threatScore - 35);
                state.eventCooldownTicks = Math.max(state.eventCooldownTicks, GradleMCConfig.EVENT_COOLDOWN_TICKS.get());
                state.ambienceCooldownTicks = Math.max(state.ambienceCooldownTicks, GradleMCConfig.AMBIENCE_COOLDOWN_TICKS.get() / 2);
                state.recentAdaptation = "Reduced intensity after death";
                sync(player);
            }
            return;
        }
        if (event.getEntity() instanceof Mob && event.getSource().getEntity() instanceof ServerPlayer player) {
            PlayerState state = state(player);
            state.recentMobKills = Math.min(80, state.recentMobKills + 1);
        }
    }

    public static int status(CommandSourceStack source) {
        Optional<ServerPlayer> maybePlayer = playerOrFailure(source);
        if (maybePlayer.isEmpty()) {
            return 0;
        }
        SmartAIStatus status = statusFor(maybePlayer.get());
        source.sendSuccess(() -> Component.literal("Adaptive Diagnostics: "
                + enabledLabel(status.adaptiveSmartAIEnabled())
                + ", Stability Risk " + status.threatLevel() + " (" + status.threatScore() + "/"
                + GradleMCConfig.MAX_THREAT_LEVEL.get() + ")"), false);
        source.sendSuccess(() -> Component.literal("Signals: dark=" + seconds(status.darknessTicks())
                + "s, underground=" + seconds(status.undergroundTicks())
                + "s, sleep=" + seconds(status.ticksSinceSleep())
                + "s, movement=" + status.movementPressure()
                + ", damage=" + status.recentDamageTaken()
                + ", kills=" + status.recentMobKills()
                + ", deaths=" + status.recentDeaths()), false);
        source.sendSuccess(() -> Component.literal("Recent adaptation: " + status.recentAdaptation()), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int reset(CommandSourceStack source) {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.literal("Resetting adaptive diagnostics runtime data requires permission level 2."));
            return 0;
        }
        Optional<ServerPlayer> maybePlayer = playerOrFailure(source);
        if (maybePlayer.isEmpty()) {
            return 0;
        }
        ServerPlayer player = maybePlayer.get();
        STATES.remove(player.getUUID());
        sync(player);
        source.sendSuccess(() -> Component.literal("Adaptive diagnostics runtime data reset for " + player.getGameProfile().getName() + "."), false);
        return Command.SINGLE_SUCCESS;
    }

    public static SmartAIStatus statusFor(ServerPlayer player) {
        if (player == null) {
            return SmartAIStatus.disabled();
        }
        if (!GradleMCConfig.ENABLE_ADAPTIVE_SMART_AI.get()) {
            return SmartAIStatus.disabled();
        }
        return state(player).toStatus();
    }

    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        GradleMCNetwork.syncSmartAIStatus(player, statusFor(player));
    }

    private static PlayerState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
    }

    private static Optional<ServerPlayer> playerOrFailure(CommandSourceStack source) {
        try {
            return Optional.of(source.getPlayerOrException());
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Adaptive diagnostics status requires an in-game player."));
            return Optional.empty();
        }
    }

    private static String enabledLabel(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    private static int seconds(int ticks) {
        return ticks / 20;
    }

    private static final class PlayerState {
        private int threatScore;
        private int darknessTicks;
        private int undergroundTicks;
        private int recentDamageTaken;
        private int recentMobKills;
        private int recentDeaths;
        private int eventCooldownTicks;
        private int ambienceCooldownTicks;
        private int ticksSinceSleep;
        private int movementPressure;
        private String recentAdaptation = "Collecting local behavior signals";
        private double lastX;
        private double lastZ;
        private boolean hasLastPosition;

        private void sample(ServerPlayer player, long serverTick) {
            BlockPos pos = player.blockPosition();
            int deltaTicks = SAMPLE_INTERVAL_TICKS;
            ticksSinceSleep = player.isSleeping() ? 0 : Math.min(MAX_TRACKED_SIGNAL_TICKS, ticksSinceSleep + deltaTicks);
            if (player.level().getMaxLocalRawBrightness(pos) <= DARK_LIGHT_THRESHOLD) {
                darknessTicks = Math.min(MAX_TRACKED_SIGNAL_TICKS, darknessTicks + deltaTicks);
            } else {
                darknessTicks = Math.max(0, darknessTicks - deltaTicks);
            }
            if (pos.getY() < player.level().getSeaLevel() - 12) {
                undergroundTicks = Math.min(MAX_TRACKED_SIGNAL_TICKS, undergroundTicks + deltaTicks);
            } else {
                undergroundTicks = Math.max(0, undergroundTicks - deltaTicks);
            }

            int score = calculateScore(player);
            int maxThreat = GradleMCConfig.MAX_THREAT_LEVEL.get();
            threatScore = clamp(score, 0, maxThreat);
            eventCooldownTicks = Math.max(0, eventCooldownTicks - deltaTicks);
            ambienceCooldownTicks = Math.max(0, ambienceCooldownTicks - deltaTicks);
            recentDamageTaken = Math.max(0, recentDamageTaken - 1);
            recentMobKills = Math.max(0, recentMobKills - 1);
            recentDeaths = Math.max(0, recentDeaths - (serverTick % 2400 == 0 ? 1 : 0));

            maybeTriggerAmbience(player);
            maybeTriggerEvent(player);
            logDebug(player);
        }

        private int calculateScore(ServerPlayer player) {
            double multiplier = GradleMCConfig.ADAPTIVE_DIFFICULTY_MULTIPLIER.get();
            int score = (int) Math.round(GradleMCConfig.BASE_THREAT_GAIN.get() * multiplier);
            score += Math.min(25, darknessTicks / 160);
            score += Math.min(20, undergroundTicks / 240);
            score += Math.min(25, recentDamageTaken * 2);
            score += Math.min(20, recentMobKills * 3);
            score += Math.min(15, player.getArmorValue());
            score += Math.min(10, player.experienceLevel / 5);
            movementPressure = distancePressure(player);
            score += Math.min(10, movementPressure);
            score += player.level().dimension() == net.minecraft.world.level.Level.NETHER ? 10 : 0;
            score += player.level().dimension() == net.minecraft.world.level.Level.END ? 15 : 0;
            if (ticksSinceSleep > 18_000) {
                score += 10;
            }
            if (player.getHealth() < player.getMaxHealth() * 0.35F) {
                score -= 15;
            }
            if (GradleMCConfig.REDUCE_INTENSITY_AFTER_DEATH.get()) {
                score -= Math.min(35, recentDeaths * 15);
            }
            return score;
        }

        private int distancePressure(ServerPlayer player) {
            if (!hasLastPosition) {
                lastX = player.getX();
                lastZ = player.getZ();
                hasLastPosition = true;
                return 0;
            }
            double dx = player.getX() - lastX;
            double dz = player.getZ() - lastZ;
            lastX = player.getX();
            lastZ = player.getZ();
            return (int) Math.min(10, Math.sqrt(dx * dx + dz * dz) / 6.0D);
        }

        private void maybeTriggerAmbience(ServerPlayer player) {
            if (!GradleMCConfig.ENABLE_ADAPTIVE_AMBIENCE.get() || ambienceCooldownTicks > 0 || threatScore < SAFE_EVENT_MIN_SCORE) {
                return;
            }
            int chance = threatScore >= HIGH_EVENT_MIN_SCORE ? 4 : 9;
            if (player.getRandom().nextInt(chance) != 0) {
                return;
            }
            ambienceCooldownTicks = randomizedCooldown(player, GradleMCConfig.AMBIENCE_COOLDOWN_TICKS.get());
            recentAdaptation = threatScore >= HIGH_EVENT_MIN_SCORE ? "Increased ambience frequency" : "Queued low intensity ambience";
            player.displayClientMessage(Component.translatable("message.gradlemc.ai.ambience", ThreatLevel.fromScore(threatScore).name()), true);
            sync(player);
        }

        private void maybeTriggerEvent(ServerPlayer player) {
            if (!GradleMCConfig.ENABLE_ADAPTIVE_EVENTS.get() || eventCooldownTicks > 0 || threatScore < SAFE_EVENT_MIN_SCORE) {
                return;
            }
            if (player.getHealth() < player.getMaxHealth() * 0.50F) {
                return;
            }
            if (!GradleMCConfig.ALLOW_HIGH_INTENSITY_EVENTS.get() && threatScore >= HIGH_EVENT_MIN_SCORE) {
                return;
            }
            int chance = threatScore >= EXTREME_EVENT_MIN_SCORE ? 3 : threatScore >= HIGH_EVENT_MIN_SCORE ? 6 : 12;
            if (player.getRandom().nextInt(chance) != 0) {
                return;
            }
            eventCooldownTicks = randomizedCooldown(player, GradleMCConfig.EVENT_COOLDOWN_TICKS.get());
            recentAdaptation = threatScore >= HIGH_EVENT_MIN_SCORE ? "Raised adaptive event intensity" : "Scheduled a subtle adaptive warning";
            player.displayClientMessage(Component.translatable("message.gradlemc.ai.event", ThreatLevel.fromScore(threatScore).name()), false);
            sync(player);
        }

        private int randomizedCooldown(ServerPlayer player, int baseTicks) {
            int spread = Math.max(20, baseTicks / 3);
            return Math.max(20, baseTicks + player.getRandom().nextInt(spread + 1));
        }

        private SmartAIStatus toStatus() {
            return new SmartAIStatus(
                    GradleMCConfig.ENABLE_ADAPTIVE_SMART_AI.get(),
                    GradleMCConfig.ENABLE_ADAPTIVE_AMBIENCE.get(),
                    GradleMCConfig.ENABLE_ADAPTIVE_EVENTS.get(),
                    threatScore,
                    ThreatLevel.fromScore(threatScore),
                    recentAdaptation,
                    eventCooldownTicks,
                    ambienceCooldownTicks,
                    darknessTicks,
                    undergroundTicks,
                    ticksSinceSleep,
                    movementPressure,
                    recentDamageTaken,
                    recentMobKills,
                    recentDeaths
            );
        }

        private void recordDamage(int amount) {
            recentDamageTaken = Math.min(80, recentDamageTaken + amount);
        }

        private void logDebug(ServerPlayer player) {
            if (GradleMCConfig.DEBUG_SMART_AI_LOGGING.get()) {
                GradleMC.LOGGER.debug("GradleMC adaptive diagnostics {} score={} level={} darkTicks={} undergroundTicks={} cooldowns={}/{}",
                        player.getGameProfile().getName(), threatScore, ThreatLevel.fromScore(threatScore),
                        darknessTicks, undergroundTicks, eventCooldownTicks, ambienceCooldownTicks);
            }
        }

        private int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
