package net.kaikk.mc.sponge.simplepermissions.subject;

import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

public class UserSubjectCollection extends SimpleSubjectCollection {
	public UserSubjectCollection() {
		super(PermissionService.SUBJECTS_USER);
	}
	
	@Override
	protected SimpleSubject newSubject(String identifier) {
		return new UserSubject(identifier, this);
	}
	
	public void invalidateCache() {
		for (Subject s : this.identifiersToSubject.values()) {
			((UserSubject) s).invalidateInheritedGroupsCache();
		}
	}
}