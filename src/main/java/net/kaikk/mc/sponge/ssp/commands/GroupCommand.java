package net.kaikk.mc.sponge.ssp.commands;

import java.io.IOException;

import javax.annotation.Nullable;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import net.kaikk.mc.sponge.ssp.SimpleSpongePermissions;
import net.kaikk.mc.sponge.ssp.subject.GroupSubject;
import net.kaikk.mc.sponge.ssp.subject.GroupSubjectCollection;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class GroupCommand implements CommandExecutor {
	private SimpleSpongePermissions instance;
	
	public GroupCommand(SimpleSpongePermissions instance) {
		this.instance = instance;
	}
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		String groupName = args.<String>getOne("group").get();
		GroupSubject group = (GroupSubject) instance.getGroupSubjects().get(groupName);
		
		if (!args.<String>getOne("choice").isPresent()) {
			this.groupInfo(src, group);
			return CommandResult.success();
		}
		
		String choice = args.<String>getOne("choice").get();
		if (choice.equals("create")) {
			this.create(src, group);
			return CommandResult.success();
		}
		
		if (!instance.getGroupSubjects().hasRegistered(groupName) && !groupName.equals("default")) {
			src.sendMessage(Text.of(TextColors.RED, "Invalid group"));
			return CommandResult.empty();
		}
		
		String param = args.<String>getOne("param").orElse(null);
		switch(choice) {
		case "add": this.add(src, group, param); break;
		case "remove": this.remove(src, group, param); break;
		case "parent": this.parent(src, group, param); break;
		case "weight": this.weight(src, group, args.<Integer>getOne("weight").orElse(null)); break;
		case "test": this.test(src, group, param); break;
		case "delete": this.delete(src, group); break;
		default: return CommandResult.empty();
		}
		
		return CommandResult.success();
	}

	private void groupInfo(CommandSource src, GroupSubject group) {
		src.sendMessage(group.info());
	}

	private void add(CommandSource src, GroupSubject group, String permission) {
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
		instance.setGroupPermission(group.getIdentifier(), permission, t);
		src.sendMessage(Text.of(TextColors.AQUA, group.getIdentifier(), "'s permission ", TextColors.GOLD, permission, TextColors.AQUA, " set to ", TextColors.GOLD, t));
	}

	private void remove(CommandSource src, GroupSubject group, String permission) {
		if (permission==null) {
			src.sendMessage(Text.of(TextColors.RED, "Undefined permission"));
			return;
		}
		if (permission.startsWith("-")) {
			permission = permission.substring(1);
		}
		group.getSubjectData().setPermission(null, permission, Tristate.UNDEFINED);
		src.sendMessage(Text.of(TextColors.AQUA, group.getIdentifier(), "'s permission ", TextColors.GOLD, permission, TextColors.AQUA, " removed"));
	}

	private void parent(CommandSource src, GroupSubject group, String parentGroupName) {
		if (!instance.getGroupSubjects().hasRegistered(parentGroupName)) {
			src.sendMessage(Text.of(TextColors.RED, "Invalid parent group"));
			return;
		}
		GroupSubject parentGroup = (GroupSubject) instance.getGroupSubjects().get(parentGroupName);
		
		if (parentGroup.parentCheck(group)) {
			src.sendMessage(Text.of(TextColors.RED, "The specified parent group or one of the specified parent's parent has this group as parent. This would result in a parent loop so it's not allowed."));
			return;
		}
		
		group.setParent(parentGroup);
		try {
			instance.saveData();
			src.sendMessage(Text.of(TextColors.AQUA, group.getIdentifier(), "'s parent set to ", TextColors.GOLD, parentGroupName));
		} catch (IOException | ObjectMappingException e) {
			src.sendMessage(Text.of(TextColors.RED, "Error"));
			e.printStackTrace();
		}
	}
	
	private void weight(CommandSource src, GroupSubject group, @Nullable Integer weight) {
		if (weight==null) {
			src.sendMessage(Text.of(TextColors.RED, "Missing weight parameter"));
			return;
		}
		
		group.setWeight(weight);
		try {
			instance.saveData();
			src.sendMessage(Text.of(TextColors.AQUA, group.getIdentifier(), "'s weight set to ", TextColors.GOLD, weight));
		} catch (IOException | ObjectMappingException e) {
			src.sendMessage(Text.of(TextColors.RED, "Error"));
			e.printStackTrace();
		}
	}
	
	private void test(CommandSource src, GroupSubject group, String permission) {
		if (permission==null) {
			src.sendMessage(Text.of(TextColors.RED, "Undefined permission"));
			return;
		}
		src.sendMessage(Text.of(TextColors.AQUA,  "Group ", TextColors.GOLD, group.getIdentifier(), TextColors.AQUA, " permission ", permission, " value is ", TextColors.GOLD, group.getPermissionValue(null, permission)));
	}

	private void create(CommandSource src, GroupSubject group) {
		if (instance.getGroupSubjects().hasRegistered(group.getIdentifier())) {
			src.sendMessage(Text.of(TextColors.RED, "This group already exists!"));
			return;
		}
		
		((GroupSubjectCollection) instance.getGroupSubjects()).storePermission(group, "", Tristate.UNDEFINED);
		src.sendMessage(Text.of(TextColors.AQUA, "Group ", TextColors.GOLD, group.getIdentifier(), TextColors.AQUA, " created"));
	}
	
	private void delete(CommandSource src, GroupSubject group) {
		if (group.getIdentifier().equalsIgnoreCase("default")) {
			src.sendMessage(Text.of(TextColors.RED, "The default group cannot be deleted."));
			return;
		}
		((GroupSubjectCollection) instance.getGroupSubjects()).remove(group);
		src.sendMessage(Text.of(TextColors.AQUA, "Group ", TextColors.GOLD, group.getIdentifier(), TextColors.AQUA, " deleted"));
	}
}
