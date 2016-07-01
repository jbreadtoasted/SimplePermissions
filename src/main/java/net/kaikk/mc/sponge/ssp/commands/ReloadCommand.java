package net.kaikk.mc.sponge.ssp.commands;

import java.io.IOException;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import net.kaikk.mc.sponge.ssp.SimpleSpongePermissions;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class ReloadCommand implements CommandExecutor {
	private SimpleSpongePermissions instance;
	
	public ReloadCommand(SimpleSpongePermissions instance) {
		this.instance = instance;
	}
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		try {
			instance.loadData();
			src.sendMessage(Text.of(TextColors.GREEN, "SimpleSpongePermissions reloaded."));
			return CommandResult.success();
		} catch (IOException | ObjectMappingException e) {
			src.sendMessage(Text.of(TextColors.RED, "An error occurred while reloading the plugin."));
			e.printStackTrace();
			return CommandResult.empty();
		}
	}
}
