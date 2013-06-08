
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
import org.bukkit.Location;

import fr.tobast.bukkit.nomadicgameplay.NomadicGameplay;

public class BlocksHandler {
	NomadicGameplay plugin;
	
	BlocksHandler(NomadicGameplay plugin) {
		this.plugin = plugin;
	}

	void destroyUnplaceableBlocks(final Location center, final int radius) {
		Location beginLoc = center.clone().add(-radius, 0, -radius);
		plugin.getServer().getLogger().info("Beginning with "+
				beginLoc.toString());

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

}

