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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit;

import com.sk89q.worldedit.bags.BlockBag;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.util.eventbus.EventBus;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A factory for {@link EditSession}s.
 */
public final class EditSessionFactory {

    private final EventBus eventBus;

    /**
     * Create a new factory.
     *
     * @param eventBus the event bus
     */
    public EditSessionFactory(EventBus eventBus) {
        checkNotNull(eventBus);
        this.eventBus = eventBus;
    }

    /**
     * Construct an edit session with a maximum number of blocks.
     *
     * @param world the world
     * @param maxBlocks the maximum number of blocks that can be changed, or -1 to use no limit
     */
    public EditSession getEditSession(LocalWorld world, int maxBlocks) {
        return new EditSession(eventBus, world, maxBlocks, null, new EditSessionEvent(world, null, maxBlocks));
    }

    /**
     * Construct an edit session with a maximum number of blocks.
     *
     * @param world the world
     * @param maxBlocks the maximum number of blocks that can be changed, or -1 to use no limit
     * @param player the player that the {@link EditSession} is for
     */
    public EditSession getEditSession(LocalWorld world, int maxBlocks, LocalPlayer player) {
        return new EditSession(eventBus, world, maxBlocks, null, new EditSessionEvent(world, player, maxBlocks));
    }

    /**
     * Construct an edit session with a maximum number of blocks and a block bag.
     *
     * @param world the world
     * @param maxBlocks the maximum number of blocks that can be changed, or -1 to use no limit
     * @param blockBag an optional {@link BlockBag} to use, otherwise null
     */
    public EditSession getEditSession(LocalWorld world, int maxBlocks, BlockBag blockBag) {
        return new EditSession(eventBus, world, maxBlocks, blockBag, new EditSessionEvent(world, null, maxBlocks));
    }

    /**
     * Construct an edit session with a maximum number of blocks and a block bag.
     *
     * @param world the world
     * @param maxBlocks the maximum number of blocks that can be changed, or -1 to use no limit
     * @param blockBag an optional {@link BlockBag} to use, otherwise null
     * @param player the player that the {@link EditSession} is for
     */
    public EditSession getEditSession(LocalWorld world, int maxBlocks, BlockBag blockBag, LocalPlayer player) {
        return new EditSession(eventBus, world, maxBlocks, blockBag, new EditSessionEvent(world, player, maxBlocks));
    }

}
