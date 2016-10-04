package net.kaikk.mc.sponge.simplepermissions.subject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Builder;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import net.kaikk.mc.sponge.simplepermissions.SimplePermissions;
import net.kaikk.mc.sponge.simplepermissions.Utils;

public class SimpleSubject implements Subject, SubjectData {
	private final String identifier;
	private final SimpleSubjectCollection collection; 
	private final Map<String,Boolean> globalPermissions = new ConcurrentHashMap<String,Boolean>();
	private final Map<String,String> globalOptions = new ConcurrentHashMap<String,String>();
	private final Map<Set<Context>, Map<String, Boolean>> permissions = new ConcurrentHashMap<Set<Context>, Map<String, Boolean>>();
	private final Map<Set<Context>, Map<String, String>> options = new ConcurrentHashMap<Set<Context>, Map<String, String>>();
	private boolean removeIfEmpty = false;
	
	public SimpleSubject(String identifier, SimpleSubjectCollection collection) {
		this.identifier = identifier;
		this.collection = collection;
		permissions.put(GLOBAL_CONTEXT, globalPermissions);
		options.put(GLOBAL_CONTEXT, globalOptions);
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
		return this;
	}

	@Override
	public SubjectData getTransientSubjectData() {
		return this;
	}

	@Override
	public boolean hasPermission(Set<Context> contexts, String permission) {
		return this.getPermissionValue(contexts, permission) == Tristate.TRUE;
	}

	@Override
	public Tristate getPermissionValue(Set<Context> contexts, String permission) {
		permission = permission.toLowerCase();
		Boolean b = this.globalPermissions.get(permission);
		if (b==null) {
			b = this.globalPermissions.get("*");
			if (b==null) {
				String[] split = permission.split("[.]");
				StringBuilder pb = new StringBuilder();
				for(int i=0; i<split.length-1; i++) {
					pb.append(split[i]);
					pb.append('.');
					b = this.globalPermissions.get(pb.toString()+"*");
					if (b!=null) {
						break;
					}
				}
			}
		}
		if (SimplePermissions.instance().debug) {
			SimplePermissions.instance().logger().info("Requested permission "+permission+" to "+this.getClass().getSimpleName()+" '"+identifier+"', result: "+Utils.tristate(b));
		}
		return Utils.tristate(b);
	}

	public Tristate getDefaultPermissionValue(String permission) {
		return ((SimpleSubjectCollection) this.getContainingCollection()).getDefaults().getPermissionValue(null, permission);
	}

	@Override
	public boolean isChildOf(Set<Context> contexts, Subject parent) {
		return false;
	}

	@Override
	public String toString() {
		return "SimpleSubject [identifier=" + identifier + "]";
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
		b.append(Text.of(TextColors.GREEN, "-- SimplePermissions - ", TextColors.GOLD, this.getContainingCollection().getIdentifier(), " : ", this.getIdentifier(), TextColors.GREEN, " --", Text.NEW_LINE));
		b.append(Text.of(TextColors.GREEN, "Permissions:", Text.NEW_LINE));
		for(Entry<String,Boolean> e : this.getSubjectData().getPermissions(null).entrySet()) {
			b.append(e.getValue() ? Text.of(TextColors.GREEN, "+ ") : Text.of(TextColors.RED, "- "), Text.of(TextColors.AQUA, e.getKey()), Text.NEW_LINE);
		}
		
		return b.build();
	}


	@Override
	public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
		return permissions;
	}

	@Override
	public Map<String, Boolean> getPermissions(Set<Context> contexts) {
		return globalPermissions;
	}

	@Override
	public boolean setPermission(Set<Context> contexts, String permission, Tristate value) {
		if (SimplePermissions.instance().debug) {
			SimplePermissions.instance().logger().info("Adding permission "+permission+" to "+this.getClass().getSimpleName()+" '"+identifier+"'");
		}
		permission = permission.toLowerCase();
		if (value==Tristate.UNDEFINED) {
			globalPermissions.remove(permission);
		} else {
			globalPermissions.put(permission, value == Tristate.TRUE);
		}
		
		((SimpleSubjectCollection) this.getContainingCollection()).storePermission(this, permission, value);
		return true;
	}

	@Override
	public boolean clearPermissions() {
		globalPermissions.clear();
		return true;
	}

	@Override
	public boolean clearPermissions(Set<Context> contexts) {
		globalPermissions.clear();
		return true;
	}

	@Override
	public Map<Set<Context>, List<Subject>> getAllParents() {
		return Collections.emptyMap();// TODO
	}

	@Override
	public List<Subject> getParents(Set<Context> contexts) {
		return Collections.emptyList();// TODO
	}

	@Override
	public boolean addParent(Set<Context> contexts, Subject parent) {
		return false;// TODO
	}

	@Override
	public boolean removeParent(Set<Context> contexts, Subject parent) {
		return false;// TODO
	}

	@Override
	public boolean clearParents() {
		return false;// TODO
	}

	@Override
	public boolean clearParents(Set<Context> contexts) {
		return false;// TODO
	}

	// SpongeAPI 5 Override
	@Override
	public Map<Set<Context>, Map<String, String>> getAllOptions() {
		return this.options;
	}

	// SpongeAPI 5 Override
	public Map<String, String> getOptions(Set<Context> contexts) {
		Map<String, String> opts = this.options.get(contexts);
		if (opts == null) {
			opts = new ConcurrentHashMap<String, String>();
			this.options.put(contexts, opts);
		}
		return opts;
	}

	// SpongeAPI 5 Override
	public boolean setOption(Set<Context> contexts, String key, String value) {
		Map<String, String> opts = this.options.get(contexts);
		if (opts == null) {
			opts = new ConcurrentHashMap<String, String>();
			this.options.put(contexts, opts);
		}
		return opts.put(key, value) == null;
	}

	// SpongeAPI 5 Override
	public boolean clearOptions(Set<Context> contexts) {
		return this.options.remove(contexts) != null;
	}

	// SpongeAPI 5 Override
	public boolean clearOptions() {
		this.options.clear();
		return true;
	}

	// SpongeAPI 5 Override
	public Optional<String> getOption(Set<Context> contexts, String key) {
		Map<String, String> opts = this.options.get(contexts);
		if (opts == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(opts.get(key));
	}
	
	public void setOptions(Set<Context> contexts, Map<String, String> options) {
		Map<String, String> opts = this.options.get(contexts);
		if (opts == null) {
			this.options.put(contexts, options);
		} else {
			opts.putAll(options);
		}
	}
	
	public void setAllOptions(Map<Set<Context>, Map<String, String>> options) {
		this.options.putAll(options);
	}
	
	/**
	 * Invoking this method will disallow persistence while saving the config file. If the Subject doesn't have any configuration, it'll be removed from the config file.
	 */
	public void removeIfEmpty() {
		this.removeIfEmpty = true;
	}
	
	public boolean canBeRemovedIfEmpty() {
		return this.removeIfEmpty;
	}
}
