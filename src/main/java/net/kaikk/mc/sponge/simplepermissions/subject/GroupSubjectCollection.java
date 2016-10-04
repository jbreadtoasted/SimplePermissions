package net.kaikk.mc.sponge.simplepermissions.subject;

import org.spongepowered.api.service.permission.PermissionService;

public class GroupSubjectCollection extends SimpleSubjectCollection {
	public GroupSubjectCollection() {
		super(PermissionService.SUBJECTS_GROUP);
	}
	
	@Override
	protected SimpleSubject newSubject(String identifier) {
		return new GroupSubject(identifier, this);
	}
}
