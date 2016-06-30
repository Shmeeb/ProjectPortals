package com.gmail.trentech.pjp.listeners;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Optional;

import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
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
import org.spongepowered.api.world.TeleportHelper;
import org.spongepowered.api.world.World;

import com.gmail.trentech.pjp.Main;
import com.gmail.trentech.pjp.commands.CMDBack;
import com.gmail.trentech.pjp.effects.Particle;
import com.gmail.trentech.pjp.effects.Particles;
import com.gmail.trentech.pjp.events.TeleportEvent;
import com.gmail.trentech.pjp.utils.ConfigManager;
import com.gmail.trentech.pjp.utils.Utils;

import ninja.leaping.configurate.ConfigurationNode;

public class TeleportListener {

	@Listener
	public void onTeleportEvent(TeleportEvent event) {
		Player player = event.getPlayer();

		Location<World> src = player.getLocation();

		double price = event.getPrice();

		Optional<EconomyService> optionalEconomy = Main.getGame().getServiceManager().provide(EconomyService.class);

		if (price != 0 && optionalEconomy.isPresent()) {
			EconomyService economy = optionalEconomy.get();

			UniqueAccount account = economy.getOrCreateAccount(player.getUniqueId()).get();

			if (account.withdraw(economy.getDefaultCurrency(), new BigDecimal(price), Cause.of(NamedCause.source(Main.getPlugin()))).getResult() != ResultType.SUCCESS) {
				player.sendMessage(Text.of(TextColors.DARK_RED, "Not enough money. You need $", new DecimalFormat("#,###,##0.00").format(price)));
				event.setCancelled(true);
				return;
			}

			player.sendMessage(Text.of(TextColors.GREEN, "Charged $", new DecimalFormat("#,###,##0.00").format(price)));
		}

		Particle particle = Particles.getDefaultEffect("teleport");
		particle.spawnParticle(src, true, Particles.getDefaultColor("teleport", particle.isColorable()));
		particle.spawnParticle(src.getRelative(Direction.UP), true, Particles.getDefaultColor("teleport", particle.isColorable()));
	}

	@Listener
	public void onTeleportEventLocal(TeleportEvent.Local event) {
		Player player = event.getPlayer();

		Location<World> src = event.getSource();
		src = src.getExtent().getLocation(src.getBlockX(), src.getBlockY(), src.getBlockZ());
		Location<World> dest = event.getDestination();

		if (!player.hasPermission("pjp.worlds." + dest.getExtent().getName()) && !player.hasPermission("pjw.worlds." + dest.getExtent().getName())) {
			player.sendMessage(Text.of(TextColors.DARK_RED, "You do not have permission to travel to ", dest.getExtent().getName()));
			event.setCancelled(true);
			return;
		}

		TeleportHelper teleportHelper = Main.getGame().getTeleportHelper();

		Optional<Location<World>> optionalLocation = teleportHelper.getSafeLocation(dest);

		if (!optionalLocation.isPresent()) {
			player.sendMessage(Text.builder().color(TextColors.DARK_RED).append(Text.of("Unsafe spawn point detected. Teleport anyway? ")).onClick(TextActions.executeCallback(Utils.unsafeTeleport(dest))).append(Text.of(TextColors.GOLD, TextStyles.UNDERLINE, "Click Here")).build());
			event.setCancelled(true);
			return;
		}

		ConfigurationNode config = new ConfigManager().getConfig();

		Text title = TextSerializers.FORMATTING_CODE.deserialize(config.getNode("options", "teleport_message", "title").getString().replaceAll("%WORLD%", dest.getExtent().getName()).replaceAll("\\%X%", Integer.toString(dest.getBlockX())).replaceAll("\\%Y%", Integer.toString(dest.getBlockY())).replaceAll("\\%Z%", Integer.toString(dest.getBlockZ())));
		Text subTitle = TextSerializers.FORMATTING_CODE.deserialize(config.getNode("options", "teleport_message", "sub_title").getString().replaceAll("%WORLD%", dest.getExtent().getName()).replaceAll("\\%X%", Integer.toString(dest.getBlockX())).replaceAll("\\%Y%", Integer.toString(dest.getBlockY())).replaceAll("\\%Z%", Integer.toString(dest.getBlockZ())));

		player.sendTitle(Title.of(title, subTitle));

		if (player.hasPermission("pjp.cmd.back")) {
			CMDBack.players.put(player, src);
		}
	}

	@Listener
	public void onTeleportEventServer(TeleportEvent.Server event) {
		Player player = event.getPlayer();

		Optional<PluginContainer> optionalPlugin = Main.getGame().getPluginManager().getPlugin("spongee");

		if (!optionalPlugin.isPresent()) {
			player.sendMessage(Text.of(TextColors.DARK_RED, "Bungee portals require Spongee plugin dependency"));
			event.setCancelled(true);
			return;
		}
	}

	@Listener
	public void onMoveEntityEvent(MoveEntityEvent.Teleport event) {
		Entity entity = event.getTargetEntity();

		if (!(entity instanceof Player)) {
			return;
		}
		Player player = (Player) entity;

		if (player.hasPermission("pjp.cmd.back")) {
			CMDBack.players.put(player, event.getFromTransform().getLocation());
		}
	}

	@Listener
	public void onDestructEntityEvent(DestructEntityEvent.Death event) {
		Living entity = event.getTargetEntity();

		if (!(entity instanceof Player)) {
			return;
		}
		Player player = (Player) entity;

		if (player.hasPermission("pjp.cmd.back")) {
			CMDBack.players.put(player, player.getLocation());
		}
	}
}