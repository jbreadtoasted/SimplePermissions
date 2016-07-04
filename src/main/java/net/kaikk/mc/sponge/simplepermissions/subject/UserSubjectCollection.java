package net.kaikk.mc.sponge.simplepermissions.subject;

import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

public class UserSubjectCollection extends SimpleSubjectCollection {
	public UserSubjectCollection() {
		super(PermissionService.SUBJECTS_USER);
	}

	@Override
	public Subject get(String identifier) {
		Subject s = this.identifiersToSubject.get(identifier);
		if (s==null) {
			s = new UserSubject(identifier, this);
			this.identifiersToSubject.put(identifier, s);
		}
		return s;
	}
}
