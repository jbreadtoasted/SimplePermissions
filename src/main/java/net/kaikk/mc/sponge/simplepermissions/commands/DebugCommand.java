package net.kaikk.mc.sponge.simplepermissions.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import net.kaikk.mc.sponge.simplepermissions.SimplePermissions;

public class DebugCommand implements CommandExecutor {
	private SimplePermissions instance;
	
	public DebugCommand(SimplePermissions instance) {
		this.instance = instance;
	}
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		instance.debug = args.<Boolean>getOne("truefalse").get();
		src.sendMessage(Text.of(TextColors.RED, "Debug mode is now ", (instance.debug ? "enabled. Check the server log." : "disabled.")));
		return CommandResult.success();
	}
}
