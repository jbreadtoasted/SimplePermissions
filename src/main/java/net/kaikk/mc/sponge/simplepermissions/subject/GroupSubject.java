package net.kaikk.mc.sponge.simplepermissions.subject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Builder;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import com.google.common.collect.Lists;

import net.kaikk.mc.sponge.simplepermissions.SimplePermissions;

public class GroupSubject extends SimpleSubject {
	private GroupSubject parent;
	private int weight;

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
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	@Override
	public Tristate getPermissionValue(Set<Context> contexts, String permission) {
		Tristate tristate = super.getPermissionValue(contexts, permission);
		if (tristate!=Tristate.UNDEFINED) {
			return tristate;
		}

		Optional<GroupSubject> group = this.getHeaviestParentGroupFor(permission);
		if (group.isPresent()) {
			return group.get().getPermissionValue(null, permission);
		}

		return this.getDefaultPermissionValue(permission);
	}

	public Tristate getDefaultPermissionValue(String permission) {
		if (this.equals(((SimpleSubjectCollection) this.getContainingCollection()).getDefaults())) {
			return Tristate.UNDEFINED;
		}

		return ((SimpleSubjectCollection) this.getContainingCollection()).getDefaults().getPermissionValue(null, permission);
	}

	public Optional<GroupSubject> getHeaviestParentGroupFor(String permission) {
		if (this.parent==null) {
			return Optional.empty();
		}

		permission = permission.toLowerCase();

		Boolean b = parent.getSubjectData().getPermissions(null).get(permission);

		Optional<GroupSubject> g = parent.getHeaviestParentGroupFor(permission);
		if (g.isPresent() && (g.get().getWeight()>parent.getWeight() || b==null)) {
			Boolean b2 = g.get().getSubjectData().getPermissions(null).get(permission);
			if (b2!=null) {
				return g;
			}
		}

		if (b!=null) {
			return Optional.of(parent);
		}
		return Optional.empty();
	}

	@Override
	public Text info() {
		Builder b = Text.builder();
		b.append(Text.of(TextColors.GREEN, "-- SimplePermissions - Group ", TextColors.GOLD, this.getIdentifier(), TextColors.GREEN, " --", Text.NEW_LINE));

		if (this.parent!=null) {
			b.append(Text.of(TextColors.GREEN, "Parent: ", TextColors.AQUA, this.parent.getIdentifier(), Text.NEW_LINE));
		}

		if (this.weight!=0) {
			b.append(Text.of(TextColors.GREEN, "Weight: ", TextColors.AQUA, this.weight, Text.NEW_LINE));
		}

		b.append(Text.of(TextColors.GREEN, "Permissions:", Text.NEW_LINE));
		for(Entry<String,Boolean> e : this.getSubjectData().getPermissions(null).entrySet()) {
			b.append(e.getValue() ? Text.of(TextColors.GREEN, "+ ") : Text.of(TextColors.RED, "- "), Text.of(TextColors.AQUA, e.getKey()), Text.NEW_LINE);
		}

		return b.build();
	}

	@Override
	public Map<Set<Context>, List<Subject>> getAllParents() {
		Map<Set<Context>, List<Subject>> map = new HashMap<Set<Context>, List<Subject>>();
		map.put(null, this.getParents());
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
}
