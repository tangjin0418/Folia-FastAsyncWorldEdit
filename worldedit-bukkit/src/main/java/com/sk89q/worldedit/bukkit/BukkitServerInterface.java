/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit;

import com.fastasyncworldedit.bukkit.util.MinecraftVersion;
import com.fastasyncworldedit.core.configuration.Settings;
import com.fastasyncworldedit.core.extent.processor.PlacementStateProcessor;
import com.fastasyncworldedit.core.extent.processor.lighting.RelighterFactory;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.google.common.collect.Sets;
import com.sk89q.bukkit.util.CommandInfo;
import com.sk89q.bukkit.util.CommandRegistration;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.command.util.PermissionCondition;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.AbstractPlatform;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.MultiUserPlatform;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.extension.platform.Watchdog;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.lifecycle.Lifecycled;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.registry.Registries;
import io.papermc.lib.PaperLib;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.enginehub.piston.CommandManager;
import org.tjdev.util.tjpluginutil.spigot.FoliaUtil;
import org.tjdev.util.tjpluginutil.spigot.scheduler.universalscheduler.scheduling.tasks.MyScheduledTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sk89q.worldedit.util.formatting.WorldEditText.reduceToText;

public class BukkitServerInterface extends AbstractPlatform implements MultiUserPlatform {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    public final Server server;
    public final WorldEditPlugin plugin;
    private final CommandRegistration dynamicCommands;
    private final Lifecycled<Watchdog> watchdog;
    //FAWE start
    private RelighterFactory relighterFactory;
    //FAWE end
    private boolean hookingEvents;

    public BukkitServerInterface(WorldEditPlugin plugin, Server server) {
        this.plugin = plugin;
        this.server = server;
        this.dynamicCommands = new CommandRegistration(plugin);
        this.watchdog = plugin.getLifecycledBukkitImplAdapter()
                .filter(BukkitImplAdapter::supportsWatchdog)
                .map(BukkitWatchdog::new);
    }

    CommandRegistration getDynamicCommands() {
        return dynamicCommands;
    }

    boolean isHookingEvents() {
        return hookingEvents;
    }

