package com.gmail.trentech.pjp.data.mutable;

import static com.gmail.trentech.pjp.data.Keys.LAST_LOCATIONS;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractMappedData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.value.mutable.MapValue;

import com.gmail.trentech.pjp.data.immutable.ImmutableLastLocationData;
import com.gmail.trentech.pjp.portal.features.Coordinate;
import com.google.common.base.Preconditions;

public class LastLocationData extends AbstractMappedData<String, Coordinate, LastLocationData, ImmutableLastLocationData> {

	public LastLocationData(Map<String, Coordinate> value) {
		super(value, LAST_LOCATIONS);
	}

	public LastLocationData() {
		super(new HashMap<>(), LAST_LOCATIONS);
	}

	public MapValue<String, Coordinate> portals() {
		return Sponge.getRegistry().getValueFactory().createMapValue(LAST_LOCATIONS, getValue());
	}

	@Override
	public Optional<Coordinate> get(String key) {
		if (getValue().containsKey(key)) {
			return Optional.of(getValue().get(key));
		}
		return Optional.empty();
	}

	@Override
	public Set<String> getMapKeys() {
		return getValue().keySet();
	}

	@Override
	public LastLocationData put(String key, Coordinate value) {
		getValue().put(key, value);
		return this;
	}

	@Override
	public LastLocationData putAll(Map<? extends String, ? extends Coordinate> map) {
		getValue().putAll(map);
		return this;
	}

	@Override
	public LastLocationData remove(String key) {
		getValue().remove(key);
		return this;
	}

	@Override
	public Optional<LastLocationData> fill(DataHolder dataHolder, MergeFunction mergeFn) {
		LastLocationData coordinateData = Preconditions.checkNotNull(mergeFn).merge(copy(), dataHolder.get(LastLocationData.class).orElse(copy()));
		return Optional.of(set(LAST_LOCATIONS, coordinateData.get(LAST_LOCATIONS).get()));
	}

	@Override
	public Optional<LastLocationData> from(DataContainer container) {
		if (container.contains(LAST_LOCATIONS.getQuery())) {
			HashMap<String, Coordinate> coordinateList = new HashMap<>();

			DataView coordinates = container.getView(LAST_LOCATIONS.getQuery()).get();

			for (DataQuery coordinate : coordinates.getKeys(false)) {
				Optional<Coordinate> optionalCoordinate = coordinates.getSerializable(coordinate, Coordinate.class);
				
				if(optionalCoordinate.isPresent()) {
					coordinateList.put(coordinate.toString(), optionalCoordinate.get());
				}
			}
			return Optional.of(new LastLocationData(coordinateList));
		}
		return Optional.empty();
	}

	@Override
	public LastLocationData copy() {
		return new LastLocationData(getValue());
	}

	@Override
	public int getContentVersion() {
		return 0;
	}

	@Override
	public ImmutableLastLocationData asImmutable() {
		return new ImmutableLastLocationData(getValue());
	}

	@Override
	public DataContainer toContainer() {
		return super.toContainer().set(LAST_LOCATIONS, getValue());
	}

	public static class Builder extends AbstractDataBuilder<LastLocationData> implements DataManipulatorBuilder<LastLocationData, ImmutableLastLocationData> {

		public Builder() {
			super(LastLocationData.class, 0);
		}

		@Override
		public Optional<LastLocationData> buildContent(DataView container) throws InvalidDataException {
			if (container.contains(LAST_LOCATIONS.getQuery())) {
				HashMap<String, Coordinate> coordinateList = new HashMap<>();

				DataView coordinates = container.getView(LAST_LOCATIONS.getQuery()).get();

				for (DataQuery coordinate : coordinates.getKeys(false)) {
					Optional<Coordinate> optionalCoordinate = coordinates.getSerializable(coordinate, Coordinate.class);
					
					if(optionalCoordinate.isPresent()) {
						coordinateList.put(coordinate.toString(), optionalCoordinate.get());
					}
				}
				return Optional.of(new LastLocationData(coordinateList));
			}
			return Optional.empty();
		}

		@Override
		public LastLocationData create() {
			return new LastLocationData(new HashMap<String, Coordinate>());
		}

		@Override
		public Optional<LastLocationData> createFrom(DataHolder dataHolder) {
			return create().fill(dataHolder);
		}
	}
}