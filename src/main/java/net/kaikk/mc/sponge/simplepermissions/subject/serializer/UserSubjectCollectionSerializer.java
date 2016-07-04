package net.kaikk.mc.sponge.simplepermissions.subject.serializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.spongepowered.api.service.permission.Subject;

import com.google.common.reflect.TypeToken;

import net.kaikk.mc.sponge.simplepermissions.SimplePermissions;
import net.kaikk.mc.sponge.simplepermissions.subject.GroupSubject;
import net.kaikk.mc.sponge.simplepermissions.subject.UserSubject;
import net.kaikk.mc.sponge.simplepermissions.subject.UserSubjectCollection;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class UserSubjectCollectionSerializer implements TypeSerializer<UserSubjectCollection> {
	@Override
	public void serialize(TypeToken<?> type, UserSubjectCollection obj, ConfigurationNode value) throws ObjectMappingException {
		for (Subject s : obj.getAllSubjects()) {
			List<String> granted = new ArrayList<String>();
			List<String> denied = new ArrayList<String>();
			for (Entry<String,Boolean> e : s.getSubjectData().getPermissions(null).entrySet()) {
				if (e.getValue()) {
					granted.add(e.getKey());
				} else {
					denied.add(e.getKey());
				}
			}

			List<String> groups = new ArrayList<String>();
			for (GroupSubject gs : ((UserSubject) s).getGroups()) {
				groups.add(gs.getIdentifier());
			}
			if (granted.isEmpty() && denied.isEmpty() && groups.isEmpty()) {
				value.removeChild(s.getIdentifier());
			} else {
				if (!granted.isEmpty()) {
					value.getNode(s.getIdentifier()).getNode("granted").setValue(granted);
				} else {
					value.getNode(s.getIdentifier()).removeChild("granted");
				}

				if (!denied.isEmpty()) {
					value.getNode(s.getIdentifier()).getNode("denied").setValue(denied);
				} else {
					value.getNode(s.getIdentifier()).removeChild("denied");
				}

				if (!groups.isEmpty()) {
					value.getNode(s.getIdentifier()).getNode("groups").setValue(groups);
				} else {
					value.getNode(s.getIdentifier()).removeChild("groups");
				}
			}
		}
	}

	@Override
	public UserSubjectCollection deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
		UserSubjectCollection collection = new UserSubjectCollection();
		Map<Object, ? extends ConfigurationNode> users = value.getChildrenMap();
		
		for (Entry<Object, ? extends ConfigurationNode> userEntry : users.entrySet()) {
			UserSubject subject = new UserSubject(userEntry.getKey().toString(), collection);
			ConfigurationNode node = userEntry.getValue().getNode("granted");
			for (String permission : node.getList(TypeToken.of(String.class))) {
				subject.getSubjectData().getPermissions(null).put(permission, true);
			}
			
			node = userEntry.getValue().getNode("denied");
			for (String permission : node.getList(TypeToken.of(String.class))) {
				subject.getSubjectData().getPermissions(null).put(permission, false);
			}
			
			node = userEntry.getValue().getNode("groups");
			for (String groupName : node.getList(TypeToken.of(String.class))) {
				if (SimplePermissions.instance().getGroupSubjects().hasRegistered(groupName)) {
					subject.getGroups().add((GroupSubject) SimplePermissions.instance().getGroupSubjects().get(groupName));
				}
			}
			
			collection.add(subject);
		}

		return collection;
	}
}