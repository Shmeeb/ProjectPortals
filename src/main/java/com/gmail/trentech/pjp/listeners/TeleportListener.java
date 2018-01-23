package com.gmail.trentech.pjp.listeners;

import static com.gmail.trentech.pjp.data.Keys.BED_LOCATIONS;
import static com.gmail.trentech.pjp.data.Keys.LAST_LOCATIONS;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.gmail.trentech.pjc.core.ConfigManager;
import com.gmail.trentech.pjc.core.TeleportManager;
import com.gmail.trentech.pjp.Main;
import com.gmail.trentech.pjp.commands.CMDBack;
import com.gmail.trentech.pjp.data.mutable.BedData;
import com.gmail.trentech.pjp.data.mutable.LastLocationData;
import com.gmail.trentech.pjp.effects.Particle;
import com.gmail.trentech.pjp.effects.Particles;
import com.gmail.trentech.pjp.events.TeleportEvent;
import com.gmail.trentech.pjp.portal.features.Coordinate;
import com.gmail.trentech.pjp.utils.Timings;

import ninja.leaping.configurate.ConfigurationNode;

public class TeleportListener {

	private Timings timings;

	public TeleportListener(Timings timings) {
		this.timings = timings;
	}

	@Listener
	public void onInteractBLockEvent(InteractBlockEvent.Secondary event, @Root Player player) {
		if(event.getTargetBlock().getState().getType().equals(BlockTypes.BED)) {
			Map<String, Coordinate> list = new HashMap<>();

			Optional<Map<String, Coordinate>> optionalList = player.get(BED_LOCATIONS);

			if (optionalList.isPresent()) {
				list = optionalList.get();
			}
			
			list.put(player.getWorld().getUniqueId().toString(), new Coordinate(player.getLocation()));
			
			player.offer(new BedData(list));

			player.sendMessage(Text.of(TextColors.DARK_GREEN, "Respawn location Saved."));
		}
	}
	
	@Listener
	public void onTeleportEvent(TeleportEvent event) {
		timings.onTeleportEvent().startTimingIfSync();

		try {
			Player player = event.getPlayer();

			Optional<String> optionalPermission = event.getPermission();
			
			if(optionalPermission.isPresent()) {
				if (!player.hasPermission(optionalPermission.get())) {
					player.sendMessage(Text.of(TextColors.DARK_RED, "Requires permission ", TextColors.YELLOW, optionalPermission.get()));
					event.setCancelled(true);
					return;
				}
			}
			
			Location<World> src = player.getLocation();

			double price = event.getPrice();

			Optional<EconomyService> optionalEconomy = Sponge.getServiceManager().provide(EconomyService.class);

			if (price != 0 && optionalEconomy.isPresent()) {
				EconomyService economy = optionalEconomy.get();

				UniqueAccount account = economy.getOrCreateAccount(player.getUniqueId()).get();

				if (account.withdraw(economy.getDefaultCurrency(), new BigDecimal(price), Cause.of(EventContext.builder().add(EventContextKeys.PLAYER, player).build(), player)).getResult() != ResultType.SUCCESS) {
					player.sendMessage(Text.of(TextColors.DARK_RED, "Not enough money. You need $", new DecimalFormat("#,###,##0.00").format(price)));
					event.setCancelled(true);
					return;
				}

				player.sendMessage(Text.of(TextColors.GREEN, "Charged $", new DecimalFormat("#,###,##0.00").format(price)));
			}

			Particle particle = Particles.getDefaultEffect("teleport");
			particle.spawnParticle(src, true, Particles.getDefaultColor("teleport", particle.isColorable()));
			particle.spawnParticle(src.getRelative(Direction.UP), true, Particles.getDefaultColor("teleport", particle.isColorable()));
		} finally {
			timings.onTeleportEvent().stopTimingIfSync();
		}
	}

