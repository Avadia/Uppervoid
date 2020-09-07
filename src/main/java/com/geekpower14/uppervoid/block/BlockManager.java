package com.geekpower14.uppervoid.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.samagames.api.SamaGamesAPI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
 * This file is part of Uppervoid.
 *
 * Uppervoid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Uppervoid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Uppervoid.  If not, see <http://www.gnu.org/licenses/>.
 */
public class BlockManager {
    private final List<BlockGroup> groups;
    private boolean active = true;

    public BlockManager() {
        this.groups = new ArrayList<>();
        this.loadGroups();
    }

    public boolean damage(UUID damager, Block block) {
        return this.damage(damager, block, 1);
    }

    public boolean damage(UUID damager, Block block, int damage) {
        if (!this.active)
            return false;

        if (block.getRelative(BlockFace.DOWN).getType() != Material.QUARTZ_BLOCK)
            return false;

        BlockGroup blockGroup = this.getBlockGroup(block);

        return blockGroup != null && blockGroup.damage(damager, block, damage);
    }

    public boolean repair(Block block, int damage) {
        if (!this.active)
            return false;

        if (block.getRelative(BlockFace.DOWN).getType() != Material.QUARTZ_BLOCK)
            return false;

        BlockGroup blockGroup = this.getBlockGroup(block);

        return blockGroup != null && blockGroup.repair(block, damage);
    }

    public BlockGroup getBlockGroup(Block block) {
        for (BlockGroup blockGroup : this.groups)
            if (blockGroup.isThis(block))
                return blockGroup;

        return null;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private void loadGroups() {
        JsonArray grassBlockGroup = new JsonArray();
        grassBlockGroup.add(new JsonPrimitive("GRASS, 0"));
        grassBlockGroup.add(new JsonPrimitive("DIRT, 1"));
        grassBlockGroup.add(new JsonPrimitive("DIRT, 2"));

        JsonArray clayBlockGroup = new JsonArray();
        clayBlockGroup.add(new JsonPrimitive("STAINED_CLAY, 5"));
        clayBlockGroup.add(new JsonPrimitive("STAINED_CLAY, 4"));
        clayBlockGroup.add(new JsonPrimitive("STAINED_CLAY, 14"));

        JsonArray sandBlockGroup = new JsonArray();
        sandBlockGroup.add(new JsonPrimitive("SANDSTONE, 0"));
        sandBlockGroup.add(new JsonPrimitive("SAND, 0"));
        sandBlockGroup.add(new JsonPrimitive("SAND, 1"));

        JsonArray defaultBlockGroups = new JsonArray();
        defaultBlockGroups.add(grassBlockGroup);
        defaultBlockGroups.add(clayBlockGroup);
        defaultBlockGroups.add(sandBlockGroup);

        JsonArray blockGroups = SamaGamesAPI.get().getGameManager().getGameProperties().getMapProperty("blocks", defaultBlockGroups).getAsJsonArray();

        for (JsonElement data : blockGroups)
            this.groups.add(new BlockGroup(data.getAsJsonArray()));
    }
}
