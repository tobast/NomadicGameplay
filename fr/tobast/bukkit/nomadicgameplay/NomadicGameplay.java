
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
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;

import java.util.Set;
import java.util.TreeSet;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.IndexOutOfBoundsException;

import fr.tobast.bukkit.nomadicgameplay.BlocksHandler;
import fr.tobast.bukkit.nomadicgameplay.CommandHandler;
import fr.tobast.bukkit.nomadicgameplay.ConfigManager;
import fr.tobast.bukkit.nomadicgameplay.EventListener;
import fr.tobast.bukkit.nomadicgameplay.InvasionHandler;

public class NomadicGameplay extends JavaPlugin {
	private BlocksHandler blocksHandler = new BlocksHandler(this);
	private CommandHandler cmdHandler = new CommandHandler(this);
	private ConfigManager cfgManager;
	private InvasionHandler invasionHandler;
	private long lastPauseTime = 0; 
	private HashMap<String,Boolean> mustTeleportPlayer =
		new HashMap<String,Boolean>();
	private HashMap<String,Long> canSpawnAfter =
		new HashMap<String,Long>();
	private World mainWorld;
	private Location campLocation;
	private long lastSetCampTime = 0;
	private int nbPlayers = 0;

// ==== SETTERS/GETTERS ====
	final BlocksHandler getBlocksHandler() {
		return blocksHandler;
	}
	final ConfigManager getCfgManager() {
		return cfgManager;
	}

	final InvasionHandler getInvasionHandler() {
		return invasionHandler;
	}

	final boolean isServerEmpty() {
		return nbPlayers == 0;
	}
	void playerConnected() {
		nbPlayers++;
	}
	void playerDisconnected() {
		nbPlayers--;
	}

	final long getLastPauseTime() {
		return lastPauseTime;
	}
	void setLastPauseTime(final long lastPauseTime) {
		this.lastPauseTime = lastPauseTime;
	}

	final TreeSet<String> getPlayersNames() {
		TreeSet<String> outSet =
			new TreeSet<String>(mustTeleportPlayer.keySet());
		for(String str : canSpawnAfter.keySet())
			outSet.add(str);
		return outSet;
	}

	final boolean getMustTeleportPlayer(String player) {
		Boolean out = mustTeleportPlayer.get(player);

		if(out == null) { // new player, generate.
			mustTeleportPlayer.put(player,true);
			return true;
		}
		return out;
	}
	void setMustTeleportPlayer(String player, boolean val) {
		mustTeleportPlayer.put(player,val);
	}

	final long playerCanSpawnTime(final String player) {
		Long out = canSpawnAfter.get(player);
		if(out == null) {
			canSpawnAfter.put(player, 0l);
			return 0l;
		}
		return out;
	}
	final boolean playerCanSpawn(final String player) {
		return (playerCanSpawnTime(player) < realTime());
	}
	void setPlayerCanSpawnTime(final String player, final long date) {
		canSpawnAfter.put(player, date);
	}

	final Location getCampLocation() {
		return campLocation;
	}
	final Location getCampTeleportLocation() {
		Location tpCampLoc = campLocation.clone();
		while(!isValidTeleportLocation(tpCampLoc)) {
			tpCampLoc.add(0,1,0); // ascend.
		}
		return tpCampLoc;
	}
	public boolean inCamp(final Location loc, final int posAdd) {
		Location campLoc = getCampLocation();
		int campRad = getCfgManager().campRadius;

		return (loc.getBlockX() >= campLoc.getBlockX() - campRad - posAdd) &&
			(loc.getBlockX() <= campLoc.getBlockX() + campRad + posAdd) &&
			(loc.getBlockZ() >= campLoc.getBlockZ() - campRad - posAdd) &&
			(loc.getBlockZ() <= campLoc.getBlockZ() + campRad + posAdd);
	}


	final int distToCamp(Location loc) { // Manhattan shall be enough
		return Math.abs(campLocation.getBlockX() - loc.getBlockX())
			+ Math.abs(campLocation.getBlockZ() - loc.getBlockZ());
	}
	void setCamp(Location camp) {
		// Here we suppose the tests have been made before.
		// Indeed, an admin command shall be able to set camp everywhere.
		campLocation = camp;
		lastSetCampTime = mainWorld.getFullTime();
	}
	void setCampInit(Location camp, long lastSetCampTime) {
		campLocation = camp;
		this.lastSetCampTime = lastSetCampTime;
	}

	final long getLastSetCampTime() {
		return lastSetCampTime;
	}

	final World getMainWorld() {
		return mainWorld;
	}

	final long realTime() {
		java.util.Date date = new java.util.Date();
		return (new java.sql.Timestamp(date.getTime())).getTime();
	}

	private final static int[] TIME_DIVISORS = new int[]{60, 60, 24, 1};
	private final static String[] TIME_INDICATORS =
		new String[]{"s", "min", "h", "d"};
	final String humanReadableTime(final long dateOrig) {
		long date = dateOrig;
		date /= 1000; // Convert ms to s

		ArrayList<String> timeparts = new ArrayList<String>();
		for(int unit=0; unit < 4; unit++) {
			if(date == 0)
				break;

			timeparts.add(date % TIME_DIVISORS[unit]+TIME_INDICATORS[unit]);
			date /= TIME_DIVISORS[unit];
		}
		String out = new String();

		for(int pos = timeparts.size()-1; pos >= 0; pos--) {
			out += timeparts.get(pos);
			if(pos != 0)
				out+=' ';
		}
		return out;
	}

// ==== OVERLOADED BUKKIT API FUNCTIONS ====
	public void onEnable() {
		cfgManager = new ConfigManager(this); // loads config
		initVars();
		getServer().getPluginManager().registerEvents(new EventListener(this),
				this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args)
	{
		return cmdHandler.onCommand(sender, command, label, args);
	}

	public void onDisable() {
		invasionHandler.endInvasion(); // Kill assoc. mobs
		cfgManager.saveState();
	}

// ==== MISC FUNCTIONS ====
	private void initVars() {
		mainWorld = getServer().getWorld(cfgManager.mainWorld);
		if(campLocation.getY() < 0) // Default value
			campLocation = mainWorld.getSpawnLocation();

		invasionHandler = new InvasionHandler(this);

		// force MustTeleport generation
		for(Player pl : getServer().getOnlinePlayers()) {
			nbPlayers++;
			getMustTeleportPlayer(pl.getName());
		}
	}

	private boolean isValidTeleportLocation(final Location loc) {
		return (loc.getBlock().getType() == Material.AIR &&
		   loc.getBlock().getRelative(BlockFace.UP).getType() == Material.AIR);
	}
}

