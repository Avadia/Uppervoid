package com.geekpower14.uppervoid.arena;

import com.geekpower14.uppervoid.Uppervoid;
import com.geekpower14.uppervoid.block.BlockManager;
import com.geekpower14.uppervoid.powerups.*;
import com.geekpower14.uppervoid.stuff.ItemChecker;
import com.geekpower14.uppervoid.stuff.ItemManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import net.samagames.api.SamaGamesAPI;
import net.samagames.api.games.Game;
import net.samagames.api.games.IGameProperties;
import net.samagames.api.games.Status;
import net.samagames.tools.Area;
import net.samagames.tools.LocationUtils;
import net.samagames.tools.PlayerUtils;
import net.samagames.tools.powerups.PowerupManager;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
public class Arena extends Game<ArenaPlayer> {
    private final Uppervoid plugin;
    private final List<Location> spawns;
    private final List<UUID> builders;
    private final Location lobby;
    private final BlockManager blockManager;
    private final ItemManager itemManager;
    private final ItemChecker itemChecker;
    private final PowerupManager powerupManager;

    private BukkitTask gameTime;
    private BukkitTask antiAFK;
    private Player second;
    private Player third;

    public Arena(Uppervoid plugin) {
        super("uppervoid", "Uppervoid", "Restez au dessus du vide", ArenaPlayer.class, new UUID[]{
                UUID.fromString("ad345a5e-5ae3-45bf-aba4-94f4102f37c0"),
                UUID.fromString("29b2b527-1b59-45df-b7b0-d5ab20d8731a")
        });

        this.plugin = plugin;
        this.spawns = new ArrayList<>();
        this.builders = new ArrayList<>();

        IGameProperties properties = SamaGamesAPI.get().getGameManager().getGameProperties();

        JsonArray spawnDefault = new JsonArray();
        spawnDefault.add(new JsonPrimitive("world, 0, 0, 0, 0, 0"));

        JsonArray spawnsJson = properties.getMapProperty("spawns", spawnDefault).getAsJsonArray();

        for (int i = 0; i < spawnsJson.size(); i++)
            this.spawns.add(LocationUtils.str2loc(spawnsJson.get(i).getAsString()));

        World.Environment dimension = World.Environment.valueOf(properties.getMapProperty("dimension", new JsonPrimitive(World.Environment.NORMAL.toString())).getAsString());
        this.lobby = LocationUtils.str2loc(properties.getMapProperty("waiting-lobby", new JsonPrimitive("world, 0, 0, 0, 0, 0")).getAsString());

        this.blockManager = new BlockManager();
        this.blockManager.setActive(false);

        this.itemManager = new ItemManager(plugin);

        this.itemChecker = new ItemChecker(plugin);
        this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, this.itemChecker, 1L, 1L);

        this.powerupManager = new PowerupManager(plugin);
        this.powerupManager.setInverseFrequency(200);
        this.powerupManager.registerPowerup(new BlindnessPowerup(plugin, this));
        this.powerupManager.registerPowerup(new SwapPowerup(plugin, this));
        this.powerupManager.registerPowerup(new SpeedPowerup(plugin, this));
        this.powerupManager.registerPowerup(new JumpPowerup(plugin, this));
        this.powerupManager.registerPowerup(new SnowballPowerup(plugin, this));
        this.powerupManager.registerPowerup(new InvisibilityPowerup(plugin, this));
        this.powerupManager.registerPowerup(new NauseaPowerup(plugin, this));
        this.powerupManager.registerPowerup(new SlownessPowerup(plugin, this));
        this.powerupManager.registerPowerup(new RepairPowerup(plugin, this));

        if (properties.getMapProperties().has("powerups-spawns"))
            this.powerupManager.registerArea(Area.str2area(properties.getMapProperties().get("powerups-spawns").getAsString()));

