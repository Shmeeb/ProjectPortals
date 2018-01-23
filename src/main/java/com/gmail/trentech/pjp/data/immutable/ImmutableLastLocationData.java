package com.gmail.trentech.pjp.data.immutable;

import static com.gmail.trentech.pjp.data.Keys.LAST_LOCATIONS;

import java.util.Map;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableMappedData;
import org.spongepowered.api.data.value.immutable.ImmutableMapValue;

import com.gmail.trentech.pjp.data.mutable.LastLocationData;
import com.gmail.trentech.pjp.portal.features.Coordinate;

public class ImmutableLastLocationData extends AbstractImmutableMappedData<String, Coordinate, ImmutableLastLocationData, LastLocationData> {

	public ImmutableLastLocationData(Map<String, Coordinate> value) {
		super(value, LAST_LOCATIONS);
	}

	public ImmutableMapValue<String, Coordinate> lastLocations() {
		return Sponge.getRegistry().getValueFactory().createMapValue(LAST_LOCATIONS, getValue()).asImmutable();
	}

	@Override
	public int getContentVersion() {
		return 0;
	}

	@Override
	public LastLocationData asMutable() {
		return new LastLocationData(this.getValue());
	}

	@Override
	public DataContainer toContainer() {
		return super.toContainer().set(LAST_LOCATIONS, getValue());
	}
}
