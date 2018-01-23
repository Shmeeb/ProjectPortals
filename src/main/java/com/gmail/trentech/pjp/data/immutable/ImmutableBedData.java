package com.gmail.trentech.pjp.data.immutable;

import static com.gmail.trentech.pjp.data.Keys.BED_LOCATIONS;

import java.util.Map;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableMappedData;
import org.spongepowered.api.data.value.immutable.ImmutableMapValue;

import com.gmail.trentech.pjp.data.mutable.BedData;
import com.gmail.trentech.pjp.portal.features.Coordinate;

public class ImmutableBedData extends AbstractImmutableMappedData<String, Coordinate, ImmutableBedData, BedData> {

	public ImmutableBedData(Map<String, Coordinate> value) {
		super(value, BED_LOCATIONS);
	}

	public ImmutableMapValue<String, Coordinate> homes() {
		return Sponge.getRegistry().getValueFactory().createMapValue(BED_LOCATIONS, getValue()).asImmutable();
	}

	@Override
	public int getContentVersion() {
		return 0;
	}

	@Override
	public BedData asMutable() {
		return new BedData(this.getValue());
	}

	@Override
	public DataContainer toContainer() {
		return super.toContainer().set(BED_LOCATIONS, getValue());
	}
}
