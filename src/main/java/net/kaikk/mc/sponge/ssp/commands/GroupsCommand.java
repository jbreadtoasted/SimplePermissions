package net.kaikk.mc.sponge.ssp.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import net.kaikk.mc.sponge.ssp.SimpleSpongePermissions;

public class GroupsCommand implements CommandExecutor {
	private SimpleSpongePermissions instance;
	
	public GroupsCommand(SimpleSpongePermissions instance) {
		this.instance = instance;
	}
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		StringBuilder sb = new StringBuilder();
		for (Subject s : instance.getGroupSubjects().getAllSubjects()) {
			sb.append(s.getIdentifier());
			sb.append(", ");
		}
		
		if (sb.length()!=0) {
			sb.setLength(sb.length()-2);
		}
		
		if (!sb.toString().contains("default")) {
			sb.append(", default");
		}
		
		src.sendMessage(Text.of(TextColors.GREEN, "Available groups: ", TextColors.AQUA, sb.toString()));
		return CommandResult.success();
	}
}
