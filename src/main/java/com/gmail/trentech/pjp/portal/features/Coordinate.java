package com.gmail.trentech.pjp.portal.features;

import static com.gmail.trentech.pjp.data.DataQueries.BED_RESPAWN;
import static com.gmail.trentech.pjp.data.DataQueries.PRESET;
import static com.gmail.trentech.pjp.data.DataQueries.RANDOM;
import static com.gmail.trentech.pjp.data.DataQueries.VECTOR3D;
import static com.gmail.trentech.pjp.data.DataQueries.WORLD;

import java.io.IOException;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataTranslators;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

public class Coordinate implements DataSerializable {

	private World world;
	private Optional<Vector3d> vector3d = Optional.empty();
	private Preset preset = Preset.NONE;
	
	public Coordinate(World world, Vector3d vector3d) {
		this.world = world;
		this.vector3d = Optional.of(vector3d);
	}
	
	public Coordinate(World world, Preset preset) {
		this.world = world;		
		this.preset = preset;
	}
	
	public Coordinate(Location<World> location) {
		this.world = location.getExtent();
		this.vector3d = Optional.of(location.getPosition());
	}
	
	public World getWorld() {
		return world;
	}
	
	public Preset getPreset() {
		return preset;
	}

	public Optional<Location<World>> getLocation() {
		if (vector3d.isPresent()) {
			return Optional.of(new Location<World>(world, this.vector3d.get()));
		} else {
			return Optional.of(world.getSpawnLocation());
		}
	}
	
	@Override
	public int getContentVersion() {
		return 0;
	}

	@Override
	public DataContainer toContainer() {
		DataContainer dataContainer = DataContainer.createNew().set(WORLD, world.getName()).set(PRESET, preset.getName());
		
		if(vector3d.isPresent()) {
			dataContainer.set(VECTOR3D, DataTranslators.VECTOR_3_D.translate(vector3d.get()));
		}
		
		return dataContainer;
	}
	
	public static class Builder extends AbstractDataBuilder<Coordinate> {

		public Builder() {
			super(Coordinate.class, 0);
		}

		@Override
		protected Optional<Coordinate> buildContent(DataView container) throws InvalidDataException {
			if (container.contains(WORLD)) {
				Optional<World> optionalWorld = Sponge.getServer().getWorld(container.getString(WORLD).get());

				if (!optionalWorld.isPresent()) {
					return Optional.empty();
				}
				World world = optionalWorld.get();
				
				if (container.contains(VECTOR3D)) {
					Vector3d vector3d = DataTranslators.VECTOR_3_D.translate(container.getView(VECTOR3D).get());
					
					return Optional.of(new Coordinate(world, vector3d));
				}
				
				Preset preset;
				
				if(container.contains(RANDOM)) {
					if(container.getBoolean(RANDOM).get()) {
						preset = Preset.RANDOM;
					} else if(container.getBoolean(BED_RESPAWN).get()){
						preset = Preset.BED;
					} else {
						preset = Preset.NONE;
					}
				} else {
					preset = Preset.get(container.getString(PRESET).get());
				}

				return Optional.of(new Coordinate(world, preset));
			}

			return Optional.empty();
		}
	}
	
	public enum Preset {

		RANDOM("random"), BED("bed"), LAST_LOCATION("last_location"), NONE("none");

		private final String name;

		private Preset(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public static Preset get(String name) {
			Preset[] presets = Preset.values();

			for (Preset preset : presets) {
				if (preset.getName().equals(name.toLowerCase())) {
					return preset;

				}
			}

			return null;
		}
	}
	
	public static String serialize(Location<World> location) {
		try {
			return DataFormats.JSON.write(new Coordinate(location).toContainer());
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
	}

	public static Optional<Location<World>> deserialize(String coordinate) {
		try {
			return Sponge.getDataManager().deserialize(Coordinate.class, DataFormats.JSON.read(coordinate)).get().getLocation();
		} catch (InvalidDataException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