    @Override
    public Registries getRegistries() {
        return BukkitRegistries.getInstance();
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getDataVersion() {
        if (plugin.getLifecycledBukkitImplAdapter().isValid()) {
            return Bukkit.getUnsafe().getDataVersion();
        }
        return -1;
    }

    @Override
    public DataFixer getDataFixer() {
        if (plugin.getBukkitImplAdapter() != null) {
            return plugin.getBukkitImplAdapter().getDataFixer();
        }
        return null;
    }

    @Override
    public boolean isValidMobType(String type) {
        if (!type.startsWith("minecraft:")) {
            return false;
        }
        @SuppressWarnings("deprecation") final EntityType entityType = EntityType.fromName(type.substring(10));
        return entityType != null && entityType.isAlive();
    }

    @Override
    public void reload() {
        plugin.loadConfiguration();
    }

    public MyScheduledTask THE_ONLY_TASK;
    @Override
    public int schedule(long delay, long period, Runnable task) {
        THE_ONLY_TASK = FoliaUtil.scheduler.runTaskTimer(plugin, task, delay, period);
        return 0;
    }

    @Override
    public Watchdog getWatchdog() {
        return watchdog.value().orElse(null);
    }

    @Override
    public List<com.sk89q.worldedit.world.World> getWorlds() {
        List<World> worlds = server.getWorlds();
        List<com.sk89q.worldedit.world.World> ret = new ArrayList<>(worlds.size());

        for (World world : worlds) {
            ret.add(BukkitAdapter.adapt(world));
        }

        return ret;
    }

    @Nullable
    @Override
    public Player matchPlayer(Player player) {
        if (player instanceof BukkitPlayer) {
            return player;
        } else {
            org.bukkit.entity.Player bukkitPlayer = server.getPlayerExact(player.getName());
            return bukkitPlayer != null ? WorldEditPlugin.getInstance().wrapPlayer(bukkitPlayer) : null;
        }
    }

    @Nullable
    @Override
    public BukkitWorld matchWorld(com.sk89q.worldedit.world.World world) {
        if (world instanceof BukkitWorld) {
            return (BukkitWorld) world;
        } else {
            World bukkitWorld = server.getWorld(world.getName());
            return bukkitWorld != null ? new BukkitWorld(bukkitWorld) : null;
        }
    }

    @Override
    public void registerCommands(CommandManager dispatcher) {
        BukkitCommandInspector inspector = new BukkitCommandInspector(plugin, dispatcher);

        dynamicCommands.register(dispatcher.getAllCommands()
                .map(command -> {
                    String[] permissionsArray = command.getCondition()
                            .as(PermissionCondition.class)
                            .map(PermissionCondition::getPermissions)
                            .map(s -> s.toArray(new String[0]))
                            .orElseGet(() -> new String[0]);

                    String[] aliases = Stream.concat(
                            Stream.of(command.getName()),
                            command.getAliases().stream()
                    ).toArray(String[]::new);
                    // TODO Handle localisation correctly
                    return new CommandInfo(
                            reduceToText(
                                    command.getUsage(),
                                    WorldEdit.getInstance().getConfiguration().defaultLocale
                            ),
                            reduceToText(command.getDescription(), WorldEdit.getInstance().getConfiguration().defaultLocale),
                            aliases,
                            inspector,
                            permissionsArray
                    );
                }).collect(Collectors.toList()));
    }

    @Override
    public void setGameHooksEnabled(boolean enabled) {
        this.hookingEvents = enabled;
    }

    @Override
    public LocalConfiguration getConfiguration() {
        return plugin.getLocalConfiguration();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String getPlatformName() {
        return "Bukkit-Official";
    }

    @Override
    public String getPlatformVersion() {
        return plugin.getDescription().getVersion();
    }

    //FAWE start
    @Override
    public String id() {
        return "intellectualsites:bukkit";
    }
    //FAWE end

    @Override
    public Map<Capability, Preference> getCapabilities() {
        Map<Capability, Preference> capabilities = new EnumMap<>(Capability.class);
        capabilities.put(Capability.CONFIGURATION, Preference.NORMAL);
        capabilities.put(Capability.WORLDEDIT_CUI, Preference.NORMAL);
        capabilities.put(Capability.GAME_HOOKS, Preference.PREFERRED);
        capabilities.put(Capability.PERMISSIONS, Preference.PREFERRED);
        capabilities.put(Capability.USER_COMMANDS, Preference.PREFERRED);
        capabilities.put(Capability.WORLD_EDITING, Preference.PREFER_OTHERS);
        return capabilities;
    }

    private static final Set<SideEffect> SUPPORTED_SIDE_EFFECTS = Sets.immutableEnumSet(
            SideEffect.NEIGHBORS
    );

    @Override
    public Set<SideEffect> getSupportedSideEffects() {
        if (plugin.getBukkitImplAdapter() != null) {
            return plugin.getBukkitImplAdapter().getSupportedSideEffects();
        }
        return SUPPORTED_SIDE_EFFECTS;
    }

    @Override
    public long getTickCount() {
        if (PaperLib.isPaper()) {
            return Bukkit.getCurrentTick();
        }
        return super.getTickCount();
    }

    public void unregisterCommands() {
        dynamicCommands.unregisterCommands();
    }

    @Override
    public Collection<Actor> getConnectedUsers() {
        List<Actor> users = new ArrayList<>();
        for (org.bukkit.entity.Player player : Bukkit.getServer().getOnlinePlayers()) {
            users.add(WorldEditPlugin.getInstance().wrapPlayer(player));
        }
        return users;
    }

    //FAWE start
    @Override
    public @Nonnull
    RelighterFactory getRelighterFactory() {
        if (this.relighterFactory == null) {
            this.relighterFactory = this.plugin.getBukkitImplAdapter().getRelighterFactory();
            LOGGER.info("Using {} as relighter factory.", this.relighterFactory.getClass().getCanonicalName());
        }
        return this.relighterFactory;
    }

    @Override
    public int versionMinY() {
        return MinecraftVersion.getCurrent().isEqualOrHigherThan(MinecraftVersion.CAVES_18) ? -64 : 0;
    }

    @Override
    public int versionMaxY() {
        return MinecraftVersion.getCurrent().isEqualOrHigherThan(MinecraftVersion.CAVES_18) ? 319 : 255;
    }

    @Override
    public IBatchProcessor getPlatformPostProcessor(boolean fastMode) {
        boolean tickFluid = Settings.settings().EXPERIMENTAL.ALLOW_TICK_FLUIDS;
        if (!tickFluid) {
            return null;
        }
        return this.plugin.getBukkitImplAdapter().getTickingPostProcessor();
    }

    @Override
    public PlacementStateProcessor getPlatformPlacementProcessor(Extent extent, BlockTypeMask mask, Region region) {
        return this.plugin.getBukkitImplAdapter().getPlatformPlacementProcessor(extent, mask, region);
    }
    //FAWE end
}
