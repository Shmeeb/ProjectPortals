package com.gmail.trentech.pjp.commands.home;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Builder;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;
import com.gmail.trentech.pjc.help.Help;
import com.gmail.trentech.pjp.data.Keys;
import com.gmail.trentech.pjp.portal.Portal;
import com.gmail.trentech.pjp.portal.features.Coordinate;
import com.gmail.trentech.pjp.portal.features.Coordinate.Preset;

public class CMDList implements CommandExecutor {

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		Help help = Help.get("home list").get();
		
		if (args.hasAny("help")) {		
			help.execute(src);
			return CommandResult.empty();
		}
		
		if (!(src instanceof Player)) {
			throw new CommandException(Text.of(TextColors.RED, "Must be a player"), false);
		}
		Player player = (Player) src;

		Map<String, Portal> list = new HashMap<>();

		Optional<Map<String, Portal>> optionalHomeList = player.get(Keys.PORTALS);

		if (optionalHomeList.isPresent()) {
			list = optionalHomeList.get();
		}

		List<Text> pages = new ArrayList<>();

		for (Entry<String, Portal> entry : list.entrySet()) {
			String name = entry.getKey().toString();
			Portal.Local local = (Portal.Local) entry.getValue();

			Builder builder = Text.builder().onHover(TextActions.showText(Text.of(TextColors.WHITE, "Click to teleport to home")));

			Optional<Coordinate> optionalCoordinate = local.getCoordinate();
			
			if(optionalCoordinate.isPresent()) {
				Coordinate coordinate = optionalCoordinate.get();
				String worldName = coordinate.getWorld().getName();
				
				if(coordinate.getPreset().equals(Preset.BED)) {	
					builder.onClick(TextActions.runCommand("/home " + name)).append(Text.of(TextColors.GREEN, "Name: ", TextColors.WHITE, name, TextColors.GREEN, " Destination: ", TextColors.WHITE, worldName, ", bed"));
				} else if(coordinate.getPreset().equals(Preset.RANDOM)) {
					builder.onClick(TextActions.runCommand("/home " + name)).append(Text.of(TextColors.GREEN, "Name: ", TextColors.WHITE, name, TextColors.GREEN, " Destination: ", TextColors.WHITE, worldName, ", random"));
				} else {
					Optional<Location<World>> optionalLocation = coordinate.getLocation();
					
					if (optionalLocation.isPresent()) {
						Location<World> location = optionalLocation.get();

						Vector3d vector3d = location.getPosition();
						
						builder.onClick(TextActions.runCommand("/home " + name)).append(Text.of(TextColors.GREEN, "Name: ", TextColors.WHITE, name, TextColors.GREEN, " Destination: ", TextColors.WHITE, worldName, ", ", vector3d.getFloorX(), ", ", vector3d.getFloorY(), ", ", vector3d.getFloorZ()));
					} else {
						builder.onClick(TextActions.runCommand("/home " + name)).append(Text.of(TextColors.GREEN, "Name: ", TextColors.WHITE, name, TextColors.RED, " - DESTINATION ERROR"));
					}	
				}
			} else {
				builder.onClick(TextActions.runCommand("/home " + name)).append(Text.of(TextColors.GREEN, "Name: ", TextColors.WHITE, name, TextColors.RED, " - DESTINATION ERROR"));
			}

			pages.add(builder.build());
		}

		if (pages.isEmpty()) {
			pages.add(Text.of(TextColors.YELLOW, " No saved homes"));
		}

		PaginationList.Builder paginationList = PaginationList.builder();

		paginationList.title(Text.builder().color(TextColors.DARK_GREEN).append(Text.of(TextColors.GREEN, "Homes")).build());

		paginationList.contents(pages);

		paginationList.sendTo(src);

		return CommandResult.success();
	}

}
