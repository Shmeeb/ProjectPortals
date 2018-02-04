package com.gmail.trentech.pjp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import com.gmail.trentech.pjc.core.ConfigManager;
import com.gmail.trentech.pjp.commands.CMDBack;
import com.gmail.trentech.pjp.data.Keys;
import com.gmail.trentech.pjp.data.immutable.ImmutableBedData;
import com.gmail.trentech.pjp.data.immutable.ImmutableHomeData;
import com.gmail.trentech.pjp.data.immutable.ImmutableLastLocationData;
import com.gmail.trentech.pjp.data.immutable.ImmutableSignPortalData;
import com.gmail.trentech.pjp.data.mutable.BedData;
import com.gmail.trentech.pjp.data.mutable.HomeData;
import com.gmail.trentech.pjp.data.mutable.LastLocationData;
import com.gmail.trentech.pjp.data.mutable.SignPortalData;
import com.gmail.trentech.pjp.init.Commands;
import com.gmail.trentech.pjp.init.Common;
import com.gmail.trentech.pjp.listeners.ButtonListener;
import com.gmail.trentech.pjp.listeners.DoorListener;
import com.gmail.trentech.pjp.listeners.LegacyListener;
import com.gmail.trentech.pjp.listeners.LeverListener;
import com.gmail.trentech.pjp.listeners.PlateListener;
import com.gmail.trentech.pjp.listeners.PortalListener;
import com.gmail.trentech.pjp.listeners.SignListener;
import com.gmail.trentech.pjp.listeners.TeleportListener;
import com.gmail.trentech.pjp.portal.Portal;
import com.gmail.trentech.pjp.portal.PortalService;
import com.gmail.trentech.pjp.portal.features.Command;
import com.gmail.trentech.pjp.portal.features.Coordinate;
import com.gmail.trentech.pjp.portal.features.Properties;
import com.gmail.trentech.pjp.utils.Resource;
import com.gmail.trentech.pjp.utils.Timings;
import com.google.inject.Inject;

import ninja.leaping.configurate.ConfigurationNode;

@Plugin(id = Resource.ID, name = Resource.NAME, version = Resource.VERSION, description = Resource.DESCRIPTION, authors = Resource.AUTHOR, url = Resource.URL, dependencies = { @Dependency(id = "pjc", optional = false) })
public class Main {

	@Inject
	@ConfigDir(sharedRoot = false)
	private Path path;

	@Inject
	private Logger log;

	private static PluginContainer plugin;
	private static Main instance;