	@Listener
	public void onTeleportEventLocal(TeleportEvent.Local event) {
		timings.onTeleportEventLocal().startTimingIfSync();

		try {
			Player player = event.getPlayer();

			Location<World> src = event.getSource();
			src = src.getExtent().getLocation(src.getBlockX(), src.getBlockY(), src.getBlockZ());
			Location<World> dest = event.getDestination();

			Optional<Location<World>> optionalLocation = Optional.empty();
			
			if(event.force()) {
				optionalLocation = Optional.of(dest);
			} else {
				optionalLocation = TeleportManager.getSafeLocation(dest);
				
				if (!optionalLocation.isPresent()) {
					player.sendMessage(Text.of(Text.builder().color(TextColors.RED).append(Text.of("Unsafe spawn point detected. ")).onClick(TextActions.executeCallback(TeleportManager.setUnsafeLocation(dest))).append(Text.of(TextColors.GOLD, TextStyles.UNDERLINE, "Click Here")).build(), TextColors.RED, " or use the -f flag on portal to force teleport."));
					event.setCancelled(true);
					return;
				}
			}

			event.setDestination(optionalLocation.get());

			ConfigurationNode node = ConfigManager.get(Main.getPlugin()).getConfig().getNode("options", "teleport_message");

			if (node.getNode("enable").getBoolean()) {
				Text title = TextSerializers.FORMATTING_CODE.deserialize(node.getNode("title").getString().replaceAll("%WORLD%", dest.getExtent().getName()).replaceAll("\\%X%", Integer.toString(dest.getBlockX())).replaceAll("\\%Y%", Integer.toString(dest.getBlockY())).replaceAll("\\%Z%", Integer.toString(dest.getBlockZ())));
				Text subTitle = TextSerializers.FORMATTING_CODE.deserialize(node.getNode("sub_title").getString().replaceAll("%WORLD%", dest.getExtent().getName()).replaceAll("\\%X%", Integer.toString(dest.getBlockX())).replaceAll("\\%Y%", Integer.toString(dest.getBlockY())).replaceAll("\\%Z%", Integer.toString(dest.getBlockZ())));

				player.sendTitle(Title.of(title, subTitle));
			}

			event.getDestination().getExtent().loadChunk(event.getDestination().getChunkPosition(), true);

			if (player.hasPermission("pjp.cmd.back")) {
				CMDBack.players.put(player, src);
			}
		} finally {
			timings.onTeleportEventLocal().stopTimingIfSync();
		}
	}

	@Listener
	public void onTeleportEventServer(TeleportEvent.Server event) {
		timings.onTeleportEventServer().startTimingIfSync();

		try {
			Player player = event.getPlayer();

			Optional<PluginContainer> optionalPlugin = Sponge.getPluginManager().getPlugin("spongycord");

			if (!optionalPlugin.isPresent()) {
				player.sendMessage(Text.of(TextColors.DARK_RED, "Bungee portals require Spongee plugin dependency"));
				event.setCancelled(true);
				return;
			}
		} finally {
			timings.onTeleportEventServer().stopTimingIfSync();
		}
	}

	@Listener
	public void onMoveEntityEvent(MoveEntityEvent.Teleport event, @Getter("getTargetEntity") Player player) {
		timings.onMoveEntityEvent().startTimingIfSync();

		try {
			if (player.hasPermission("pjp.cmd.back")) {
				CMDBack.players.put(player, event.getFromTransform().getLocation());
			}
			
			World from = event.getFromTransform().getExtent();
			World to = event.getToTransform().getExtent();

			if (!from.equals(to)) {
				Map<String, Coordinate> list = new HashMap<>();

				Optional<Map<String, Coordinate>> optionalList = player.get(LAST_LOCATIONS);

				if (optionalList.isPresent()) {
					list = optionalList.get();
				}
				
				list.put(from.getUniqueId().toString(), new Coordinate(event.getFromTransform().getLocation()));
				
				player.offer(new LastLocationData(list));
			}
		} finally {
			timings.onMoveEntityEvent().stopTimingIfSync();
		}
	}

	@Listener
	public void onDestructEntityEventDeath(DestructEntityEvent.Death event, @Getter("getTargetEntity") Player player) {
		timings.onDestructEntityEventDeath().startTiming();

		try {
			if (player.hasPermission("pjp.cmd.back")) {
				CMDBack.players.put(player, player.getLocation());
			}
		} finally {
			timings.onDestructEntityEventDeath().stopTimingIfSync();
		}
	}
}