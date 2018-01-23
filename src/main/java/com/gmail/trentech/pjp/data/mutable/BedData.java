package com.gmail.trentech.pjp.data.mutable;

import static com.gmail.trentech.pjp.data.Keys.BED_LOCATIONS;

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

import com.gmail.trentech.pjp.data.immutable.ImmutableBedData;
import com.gmail.trentech.pjp.portal.features.Coordinate;
import com.google.common.base.Preconditions;

public class BedData extends AbstractMappedData<String, Coordinate, BedData, ImmutableBedData> {

	public BedData(Map<String, Coordinate> value) {
		super(value, BED_LOCATIONS);
	}

	public BedData() {
		super(new HashMap<>(), BED_LOCATIONS);
	}

	public MapValue<String, Coordinate> portals() {
		return Sponge.getRegistry().getValueFactory().createMapValue(BED_LOCATIONS, getValue());
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
	public BedData put(String key, Coordinate value) {
		getValue().put(key, value);
		return this;
	}

	@Override
	public BedData putAll(Map<? extends String, ? extends Coordinate> map) {
		getValue().putAll(map);
		return this;
	}

	@Override
	public BedData remove(String key) {
		getValue().remove(key);
		return this;
	}

	@Override
	public Optional<BedData> fill(DataHolder dataHolder, MergeFunction mergeFn) {
		BedData coordinateData = Preconditions.checkNotNull(mergeFn).merge(copy(), dataHolder.get(BedData.class).orElse(copy()));
		return Optional.of(set(BED_LOCATIONS, coordinateData.get(BED_LOCATIONS).get()));
	}

	@Override
	public Optional<BedData> from(DataContainer container) {
		if (container.contains(BED_LOCATIONS.getQuery())) {
			HashMap<String, Coordinate> coordinateList = new HashMap<>();

			DataView coordinates = container.getView(BED_LOCATIONS.getQuery()).get();

			for (DataQuery coordinate : coordinates.getKeys(false)) {
				Optional<Coordinate> optionalCoordinate = coordinates.getSerializable(coordinate, Coordinate.class);
				
				if(optionalCoordinate.isPresent()) {
					coordinateList.put(coordinate.toString(), optionalCoordinate.get());
				}
			}
			return Optional.of(new BedData(coordinateList));
		}
		return Optional.empty();
	}

	@Override
	public BedData copy() {
		return new BedData(getValue());
	}

	@Override
	public int getContentVersion() {
		return 0;
	}

	@Override
	public ImmutableBedData asImmutable() {
		return new ImmutableBedData(getValue());
	}

	@Override
	public DataContainer toContainer() {
		return super.toContainer().set(BED_LOCATIONS, getValue());
	}

	public static class Builder extends AbstractDataBuilder<BedData> implements DataManipulatorBuilder<BedData, ImmutableBedData> {

		public Builder() {
			super(BedData.class, 0);
		}

		@Override
		public Optional<BedData> buildContent(DataView container) throws InvalidDataException {
			if (container.contains(BED_LOCATIONS.getQuery())) {
				HashMap<String, Coordinate> coordinateList = new HashMap<>();

				DataView coordinates = container.getView(BED_LOCATIONS.getQuery()).get();

				for (DataQuery coordinate : coordinates.getKeys(false)) {
					Optional<Coordinate> optionalCoordinate = coordinates.getSerializable(coordinate, Coordinate.class);
					
					if(optionalCoordinate.isPresent()) {
						coordinateList.put(coordinate.toString(), optionalCoordinate.get());
					}
				}
				return Optional.of(new BedData(coordinateList));
			}
			return Optional.empty();
		}

		@Override
		public BedData create() {
			return new BedData(new HashMap<String, Coordinate>());
		}

		@Override
		public Optional<BedData> createFrom(DataHolder dataHolder) {
			return create().fill(dataHolder);
		}
	}
}