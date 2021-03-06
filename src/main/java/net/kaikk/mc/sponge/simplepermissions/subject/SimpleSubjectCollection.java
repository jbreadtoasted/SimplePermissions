package net.kaikk.mc.sponge.simplepermissions.subject;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.util.Tristate;

import net.kaikk.mc.sponge.simplepermissions.SimplePermissions;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class SimpleSubjectCollection implements SubjectCollection {
	protected String identifier;
	protected Map<String,Subject> identifiersToSubject = new ConcurrentHashMap<String,Subject>();
	protected Map<String,Map<Subject,Boolean>> grantedPermissionsToSubjects = new ConcurrentHashMap<String,Map<Subject,Boolean>>();
	
	public SimpleSubjectCollection() {
		this.identifier = "undefined";
	}
	
	public SimpleSubjectCollection(String identifier) {
		this.identifier = identifier;
	}
	
	@Override
	public String getIdentifier() {
		return identifier;
	}
	
	@Override
	public Subject get(String identifier) {
		Subject s = this.identifiersToSubject.get(identifier.toLowerCase());
		if (s==null) {
			s = this.newSubject(identifier);
			this.identifiersToSubject.put(identifier.toLowerCase(), s);
		}
		return s;
	}

	@Override
	public boolean hasRegistered(String identifier) {
		return identifiersToSubject.containsKey(identifier.toLowerCase());
	}

	@Override
	public Iterable<Subject> getAllSubjects() {
		return identifiersToSubject.values();
	}

	@Override
	public Map<Subject, Boolean> getAllWithPermission(String permission) {
		Map<Subject,Boolean> m = grantedPermissionsToSubjects.get(permission);
		return m!=null ? m : Collections.emptyMap();
	}

	@Override
	public Map<Subject, Boolean> getAllWithPermission(Set<Context> contexts, String permission) {
		return this.getAllWithPermission(permission);
	}

	// SpongeAPI 5 Override
	public Subject getDefaults() {
		return SimplePermissions.instance().getDefaults();
	}
	
	public void storePermission(Subject subject, String permission, Tristate value) {
		this.transientStorePermission(subject, permission, value);
		
		try {
			SimplePermissions.instance().saveData();
		} catch (IOException | ObjectMappingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void transientStorePermission(Subject subject, String permission, Tristate value) {
		this.identifiersToSubject.putIfAbsent(subject.getIdentifier().toLowerCase(), subject);
		
		if (value==Tristate.TRUE) {
			Map<Subject, Boolean> m = this.grantedPermissionsToSubjects.get(permission);
			if (m==null) {
				m = new ConcurrentHashMap<Subject, Boolean>();
				this.grantedPermissionsToSubjects.put(permission, m);
			}
			m.put(subject, true);
		} else {
			Map<Subject, Boolean> m = this.grantedPermissionsToSubjects.get(permission);
			if (m!=null) {
				m.remove(subject);
			}
		}
	}
	
	public void remove(String identifier) {
		Subject s = this.identifiersToSubject.remove(identifier.toLowerCase());
		if (s!=null) {
			this.remove(s);
		}
	}
	
	public void remove(Subject subject) {
		for (String permission : subject.getSubjectData().getPermissions(null).keySet()) {
			this.grantedPermissionsToSubjects.get(permission).remove(subject);
		}
	}
	
	public void add(SimpleSubject subject) {
		if (this.identifiersToSubject.putIfAbsent(subject.getIdentifier().toLowerCase(), subject)==null) {
			for (Entry<String, Boolean> permission : subject.getSubjectData().getPermissions(null).entrySet()) {
				Map<Subject,Boolean> map = this.grantedPermissionsToSubjects.get(permission.getKey());
				if (map==null) {
					map = new ConcurrentHashMap<Subject,Boolean>();
					this.grantedPermissionsToSubjects.put(permission.getKey(), map);
				}
				map.put(subject, permission.getValue());
			}
		}
	}
	
	public int size() {
		return this.identifiersToSubject.size();
	}

	protected SimpleSubject newSubject(String identifier) {
		return new SimpleSubject(identifier, this);
	}
}
