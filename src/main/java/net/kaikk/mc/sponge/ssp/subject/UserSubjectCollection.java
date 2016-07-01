package net.kaikk.mc.sponge.ssp.subject;

import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

public class UserSubjectCollection extends SimpleSubjectCollection {
	public UserSubjectCollection() {
		super(PermissionService.SUBJECTS_USER);
	}

	@Override
	public Subject get(String identifier) {
		Subject s = this.identifiersToSubject.get(identifier);
		if (s!=null) {
			return s;
		}
		return new UserSubject(identifier, this);
	}
}