	@Listener
	public void onPreInitializationEvent(GamePreInitializationEvent event) {
		plugin = Sponge.getPluginManager().getPlugin(Resource.ID).get();
		instance = this;

		try {
			Files.createDirectories(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		@SuppressWarnings("unused")
		Object key = Keys.BED_LOCATIONS;
	}

	@Listener
	public void onInitialization(GameInitializationEvent event) {
		Common.initConfig();
		
		ConfigurationNode config = ConfigManager.get(getPlugin()).getConfig();

		Timings timings = new Timings();

		DataRegistration.builder().dataClass(BedData.class).immutableClass(ImmutableBedData.class).builder(new BedData.Builder()).dataName("bed")
			.manipulatorId("pjp_bed").buildAndRegister(Main.getPlugin());
		DataRegistration.builder().dataClass(LastLocationData.class).immutableClass(ImmutableLastLocationData.class).builder(new LastLocationData.Builder()).dataName("last_location")
			.manipulatorId("pjp_last_location").buildAndRegister(Main.getPlugin());
	
		Sponge.getDataManager().registerBuilder(Coordinate.class, new Coordinate.Builder());
		Sponge.getDataManager().registerBuilder(Command.class, new Command.Builder());
		Sponge.getDataManager().registerBuilder(Properties.class, new Properties.Builder());
		
		Sponge.getDataManager().registerBuilder(Portal.Local.class, new Portal.Local.Builder());	
		Sponge.getDataManager().registerBuilder(Portal.Server.class, new Portal.Server.Builder());

		Sponge.getEventManager().registerListeners(this, new TeleportListener(timings));
		
		Sponge.getServiceManager().setProvider(getPlugin(), PortalService.class, new PortalService());

		ConfigurationNode modules = config.getNode("settings", "modules");
		
		if (modules.getNode("back").getBoolean()) {
			Sponge.getCommandManager().register(this, new CMDBack().cmdBack, "back");
		}
		if (modules.getNode("buttons").getBoolean()) {
			Sponge.getEventManager().registerListeners(this, new ButtonListener(timings));
			Sponge.getCommandManager().register(this, new Commands().cmdButton, "button", "b");

			getLog().info("Button module activated");
		}
		if (modules.getNode("doors").getBoolean()) {
			Sponge.getEventManager().registerListeners(this, new DoorListener(timings));
			Sponge.getCommandManager().register(this, new Commands().cmdDoor, "door", "d");

			getLog().info("Door module activated");
		}
		if (modules.getNode("plates").getBoolean()) {
			Sponge.getEventManager().registerListeners(this, new PlateListener(timings));
			Sponge.getCommandManager().register(this, new Commands().cmdPlate, "plate", "pp");

			getLog().info("Pressure plate module activated");
		}
		if (modules.getNode("signs").getBoolean()) {
			DataRegistration<SignPortalData, ImmutableSignPortalData> signData = DataRegistration.builder().dataClass(SignPortalData.class).immutableClass(ImmutableSignPortalData.class)
				.builder(new SignPortalData.Builder()).dataName("sign").manipulatorId("pjp_sign").buildAndRegister(Main.getPlugin());
			
			Sponge.getDataManager().registerLegacyManipulatorIds("com.gmail.trentech.pjp.data.mutable.SignPortalData", signData);
			
			Sponge.getEventManager().registerListeners(this, new SignListener(timings));
			Sponge.getCommandManager().register(this, new Commands().cmdSign, "sign", "s");

			getLog().info("Sign module activated");
		}
		if (modules.getNode("levers").getBoolean()) {
			Sponge.getEventManager().registerListeners(this, new LeverListener(timings));
			Sponge.getCommandManager().register(this, new Commands().cmdLever, "lever", "l");

			getLog().info("Lever module activated");
		}
		
		if (modules.getNode("portals").getBoolean()) {
			Sponge.getEventManager().registerListeners(this, new PortalListener(timings));
			Sponge.getCommandManager().register(this, new Commands().cmdPortal, "portal", "p");

			if (config.getNode("options", "portal", "legacy_builder").getBoolean()) {
				Sponge.getEventManager().registerListeners(this, new LegacyListener(timings));
			}

			getLog().info("Portal module activated");
		}
		
		if (modules.getNode("homes").getBoolean()) {
			DataRegistration<HomeData, ImmutableHomeData> homeData = DataRegistration.builder().dataClass(HomeData.class).immutableClass(ImmutableHomeData.class)
				.builder(new HomeData.Builder()).dataName("home").manipulatorId("pjp_home").buildAndRegister(Main.getPlugin());

			Sponge.getDataManager().registerLegacyManipulatorIds("com.gmail.trentech.pjp.data.mutable.HomeData", homeData);
			
			Sponge.getCommandManager().register(this, new Commands().cmdHome, "home", "h");

			getLog().info("Home module activated");
		}
		if (modules.getNode("warps").getBoolean()) {
			Sponge.getCommandManager().register(this, new Commands().cmdWarp, "warp", "w");

			getLog().info("Warp module activated");
		}

		Common.initHelp();
		Common.initData();
	}

	@Listener
	public void onStartedServer(GameStartedServerEvent event) {
		Sponge.getServiceManager().provide(PortalService.class).get().init();
	}

	@Listener
	public void onReloadEvent(GameReloadEvent event) {
		Sponge.getEventManager().unregisterPluginListeners(getPlugin());

		for (CommandMapping mapping : Sponge.getCommandManager().getOwnedBy(getPlugin())) {
			Sponge.getCommandManager().removeMapping(mapping);
		}

		Common.initConfig();
		
		ConfigurationNode config = ConfigManager.get(getPlugin()).getConfig();

		Timings timings = new Timings();

		Sponge.getEventManager().registerListeners(this, new TeleportListener(timings));

		ConfigurationNode modules = config.getNode("settings", "modules");

		if (modules.getNode("back").getBoolean()) {
			Sponge.getCommandManager().register(this, new CMDBack().cmdBack, "back");
		}
		if (modules.getNode("portals").getBoolean()) {
			Sponge.getEventManager().registerListeners(this, new PortalListener(timings));
			Sponge.getCommandManager().register(this, new Commands().cmdPortal, "portal", "p");

			if (config.getNode("options", "portal", "legacy_builder").getBoolean()) {
				Sponge.getEventManager().registerListeners(this, new LegacyListener(timings));
			}

			getLog().info("Portal module activated");
		}
		if (modules.getNode("buttons").getBoolean()) {
			Sponge.getEventManager().registerListeners(this, new ButtonListener(timings));
			Sponge.getCommandManager().register(this, new Commands().cmdButton, "button", "b");

			getLog().info("Button module activated");
		}
		if (modules.getNode("doors").getBoolean()) {
			Sponge.getEventManager().registerListeners(this, new DoorListener(timings));
			Sponge.getCommandManager().register(this, new Commands().cmdDoor, "door", "d");

			getLog().info("Door module activated");
		}
		if (modules.getNode("plates").getBoolean()) {
			Sponge.getEventManager().registerListeners(this, new PlateListener(timings));
			Sponge.getCommandManager().register(this, new Commands().cmdPlate, "plate", "pp");

			getLog().info("Pressure plate module activated");
		}
		if (modules.getNode("signs").getBoolean()) {
			Sponge.getEventManager().registerListeners(this, new SignListener(timings));
			Sponge.getCommandManager().register(this, new Commands().cmdSign, "sign", "s");

			getLog().info("Sign module activated");
		}
		if (modules.getNode("levers").getBoolean()) {
			Sponge.getEventManager().registerListeners(this, new LeverListener(timings));
			Sponge.getCommandManager().register(this, new Commands().cmdLever, "lever", "l");

			getLog().info("Lever module activated");
		}
		if (modules.getNode("homes").getBoolean()) {
			Sponge.getCommandManager().register(this, new Commands().cmdHome, "home", "h");

			getLog().info("Home module activated");
		}
		if (modules.getNode("warps").getBoolean()) {
			Sponge.getCommandManager().register(this, new Commands().cmdWarp, "warp", "w");

			getLog().info("Warp module activated");
		}

		Sponge.getServiceManager().provide(PortalService.class).get().init();
	}

	public Logger getLog() {
		return log;
	}

	public Path getPath() {
		return path;
	}

	public static PluginContainer getPlugin() {
		return plugin;
	}

	public static Main instance() {
		return instance;
	}

}