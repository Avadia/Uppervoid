package com.geekpower14.uppervoid.stuff.grenada;

import com.geekpower14.uppervoid.Uppervoid;
import com.geekpower14.uppervoid.arena.Arena;
import com.geekpower14.uppervoid.arena.ArenaPlayer;
import com.geekpower14.uppervoid.arena.ArenaStatisticsHelper;
import com.geekpower14.uppervoid.stuff.Stuff;
import net.samagames.api.SamaGamesAPI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
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
public class Grenada extends Stuff {
    public Grenada(Uppervoid plugin, int id, int uses) {
        super(plugin, id, new ItemStack(Material.CLAY_BALL, 1), ChatColor.RED + "Grenada", "Greeenaaadaa!", 2, 10L, false);
        this.uses = uses;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void use(ArenaPlayer arenaPlayer) {
        Player player = arenaPlayer.getPlayerIfOnline();
        ItemStack stack = player.getInventory().getItemInHand();

        if (stack == null || !this.canUse(true) || !this.plugin.getArena().getBlockManager().isActive() || stack.getAmount() <= 0)
            return;

        this.setReloading();
        this.setUses(this.getUses() - 1);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SNOW_STEP, 3F, 2.0F);

        Item tnt = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.CLAY_BALL));
        tnt.setMetadata("uv-owner", new FixedMetadataValue(this.plugin, arenaPlayer.getUUID().toString()));
        tnt.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(1.5));
        tnt.setPickupDelay(Integer.MAX_VALUE);

        new GrenadaExplosionTask(this.plugin, this, tnt).runTaskTimerAsynchronously(this.plugin, 0L, 5L);

        stack.setAmount(stack.getAmount() - 1);
        player.getInventory().setItemInHand(stack);

        arenaPlayer.giveStuff();

        ((ArenaStatisticsHelper) SamaGamesAPI.get().getGameManager().getGameStatisticsHelper()).increaseGrenades(arenaPlayer.getUUID());
    }

    @Override
    public void onItemTouchGround(Arena arena, Item item) {
        Location center = item.getLocation();
        UUID launcher = UUID.fromString(item.getMetadata("uv-owner").get(0).asString());
        Block real = center.add(0, -0.5, 0).getBlock();
        World world = center.getWorld();

        ArrayList<Block> levelOne = new ArrayList<>();
        ArrayList<Block> levelTwo = new ArrayList<>();
        ArrayList<Block> levelThree = new ArrayList<>();

        String[] schema = new String[]{
                "00011111000",
                "00111111100",
                "01112221110",
                "11122222111",
                "11222322211",
                "11223332211",
                "11222322211",
                "11122222111",
                "01112221110",
                "00111111100",
                "00011111000"
        };

        int middle = (schema.length - 1) / 2;

        int refX = real.getX() - middle;
        int refY = real.getY();
        int refZ = real.getZ() - middle;

        int incrX;
        int incrZ = 0;

        for (String str : schema) {
            incrX = 0;

            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);

                if (c == '1')
                    levelOne.add(world.getBlockAt(refX + incrX, refY, refZ + incrZ));
                else if (c == '2')
                    levelTwo.add(world.getBlockAt(refX + incrX, refY, refZ + incrZ));
                else if (c == '3')
                    levelThree.add(world.getBlockAt(refX + incrX, refY, refZ + incrZ));

                incrX++;
            }

            incrZ++;
        }

        for (Block block : levelOne)
            arena.getBlockManager().damage(launcher, block, 1);

        for (Block block : levelTwo)
            arena.getBlockManager().damage(launcher, block, 2);

        for (Block block : levelThree)
            arena.getBlockManager().damage(launcher, block, 3);

        center.getWorld().createExplosion(center.getX(), center.getY(), center.getZ(), 2.5F, false, false);
    }

    @Override
    public ItemStack getItem(ItemStack base) {
        return base;
    }
}
