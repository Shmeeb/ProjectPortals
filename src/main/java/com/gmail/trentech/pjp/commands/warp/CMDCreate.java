package com.gmail.trentech.pjp.commands.warp;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;
import com.gmail.trentech.pjc.core.BungeeManager;
import com.gmail.trentech.pjc.help.Help;
import com.gmail.trentech.pjp.portal.Portal;
import com.gmail.trentech.pjp.portal.Portal.Local;
import com.gmail.trentech.pjp.portal.Portal.PortalType;
import com.gmail.trentech.pjp.portal.Portal.Server;
import com.gmail.trentech.pjp.portal.features.Command;
import com.gmail.trentech.pjp.portal.features.Coordinate;
import com.gmail.trentech.pjp.portal.features.Command.SourceType;
import com.gmail.trentech.pjp.portal.features.Coordinate.Preset;
import com.gmail.trentech.pjp.portal.PortalService;
import com.gmail.trentech.pjp.rotation.Rotation;

public class CMDCreate implements CommandExecutor {

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		Help help = Help.get("warp create").get();
		
		if (args.hasAny("help")) {		
			help.execute(src);
			return CommandResult.empty();
		}
		
		if (!(src instanceof Player)) {
			throw new CommandException(Text.of(TextColors.RED, "Must be a player"), false);
		}
		Player player = (Player) src;

		if (!args.hasAny("name")) {
			throw new CommandException(Text.builder().onClick(TextActions.executeCallback(help.execute())).append(help.getUsageText()).build(), false);
		}
		String name = args.<String>getOne("name").get().toLowerCase();

		PortalService portalService = Sponge.getServiceManager().provide(PortalService.class).get();
		
		if (portalService.get(name, PortalType.WARP).isPresent()) {
			throw new CommandException(Text.of(TextColors.RED, name, " already exists"), false);
		}

		Optional<Coordinate> coordinate = Optional.empty();
		boolean force = false;
		AtomicReference<Rotation> rotation = new AtomicReference<>(Rotation.EAST);
		AtomicReference<Double> price = new AtomicReference<>(0.0);
		Optional<String> permission = args.<String>getOne("permission");
		AtomicReference<Optional<Command>> command = new AtomicReference<>(Optional.empty());
		
		if (args.hasAny("price")) {
			price.set(args.<Double>getOne("price").get());
		}

		if (args.hasAny("command")) {
			String rawCommand = args.<String>getOne("command").get();
			String source = rawCommand.substring(0, 2);
			
			if(rawCommand.length() < 2) {
				throw new CommandException(Text.of(TextColors.RED, "Did not specify command source. P: for player or C: for console. Example \"P:say hello world\""), false);
			}
			
			if(source.equalsIgnoreCase("P:")) {
				command.set(Optional.of(new Command(SourceType.PLAYER, rawCommand.substring(2))));
			} else if(source.equalsIgnoreCase("C:")) {
				command.set(Optional.of(new Command(SourceType.CONSOLE, rawCommand.substring(2))));
			} else {
				throw new CommandException(Text.of(TextColors.RED, "Did not specify command source. P: for player or C: for console. Example \"P:say hello world\""), false);
			}
		}
		
		if (args.hasAny("destination")) {
			String destination = args.<String>getOne("destination").get();

			if (args.hasAny("b")) {
				Consumer<List<String>> consumer1 = (list) -> {
					if (!list.contains(destination)) {
						try {
							throw new CommandException(Text.of(TextColors.RED, destination, " does not exist"), false);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					Consumer<String> consumer2 = (s) -> {
						if (destination.equalsIgnoreCase(s)) {
							try {
								throw new CommandException(Text.of(TextColors.RED, "Destination cannot be the server you are currently on"), false);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						Server portal = new Portal.Server(PortalType.WARP, destination, rotation.get(), price.get());
						
						if(permission.isPresent()) {
							portal.setPermission(permission.get());
						}
						
						if(command.get().isPresent()) {
							portal.setCommand(command.get().get());
						}

						portalService.create(portal, name);

						player.sendMessage(Text.of(TextColors.DARK_GREEN, "Warp ", name, " create"));
					};
					BungeeManager.getServer(consumer2, player);
				};				
				BungeeManager.getServers(consumer1, player);

				return CommandResult.success();
			} else {
				if (!Sponge.getServer().getWorld(destination).isPresent()) {
					throw new CommandException(Text.of(TextColors.RED, destination, " is not loaded or does not exist"), false);
				}

				Optional<World> world = Sponge.getServer().getWorld(destination);

				if (!world.isPresent()) {
					throw new CommandException(Text.of(TextColors.RED, destination, " is not loaded or does not exist"), false);
				}
				
				if (args.hasAny("x,y,z")) {
					String[] coords = args.<String>getOne("x,y,z").get().split(",");

					if (coords[0].equalsIgnoreCase("random")) {
						coordinate = Optional.of(new Coordinate(world.get(), Preset.RANDOM));
					} else if(coords[0].equalsIgnoreCase("bed")) {
						coordinate = Optional.of(new Coordinate(world.get(), Preset.BED));
					} else if(coords[0].equalsIgnoreCase("last")) {
						coordinate = Optional.of(new Coordinate(world.get(), Preset.LAST_LOCATION));
					} else {
						try {
							coordinate = Optional.of(new Coordinate(world.get(), new Vector3d(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]))));
						} catch (Exception e) {
							throw new CommandException(Text.of(TextColors.RED, coords.toString(), " is not valid"), true);
						}
					}
				} else {
					coordinate = Optional.of(new Coordinate(world.get(), Preset.NONE));
				}

				if (args.hasAny("direction")) {
					rotation.set(args.<Rotation>getOne("direction").get());
				}
				
				if (args.hasAny("f")) {
					force = true;
				}
			}
		} else {
			coordinate = Optional.of(new Coordinate(player.getLocation()));
			rotation.set(Rotation.getClosest(player.getRotation().getFloorY()));
		}

		Local portal = new Portal.Local(PortalType.BUTTON, rotation.get(), price.get(), force);
		
		if(coordinate.isPresent()) {
			portal.setCoordinate(coordinate.get());
		}
		
		if(permission.isPresent()) {
			portal.setPermission(permission.get());
		}
		
		if(command.get().isPresent()) {
			portal.setCommand(command.get().get());
		}
		
		portalService.create(portal, name);

		player.sendMessage(Text.of(TextColors.DARK_GREEN, "Warp ", name, " create"));

		return CommandResult.success();
	}

}
