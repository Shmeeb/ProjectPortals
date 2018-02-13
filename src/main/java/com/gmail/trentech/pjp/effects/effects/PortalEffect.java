package com.gmail.trentech.pjp.effects.effects;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleType;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.gmail.trentech.pjp.Main;
import com.gmail.trentech.pjp.portal.Portal;
import com.gmail.trentech.pjp.portal.features.Properties;

public class PortalEffect implements Effect {

	private ParticleType particleType = ParticleTypes.PORTAL;
	private long time = 10;

	@Override
	public void activate(Portal portal) {
		Optional<Properties> optionalProperties = portal.getProperties();
		
		if(!optionalProperties.isPresent()) {
			return;
		}
		Properties properties = optionalProperties.get();
		
		Sponge.getScheduler().createTaskBuilder().interval(time, TimeUnit.MILLISECONDS).name(portal.getName()).execute(t -> {
			ParticleEffect particle = ParticleEffect.builder().type(particleType).build();

			for (Location<World> location : properties.getFill()) {
				Optional<Chunk> optionalChunk = location.getExtent().getChunk(location.getChunkPosition());
				
				if(optionalChunk.isPresent() && optionalChunk.get().isLoaded()) {
					location.getExtent().spawnParticles(particle, location.getPosition().add(random.nextDouble(), random.nextDouble(), random.nextDouble()));
					location.getExtent().spawnParticles(particle, location.getPosition().add(random.nextDouble(), random.nextDouble(), random.nextDouble()));
				}
			}
		}).submit(Main.getPlugin());
	}

	@Override
	public void burst(Location<World> location, boolean player) {
		ParticleEffect particle = ParticleEffect.builder().type(particleType).build();

		for (int i = 0; i < 9; i++) {
			if (player) {
				location.getExtent().spawnParticles(particle, location.getPosition().add(random.nextDouble() - .5, random.nextDouble() - .5, random.nextDouble() - .5));
				location.getExtent().spawnParticles(particle, location.getPosition().add(random.nextDouble() - .5, random.nextDouble() - .5, random.nextDouble() - .5));
			} else {
				location.getExtent().spawnParticles(particle, location.getPosition().add(random.nextDouble(), random.nextDouble(), random.nextDouble()));
				location.getExtent().spawnParticles(particle, location.getPosition().add(random.nextDouble(), random.nextDouble(), random.nextDouble()));
			}
		}
	}

	@Override
	public void deactivate(Portal portal) {
		for (Task task : Sponge.getScheduler().getScheduledTasks()) {
			if (task.getName().equals(portal.getName())) {
				task.cancel();
				break;
			}
		}
	}

}
