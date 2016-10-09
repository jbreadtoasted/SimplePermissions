package net.kaikk.mc.sponge.simplepermissions.subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
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

import net.kaikk.mc.sponge.simplepermissions.SimplePermissions;

public class UserSubject extends SimpleSubject {
	private List<GroupSubject> groups = Collections.synchronizedList(new ArrayList<GroupSubject>());
	/** Never access directly! Use cachedInheritedGroups() instead */
	private GroupSubject[] cachedInheritedGroups;
	
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
		
		for (GroupSubject g : this.cachedInheritedGroups()) {
			Tristate tristate = g.getPermissionValue(GLOBAL_CONTEXT, permission);
			if (tristate != Tristate.UNDEFINED) {
				return tristate;
			}
		}
		
		return Tristate.UNDEFINED;
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

	/** @return unmodifiable list of groups */
	public List<GroupSubject> getGroups() {
		return Collections.unmodifiableList(groups);
	}
	
	public void addGroup(GroupSubject group) {
		SimplePermissions.instance().invalidateCache();
		this.groups.add(group);
	}
	
	public void removeGroup(GroupSubject group) {
		SimplePermissions.instance().invalidateCache();
		this.groups.remove(group);
	}
	
	public void clearGroups() {
		SimplePermissions.instance().invalidateCache();
		this.groups.clear();
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
	
	@Override
	public Optional<String> getOption(Set<Context> contexts, String key) {
		Optional<String> value = super.getOption(contexts, key);
		if (value.isPresent()) {
			return value;
		}
		
		Optional<String> parent = this.getOptionFromHeaviestGroupFor(key);
		if (parent.isPresent()) {
			return parent;
		}
		
		return this.getDefaultOptionValue(key);
	}
	
	public Optional<String> getOptionFromHeaviestGroupFor(String key) {
		if (this.groups.isEmpty()) {
			return Optional.empty();
		}
		
		for (GroupSubject g : this.cachedInheritedGroups()) {
			String value = g.getSubjectData().getOptions(GLOBAL_CONTEXT).get(key);
			if (SimplePermissions.instance().debug) {
				SimplePermissions.instance().logger().info("Requested option "+key+" to "+g.getClass().getSimpleName()+" '"+g.getIdentifier()+"', result: "+(value != null ? value : "undefined"));
			}
			if (value!=null) {
				return Optional.of(value);
			}
		}

		return Optional.empty();
	}
	
	public Set<GroupSubject> getAllInheritedGroups() {
		return getAllInheritedGroups(new LinkedHashSet<GroupSubject>());
	}
	
	public Set<GroupSubject> getAllInheritedGroups(Set<GroupSubject> set) {
		for (GroupSubject g : this.groups) {
			if (set.add(g)) {
				set.addAll(g.getAllInheritedGroups());
			}
		}
		return set;
	}
	
	/** Ordered array (highest weight to the lowest weight) */
	public GroupSubject[] cachedInheritedGroups() {
		if (this.cachedInheritedGroups == null) { // TODO invalid cache when user changes groups or group weights/parents changes
			List<GroupSubject> parents = new ArrayList<GroupSubject>(this.getAllInheritedGroups());
			Collections.sort(parents);
			
			this.cachedInheritedGroups = parents.toArray(new GroupSubject[parents.size()]);
		}
		return this.cachedInheritedGroups;
	}
	
	public void invalidateInheritedGroupsCache() {
		this.cachedInheritedGroups = null;
	}
}
