package net.kaikk.mc.sponge.simplepermissions.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.Tristate;

import net.kaikk.mc.sponge.simplepermissions.SimplePermissions;

public class OpCommand implements CommandExecutor {
	private SimplePermissions instance;
	
	public OpCommand(SimplePermissions instance) {
		this.instance = instance;
	}
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if (!args.<User>getOne("user").isPresent()) {
			src.sendMessage(Text.of(TextColors.RED, "Invalid user"));
			return CommandResult.empty();
		}
		
		User user = args.<User>getOne("user").get();
		
		instance.setPermission(user, "*", Tristate.TRUE);
		
		if (user.getPlayer().isPresent()) {
			user.getPlayer().get().sendMessage(Text.of(TextColors.GOLD, TextStyles.BOLD, "You are now opped"));
			if (user == src) {
				return CommandResult.success();
			}
		}
		
		src.sendMessage(Text.of(TextColors.GOLD, "User ", TextColors.AQUA, user.getName(), TextColors.GOLD, " opped"));
		
		return CommandResult.success();
	}
}
