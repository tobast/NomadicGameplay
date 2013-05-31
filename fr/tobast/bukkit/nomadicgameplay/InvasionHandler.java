
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;

import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import fr.tobast.bukkit.nomadicgameplay.NomadicGameplay;

public class InvasionHandler {
	private NomadicGameplay plugin;
	private Random randGen;
	private TreeSet<Integer> invasionEntities = new TreeSet<Integer>();
	private HashMap<String,Boolean> isPlayerStalked =
		new	HashMap<String,Boolean>();
	private HashMap<Integer, String> mobToStalkedPlayer =
		new HashMap<Integer, String>();
	private ArrayList<LivingEntity> toKillEntities = new ArrayList<LivingEntity>();
	private boolean isInvading = false;
	private WeatherRegulator weatherReg;

	InvasionHandler(NomadicGameplay plugin) {
		this.plugin = plugin;
		weatherReg = new WeatherRegulator(plugin.getCampLocation().getWorld());
	}
	
	void triggerInvasion() {
		Location campLoc = plugin.getCampLocation();

		if(!campLoc.getChunk().isLoaded()) { // nobody's here
			if(isInvading)
				endInvasion();
			return;
		}

		if(isInvading)
			return;

		isInvading = true;

		weatherReg.runTaskTimer(plugin, 0, 9000);

		int campRadius = plugin.getCfgManager().campRadius;
		int nbMobs = (int)Math.ceil(plugin.getCfgManager().mobDensity *
				((double)(2*campRadius + 1)));

		for(int curMob = 0; curMob < nbMobs; curMob++) {
			invasionEntities.add(spawnMobAround(campLoc, campRadius));
		}

		int nbStalkers = plugin.getCfgManager().nbMobAroundPlayer;
		for(Player player : plugin.getServer().getOnlinePlayers()) {
			if(inCamp(player.getLocation())) {
				// per-player spawn
				for(int curMob=0; curMob < nbStalkers; curMob++) {

					mobToStalkedPlayer.put(spawnMobAround(player.getLocation(),
							plugin.getCfgManager().stalkArea), player.getName());
				}
				isPlayerStalked.put(player.getName(), true);
			}
			else {
				isPlayerStalked.put(player.getName(), false);
			}
		}
	}

	void entityDied(int id, Location deathLoc) {
		if(!isInvading)
			return;

		if(invasionEntities.contains(id)) {
			invasionEntities.remove(id);
			invasionEntities.add(spawnMobAround(deathLoc, 4));
		}
		else {
			String stalkedPl = mobToStalkedPlayer.get(id);
			if(stalkedPl != null) {
				Location plLoc =
					plugin.getServer().getPlayerExact(stalkedPl).getLocation();

				if(inCamp(plLoc)) {
					mobToStalkedPlayer.put(spawnMobAround(plLoc,
						plugin.getCfgManager().stalkArea), stalkedPl);
				}
				else {
					mobToStalkedPlayer.remove(stalkedPl);
				}
			}
		}
	}

	void endInvasion() {
		isInvading = false;
		weatherReg.cancel();
		plugin.getCampLocation().getWorld().setStorm(false);

		for(LivingEntity entity : toKillEntities) {
			if(entity == null) //unloaded
				continue;
			entity.setHealth(1);
			entity.setFireTicks(10); // Kill him (visually) properly
		}
		toKillEntities.clear();
		invasionEntities.clear();
		isPlayerStalked.clear();
		mobToStalkedPlayer.clear();
	}

	private int spawnMobAround(Location loc, int zoneRadius) {
			int locX = randGen.nextInt(2*zoneRadius+1) - zoneRadius;
			int locZ = randGen.nextInt(2*zoneRadius+1) - zoneRadius;
			Location spawnLoc = new Location(loc.getWorld(), locX, loc.getY(), locZ);
			spawnLoc = nearestAir(spawnLoc);

			LivingEntity mob = ((LivingEntity)loc.getWorld().spawnEntity(
						spawnLoc, EntityType.ZOMBIE));
			toKillEntities.add(mob);
			return mob.getEntityId();
	}

	private Location nearestAir(final Location loc) {
		int yOrig=loc.getBlockY();
		int yDiff=0;
		
		Location downLoc = loc.clone(), upLoc = loc.clone();

		while(yOrig-yDiff > 0 || yOrig+yDiff <= 256) {
			if(yOrig-yDiff > 0 && isSpawnable(downLoc)) {
				return downLoc;
			}
			else if(yOrig+yDiff <= 256 && isSpawnable(upLoc)) {
				return upLoc;
			}

			yDiff++;
			downLoc.add(0,-1,0);
			upLoc.add(0,1,0);
		}

		return loc;
	}

	private boolean isSpawnable(final Location loc) {
		return (loc.getBlock().getType() == Material.AIR) &&
			(loc.getBlock().getRelative(BlockFace.UP).getType() == Material.AIR);
	}

	private boolean inCamp(final Location loc) {
		Location campLoc = plugin.getCampLocation();
		int campRad = plugin.getCfgManager().campRadius;

		return (loc.getBlockX() >= campLoc.getBlockX() - campRad) &&
			(loc.getBlockX() <= campLoc.getBlockX() + campRad) &&
			(loc.getBlockZ() >= campLoc.getBlockZ() - campRad) &&
			(loc.getBlockZ() <= campLoc.getBlockZ() + campRad);
	}

	private class WeatherRegulator extends BukkitRunnable {
		private World handledWorld;

		WeatherRegulator(World handledWorld) {
			this.handledWorld = handledWorld;
		}

		@Override
		public void run() {
			handledWorld.setStorm(true);
			handledWorld.setWeatherDuration(10000);
		}
	}
}

