package com.gmail.trentech.pjp.portal;

import static com.gmail.trentech.pjp.data.Keys.BED_LOCATIONS;
import static com.gmail.trentech.pjp.data.Keys.LAST_LOCATIONS;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;
import com.gmail.trentech.pjc.core.BungeeManager;
import com.gmail.trentech.pjc.core.ConfigManager;
import com.gmail.trentech.pjc.core.SQLManager;
import com.gmail.trentech.pjc.core.TeleportManager;
import com.gmail.trentech.pjp.Main;
import com.gmail.trentech.pjp.effects.Particle;
import com.gmail.trentech.pjp.effects.Particles;
import com.gmail.trentech.pjp.events.TeleportEvent;
import com.gmail.trentech.pjp.portal.Portal.PortalType;
import com.gmail.trentech.pjp.portal.features.Command;
import com.gmail.trentech.pjp.portal.features.Command.SourceType;
import com.gmail.trentech.pjp.portal.features.Coordinate;
import com.gmail.trentech.pjp.portal.features.Coordinate.Preset;
import com.gmail.trentech.pjp.portal.features.Properties;

public class PortalService {

	private static ConcurrentHashMap<String, Portal> cache = new ConcurrentHashMap<>();
	
	public Optional<Portal> get(String name, PortalType type) {
		if (cache.containsKey(name)) {
			Portal portal = cache.get(name);

			if (portal.getType().equals(type)) {
				return Optional.of(cache.get(name));
			}
		}

		return Optional.empty();
	}

	public Optional<Portal> get(Location<World> location, PortalType type) {
		if (type.equals(PortalType.PORTAL)) {
			for (Entry<String, Portal> entry : cache.entrySet()) {
				Portal portal = entry.getValue();

				if (!portal.getType().equals(type)) {
					continue;
				}

				Properties properties = portal.getProperties().get();

				List<Location<World>> frame = properties.getFrame();

				if (!frame.get(0).getExtent().equals(location.getExtent())) {
					continue;
				}

				for (Location<World> loc : frame) {
					if (loc.getBlockPosition().equals(location.getBlockPosition())) {
						return Optional.of(portal);
					}
				}

				for (Location<World> loc : properties.getFill()) {
					if (loc.getBlockPosition().equals(location.getBlockPosition())) {
						return Optional.of(portal);
					}
				}
			}
		}

		return get(location.getExtent().getName() + ":" + location.getBlockX() + "." + location.getBlockY() + "." + location.getBlockZ(), type);
	}

	public List<Portal> all(PortalType type) {
		List<Portal> list = new ArrayList<>();

		for (Entry<String, Portal> entry : cache.entrySet()) {
			Portal portal = entry.getValue();

			if (portal.getType().equals(type)) {
				list.add(portal);
			}
		}

		return list;
	}

