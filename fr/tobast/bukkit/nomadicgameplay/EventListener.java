
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

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.World;

import fr.tobast.bukkit.nomadicgameplay.NomadicGameplay;

public class EventListener implements Listener {
	private NomadicGameplay plugin;

	EventListener(NomadicGameplay plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST) // Teleportation
	void onPlayerJoinEvent(PlayerJoinEvent event) {
		if(plugin.getServer().getOnlinePlayers().length == 1) { // Only player
			for(World world : plugin.getServer().getWorlds())
				world.setFullTime(plugin.getLastPauseTime());
		}

		int playerId = plugin.getPlayerId(event.getPlayer().getName());
		if(plugin.getMustTeleportPlayer(playerId) == true) {
			plugin.setMustTeleportPlayer(playerId, false);
			event.getPlayer().teleport(plugin.getCampTeleportLocation(),
					TeleportCause.PLUGIN);
		}
		
		event.setJoinMessage(event.getPlayer().getDisplayName()+" joined the "+
			"caravan.");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onPlayerQuitEvent(PlayerQuitEvent event) {
		event.setQuitMessage(event.getPlayer().getDisplayName()+" left the "+
			"caravan.");

		if(plugin.getServer().getOnlinePlayers().length == 0) { // No one left
			// Save game's state for further restore
			plugin.setLastPauseTime(event.getPlayer().getLocation().getWorld().getFullTime());
		}
	}
}