        SamaGamesAPI.get().getSkyFactory().setDimension(this.plugin.getServer().getWorld("world"), dimension);
    }

    @Override
    public void handleLogin(Player player) {
        super.handleLogin(player);

        player.teleport(this.lobby);
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().setItem(8, this.coherenceMachine.getLeaveItem());
        player.getInventory().setHeldItemSlot(0);
    }

    @Override
    public void handleLogout(Player player) {
        ArenaPlayer arenaPlayer = this.getPlayer(player.getUniqueId());
        super.handleLogout(player);

        if (this.getStatus() == Status.IN_GAME && arenaPlayer != null && !arenaPlayer.isSpectator()) {
            if (this.getInGamePlayers().size() == 1)
                this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, this::win, 1L);
            else if (this.getConnectedPlayers() <= 0)
                this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, this::handleGameEnd, 1L);
            else
                this.lose(player);
        }
    }

    @Override
    public void startGame() {
        super.startGame();

        for (ArenaPlayer arenaPlayer : this.gamePlayers.values()) {
            Player player = arenaPlayer.getPlayerIfOnline();
            player.getInventory().clear();

            player.setWalkSpeed(0.25F);
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 0, false, false));

            arenaPlayer.setScoreboard();
            arenaPlayer.giveStuff();
            arenaPlayer.setReloading(6 * 20L);

            this.teleportRandomSpawn(player);
        }

        int time = 4;

        this.blockManager.setActive(false);
        this.powerupManager.start();

        this.getInGamePlayers().values().forEach(ArenaPlayer::updateLastChangeBlock);
        this.antiAFK = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> this.getInGamePlayers().values().forEach(ArenaPlayer::checkAntiAFK), time * 20L, 20L);
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.blockManager.setActive(true), time * 20L);

        this.gameTime = this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(this.plugin, new Runnable() {
            private int time = 0;

            @Override
            public void run() {
                this.time++;
                updateScoreboards(this.formatTime(this.time));
            }

            public String formatTime(int time) {
                int mins = time / 60;
                int secs = time - mins * 60;

                String secsSTR = (secs < 10) ? "0" + secs : Integer.toString(secs);

                return mins + ":" + secsSTR;
            }
        }, 0L, 20L);
    }

    @Override
    public void handleGameEnd() {
        this.blockManager.setActive(false);
        this.gameTime.cancel();
        this.antiAFK.cancel();

        super.handleGameEnd();
    }

    public void win() {
        this.setStatus(Status.FINISHED);

        this.blockManager.setActive(false);
        this.powerupManager.stop();

        Player player = this.getWinner();

        if (player == null) {
            this.handleGameEnd();
            return;
        }

        this.handleWinner(player.getUniqueId());
        this.effectsOnWinner(player);

        try {
            this.coherenceMachine.getTemplateManager().getPlayerLeaderboardWinTemplate().execute(player, this.second, this.third);
        } catch (Exception ignored) {
        }

        this.addCoins(player, 30, "Victoire !");

        this.getPlayer(player.getUniqueId()).updateScoreboard();

        this.handleGameEnd();
    }

    public void lose(Player player) {
        this.setSpectator(player);
        this.teleportRandomSpawn(player);

        if (this.builders.contains(player.getUniqueId()))
            this.removeBuilder(player);

        player.getInventory().setItem(8, this.coherenceMachine.getLeaveItem());
        player.getInventory().setHeldItemSlot(0);

        if (this.getStatus().equals(Status.IN_GAME)) {
            int left = this.getInGamePlayers().size();
            this.coherenceMachine.getMessageManager().writeCustomMessage(PlayerUtils.getColoredFormattedPlayerName(player) + ChatColor.YELLOW + " a perdu ! (" + left + " joueur" + ((left > 1) ? "s" : "") + " restant" + ((left > 1) ? "s" : "") + ")", true);
        }

        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () ->
        {
            for (ArenaPlayer arenaPlayer : this.getInGamePlayers().values()) {
                if (arenaPlayer.getUUID().equals(player.getUniqueId()))
                    continue;

                this.addCoins(arenaPlayer.getPlayerIfOnline(), 3, "Mort de " + PlayerUtils.getColoredFormattedPlayerName(player));
            }
        });

        if (this.getInGamePlayers().isEmpty() && getStatus().equals(Status.IN_GAME)) {
            this.win();
        } else if (this.getInGamePlayers().size() == 1 && getStatus().equals(Status.IN_GAME)) {
            this.second = player;
            this.win();
        } else if (this.getInGamePlayers().size() == 2 && getStatus().equals(Status.IN_GAME)) {
            this.third = player;
        }

        this.updateScoreboards();
    }

    public void updateScoreboards() {
        this.gamePlayers.values().forEach(ArenaPlayer::updateScoreboard);
    }

    public void updateScoreboards(String time) {
        for (ArenaPlayer arena : this.gamePlayers.values())
            arena.setScoreboardTime(time);
    }

    public void addBuilder(Player player) {
        this.builders.add(player.getUniqueId());
    }

    public void removeBuilder(Player player) {
        this.builders.remove(player.getUniqueId());
    }

    public void teleportRandomSpawn(Player p) {
        p.teleport(this.spawns.get(new Random().nextInt(this.spawns.size())));
    }

    public Player getWinner() {
        if (this.getInGamePlayers().isEmpty())
            return null;
        return this.getInGamePlayers().values().iterator().next().getPlayerIfOnline();
    }

    public Uppervoid getPlugin() {
        return this.plugin;
    }

    public BlockManager getBlockManager() {
        return this.blockManager;
    }

    public ItemManager getItemManager() {
        return this.itemManager;
    }

    public ItemChecker getItemChecker() {
        return this.itemChecker;
    }

    public Location getLobby() {
        return this.lobby;
    }

    public boolean isBuilder(UUID uuid) {
        return this.builders.contains(uuid);
    }
}
