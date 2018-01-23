package com.gmail.trentech.pjp.data;

import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.mutable.MapValue;
import org.spongepowered.api.data.value.mutable.Value;

import com.gmail.trentech.pjp.portal.Portal;
import com.gmail.trentech.pjp.portal.features.Coordinate;
import com.google.common.reflect.TypeToken;

public class Keys {

	private static final TypeToken<MapValue<String, Portal>> PORTALS_MAP_VALUE_TOKEN = new TypeToken<MapValue<String, Portal>>() {
		private static final long serialVersionUID = -1;
	};
	private static final TypeToken<Value<Portal>> PORTAL_VALUE_TOKEN = new TypeToken<Value<Portal>>() {
		private static final long serialVersionUID = -1;
	};
	private static final TypeToken<MapValue<String, Coordinate>> BED_LOCATIONS_MAP_VALUE_TOKEN = new TypeToken<MapValue<String, Coordinate>>() {
		private static final long serialVersionUID = -1;
	};
	private static final TypeToken<MapValue<String, Coordinate>> LAST_LOCATIONS_MAP_VALUE_TOKEN = new TypeToken<MapValue<String, Coordinate>>() {
		private static final long serialVersionUID = -1;
	};
	public static final Key<Value<Portal>> PORTAL = Key.builder().type(PORTAL_VALUE_TOKEN).id("portal").name("portal").query(DataQuery.of("portal")).build();
	public static final Key<MapValue<String, Portal>> PORTALS = Key.builder().type(PORTALS_MAP_VALUE_TOKEN).id("portals").name("portals").query(DataQuery.of("portals")).build();
	public static final Key<MapValue<String, Coordinate>> BED_LOCATIONS = Key.builder().type(BED_LOCATIONS_MAP_VALUE_TOKEN).id("bed_locations").name("bed_locations").query(DataQuery.of("bed_locations")).build();
	public static final Key<MapValue<String, Coordinate>> LAST_LOCATIONS = Key.builder().type(LAST_LOCATIONS_MAP_VALUE_TOKEN).id("last_locations").name("last_locations").query(DataQuery.of("last_locations")).build();
}
