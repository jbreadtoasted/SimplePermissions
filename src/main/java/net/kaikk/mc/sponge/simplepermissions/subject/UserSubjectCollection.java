package net.kaikk.mc.sponge.simplepermissions.subject;

import org.spongepowered.api.service.permission.PermissionService;

public class UserSubjectCollection extends SimpleSubjectCollection {
	public UserSubjectCollection() {
		super(PermissionService.SUBJECTS_USER);
	}
	
	@Override
	protected SimpleSubject newSubject(String identifier) {
		return new UserSubject(identifier, this);
	}
}