	public void init() {
		try {
			SQLManager sqlManager = SQLManager.get(Main.getPlugin());
			Connection connection = sqlManager.getDataSource().getConnection();

			PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + sqlManager.getPrefix("PORTALS"));

			ResultSet result = statement.executeQuery();

			while (result.next()) {
				String name = result.getString("Name");

				Portal portal;
				try {
					portal = Portal.deserialize(result.getString("Data"));
					
					portal.setName(name);

					cache.put(name, portal);

					Optional<Properties> optionalProperties = portal.getProperties();
					
					if (optionalProperties.isPresent()) {
						Properties properties = portal.getProperties().get();
						properties.getParticle().createTask(name, properties.getFill(), properties.getParticleColor());
					}
				} catch(Exception e) {
					e.printStackTrace();
					portal = new Portal.Local(null, null, 0, false);
					portal.setName(name);
					remove(portal);
				}		
			}

			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void create(Portal portal, String name) {
		portal.setName(name);
		try {
			SQLManager sqlManager = SQLManager.get(Main.getPlugin());
			Connection connection = sqlManager.getDataSource().getConnection();

			PreparedStatement statement = connection.prepareStatement("INSERT into " + sqlManager.getPrefix("PORTALS") + " (Name, Data) VALUES (?, ?)");

			statement.setString(1, portal.getName());
			statement.setString(2, Portal.serialize(portal));

			statement.executeUpdate();

			connection.close();

			cache.put(portal.getName(), portal);

			Optional<Properties> optionalProperties = portal.getProperties();
			
			if (optionalProperties.isPresent()) {
				Properties properties = optionalProperties.get();
				properties.getParticle().createTask(portal.getName(), properties.getFill(), properties.getParticleColor());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void create(Portal portal, Location<World> location) {
		portal.setName(location.getExtent().getName() + ":" + location.getBlockX() + "." + location.getBlockY() + "." + location.getBlockZ());

		create(portal, portal.getName());

		Particle particle = Particles.getDefaultEffect("creation");
		particle.spawnParticle(location, false, Particles.getDefaultColor("creation", particle.isColorable()));
	}

	public void update(Portal portal) {
		try {
			SQLManager sqlManager = SQLManager.get(Main.getPlugin());
			Connection connection = sqlManager.getDataSource().getConnection();

			PreparedStatement statement = connection.prepareStatement("UPDATE " + sqlManager.getPrefix("PORTALS") + " SET Data = ? WHERE Name = ?");

			statement.setString(1, Portal.serialize(portal));
			statement.setString(2, portal.getName());

			statement.executeUpdate();

			connection.close();

			cache.put(portal.getName(), portal);

			Optional<Properties> optionalProperties = portal.getProperties();
			
			if (optionalProperties.isPresent()) {
				Properties properties = optionalProperties.get();

				for (Task task : Sponge.getScheduler().getScheduledTasks()) {
					if (task.getName().equals(portal.getName())) {
						task.cancel();
						break;
					}
				}
				properties.update(true);
				properties.getParticle().createTask(portal.getName(), properties.getFill(), properties.getParticleColor());
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void remove(Portal portal) {
		try {
			SQLManager sqlManager = SQLManager.get(Main.getPlugin());
			Connection connection = sqlManager.getDataSource().getConnection();

			PreparedStatement statement = connection.prepareStatement("DELETE from " + sqlManager.getPrefix("PORTALS") + " WHERE Name = ?");

			statement.setString(1, portal.getName());
			statement.executeUpdate();

			connection.close();

			if(cache.contains(portal.getName())) {
				cache.remove(portal.getName());
			}

			Optional<Properties> optionalProperties = portal.getProperties();
			
			if (optionalProperties.isPresent()) {
				Properties properties = optionalProperties.get();

				for (Task task : Sponge.getScheduler().getScheduledTasks()) {
					if (task.getName().equals(portal.getName())) {
						task.cancel();
						break;
					}
				}
				properties.update(true);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean execute(Player player, Portal portal) {
		AtomicReference<Boolean> bool = new AtomicReference<>(false);

		Optional<String> optionalPermission = portal.getPermission();
		
		if(optionalPermission.isPresent()) {
			if (!player.hasPermission(optionalPermission.get())) {
				player.sendMessage(Text.of(TextColors.DARK_RED, "Requires permission ", TextColors.YELLOW, optionalPermission.get()));
				return false;
			}
		}
		
		Optional<Command> optionalCommand = portal.getCommand();
		
		if(optionalCommand.isPresent()) {
			Command command = optionalCommand.get();
			
			if(command.getSrcType().equals(SourceType.CONSOLE)) {
				command.execute();
			} else {
				command.execute(player);
			}
		}
		
		if (portal instanceof Portal.Server) {
			Portal.Server server = (Portal.Server) portal;

			Consumer<String> consumer = (serverName) -> {
				Task.builder().execute(() -> {
					TeleportEvent.Server teleportEvent = new TeleportEvent.Server(player, serverName, server.getServer(), server.getPrice(), server.getPermission(), Cause.of(EventContext.builder().add(EventContextKeys.PLAYER, player).build(), server));

					if (!Sponge.getEventManager().post(teleportEvent)) {
						BungeeManager.connect(player, teleportEvent.getDestination());
						player.setLocation(player.getWorld().getSpawnLocation());

						bool.set(true);
					}
				}).submit(Main.instance());
			};
			BungeeManager.getServer(consumer, player);
		} else {
			Portal.Local local = (Portal.Local) portal;

			Optional<Coordinate> optionalCoodinate = local.getCoordinate();
			
			if(optionalCoodinate.isPresent()) {
				Coordinate coordinate = optionalCoodinate.get();
				
				Optional<Location<World>> optionalSpawnLocation;

				if(coordinate.getPreset().equals(Preset.BED)) {
					Map<String, Coordinate> list = new HashMap<>();

					Optional<Map<String, Coordinate>> optionalList = player.get(BED_LOCATIONS);

					if (optionalList.isPresent()) {
						list = optionalList.get();
					}
					
					String worldUuid = coordinate.getWorld().getUniqueId().toString();
					
					if(list.containsKey(worldUuid)) {
						optionalSpawnLocation = list.get(worldUuid).getLocation();
					}else {
						optionalSpawnLocation = coordinate.getLocation();
					}
				} else if(coordinate.getPreset().equals(Preset.LAST_LOCATION)) {
					Map<String, Coordinate> list = new HashMap<>();

					Optional<Map<String, Coordinate>> optionalList = player.get(LAST_LOCATIONS);

					if (optionalList.isPresent()) {
						list = optionalList.get();
					}
					
					String worldUuid = coordinate.getWorld().getUniqueId().toString();
					
					if(list.containsKey(worldUuid)) {
						optionalSpawnLocation = list.get(worldUuid).getLocation();
					}else {
						optionalSpawnLocation = coordinate.getLocation();
					}
				} else if(coordinate.getPreset().equals(Preset.RANDOM)) {
					optionalSpawnLocation = TeleportManager.getRandomLocation(coordinate.getWorld(), ConfigManager.get(Main.getPlugin()).getConfig().getNode("options", "random_spawn_radius").getInt());
				} else {
					optionalSpawnLocation = coordinate.getLocation();
				}

				if (optionalSpawnLocation.isPresent()) {
					Location<World> spawnLocation = optionalSpawnLocation.get();
		
					com.gmail.trentech.pjp.events.TeleportEvent.Local teleportEvent = new TeleportEvent.Local(player, player.getLocation(), spawnLocation, local.getPrice(), local.force(), local.getPermission(), Cause.of(EventContext.builder().add(EventContextKeys.PLAYER, player).build(), local));

					if (!Sponge.getEventManager().post(teleportEvent)) {
//						spawnLocation = teleportEvent.getDestination();
//						Vector3d rotation = local.getRotation().toVector3d();

						String warp = "warp " + portal.getName();

						if (warp.contains("hubto")) {
							warp = warp.replace("hubto", "");
						}

						Sponge.getCommandManager().process(player, warp);

//						player.setLocationAndRotation(spawnLocation, rotation);

						return true;
					}
				} else {
					player.sendMessage(Text.of(TextColors.RED, "Could not find location"));
				}
			}
		}

		return bool.get();
	}
}
