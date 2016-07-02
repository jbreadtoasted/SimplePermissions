package net.kaikk.mc.sponge.simplepermissions.commands;

import java.io.IOException;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import net.kaikk.mc.sponge.simplepermissions.SimplePermissions;
import net.kaikk.mc.sponge.simplepermissions.subject.UserSubject;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class UserCommand implements CommandExecutor {
	private SimplePermissions instance;
	
	public UserCommand(SimplePermissions instance) {
		this.instance = instance;
	}

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if (!args.<User>getOne("user").isPresent()) {
			src.sendMessage(Text.of(TextColors.RED, "Invalid user"));
			return CommandResult.empty();
		}
		
		User user = args.<User>getOne("user").get();
		
		if (!args.<String>getOne("choice").isPresent()) {
			this.userInfo(src, user);
			return CommandResult.success();
		}
		
		String param = args.<String>getOne("param").orElse(null);
		try {
			switch(args.<String>getOne("choice").get()) {
			case "add": this.add(src, user, param); break;
			case "remove": this.remove(src, user, param); break;
			case "addgroup": this.addGroup(src, user, param); break;
			case "removegroup": this.removeGroup(src, user, param); break;
			case "setgroup": this.setGroup(src, user, param); break;
			case "test": this.test(src, user, param); break;
			default: return CommandResult.empty();
			}
		} catch (Throwable e) {
			throw new CommandException(Text.of(TextColors.RED, "An error occurred: ", e.getMessage()), e);
		}
		
		return CommandResult.success();
	}
	
	public void userInfo(CommandSource src, User user) {
		src.sendMessage(((UserSubject) instance.getUserSubjects().get(user.getIdentifier())).info());
	}
	
	public void add(CommandSource src, User user, String permission) {
		if (permission==null) {
			src.sendMessage(Text.of(TextColors.RED, "Undefined permission"));
			return;
		}
		Tristate t;
		if (permission.startsWith("-")) {
			permission = permission.substring(1);
			t = Tristate.FALSE;
		} else {
			t = Tristate.TRUE;
		}
		instance.setPermission(user, permission, t);
		
		src.sendMessage(Text.of(TextColors.AQUA, user.getName(), "'s permission ", TextColors.GOLD, permission, TextColors.AQUA, " set to ", TextColors.GOLD, t));
	}
	
	public void remove(CommandSource src, User user, String permission) {
		if (permission==null) {
			src.sendMessage(Text.of(TextColors.RED, "Undefined permission"));
			return;
		}
		if (permission.startsWith("-")) {
			permission = permission.substring(1);
		}
		instance.setPermission(user, permission, Tristate.UNDEFINED);
		src.sendMessage(Text.of(TextColors.AQUA, user.getName(), "'s permission ", TextColors.GOLD, permission, TextColors.AQUA, " removed"));
	}
	
	public void addGroup(CommandSource src, User user, String group) {
		try {
			instance.addUserToGroup(user, group);
			src.sendMessage(Text.of(TextColors.AQUA, user.getName(), " added to group ", TextColors.GOLD, group));
		} catch (IllegalArgumentException e) {
			src.sendMessage(Text.of(TextColors.RED, "The specified group doesn't exist!"));
		} catch (IOException | ObjectMappingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void removeGroup(CommandSource src, User user, String group) {
		try {
			instance.removeUserFromGroup(user, group);
			src.sendMessage(Text.of(TextColors.AQUA, user.getName(), " removed from group ", TextColors.GOLD, group));
		} catch (IllegalArgumentException e) {
			src.sendMessage(Text.of(TextColors.RED, "The specified group doesn't exist!"));
		} catch (IOException | ObjectMappingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void setGroup(CommandSource src, User user, String group) {
		try {
			instance.setUserToGroup(user, group);
			src.sendMessage(Text.of(TextColors.AQUA, user.getName(), " set to group ", TextColors.GOLD, group));
		} catch (IllegalArgumentException e) {
			src.sendMessage(Text.of(TextColors.RED, "The specified group doesn't exist!"));
		} catch (IOException | ObjectMappingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void test(CommandSource src, User user, String permission) {
		if (permission==null) {
			src.sendMessage(Text.of(TextColors.RED, "Undefined permission"));
			return;
		}
		src.sendMessage(Text.of(TextColors.AQUA, "User ", TextColors.GOLD, user.getName(), TextColors.AQUA, " permission ", TextColors.GOLD, permission, TextColors.AQUA, " value is ", TextColors.GOLD, instance.getUserSubjects().get(user.getIdentifier()).getPermissionValue(null, permission)));
	}
}
