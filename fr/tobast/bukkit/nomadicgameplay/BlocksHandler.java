
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

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;

import fr.tobast.bukkit.nomadicgameplay.NomadicGameplay;

public class BlocksHandler {
	NomadicGameplay plugin;
	
	BlocksHandler(NomadicGameplay plugin) {
		this.plugin = plugin;
	}

	void destroyUnplaceableBlocks(final Location center, final int radius) {
		Location beginLoc = center.clone().add(-radius, 0, -radius);

		final int maxX = beginLoc.getBlockX() + 2*radius+1;
		final int maxZ = beginLoc.getBlockZ() + 2*radius+1;
		for(int y=1; y < 256; y++) {
			for(int x=beginLoc.getBlockX(); x < maxX; x++) {
				for(int z=beginLoc.getBlockZ(); z < maxZ; z++){
					Location cLoc = beginLoc.clone();
					cLoc.setX(x);
					cLoc.setY(y);
					cLoc.setZ(z);

					if(!plugin.getCfgManager().allowedOutOfCamp(
								cLoc.getBlock().getTypeId()))
					{
						cLoc.getBlock().breakNaturally();
					}
				}
			}
		}
	}

	private final static int NB_FACES = 4;
	private final static BlockFace[] FACES = { BlockFace.NORTH, BlockFace.EAST,
		BlockFace.SOUTH, BlockFace.WEST };
	boolean isTotemBase(final Location base) {
		Location locIt = base.clone();

		for(int asc=0; asc < 3; asc++) { // iterate through logs
			if(locIt.getBlock().getType() != Material.LOG)
				return false;
			locIt.add(0,1,0); //ascend
		}
		if(locIt.getBlock().getType() != Material.PUMPKIN) // head
			return false;

		locIt.add(0,-1,0);
		Block armsHolder = locIt.getBlock();
		for(int face=0; face < NB_FACES/2; face++) { // /2 because symetry
			if(armsHolder.getRelative(FACES[face]).getType() ==
					Material.REDSTONE_TORCH_ON &&
					armsHolder.getRelative(FACES[face].getOppositeFace()).
					getType() == Material.REDSTONE_TORCH_ON)
				return true;
		}
		return false;
	}

	boolean hasAdjacentTotem(final Location loc) {
		return getAdjacentTotem(loc) != null;
	}
	final Location getAdjacentTotem(final Location loc) {
		Block locBlock = loc.getBlock();
		for(int face=0; face < NB_FACES; face++) {
			if(isTotemBase(locBlock.getRelative(FACES[face]).getLocation())) {
				return locBlock.getRelative(FACES[face]).getLocation();
			}
		}
		return null;
	}

	void playTotemEffect(final Location loc) {
		Block pumpkin = loc.getBlock().getRelative(BlockFace.UP, 3);
		if(pumpkin.getType() != Material.PUMPKIN)
			return; // not a totem
		
		loc.getWorld().strikeLightningEffect(pumpkin.getLocation());
		pumpkin.setType(Material.JACK_O_LANTERN);

		Block torchHolder = pumpkin.getRelative(BlockFace.DOWN);
		for(int face=0; face < NB_FACES; face++) { // remove torches
			Block torchBlock = torchHolder.getRelative(FACES[face]);
			if(torchBlock.getType() == Material.REDSTONE_TORCH_ON)
				torchBlock.setType(Material.TORCH);
		}

		loc.getWorld().playSound(loc, Sound.PORTAL, 20.0f, 1.0f);
	}
}

