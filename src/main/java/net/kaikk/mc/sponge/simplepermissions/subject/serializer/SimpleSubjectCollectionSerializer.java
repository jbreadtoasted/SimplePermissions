package net.kaikk.mc.sponge.simplepermissions.subject.serializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.spongepowered.api.service.permission.Subject;

import com.google.common.reflect.TypeToken;

import net.kaikk.mc.sponge.simplepermissions.subject.SimpleSubject;
import net.kaikk.mc.sponge.simplepermissions.subject.SimpleSubjectCollection;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class SimpleSubjectCollectionSerializer implements TypeSerializer<SimpleSubjectCollection> {
	@Override
	public void serialize(TypeToken<?> type, SimpleSubjectCollection obj, ConfigurationNode value) throws ObjectMappingException {
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

			if (granted.isEmpty() && denied.isEmpty()) {
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
			}
		}
	}

	@Override
	public SimpleSubjectCollection deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
		SimpleSubjectCollection collection = new SimpleSubjectCollection(value.getNode("identifier").getString("undefined"));
		Map<Object, ? extends ConfigurationNode> users = value.getChildrenMap();
		
		for (Entry<Object, ? extends ConfigurationNode> groupEntry : users.entrySet()) {
			SimpleSubject subject = new SimpleSubject(groupEntry.getKey().toString(), collection);
			ConfigurationNode node = groupEntry.getValue().getNode("granted");
			for (String permission : node.getList(TypeToken.of(String.class))) {
				subject.getSubjectData().getPermissions(null).put(permission, true);
			}
			
			node = groupEntry.getValue().getNode("denied");
			for (String permission : node.getList(TypeToken.of(String.class))) {
				subject.getSubjectData().getPermissions(null).put(permission, false);
			}
			
			collection.add(subject);
		}

		return collection;
	}
}