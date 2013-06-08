
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

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

import fr.tobast.bukkit.nomadicgameplay.NomadicGameplay;

public class ConfigManager {
	private NomadicGameplay plugin;
	private FileConfiguration conf;

	public String mainWorld;
	public int minTravelDistance;
	public int daysBeforeInvasion;
	public int setCampDist;
	public double setCampProportion;
	public int campRadius;
	public double mobDensity;
	public int nbMobAroundPlayer;
	public int stalkArea;
	public ArrayList<Integer> blocksCampOnly;
	public int respawnDelay;

	ConfigManager(NomadicGameplay plugin) {
		this.plugin = plugin;
		conf = plugin.getConfig();
		setDefaults();
		loadConfig();
	}

	void setDefaults() {
		// map.*
		conf.addDefault("map.mainworld", "world");

		// roam.*
		conf.addDefault("roam.minTravelDistance", 500);
		conf.addDefault("roam.daysBeforeInvasion", 3);

		// camp.*
		conf.addDefault("camp.setCampDist", 5);
		conf.addDefault("camp.setCampProportion", 0.5);
		conf.addDefault("camp.radius", 10);

		ArrayList<Integer> hereOnly = new ArrayList<Integer>();
		hereOnly.add(Material.WORKBENCH.getId());
		hereOnly.add(Material.FURNACE.getId());
		conf.addDefault("camp.blocksCampOnly", (List<Integer>)hereOnly);

		// invasion.*
		conf.addDefault("invasion.mobDensity", 0.05);
		conf.addDefault("invasion.nbMobAroundPlayer", 3);
		conf.addDefault("invasion.stalkArea", 3);

		// death.*
		conf.addDefault("death.respawnDelay", 1800000); // millisec = 30min

		// gamestate.*
		conf.addDefault("gamestate.lastPauseTime", 0);
		conf.addDefault("gamestate.campLocation.x", -1);
		conf.addDefault("gamestate.campLocation.y", -1);
		conf.addDefault("gamestate.campLocation.z", -1);
		conf.addDefault("gamestate.campLocation.world", "world");
		conf.addDefault("gamestate.lastSetCampTime", 0);
		if(!conf.isConfigurationSection("gamestate.players"))
			conf.createSection("gamestate.players"); // Empty section @default

		conf.options().copyDefaults(true);
		plugin.saveConfig();
	}

	void loadConfig() {
		loadOptions();
		loadGamestate();
	}

	void reloadConfig() {
		plugin.reloadConfig();
		conf = plugin.getConfig();
		loadOptions();
	}

	void loadGamestate() {
		plugin.setLastPauseTime(conf.getLong("gamestate.lastPauseTime"));
		Location campLoc = new Location(
				plugin.getServer().getWorld(
					conf.getString("gamestate.campLocation.world")),
				conf.getInt("gamestate.campLocation.x"),
				conf.getInt("gamestate.campLocation.y"),
				conf.getInt("gamestate.campLocation.z"));
		plugin.setCampInit(campLoc, conf.getInt("gamestate.lastSetCampTime"));

		ConfigurationSection playersSect = 
			conf.getConfigurationSection("gamestate.players");
		for(String player : playersSect.getKeys(false)) {
			plugin.setMustTeleportPlayer(player, playersSect.getBoolean(
						player+".mustTeleport"));
			plugin.setPlayerCanSpawnTime(player, playersSect.getLong(
						player+".respawnTime", 0));
		}
	}

	void loadOptions() {
		mainWorld = conf.getString("map.mainworld");

		minTravelDistance  = conf.getInt("roam.minTravelDistance");
		daysBeforeInvasion = conf.getInt("roam.daysBeforeInvasion");

		setCampDist = conf.getInt("camp.setCampDist");
		setCampProportion = conf.getDouble("camp.setCampProportion");
		campRadius = conf.getInt("camp.radius");
		blocksCampOnly = new ArrayList<Integer>(conf.getIntegerList(
					"camp.blocksCampOnly"));

		mobDensity = conf.getDouble("invasion.mobDensity");
		nbMobAroundPlayer = conf.getInt("invasion.nbMobAroundPlayer");
		stalkArea = conf.getInt("invasion.stalkArea");

		respawnDelay = conf.getInt("death.respawnDelay");
	}

	void saveState() {
		reloadConfig();
		ConfigurationSection gsConf = conf.getConfigurationSection("gamestate");
		
		gsConf.set("lastPauseTime", plugin.getLastPauseTime());
		long setcamptime = plugin.getLastSetCampTime();
		gsConf.set("lastSetCampTime", setcamptime);
		
		Location campLoc = plugin.getCampLocation();
		gsConf.set("campLocation.x", campLoc.getBlockX());
		gsConf.set("campLocation.y", campLoc.getBlockY());
		gsConf.set("campLocation.z", campLoc.getBlockZ());
		gsConf.set("campLocation.world", campLoc.getWorld().getName());

		for(String player : plugin.getPlayersNames()) {
			ConfigurationSection plSect = gsConf.createSection(
					"players."+player);
			plSect.set("mustTeleport",
					plugin.getMustTeleportPlayer(player));
			plSect.set("respawnTime",
					plugin.playerCanSpawnTime(player));
		}

		plugin.saveConfig();
	}

	public boolean allowedOutOfCamp(int typeId) {
		for(int forbiddenType : blocksCampOnly) {
			if(typeId == forbiddenType)
				return false;
		}
		return true;
	}
}

