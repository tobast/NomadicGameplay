
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

import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.ArrayList;
import java.lang.IndexOutOfBoundsException;

import fr.tobast.bukkit.nomadicgameplay.CommandHandler;

public class NomadicGameplay extends JavaPlugin {
	private CommandHandler cmdHandler = new CommandHandler(this);
	private long lastPauseTime = 0; // TODO restore from saved state
	private HashMap<String, Integer> playersIds = new HashMap<String, Integer>();
	private int nextPlayerId = 0;
	private ArrayList<Boolean> mustTeleportPlayer = new ArrayList<Boolean>();
	private Location campLocation;

// ==== SETTERS/GETTERS ====
	final long getLastPauseTime() {
		return lastPauseTime;
	}
	void setLastPauseTime(final long lastPauseTime) {
		this.lastPauseTime = lastPauseTime;
	}
	
	final int getPlayerId(String name) {
		Integer id = playersIds.get(name);

		if(id == null) { // Insert the new player
			id=nextPlayerId;
			playersIds.put(name, id);
			mustTeleportPlayer.add(false);
			nextPlayerId++;
		}

		return id;
	}

	final boolean getMustTeleportPlayer(int playerId) {
		boolean out;
		try {
			out = mustTeleportPlayer.get(playerId);
		} catch(IndexOutOfBoundsException e) {
			out = true; // Where the f*ck did he came from?
		}
		return out;
	}
	void setMustTeleportPlayer(int playerId, boolean val) {
		if(playerId < 0 || playerId >= mustTeleportPlayer.size())
			return;
		mustTeleportPlayer.set(playerId,val);
	}

	final Location getCampTeleportLocation() {
		Location tpCampLoc = campLocation.clone();
		while(!isValidTeleportLocation(tpCampLoc)) {
			tpCampLoc.add(0,1,0); // ascend.
		}
		return tpCampLoc;
	}

// ==== OVERLOADED BUKKIT API FUNCTIONS ====
	public void onEnable() {
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label,
			String[] args) {
		return cmdHandler.onCommand(sender, command, label, args);
	}

//	public void onDisable {}

// ==== MISC FUNCTIONS ====
	private boolean isValidTeleportLocation(final Location loc) {
		return (loc.getBlock().getType() == Material.AIR &&
		   loc.getBlock().getRelative(BlockFace.UP).getType() == Material.AIR);
	}
}

