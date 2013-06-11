
/*
 * PROGRAM:
 *   NomadicGameplay - bukkit plugin
 *
 * AUTHOR:
 *   Théophile BASTIAN (a.k.a. Tobast)
 *
 * CONTACT & WEBSITE:
 *   http://tobast.fr/ (contact feature included)
 *   error-report@tobast.fr (error reporting only)
 *
 * SHORT DESCRIPTION:
 *   See first license line.
 *
 * LICENSE:
 *   NomadicGameplay - Bukkit plugin. A new way to play minecraft - just as nomads
 *   Copyright (C) 2013  Théophile BASTIAN
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see http://www.gnu.org/licenses/gpl.txt.
*/

package fr.tobast.bukkit.nomadicgameplay;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;

import fr.tobast.bukkit.nomadicgameplay.NomadicGameplay;

public class EventListener implements Listener {
	private NomadicGameplay plugin;

	EventListener(NomadicGameplay plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	void onBlockPlaceEvent(BlockPlaceEvent event) {
		if(event.isCancelled())
			return;

		if(event.getBlock().getType() == Material.RED_ROSE) { // Resurrecting ?
			Location totemBase = plugin.getBlocksHandler().getAdjacentTotem(
					event.getBlock().getLocation());
			if(totemBase != null &&
					plugin.isSaveWishValid(event.getPlayer().getName()))
			{
				String saveWish = plugin.getSaveWish(event.getPlayer().
						getName());
				plugin.getBlocksHandler().playTotemEffect(totemBase);
				plugin.accomplishSaveWish(event.getPlayer().getName());
				plugin.getServer().broadcastMessage(ChatColor.YELLOW+saveWish+
						" is no longer doomed, and is now free to walk on "+
						"these lands again.");
			}
		}
		
		if(!plugin.inCamp(event.getBlock().getLocation(), 0) &&
			!plugin.getCfgManager().allowedOutOfCamp(
				event.getBlock().getTypeId()))
		{
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED+"You may place this "+
					"block only in your camp!");
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onEntityDeathEvent(EntityDeathEvent event) {
		if(event.getEntity().getType() == EntityType.PLAYER) {
			playerDiedEvent((Player)event.getEntity());
		} else {
			int entityId = event.getEntity().getEntityId();
			Location deathLoc = event.getEntity().getLocation();
			plugin.getInvasionHandler().entityDied(entityId, deathLoc);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerChatEvent(AsyncPlayerChatEvent event) {
		String mess = event.getMessage();
		String repl = mess.replace(plugin.getCfgManager().resurrectIncantation,
				"");
		if(repl.equals(mess)) // nothing replaced
			return;
		else if(repl.contains(" "))
			return;

		plugin.setSaveWish(event.getPlayer().getName(), repl);
	}

	@EventHandler(priority = EventPriority.HIGHEST) // Teleportation
	void onPlayerJoinEvent(PlayerJoinEvent event) {
		plugin.playerConnected();

		if(!plugin.playerCanSpawn(event.getPlayer().getName())) {
			kickDeadPlayer(event.getPlayer());
		}

		if(plugin.getServer().getOnlinePlayers().length == 1) { // Only player
			for(World world : plugin.getServer().getWorlds())
				world.setFullTime(plugin.getLastPauseTime());
		}

		String player = event.getPlayer().getName();
		if(plugin.getMustTeleportPlayer(player) == true) {
			plugin.setMustTeleportPlayer(player, false);
			event.getPlayer().teleport(plugin.getCampTeleportLocation(),
					TeleportCause.PLUGIN);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerQuitEvent(PlayerQuitEvent event) {
		plugin.playerDisconnected();
		if(plugin.getServer().getOnlinePlayers().length == 1) { // No one left
			// Save game's state for further restore
			plugin.setLastPauseTime(event.getPlayer().getLocation().
					getWorld().getFullTime());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerRespawnEvent(PlayerRespawnEvent event) {
		Player pl = event.getPlayer();

		if(!plugin.playerCanSpawn(pl.getName()))
			kickDeadPlayerLater(pl);

		// TP to the camp
		pl.teleport(plugin.getCampTeleportLocation(), TeleportCause.PLUGIN);

		// Add some survivalist stuff to the player's inventory
		pl.getInventory().clear();
		pl.getInventory().addItem(
				new ItemStack(Material.APPLE, 1),
				new ItemStack(Material.POTION, 1, (short)0),
				new ItemStack(Material.FLINT_AND_STEEL, 1),
				new ItemStack(Material.TORCH, 1));
	}

	void playerDiedEvent(Player pl) {
		int respawnDelay = plugin.getCfgManager().respawnDelay;
		plugin.setPlayerCanSpawnTime(pl.getName(), respawnDelay + 
				plugin.realTime());
	}
	void kickDeadPlayer(Player pl) {
		if(pl==null)
			return;
		pl.kickPlayer("You died. You'll be able to respawn in "+
				plugin.humanReadableTime(plugin.playerCanSpawnTime(pl.getName())
				- plugin.realTime()) + ", if nobody saves you before.");
	}

	// Workaround: if we kick a player directly upon its respawn,
	// he will not be respawned properly. This way, we let a few
	// ticks to the server to respawn him properly.
	void kickDeadPlayerLater(Player pl) {
		DelayedKicker kicker = new DelayedKicker(this,pl);
		kicker.runTaskLater(plugin, 5);
	}
	private class DelayedKicker extends BukkitRunnable {
		private EventListener listener;
		private Player pl;
		public DelayedKicker(EventListener listener, Player pl) {
			this.listener=listener;
			this.pl=pl;
		}

		@Override
		public void run() {
			listener.kickDeadPlayer(pl);
		}
	}
}

