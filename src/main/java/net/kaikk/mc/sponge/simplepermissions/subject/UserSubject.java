package net.kaikk.mc.sponge.simplepermissions.subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Builder;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

public class UserSubject extends SimpleSubject {
	private List<GroupSubject> groups = Collections.synchronizedList(new ArrayList<GroupSubject>());
	
	public UserSubject(String identifier, UserSubjectCollection collection) {
		super(identifier, collection);
	}
	
	@Override
	public Optional<CommandSource> getCommandSource() {
		return Optional.ofNullable(Sponge.getServer().getPlayer(UUID.fromString(this.getIdentifier())).orElse(null));
	}
	
	@Override
	public boolean isChildOf(Set<Context> contexts, Subject parent) {
		return groups.contains(parent);
	}
	
	@Override
	public Tristate getPermissionValue(Set<Context> contexts, String permission) {
		Tristate tristate = super.getPermissionValue(contexts, permission);
		if (tristate!=Tristate.UNDEFINED) {
			return tristate;
		}
		
		Optional<GroupSubject> group = this.getHeaviestGroupFor(permission);
		if (group.isPresent()) {
			return group.get().getPermissionValue(null, permission);
		}
		
		return this.getDefaultPermissionValue(permission);
	}
	
	public Optional<GroupSubject> getHeaviestGroupFor(String permission) {
		if (this.groups.isEmpty()) {
			return Optional.empty();
		}
		
		GroupSubject heaviest = this.groups.iterator().next();
		Iterator<GroupSubject> it = this.groups.iterator();
		while (it.hasNext()) {
			GroupSubject g = it.next();
			if (g.getWeight()>heaviest.getWeight()) {
				Boolean b = g.getSubjectData().getPermissions(null).get(permission);
				if (b!=null) {
					heaviest = g;
				}
			}
		}
		
		Boolean b = heaviest.getSubjectData().getPermissions(null).get(permission);
		if (b!=null) {
			return Optional.of(heaviest);
		}
		return Optional.empty();
	}
	
	@Override
	public Text info() {
		Builder b = Text.builder();
		Optional<GameProfile> profile = Sponge.getServer().getGameProfileManager().getCache().getById(UUID.fromString(this.getIdentifier()));
		String name = profile.isPresent() && profile.get().getName().isPresent() ? profile.get().getName().get() : this.getIdentifier();
		b.append(Text.of(TextColors.GREEN, "-- SimplePermissions - ", TextColors.GOLD, name, TextColors.GREEN, " --", Text.NEW_LINE));
		
		if (!this.getGroups().isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for(GroupSubject g : this.getGroups()) {
				sb.append(g.getIdentifier());
				sb.append(", ");
			}
			b.append(Text.of(TextColors.GREEN, "Groups: ", TextColors.AQUA, sb.substring(0, sb.length()-2), Text.NEW_LINE));
		}
		
		b.append(Text.of(TextColors.GREEN, "Permissions:", Text.NEW_LINE));
		for(Entry<String,Boolean> e : this.getSubjectData().getPermissions(null).entrySet()) {
			b.append(e.getValue() ? Text.of(TextColors.GREEN, "+ ") : Text.of(TextColors.RED, "- "), Text.of(TextColors.AQUA, e.getKey()), Text.NEW_LINE);
		}
		
		return b.build();
	}

	public List<GroupSubject> getGroups() {
		return groups;
	}
	
	@Override
	public List<Subject> getParents(Set<Context> contexts) {
		return Collections.unmodifiableList(this.groups);
	}
	
	@Override
	public Map<Set<Context>, List<Subject>> getAllParents() {
		Map<Set<Context>, List<Subject>> map = new HashMap<Set<Context>, List<Subject>>();
		map.put(null, this.getParents());
		return map;
	}
}