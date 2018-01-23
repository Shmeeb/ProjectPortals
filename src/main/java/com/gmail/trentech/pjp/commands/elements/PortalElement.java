package com.gmail.trentech.pjp.commands.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import com.gmail.trentech.pjp.portal.Portal;
import com.gmail.trentech.pjp.portal.PortalService;
import com.gmail.trentech.pjp.portal.Portal.PortalType;

public class PortalElement extends CommandElement {

	private PortalType type;
	
    public PortalElement(Text key, PortalType type) {
        super(key);
        this.type = type;
    }

    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
    	final String next = args.next().toLowerCase();
    	
    	Optional<Portal> optionalPortal = Sponge.getServiceManager().provide(PortalService.class).get().get(next, type);

        if(optionalPortal.isPresent()) {
        	return optionalPortal.get();
        }
        
		throw args.createError(Text.of(TextColors.RED, "Portal not found"));
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
    	List<String> list = new ArrayList<>();
    	
    	Optional<String> next = args.nextIfPresent();
    	
    	PortalService portalService = Sponge.getServiceManager().provide(PortalService.class).get();
    	
    	if(next.isPresent()) {
            for(Portal portal : portalService.all(type)) {
            	if(portal.getName().startsWith(next.get().toLowerCase())) {
            		list.add(portal.getName());
            	}
            }
    	} else {
            for(Portal portal : portalService.all(type)) {
            	list.add(portal.getName());
            }
    	}

        return list;
    }

    @Override
    public Text getUsage(CommandSource src) {
        return Text.of(getKey());
    }
}
