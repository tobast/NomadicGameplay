
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

import fr.tobast.bukkit.nomadicgameplay.NomadicGameplay;

public class CommandHandler {
	private NomadicGameplay plugin;

	public CommandHandler(NomadicGameplay plugin) {
		this.plugin = plugin;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(label.equals("setcamp")) {
			if(onSetCamp(sender))
				return true;
		}

		if(label.equals("nomadic")) {
			if(onNomadicCmd(sender, args))
				return true;
		}
		return false;
	}

	private boolean onSetCamp(CommandSender sender) {
		if(!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to perform this command!");
			return false;
		}

		Player pSender = (Player)sender;
		Location pLoc = pSender.getLocation();

		// On a solid block
		Material supportMaterial = pLoc.getBlock().getRelative(BlockFace.DOWN).getType();
		if(supportMaterial == Material.AIR || supportMaterial == Material.WATER) { // Non-solid
			sender.sendMessage("You must be standing on a solid block to settle a camp.");
			return true;
		}

		// At least n blocks away from the prev. camp
		if(plugin.distToCamp(pLoc) < plugin.getCfgManager().minTravelDistance) {
			sender.sendMessage("You're only "+plugin.distToCamp(pLoc)+
					" away from your former camp. You must be at least "+
					plugin.getCfgManager().minTravelDistance+" meters "+
					"away to settle a new camp! The action was cancelled.");
			return true; // Player does not want help
		}

		// Right proportion of players around the camp setter
		int nbNear=0;
		for(Player pl : plugin.getServer().getOnlinePlayers()) {
			if(manhattan_2d(pl.getLocation(), pLoc) <= plugin.getCfgManager().setCampDist) {
				nbNear++;
			}
		}
		if(((double)nbNear) / ((double)plugin.getServer().getOnlinePlayers().length) <
				plugin.getCfgManager().setCampProportion) {
			sender.sendMessage("At least "+plugin.getCfgManager().setCampProportion *100+
					"% of the players shall be in a "+plugin.getCfgManager().setCampDist+
					" range from you to set camp. The action was cancelled.");
			return true;
		}

		// All the required conditions are met. Let's set camp here!
		plugin.setCamp(pLoc);

		// also update mustTeleport status of offline players to true,
		// online players to false.
		for(String plName : plugin.getPlayersNames()) {
			plugin.setMustTeleportPlayer(plName, !isOnline(plName));
		}

		plugin.getServer().broadcastMessage(pSender.getDisplayName()+" settled a new camp!");
		return true;
	}

	private boolean onNomadicCmd(CommandSender sender, String[] args) {
		if(args.length < 1)
			return false;

		if(args[0].equals("reload")) {
			plugin.getCfgManager().reloadConfig();
			sender.sendMessage("Nomadic reload complete.");
			return true;
		} else if(args[0].equals("save")) {
			plugin.getCfgManager().saveState();
			sender.sendMessage("Nomadic save complete.");
			return true;
		}

		return false;
	}

	private final int manhattan_2d(final Location l1, final Location l2) {
		return Math.abs(l1.getBlockX() - l2.getBlockX()) +
			Math.abs(l1.getBlockZ() - l2.getBlockZ());
	}

	private boolean isOnline(String player) {
		return (plugin.getServer().getPlayerExact(player) != null);
	}
}

