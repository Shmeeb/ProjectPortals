package com.gmail.trentech.pjp.listeners;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;
import com.gmail.trentech.pjp.Main;
import com.gmail.trentech.pjp.events.TeleportEvent;
import com.gmail.trentech.pjp.portals.Button;
import com.gmail.trentech.pjp.utils.ConfigManager;
import com.gmail.trentech.pjp.utils.Utils;

public class ButtonListener {

	public static ConcurrentHashMap<Player, String> builders = new ConcurrentHashMap<>();

	@Listener
	public void onChangeBlockEvent(ChangeBlockEvent.Modify event, @First Player player) {
		for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
			BlockSnapshot snapshot = transaction.getFinal();
			BlockState block = snapshot.getExtendedState();
			BlockType blockType = block.getType();
			
			if(!blockType.equals(BlockTypes.STONE_BUTTON) && !blockType.equals(BlockTypes.WOODEN_BUTTON)){
				return;
			}

			
			if(!block.get(Keys.POWERED).isPresent()){
				return;
			}

			if(!block.get(Keys.POWERED).get()){
				return;
			}

			Location<World> location = snapshot.getLocation().get();		

			Optional<Button> optionalButton = Button.get(location);
			
			if(!optionalButton.isPresent()){
				return;
			}
			
			Button button = optionalButton.get();

			if(!player.hasPermission("pjp.button.interact")){
				player.sendMessage(Text.of(TextColors.DARK_RED, "you do not have permission to interact with button portals"));
				event.setCancelled(true);
				return;
			}

			Optional<Location<World>> optionalSpawnLocation = button.getDestination();
			
			if(!optionalSpawnLocation.isPresent()){
				player.sendMessage(Text.of(TextColors.DARK_RED, "World does not exist"));
				return;
			}
			Location<World> spawnLocation = optionalSpawnLocation.get();

			TeleportEvent teleportEvent = new TeleportEvent(player, player.getLocation(), spawnLocation, Cause.of("button"));

			if(!Main.getGame().getEventManager().post(teleportEvent)){
				spawnLocation = teleportEvent.getDestination();
				
				Optional<Vector3d> optionalRotation = button.getRotation();
				
				if(optionalRotation.isPresent()){
					player.setLocationAndRotation(spawnLocation, optionalRotation.get());
				}else{
					player.setLocation(spawnLocation);
				}
			}
		}
	}
	
	@Listener
	public void onChangeBlockEvent(ChangeBlockEvent.Break event, @First Player player) {
		for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
			Location<World> location = transaction.getFinal().getLocation().get();		

			if(!Button.get(location).isPresent()){
				continue;
			}
			
			if(!player.hasPermission("pjp.button.break")){
				player.sendMessage(Text.of(TextColors.DARK_RED, "you do not have permission to break button portals"));
				event.setCancelled(true);
			}else{
				Button.remove(location);
				player.sendMessage(Text.of(TextColors.DARK_GREEN, "Broke button portal"));
				
			}
			return;
		}
	}

	@Listener
	public void onChangeBlockEvent(ChangeBlockEvent.Place event, @First Player player) {
		if(!builders.containsKey(player)){
			return;
		}

		for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
			BlockType blockType = transaction.getFinal().getState().getType();
			
			if(!blockType.equals(BlockTypes.STONE_BUTTON) && !blockType.equals(BlockTypes.WOODEN_BUTTON)){
				continue;
			}

			Location<World> location = transaction.getFinal().getLocation().get();

			if(!player.hasPermission("pjp.button.place")){
	        	player.sendMessage(Text.of(TextColors.DARK_RED, "you do not have permission to place button portals"));
	        	builders.remove(player);
	        	return;
			}

            String destination = builders.get(player);
            
            Button.save(location, destination);

    		if(new ConfigManager().getConfig().getNode("options", "particles").getBoolean()){
    			Utils.spawnParticles(location, 1.0, false);
    		}

            player.sendMessage(Text.of(TextColors.DARK_GREEN, "New button portal created"));
            
            builders.remove(player);
		}
	}
}
