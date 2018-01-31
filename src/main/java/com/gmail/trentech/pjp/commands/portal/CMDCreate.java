package com.gmail.trentech.pjp.commands.portal;

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
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;
import com.gmail.trentech.pjc.core.BungeeManager;
import com.gmail.trentech.pjc.core.ConfigManager;
import com.gmail.trentech.pjc.help.Help;
import com.gmail.trentech.pjp.Main;
import com.gmail.trentech.pjp.effects.Particle;
import com.gmail.trentech.pjp.effects.ParticleColor;
import com.gmail.trentech.pjp.effects.Particles;
import com.gmail.trentech.pjp.listeners.LegacyListener;
import com.gmail.trentech.pjp.listeners.PortalListener;
import com.gmail.trentech.pjp.portal.LegacyBuilder;
import com.gmail.trentech.pjp.portal.Portal;
import com.gmail.trentech.pjp.portal.Portal.PortalType;
import com.gmail.trentech.pjp.portal.PortalService;
import com.gmail.trentech.pjp.portal.features.Command;
import com.gmail.trentech.pjp.portal.features.Command.SourceType;
import com.gmail.trentech.pjp.portal.features.Coordinate.Preset;
import com.gmail.trentech.pjp.portal.features.Coordinate;
import com.gmail.trentech.pjp.portal.features.Properties;
import com.gmail.trentech.pjp.rotation.Rotation;

public class CMDCreate implements CommandExecutor {

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		Help help = Help.get("portal create").get();
		
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

		if (Sponge.getServiceManager().provide(PortalService.class).get().get(name, PortalType.PORTAL).isPresent()) {
			throw new CommandException(Text.of(TextColors.RED, name, " already exists"), false);
		}

		String destination = args.<String>getOne("destination").get();

		Optional<Coordinate> coordinate = Optional.empty();
		AtomicReference<Rotation> rotation = new AtomicReference<>(Rotation.EAST);
		AtomicReference<Double> price = new AtomicReference<>(0.0);
		boolean force = false;
		AtomicReference<Particle> particle = new AtomicReference<>(Particles.getDefaultEffect("portal"));
		AtomicReference<Optional<ParticleColor>> color = new AtomicReference<>(Particles.getDefaultColor("portal", particle.get().isColorable()));
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
		
		if (args.hasAny("particle")) {
			particle.set(args.<Particles>getOne("particle").get().getParticle());

			if (args.hasAny("color")) {
				color.set(Optional.of(args.<ParticleColor>getOne("color").get()));
			}
		}

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

					Portal.Server server = new Portal.Server(PortalType.PORTAL, destination, rotation.get(), price.get());
					
					if(permission.isPresent()) {
						server.setPermission(permission.get());
					}
					
					if(command.get().isPresent()) {
						server.setCommand(command.get().get());
					}

					Properties properties = new Properties(particle.get(), color.get());
					server.setProperties(properties);
					server.setName(name);

					if (ConfigManager.get(Main.getPlugin()).getConfig().getNode("options", "portal", "legacy_builder").getBoolean()) {
						LegacyListener.builders.put(player.getUniqueId(), new LegacyBuilder(server));
						player.sendMessage(Text.builder().color(TextColors.DARK_GREEN).append(Text.of("Begin building your portal frame, followed by ")).onClick(TextActions.runCommand("/pjp:portal save")).append(Text.of(TextColors.YELLOW, TextStyles.UNDERLINE, "/portal save")).build());
					} else {
						PortalListener.builders.put(player.getUniqueId(), server);

						player.sendMessage(Text.of(TextColors.DARK_GREEN, "Right click bottom with empty hand similar to vanilla nether portals "));
					}
				};
				BungeeManager.getServer(consumer2, player);
			};			
			BungeeManager.getServers(consumer1, player);
		} else {
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
			
			Portal.Local local = new Portal.Local(PortalType.PORTAL, rotation.get(), price.get(), force);
			
			if(coordinate.isPresent()) {
				local.setCoordinate(coordinate.get());
			}
			
			if(permission.isPresent()) {
				local.setPermission(permission.get());
			}
			
			if(command.get().isPresent()) {
				local.setCommand(command.get().get());
			}
			
			Properties properties = new Properties(particle.get(), color.get());
			local.setProperties(properties);
			local.setName(name);

			if (ConfigManager.get(Main.getPlugin()).getConfig().getNode("options", "portal", "legacy_builder").getBoolean()) {
				LegacyListener.builders.put(player.getUniqueId(), new LegacyBuilder(local));
				player.sendMessage(Text.builder().color(TextColors.DARK_GREEN).append(Text.of("Begin building your portal frame, followed by ")).onClick(TextActions.runCommand("/pjp:portal save")).append(Text.of(TextColors.YELLOW, TextStyles.UNDERLINE, "/portal save")).build());
			} else {
				PortalListener.builders.put(player.getUniqueId(), local);
				player.sendMessage(Text.of(TextColors.DARK_GREEN, "Right click bottom with empty hand similar to vanilla nether portals "));
			}
		}

		return CommandResult.success();
	}

}
