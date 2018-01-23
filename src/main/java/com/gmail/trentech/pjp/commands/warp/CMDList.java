package com.gmail.trentech.pjp.commands.warp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.spongepowered.api.Sponge;
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
import com.gmail.trentech.pjc.core.BungeeManager;
import com.gmail.trentech.pjc.help.Help;
import com.gmail.trentech.pjp.portal.Portal;
import com.gmail.trentech.pjp.portal.Portal.PortalType;
import com.gmail.trentech.pjp.portal.PortalService;
import com.gmail.trentech.pjp.portal.features.Coordinate;
import com.gmail.trentech.pjp.portal.features.Coordinate.Preset;

public class CMDList implements CommandExecutor {

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		Help help = Help.get("warp list").get();
		
		if (args.hasAny("help")) {		
			help.execute(src);
			return CommandResult.empty();
		}
		
		if (!(src instanceof Player)) {
			throw new CommandException(Text.of(TextColors.RED, "Must be a player"), false);
		}
		Player player = (Player) src;

		List<Text> list = new ArrayList<>();

		PortalService portalService = Sponge.getServiceManager().provide(PortalService.class).get();
		
		for (Portal portal : portalService.all(PortalType.WARP)) {
			String name = portal.getName();

			if (!src.hasPermission("pjp.warps." + name)) {
				continue;
			}

			Builder builder = Text.builder().onHover(TextActions.showText(Text.of(TextColors.WHITE, "Click to teleport to warp")));

			if (portal instanceof Portal.Server) {
				Portal.Server server = (Portal.Server) portal;

				Consumer<List<String>> consumer = (s) -> {
					if (!s.contains(server.getServer())) {
						try {
							throw new CommandException(Text.of(TextColors.RED, server.getServer(), " does not exist"), false);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				BungeeManager.getServers(consumer, player);

				builder.onClick(TextActions.runCommand("/warp " + name)).append(Text.of(TextColors.GREEN, "Name: ", TextColors.WHITE, name, TextColors.GREEN, " Server Destination: ", TextColors.WHITE, server.getServer()));
			} else {
				Portal.Local local = (Portal.Local) portal;

				Optional<Coordinate> optionalCoordinate = local.getCoordinate();
				
				if(optionalCoordinate.isPresent()) {
					Coordinate coordinate = optionalCoordinate.get();
					String worldName = coordinate.getWorld().getName();
					
					if(coordinate.getPreset().equals(Preset.BED)) {	
						builder.onClick(TextActions.runCommand("/warp " + name)).append(Text.of(TextColors.GREEN, "Name: ", TextColors.WHITE, name, TextColors.GREEN, " Destination: ", TextColors.WHITE, worldName, ", bed"));
					} else if(coordinate.getPreset().equals(Preset.RANDOM)) {
						builder.onClick(TextActions.runCommand("/warp " + name)).append(Text.of(TextColors.GREEN, "Name: ", TextColors.WHITE, name, TextColors.GREEN, " Destination: ", TextColors.WHITE, worldName, ", random"));
					} else if(coordinate.getPreset().equals(Preset.LAST_LOCATION)) {
						builder.onClick(TextActions.runCommand("/warp " + name)).append(Text.of(TextColors.GREEN, "Name: ", TextColors.WHITE, name, TextColors.GREEN, " Destination: ", TextColors.WHITE, worldName, ", last location"));
					} else {
						Optional<Location<World>> optionalLocation = coordinate.getLocation();
						
						if (optionalLocation.isPresent()) {
							Location<World> location = optionalLocation.get();

							Vector3d vector3d = location.getPosition();
							
							builder.onClick(TextActions.runCommand("/warp " + name)).append(Text.of(TextColors.GREEN, "Name: ", TextColors.WHITE, name, TextColors.GREEN, " Destination: ", TextColors.WHITE, worldName, ", ", vector3d.getFloorX(), ", ", vector3d.getFloorY(), ", ", vector3d.getFloorZ()));
						} else {
							builder.onClick(TextActions.runCommand("/warp " + name)).append(Text.of(TextColors.GREEN, "Name: ", TextColors.WHITE, name, TextColors.RED, " - DESTINATION ERROR"));
						}	
					}
				} else {
					builder.onClick(TextActions.runCommand("/warp " + name)).append(Text.of(TextColors.GREEN, "Name: ", TextColors.WHITE, name, TextColors.RED, " - DESTINATION ERROR"));
				}
			}

			double price = portal.getPrice();

			if (price != 0) {
				builder.append(Text.of(TextColors.GREEN, " Price: ", TextColors.WHITE, "$", price));
			}

			if(portal.getPermission().isPresent()) {
				builder.append(Text.of(TextColors.GREEN, " Permission: ", TextColors.WHITE, portal.getPermission().get()));
			}
			
			if(portal.getCommand().isPresent()) {
				builder.append(Text.of(TextColors.GREEN, " Command: ", TextColors.WHITE, portal.getCommand().get().getCommand()));
			}
			
			list.add(builder.build());
		}

		if (list.isEmpty()) {
			list.add(Text.of(TextColors.YELLOW, " No warp points"));
		}

		PaginationList.Builder paginationList = PaginationList.builder();

		paginationList.title(Text.builder().color(TextColors.DARK_GREEN).append(Text.of(TextColors.GREEN, "Warps")).build());

		paginationList.contents(list);

		paginationList.sendTo(src);

		return CommandResult.success();
	}

}
