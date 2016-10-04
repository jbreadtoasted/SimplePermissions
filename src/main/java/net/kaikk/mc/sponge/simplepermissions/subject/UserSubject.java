package net.kaikk.mc.sponge.simplepermissions.subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
		
		tristate = this.getPermissionFromHeaviestGroupFor(permission);
		if (tristate!=Tristate.UNDEFINED) {
			return tristate;
		}
		
		return this.getDefaultPermissionValue(permission);
	}
	
	public Tristate getPermissionFromHeaviestGroupFor(String permission) {
		if (this.groups.isEmpty()) {
			return Tristate.UNDEFINED;
		}
		
		permission = permission.toLowerCase();
		
		Iterator<GroupSubject> it = this.groups.iterator();
		GroupSubject heaviest = it.next();
		
		while (it.hasNext()) {
			GroupSubject g = it.next();
			if (g.getWeight()>heaviest.getWeight()) {
				Boolean b = g.getSubjectData().getPermissions(null).get(permission);
				if (b!=null) {
					heaviest = g;
				}
			}
			
			Optional<GroupSubject> heaviestParentGroup = g.getHeaviestParentGroupFor(permission);
			if (heaviestParentGroup.isPresent() && heaviestParentGroup.get().getWeight()>heaviest.getWeight()) {
				Boolean b = heaviestParentGroup.get().getSubjectData().getPermissions(null).get(permission);
				if (b!=null) {
					heaviest = heaviestParentGroup.get();
				}
			}
		}
		
		return heaviest.getPermissionValue(null, permission);
	}
	
	@Override
	public Text info() {
		Builder b = Text.builder();
		Optional<GameProfile> profile = Sponge.getServer().getGameProfileManager().getCache().getById(UUID.fromString(this.getIdentifier()));
		String name = profile.isPresent() && profile.get().getName().isPresent() ? profile.get().getName().get() : this.getIdentifier();
		b.append(Text.of(TextColors.GREEN, "-- SimplePermissions - ", TextColors.GOLD, name, TextColors.GREEN, " --", Text.NEW_LINE));
		boolean d = true;
		
		if (!this.getGroups().isEmpty()) {
			d = false;
			StringBuilder sb = new StringBuilder();
			for(GroupSubject g : this.getGroups()) {
				sb.append(g.getIdentifier());
				sb.append(", ");
			}
			b.append(Text.of(TextColors.GREEN, "Groups: ", TextColors.AQUA, sb.substring(0, sb.length()-2), Text.NEW_LINE));
		}
		
		
		if (!this.getSubjectData().getPermissions(null).isEmpty()) {
			d = false;
			b.append(Text.of(TextColors.GREEN, "Permissions:", Text.NEW_LINE));
			for(Entry<String,Boolean> e : this.getSubjectData().getPermissions(null).entrySet()) {
				b.append(e.getValue() ? Text.of(TextColors.GREEN, "+ ") : Text.of(TextColors.RED, "- "), Text.of(TextColors.AQUA, e.getKey()), Text.NEW_LINE);
			}
		}
		
		if (!this.getSubjectData().getOptions(GLOBAL_CONTEXT).isEmpty()) {
			d = false;
			b.append(Text.of(TextColors.GREEN, "Options:", Text.NEW_LINE));
			for(Entry<String,String> e : this.getSubjectData().getOptions(GLOBAL_CONTEXT).entrySet()) {
				b.append(Text.of(TextColors.AQUA, "- ", e.getKey(), ": ", TextColors.AQUA, TextColors.DARK_BLUE, e.getValue(), Text.NEW_LINE));
			}
		}
		
		if (d) {
			b.append(Text.of(TextColors.RED, "This user doesn't have any data yet!"));
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
		Map<Set<Context>, List<Subject>> map = new ConcurrentHashMap<Set<Context>, List<Subject>>();
		map.put(null, this.getParents());
		return map;
	}
	
	// Users will be always removed from the config file if there isn't any specific setting
	@Override
	public boolean canBeRemovedIfEmpty() {
		return true;
	}
}
