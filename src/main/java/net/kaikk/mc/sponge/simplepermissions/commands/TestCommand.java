package net.kaikk.mc.sponge.simplepermissions.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import net.kaikk.mc.sponge.simplepermissions.SimplePermissions;

public class TestCommand implements CommandExecutor {
	private SimplePermissions instance;
	
	public TestCommand(SimplePermissions instance) {
		this.instance = instance;
	}
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		String permission = args.<String>getOne("permission").get();
		src.sendMessage(Text.of(TextColors.AQUA, "User ", TextColors.GOLD, src.getName(), TextColors.AQUA, " permission ", TextColors.GOLD, permission, TextColors.AQUA, " value is ", TextColors.GOLD, instance.getUserSubjects().get(src.getIdentifier()).getPermissionValue(null, permission)));
		return CommandResult.success();
	}
}
