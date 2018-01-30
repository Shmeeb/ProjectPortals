package com.gmail.trentech.pjp.portal;

import static com.gmail.trentech.pjp.data.DataQueries.COMMAND;
import static com.gmail.trentech.pjp.data.DataQueries.COORDINATE;
import static com.gmail.trentech.pjp.data.DataQueries.FORCE;
import static com.gmail.trentech.pjp.data.DataQueries.PERMISSION;
import static com.gmail.trentech.pjp.data.DataQueries.PORTAL_TYPE;
import static com.gmail.trentech.pjp.data.DataQueries.PRICE;
import static com.gmail.trentech.pjp.data.DataQueries.PROPERTIES;
import static com.gmail.trentech.pjp.data.DataQueries.ROTATION;
import static com.gmail.trentech.pjp.data.DataQueries.SERVER;

import java.io.IOException;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.InvalidDataException;

import com.gmail.trentech.pjp.portal.features.Command;
import com.gmail.trentech.pjp.portal.features.Coordinate;
import com.gmail.trentech.pjp.portal.features.Properties;
import com.gmail.trentech.pjp.rotation.Rotation;

public abstract class Portal implements DataSerializable {

	private final PortalType type;
	private String name;
	private Rotation rotation = Rotation.EAST;
	private double price = 0;
	private Optional<String> permission = Optional.empty();	
	private Optional<Properties> properties = Optional.empty();
	private Optional<Command> command = Optional.empty();
	
	protected Portal(PortalType type, Rotation rotation, double price) {
		this.type = type;
		this.rotation = rotation;
		this.price = price;
	}

	public PortalType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Rotation getRotation() {
		return rotation;
	}

	public void setRotation(Rotation rotation) {
		this.rotation = rotation;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public Optional<Command> getCommand() {
		return command;
	}

	public void setCommand(Command command) {
		this.command = Optional.of(command);
	}
	
	public Optional<String> getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = Optional.of(permission);
	}
	
	public Optional<Properties> getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = Optional.of(properties);
	}

	public static class Server extends Portal {

		private String server;

		public Server(PortalType type, String server, Rotation rotation, double price) {
			super(type, rotation, price);
			
			this.server = server;
		}

		public String getServer() {
			return server;
		}

		public void setServer(String server) {
			this.server = server;
		}

		@Override
		public int getContentVersion() {
			return 0;
		}

		@Override
		public DataContainer toContainer() {
			DataContainer container = DataContainer.createNew().set(PORTAL_TYPE, getType().name()).set(SERVER, getServer()).set(ROTATION, getRotation().getName()).set(PRICE, getPrice());

			if (getPermission().isPresent()) {
				container.set(PERMISSION, getPermission().get());
			}
			
			if (getCommand().isPresent()) {
				container.set(COMMAND, getCommand().get());
			}
			
			if (getProperties().isPresent()) {
				container.set(PROPERTIES, getProperties().get());
			}

			return container;
		}

		public static class Builder extends AbstractDataBuilder<Server> {

			public Builder() {
				super(Server.class, 0);
			}

			@Override
			protected Optional<Server> buildContent(DataView container) throws InvalidDataException {
				if (container.contains(PORTAL_TYPE, SERVER, ROTATION, PRICE)) {
					PortalType type = PortalType.valueOf(container.getString(PORTAL_TYPE).get());
					String server = container.getString(SERVER).get();
					Rotation rotation = Rotation.get(container.getString(ROTATION).get()).get();
					Double price = container.getDouble(PRICE).get();

					Portal.Server portal = new Portal.Server(type, server, rotation, price);
					
					if(container.contains(PERMISSION)) {
						portal.setPermission(container.getString(PERMISSION).get());
					}

					if (container.contains(COMMAND)) {
						portal.setCommand(container.getSerializable(COMMAND, Command.class).get());
					}
					
					if (container.contains(PROPERTIES)) {
						portal.setProperties(container.getSerializable(PROPERTIES, Properties.class).get());
					}

					return Optional.of(portal);
				}

				return Optional.empty();
			}
		}
	}

	public static class Local extends Portal {

		private Optional<Coordinate> coordinate;
		private boolean force;
		
		public Local(PortalType type, Rotation rotation, double price, boolean force) {
			super(type, rotation, price);

			this.force = force;
		}

		public Optional<Coordinate> getCoordinate() {
			return coordinate;
		}
		
		public void setCoordinate(Coordinate coordinate) {
			this.coordinate = Optional.of(coordinate);
		}
		
		public boolean force() {
			return force;
		}
		
		public void setSet(boolean force) {
			this.force = force;
		}

		@Override
		public int getContentVersion() {
			return 0;
		}

		@Override
		public DataContainer toContainer() {
			DataContainer container = DataContainer.createNew().set(PORTAL_TYPE, getType().name()).set(ROTATION, getRotation().getName()).set(PRICE, getPrice()).set(FORCE, force());

			if (getPermission().isPresent()) {
				container.set(PERMISSION, getPermission().get());
			}
			
			if (getProperties().isPresent()) {
				container.set(PROPERTIES, getProperties().get());
			}

			if (getCoordinate().isPresent()) {
				container.set(COORDINATE, getCoordinate().get());
			}
			
			if (getCommand().isPresent()) {
				container.set(COMMAND, getCommand().get());
			}
			return container;
		}

		public static class Builder extends AbstractDataBuilder<Local> {

			public Builder() {
				super(Local.class, 0);
			}

			@Override
			protected Optional<Local> buildContent(DataView container) throws InvalidDataException {
				if (container.contains(PORTAL_TYPE, ROTATION, PRICE)) {
					PortalType type = PortalType.valueOf(container.getString(PORTAL_TYPE).get());
					Rotation rotation = Rotation.get(container.getString(ROTATION).get()).get();
					Double price = container.getDouble(PRICE).get();
					boolean force = container.getBoolean(FORCE).get();

					Portal.Local portal = new Portal.Local(type, rotation, price, force);

					if(container.contains(PERMISSION)) {
						portal.setPermission(container.getString(PERMISSION).get());
					}

					if (container.contains(COMMAND)) {
						portal.setCommand(container.getSerializable(COMMAND, Command.class).get());
					}
					
					if (container.contains(COORDINATE)) {
						portal.setCoordinate(container.getSerializable(COORDINATE, Coordinate.class).get());
					}
					
					if (container.contains(PROPERTIES)) {
						portal.setProperties(container.getSerializable(PROPERTIES, Properties.class).get());
					}

					return Optional.of(portal);
				}

				return Optional.empty();
			}
		}
	}

	public enum PortalType {
		BUTTON,
		DOOR,
		HOME,
		LEVER,
		PLATE,
		PORTAL,
		SIGN,
		WARP;
	}

	public static String serialize(Portal portal) {
		try {
			if (portal instanceof Server) {
				Server server = (Server) portal;
				return DataFormats.JSON.write(server.toContainer());
			} else {
				Local local = (Local) portal;
				return DataFormats.JSON.write(local.toContainer());
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Portal deserialize(String portal) {
		DataContainer container;
		try {
			container = DataFormats.JSON.read(portal);
		} catch (InvalidDataException | IOException e) {
			e.printStackTrace();
			return null;
		}
		
		if(container.contains(SERVER)) {
			try {
				return Sponge.getDataManager().deserialize(Server.class, container).get();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} else {
			try {
				return Sponge.getDataManager().deserialize(Local.class, container).get();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}
}
