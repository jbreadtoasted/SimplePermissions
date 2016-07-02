package net.kaikk.mc.sponge.simplepermissions.subject;

import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

public class GroupSubjectCollection extends SimpleSubjectCollection {
	public GroupSubjectCollection() {
		super(PermissionService.SUBJECTS_GROUP);
	}
	
	@Override
	public Subject get(String identifier) {
		Subject s = this.identifiersToSubject.get(identifier.toLowerCase());
		if (s!=null) {
			return s;
		}
		return new GroupSubject(identifier, this);
	}
}
