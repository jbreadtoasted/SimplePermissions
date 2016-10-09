package net.kaikk.mc.sponge.simplepermissions.subject;

import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

public class GroupSubjectCollection extends SimpleSubjectCollection {
	public GroupSubjectCollection() {
		super(PermissionService.SUBJECTS_GROUP);
	}
	
	@Override
	protected SimpleSubject newSubject(String identifier) {
		return new GroupSubject(identifier, this);
	}
	
	public void invalidateCache() {
		for (Subject s : this.identifiersToSubject.values()) {
			((GroupSubject) s).invalidateInheritedGroupsCache();
		}
	}
}
