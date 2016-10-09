package net.kaikk.mc.sponge.simplepermissions.subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Builder;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import com.google.common.collect.Lists;

import net.kaikk.mc.sponge.simplepermissions.SimplePermissions;
import net.kaikk.mc.sponge.simplepermissions.Utils;

public class GroupSubject extends SimpleSubject implements Comparable<GroupSubject> {
	private GroupSubject parent;
	private int weight;
	private GroupSubject[] cachedInheritedGroups;

	public GroupSubject(String identifier, GroupSubjectCollection collection) {
		this(identifier, collection, 0);
	}

	public GroupSubject(String identifier, GroupSubjectCollection collection, int weight) {
		this(identifier, collection, weight, null);
	}

	public GroupSubject(String identifier, GroupSubjectCollection collection, int weight, GroupSubject parent) {
		super(identifier, collection);
		this.weight = weight;
		this.parent = parent;
	}

	public GroupSubject getParent() {
		return parent;
	}

	public int getWeight() {
		return weight;
	}

	@Override
	public boolean isChildOf(Set<Context> contexts, Subject parent) {
		return this.parent.equals(parent);
	}

	@Override
	public List<Subject> getParents(Set<Context> contexts) {
		return Lists.newArrayList(this.parent);
	}

	@Override
	public SubjectCollection getContainingCollection() {
		return SimplePermissions.instance().getGroupSubjects();
	}

	public void setParent(GroupSubject parent) {
		this.parent = parent;
		SimplePermissions.instance().invalidateCache();
	}

	public void setWeight(int weight) {
		this.weight = weight;
		SimplePermissions.instance().invalidateCache();
	}

	@Override
	public Tristate getPermissionValue(Set<Context> contexts, String permission) {
		Tristate tristate = super.getPermissionValue(contexts, permission);
		if (tristate!=Tristate.UNDEFINED) {
			return tristate;
		}

		return this.getPermissionFromHeaviestGroupFor(permission);
	}
	
	public Tristate getPermissionFromHeaviestGroupFor(String permission) {
		if (this.parent == null) {
			return Tristate.UNDEFINED;
		}
		
		permission = permission.toLowerCase();
		
		for (GroupSubject g : this.cachedInheritedGroups()) {
			Boolean b = g.getSubjectData().getPermissions(null).get(permission);
			if (b!=null) {
				return Utils.tristate(b);
			}
		}
		
		return Tristate.UNDEFINED;
	}
	
	@Override
	public Text info() {
		Builder b = Text.builder();
		b.append(Text.of(TextColors.GREEN, "-- SimplePermissions - Group ", TextColors.GOLD, this.getIdentifier(), TextColors.GREEN, " --", Text.NEW_LINE));

		boolean d = true;
		
		if (this.parent!=null) {
			d = false;
			b.append(Text.of(TextColors.GREEN, "Parent: ", TextColors.AQUA, this.parent.getIdentifier(), Text.NEW_LINE));
		}

		if (this.weight!=0) {
			d = false;
			b.append(Text.of(TextColors.GREEN, "Weight: ", TextColors.AQUA, this.weight, Text.NEW_LINE));
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
			b.append(Text.of(TextColors.RED, "This group doesn't have any data yet!"));
		}

		return b.build();
	}

	@Override
	public Map<Set<Context>, List<Subject>> getAllParents() {
		Map<Set<Context>, List<Subject>> map = new ConcurrentHashMap<Set<Context>, List<Subject>>();
		map.put(GLOBAL_CONTEXT, this.getParents());
		return map;
	}

	public boolean parentCheck(GroupSubject other) {
		if (other==this) {
			return true;
		} else if (parent!=null){
			return parent.parentCheck(other);
		} else {
			return false;
		}
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
		if (this.parent == null) {
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
		if (this.parent != null && set.add(this.parent)) {
			set.addAll(this.parent.getAllInheritedGroups());
		}
		return set;
	}

	@Override
	public int compareTo(GroupSubject o) {
		return o.weight-this.weight;
	}
	
	/** Ordered array (highest weight to the lowest weight) */
	public GroupSubject[] cachedInheritedGroups() {
		if (this.cachedInheritedGroups == null) { // TODO invalid cache when user changes groups or group weights/parents changes
			List<GroupSubject> parents = new ArrayList<GroupSubject>();
			Collections.sort(parents);
			
			this.cachedInheritedGroups = parents.toArray(new GroupSubject[parents.size()]);
		}
		return this.cachedInheritedGroups;
	}
	
	public void invalidateInheritedGroupsCache() {
		this.cachedInheritedGroups = null;
	}
}
