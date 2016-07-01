package net.kaikk.mc.sponge.ssp.subject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Builder;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

public class SimpleSubject implements Subject {
	private String identifier;
	private SimpleSubjectCollection collection; 
	private SimpleSubjectData permissionData = new SimpleSubjectData();

	public SimpleSubject(String identifier, SimpleSubjectCollection collection) {
		this.identifier = identifier;
		this.collection = collection;
	}

	@Override
	public String getIdentifier() {
		
		return identifier;
	}
	
	@Override
	public Set<Context> getActiveContexts() {
		
		return Collections.emptySet();
	}

	@Override
	public Optional<CommandSource> getCommandSource() {
		
		return Optional.empty();
	}

	@Override
	public SubjectCollection getContainingCollection() {
		return collection;
	}

	@Override
	public SubjectData getSubjectData() {
		
		return permissionData;
	}

	@Override
	public SubjectData getTransientSubjectData() {
		
		return permissionData;
	}

	@Override
	public boolean hasPermission(Set<Context> contexts, String permission) {
		return this.getPermissionValue(contexts, permission) == Tristate.TRUE;
	}

	@Override
	public Tristate getPermissionValue(Set<Context> contexts, String permission) {
		Boolean b = permissionData.getPermissions().get(permission);
		return b==null ? Tristate.UNDEFINED : b ? Tristate.TRUE : Tristate.FALSE;
	}
	
	public Tristate getDefaultPermissionValue(String permission) {
		return this.getContainingCollection().getDefaults().getPermissionValue(null, permission);
	}

	@Override
	public boolean isChildOf(Set<Context> contexts, Subject parent) {
		return false;
	}

	@Override
	public List<Subject> getParents(Set<Context> contexts) {
		
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		return "SimpleSubject [identifier=" + identifier + "]";
	}
	
	public class SimpleSubjectData implements SubjectData {
		private Map<String,Boolean> permissions = new HashMap<String,Boolean>();
		private Map<Set<Context>, Map<String, Boolean>> map = new HashMap<Set<Context>, Map<String, Boolean>>();
		
		SimpleSubjectData() {
			map.put(GLOBAL_CONTEXT, permissions);
		}
		
		@Override
		public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
			return map;
		}

		@Override
		public Map<String, Boolean> getPermissions(Set<Context> contexts) {
			return permissions;
		}

		@Override
		public boolean setPermission(Set<Context> contexts, String permission, Tristate value) {
			if (value==Tristate.UNDEFINED) {
				permissions.remove(permission);
			} else {
				permissions.put(permission, value == Tristate.TRUE);
			}
			
			((SimpleSubjectCollection) SimpleSubject.this.getContainingCollection()).storePermission(SimpleSubject.this, permission, value);
			return true;
		}

		@Override
		public boolean clearPermissions() {
			permissions.clear();
			return true;
		}

		@Override
		public boolean clearPermissions(Set<Context> contexts) {
			permissions.clear();
			return true;
		}

		@Override
		public Map<Set<Context>, List<Subject>> getAllParents() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Subject> getParents(Set<Context> contexts) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addParent(Set<Context> contexts, Subject parent) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeParent(Set<Context> contexts, Subject parent) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean clearParents() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean clearParents(Set<Context> contexts) {
			throw new UnsupportedOperationException();
		}

		public Map<String, Boolean> getPermissions() {
			return permissions;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SimpleSubject)) {
			return false;
		}
		SimpleSubject other = (SimpleSubject) obj;
		if (identifier == null) {
			if (other.identifier != null) {
				return false;
			}
		} else if (!identifier.equals(other.identifier)) {
			return false;
		}
		return true;
	}
	
	public Text info() {
		Builder b = Text.builder();
		b.append(Text.of(TextColors.GREEN, "-- SimpleSpongePermissions - ", TextColors.GOLD, this.getContainingCollection().getIdentifier(), " : ", this.getIdentifier(), TextColors.GREEN, " --", Text.NEW_LINE));
		b.append(Text.of(TextColors.GREEN, "Permissions:", Text.NEW_LINE));
		for(Entry<String,Boolean> e : this.getSubjectData().getPermissions(null).entrySet()) {
			b.append(e.getValue() ? Text.of(TextColors.GREEN, "+ ") : Text.of(TextColors.RED, "- "), Text.of(TextColors.AQUA, e.getKey()), Text.NEW_LINE);
		}
		
		return b.build();
	}
}
