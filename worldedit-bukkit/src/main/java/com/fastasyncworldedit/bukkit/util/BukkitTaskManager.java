package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.util.TaskManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.tjdev.util.tjpluginutil.spigot.FoliaUtil;
import org.tjdev.util.tjpluginutil.spigot.scheduler.universalscheduler.scheduling.tasks.MyScheduledTask;

import javax.annotation.Nonnull;

public class BukkitTaskManager extends TaskManager {

    private final Plugin plugin;

    public BukkitTaskManager(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int repeat(@Nonnull final Runnable runnable, final int interval) {
        FoliaUtil.scheduler.scheduleSyncRepeatingTask(runnable, interval, interval);
        return -1;
    }

    @Override
    public int repeatAsync(@Nonnull final Runnable runnable, final int interval) {
        FoliaUtil.scheduler.runTaskTimerAsynchronously(runnable, interval, interval);
        return -1;
    }

    @Override
    public void async(@Nonnull final Runnable runnable) {
        FoliaUtil.scheduler.runTaskAsynchronously(runnable);
    }

    @Override
    public void task(@Nonnull final Runnable runnable) {
        FoliaUtil.scheduler.runTask(runnable);
    }

    @Override
    public void later(@Nonnull final Runnable runnable, final int delay) {
        FoliaUtil.scheduler.runTaskLater(runnable, delay);
    }

    @Override
    public void laterAsync(@Nonnull final Runnable runnable, final int delay) {
        FoliaUtil.scheduler.runTaskLaterAsynchronously(runnable, delay);
    }

    @Override
    public void cancel(final int task) {
        if (task != -1) {
            Bukkit.getScheduler().cancelTask(task);
        }
    }

}
