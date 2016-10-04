package net.kaikk.mc.sponge.simplepermissions.subject.serializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;

import com.google.common.reflect.TypeToken;

import net.kaikk.mc.sponge.simplepermissions.subject.GroupSubject;
import net.kaikk.mc.sponge.simplepermissions.subject.GroupSubjectCollection;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class GroupSubjectCollectionSerializer implements TypeSerializer<GroupSubjectCollection> {
	@Override
	public void serialize(TypeToken<?> type, GroupSubjectCollection obj, ConfigurationNode value) throws ObjectMappingException {
		for (Subject s : obj.getAllSubjects()) {
			GroupSubject gs = (GroupSubject) s;
			List<String> granted = new ArrayList<String>();
			List<String> denied = new ArrayList<String>();
			for (Entry<String,Boolean> e : s.getSubjectData().getPermissions(null).entrySet()) {
				if (e.getValue()) {
					granted.add(e.getKey());
				} else {
					denied.add(e.getKey());
				}
			}
			Map<String,String> options = s.getSubjectData().getOptions(SubjectData.GLOBAL_CONTEXT);
			
			if (granted.isEmpty() && denied.isEmpty() && options.isEmpty() && gs.getParent()==null && gs.getWeight()==0 && gs.canBeRemovedIfEmpty()) {
				value.removeChild(s.getIdentifier());
			} else {
				boolean persistent = true;
				if (!granted.isEmpty()) {
					value.getNode(s.getIdentifier()).getNode("granted").setValue(granted);
					persistent = false;
				} else {
					value.getNode(s.getIdentifier()).removeChild("granted");
				}

				if (!denied.isEmpty()) {
					value.getNode(s.getIdentifier()).getNode("denied").setValue(denied);
					persistent = false;
				} else {
					value.getNode(s.getIdentifier()).removeChild("denied");
				}
				
				if (gs.getWeight()!=0) {
					value.getNode(s.getIdentifier()).getNode("weight").setValue(gs.getWeight());
					persistent = false;
				} else {
					value.getNode(s.getIdentifier()).removeChild("weight");
				}

				if (gs.getParent()!=null) {
					value.getNode(s.getIdentifier()).getNode("parent").setValue(gs.getParent().getIdentifier());
					persistent = false;
				} else {
					value.getNode(s.getIdentifier()).removeChild("parent");
				}

				if (!options.isEmpty()) {
					value.getNode(s.getIdentifier()).getNode("options").setValue(options);
					persistent = false;
				} else {
					value.getNode(s.getIdentifier()).removeChild("options");
				}
				
				if (persistent) {
					value.getNode(s.getIdentifier()).getNode("persistent").setValue(true);
				}
			}
		}
	}

	@Override
	public GroupSubjectCollection deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
		GroupSubjectCollection collection = new GroupSubjectCollection();
		Map<Object, ? extends ConfigurationNode> users = value.getChildrenMap();
		Map<GroupSubject, String> parentsMap = new HashMap<GroupSubject, String>();
		
		for (Entry<Object, ? extends ConfigurationNode> entry : users.entrySet()) {
			GroupSubject subject = new GroupSubject(entry.getKey().toString(), collection, entry.getValue().getNode("weight").getInt(0));
			ConfigurationNode node = entry.getValue().getNode("granted");
			for (String permission : node.getList(TypeToken.of(String.class))) {
				subject.getSubjectData().getPermissions(null).put(permission, true);
			}
			
			node = entry.getValue().getNode("denied");
			for (String permission : node.getList(TypeToken.of(String.class))) {
				subject.getSubjectData().getPermissions(null).put(permission, false);
			}

			node = entry.getValue().getNode("options");
			if (node.hasMapChildren()) {
				Map<String, String> options = node.getValue(SimpleSubjectCollectionSerializer.optionsTypeToken);
				if (options != null) {
					subject.setOptions(SubjectData.GLOBAL_CONTEXT, options);
				}
			}
			
			String parent = entry.getValue().getNode("parent").getString();
			if (parent!=null) {
				parentsMap.put(subject, parent);
			}
			
			collection.add(subject);
		}
		
		for (Entry<GroupSubject, String> e : parentsMap.entrySet()) {
			if (collection.hasRegistered(e.getValue())) {
				e.getKey().setParent((GroupSubject) collection.get(e.getValue()));
			}
		}

		return collection;
	}
}