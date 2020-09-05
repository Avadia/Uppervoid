package com.geekpower14.uppervoid.stuff;

import com.geekpower14.uppervoid.Uppervoid;
import com.geekpower14.uppervoid.arena.Arena;
import com.geekpower14.uppervoid.arena.ArenaPlayer;
import net.samagames.tools.GlowEffect;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

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
public abstract class Stuff implements Cloneable {
    protected final Uppervoid plugin;
    protected final int id;
    protected final ItemStack stack;
    protected final String display;
    protected final String lore;
    protected final long reloadTime;
    protected final boolean isGlow;

    protected ArenaPlayer arenaPlayer;
    protected int uses;
    protected boolean reloading = false;

    public Stuff(Uppervoid plugin, int id, ItemStack stack, String display, String lore, int uses, long reloadTime, boolean glow) {
        this.plugin = plugin;
        this.id = id;
        this.stack = stack;
        this.display = ChatColor.GOLD + display + ChatColor.GRAY + " (Clique-Droit)";
        this.lore = lore;
        this.uses = uses;
        this.reloadTime = reloadTime;
        this.isGlow = glow;
    }

    public abstract void use(ArenaPlayer arenaPlayer);

    public abstract ItemStack getItem(ItemStack base);

    public void onItemTouchGround(Arena arena, Item item) {
    }

    public void setOwner(ArenaPlayer arenaPlayer) {
        this.arenaPlayer = arenaPlayer;
    }

    public void setReloading() {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new ReloadingTask(this.plugin, this.arenaPlayer, this));
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }

    public ItemStack getItem() {
        ItemStack modifiedStack = this.stack.clone();
        modifiedStack.setAmount(this.uses);

        ItemMeta meta = modifiedStack.getItemMeta();

        meta.setDisplayName(this.display);
        meta.setLore(Collections.singletonList(ChatColor.GRAY + this.lore));

        modifiedStack.setItemMeta(meta);

        if (this.isGlow)
            GlowEffect.addGlow(modifiedStack);

        return this.getItem(modifiedStack);
    }

    public int getId() {
        return this.id;
    }

    public int getUses() {
        return this.uses;
    }

    public void setUses(int uses) {
        this.uses = uses;
    }

    public long getReloadTime() {
        return this.reloadTime;
    }

    public boolean canUse(boolean playerDependant) {
        if (playerDependant)
            return this.arenaPlayer != null && !(this.arenaPlayer.isSpectator() || this.arenaPlayer.isReloading() || this.reloading);
        else
            return this.arenaPlayer != null && !this.reloading;
    }

    @SuppressWarnings("deprecation")
    public boolean isActiveItem() {
        if (this.arenaPlayer == null)
            return false;

        ItemStack itemStack = this.getItem();
        return itemStack != null && itemStack.isSimilar(this.arenaPlayer.getPlayerIfOnline().getItemInHand());
    }

    public Stuff clone() {
        try {
            return (Stuff) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
