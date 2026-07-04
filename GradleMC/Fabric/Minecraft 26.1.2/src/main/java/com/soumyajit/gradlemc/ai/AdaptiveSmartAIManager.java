package com.soumyajit.gradlemc.ai;

import com.mojang.brigadier.Command;
import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.network.GradleMCNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

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

    public static void onEndServerTick(MinecraftServer server) {
        if (!GradleMCConfig.ENABLE_ADAPTIVE_SMART_AI.get()) {
            if (!STATES.isEmpty()) {
                STATES.clear();
            }
            return;
        }
        long tick = server.getTickCount();
        if (tick % SAMPLE_INTERVAL_TICKS != 0) {
            return;
        }
        Set<UUID> activePlayers = server.getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID)
                .collect(Collectors.toSet());
        STATES.keySet().retainAll(activePlayers);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator()) {
                continue;
            }
            state(player).sample(player, tick);
        }
    }

    public static void onPlayerLeave(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    public static boolean onAllowDamage(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!GradleMCConfig.ENABLE_ADAPTIVE_SMART_AI.get() || !(entity instanceof ServerPlayer player)) {
            return true;
        }
        PlayerState state = state(player);
        state.recordDamage(Math.max(1, Math.round(amount)));
        state.refreshRisk(player);
        sync(player);
        return true;
    }

    public static void onAfterDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (!GradleMCConfig.ENABLE_ADAPTIVE_SMART_AI.get()) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            PlayerState state = state(player);
            state.recentDeaths = Math.min(80, state.recentDeaths + 1);
            state.refreshRisk(player);
            if (GradleMCConfig.REDUCE_INTENSITY_AFTER_DEATH.get()) {
                state.eventCooldownTicks = Math.max(state.eventCooldownTicks, GradleMCConfig.EVENT_COOLDOWN_TICKS.get());
                state.ambienceCooldownTicks = Math.max(state.ambienceCooldownTicks, GradleMCConfig.AMBIENCE_COOLDOWN_TICKS.get() / 2);
                state.recentAdaptation = "Reduced intensity after death";
            }
            sync(player);
            return;
        }
        if (entity instanceof Mob && source.getEntity() instanceof ServerPlayer player) {
            PlayerState state = state(player);
            state.recentMobKills = Math.min(80, state.recentMobKills + 1);
            state.refreshRisk(player);
            sync(player);
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
                + ", Adaptive Risk " + status.threatLevel() + " (" + status.threatScore() + "/"
                + GradleMCConfig.MAX_THREAT_LEVEL.get() + ")"), false);
        source.sendSuccess(() -> Component.literal("Signals: dark=" + seconds(status.darknessTicks())
                + "s, underground=" + seconds(status.undergroundTicks())
                + "s, sleep=" + seconds(status.ticksSinceSleep())
                + "s, movement=" + status.movementPressure()
                + ", damage=" + status.recentDamageTaken()
                + ", kills=" + status.recentMobKills()
                + ", deaths=" + status.recentDeaths()
                + ", hostiles=" + status.nearbyHostileMobs()
                + ", health=" + status.healthPercent() + "%"
                + ", food=" + status.foodLevel()), false);
        source.sendSuccess(() -> Component.literal("Top factors: " + status.topRiskFactors()), false);
        source.sendSuccess(() -> Component.literal("Recent adaptation: " + status.recentAdaptation()), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int reset(CommandSourceStack source) {
        if (!hasPermission(source)) {
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
        source.sendSuccess(() -> Component.literal("Adaptive diagnostics runtime data reset for " + player.nameAndId().name() + "."), false);
        return Command.SINGLE_SUCCESS;
    }

    public static SmartAIStatus statusFor(ServerPlayer player) {
        if (player == null) {
            return SmartAIStatus.disabled();
        }
        if (!GradleMCConfig.ENABLE_ADAPTIVE_SMART_AI.get()) {
            return SmartAIStatus.disabled();
        }
        PlayerState state = state(player);
        state.refreshRisk(player);
        return state.toStatus();
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

    private static boolean hasPermission(CommandSourceStack source) {
        PermissionSet permissions = source.permissions();
        if (permissions == PermissionSet.ALL_PERMISSIONS) {
            return true;
        }
        if (permissions instanceof LevelBasedPermissionSet levelBased) {
            return levelBased.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS);
        }
        return false;
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
        private int nearbyHostileMobs;
        private int healthPercent = 100;
        private int foodLevel = 20;
        private String topRiskFactors = "no active pressure signals";
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

            refreshRisk(player);
            eventCooldownTicks = Math.max(0, eventCooldownTicks - deltaTicks);
            ambienceCooldownTicks = Math.max(0, ambienceCooldownTicks - deltaTicks);
            recentDamageTaken = Math.max(0, recentDamageTaken - 1);
            recentMobKills = Math.max(0, recentMobKills - 1);
            recentDeaths = Math.max(0, recentDeaths - (serverTick % 2400 == 0 ? 1 : 0));

            maybeTriggerAmbience(player);
            maybeTriggerEvent(player);
            logDebug(player);
        }

        private void refreshRisk(ServerPlayer player) {
            movementPressure = distancePressure(player);
            nearbyHostileMobs = nearbyHostileMobs(player);
            healthPercent = healthPercent(player);
            foodLevel = player.getFoodData().getFoodLevel();
            AdaptiveRiskCalculator.RiskResult result = AdaptiveRiskCalculator.calculate(new AdaptiveRiskCalculator.RiskSignals(
                    GradleMCConfig.BASE_THREAT_GAIN.get(),
                    GradleMCConfig.ADAPTIVE_DIFFICULTY_MULTIPLIER.get(),
                    GradleMCConfig.MAX_THREAT_LEVEL.get(),
                    darknessTicks,
                    undergroundTicks,
                    recentDamageTaken,
                    recentMobKills,
                    recentDeaths,
                    nearbyHostileMobs,
                    healthPercent,
                    foodLevel,
                    movementPressure,
                    player.level().dimension() == net.minecraft.world.level.Level.NETHER,
                    player.level().dimension() == net.minecraft.world.level.Level.END,
                    ticksSinceSleep
            ));
            threatScore = result.score();
            topRiskFactors = result.topFactors();
        }

        private int eventReadinessScore() {
            if (!GradleMCConfig.REDUCE_INTENSITY_AFTER_DEATH.get()) {
                return threatScore;
            }
            return Math.max(0, threatScore - Math.min(35, recentDeaths * 15));
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
            int actionScore = eventReadinessScore();
            if (!GradleMCConfig.ENABLE_ADAPTIVE_AMBIENCE.get() || ambienceCooldownTicks > 0 || actionScore < SAFE_EVENT_MIN_SCORE) {
                return;
            }
            int chance = actionScore >= HIGH_EVENT_MIN_SCORE ? 4 : 9;
            if (player.getRandom().nextInt(chance) != 0) {
                return;
            }
            ambienceCooldownTicks = randomizedCooldown(player, GradleMCConfig.AMBIENCE_COOLDOWN_TICKS.get());
            recentAdaptation = actionScore >= HIGH_EVENT_MIN_SCORE ? "Increased ambience frequency" : "Queued low intensity ambience";
            player.sendOverlayMessage(Component.translatable("message.gradlemc.ai.ambience", ThreatLevel.fromScore(actionScore).name()));
            sync(player);
        }

        private void maybeTriggerEvent(ServerPlayer player) {
            int actionScore = eventReadinessScore();
            if (!GradleMCConfig.ENABLE_ADAPTIVE_EVENTS.get() || eventCooldownTicks > 0 || actionScore < SAFE_EVENT_MIN_SCORE) {
                return;
            }
            if (player.getHealth() < player.getMaxHealth() * 0.50F) {
                return;
            }
            if (!GradleMCConfig.ALLOW_HIGH_INTENSITY_EVENTS.get() && actionScore >= HIGH_EVENT_MIN_SCORE) {
                return;
            }
            int chance = actionScore >= EXTREME_EVENT_MIN_SCORE ? 3 : actionScore >= HIGH_EVENT_MIN_SCORE ? 6 : 12;
            if (player.getRandom().nextInt(chance) != 0) {
                return;
            }
            eventCooldownTicks = randomizedCooldown(player, GradleMCConfig.EVENT_COOLDOWN_TICKS.get());
            recentAdaptation = actionScore >= HIGH_EVENT_MIN_SCORE ? "Raised adaptive event intensity" : "Scheduled a subtle adaptive warning";
            player.sendSystemMessage(Component.translatable("message.gradlemc.ai.event", ThreatLevel.fromScore(actionScore).name()));
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
                    recentDeaths,
                    nearbyHostileMobs,
                    healthPercent,
                    foodLevel,
                    topRiskFactors
            );
        }

        private void recordDamage(int amount) {
            recentDamageTaken = Math.min(80, recentDamageTaken + amount);
        }

        private void logDebug(ServerPlayer player) {
            if (GradleMCConfig.DEBUG_SMART_AI_LOGGING.get()) {
                GradleMC.LOGGER.debug("GradleMC adaptive diagnostics {} score={} level={} darkTicks={} undergroundTicks={} cooldowns={}/{}",
                        player.nameAndId().name(), threatScore, ThreatLevel.fromScore(threatScore),
                        darknessTicks, undergroundTicks, eventCooldownTicks, ambienceCooldownTicks);
            }
        }

        private int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        private int nearbyHostileMobs(ServerPlayer player) {
            return Math.min(25, player.level()
                    .getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(24.0D), Monster::isAlive)
                    .size());
        }

        private int healthPercent(ServerPlayer player) {
            float maxHealth = Math.max(1.0F, player.getMaxHealth());
            return clamp(Math.round(player.getHealth() * 100.0F / maxHealth), 0, 100);
        }
    }
}
