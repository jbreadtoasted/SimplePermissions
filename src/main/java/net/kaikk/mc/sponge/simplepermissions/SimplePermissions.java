package net.kaikk.mc.sponge.simplepermissions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionDescription.Builder;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;

import net.kaikk.mc.sponge.simplepermissions.commands.DebugCommand;
import net.kaikk.mc.sponge.simplepermissions.commands.GroupCommand;
import net.kaikk.mc.sponge.simplepermissions.commands.GroupsCommand;
import net.kaikk.mc.sponge.simplepermissions.commands.TestCommand;
import net.kaikk.mc.sponge.simplepermissions.commands.UserCommand;
import net.kaikk.mc.sponge.simplepermissions.subject.GroupSubject;
import net.kaikk.mc.sponge.simplepermissions.subject.GroupSubjectCollection;
import net.kaikk.mc.sponge.simplepermissions.subject.SimpleSubjectCollection;
import net.kaikk.mc.sponge.simplepermissions.subject.UserSubject;
import net.kaikk.mc.sponge.simplepermissions.subject.UserSubjectCollection;
import net.kaikk.mc.sponge.simplepermissions.subject.serializer.GroupSubjectCollectionSerializer;
import net.kaikk.mc.sponge.simplepermissions.subject.serializer.SimpleSubjectCollectionSerializer;
import net.kaikk.mc.sponge.simplepermissions.subject.serializer.UserSubjectCollectionSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

@Plugin(id=PluginInfo.id, name = PluginInfo.name, version = PluginInfo.version, description = PluginInfo.description)
public class SimplePermissions implements PermissionService {
	private static SimplePermissions instance;
	
	private UserSubjectCollection users = new UserSubjectCollection();
	private GroupSubjectCollection groups = new GroupSubjectCollection();
	private Map<String, SubjectCollection> knownSubjectsMap = new ConcurrentHashMap<String,SubjectCollection>();
	private Map<String, SubjectCollection> customSubjectsMap = new ConcurrentHashMap<String,SubjectCollection>();
	
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path privateConfigDir;
	
	@Inject
	private Logger logger;
	
	public boolean debug;
	
	private boolean loaded;
	
	@Listener
	public void onGameInitialization(GameInitializationEvent event) throws Exception {
		instance = this;

		knownSubjectsMap.put(PermissionService.SUBJECTS_USER, this.getUserSubjects());
		knownSubjectsMap.put(PermissionService.SUBJECTS_GROUP, this.getGroupSubjects());
		
		// register serializers
		TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(SimpleSubjectCollection.class), new SimpleSubjectCollectionSerializer());
		TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(UserSubjectCollection.class), new UserSubjectCollectionSerializer());
		TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(GroupSubjectCollection.class), new GroupSubjectCollectionSerializer());
		
		// load data
		this.loadData();
		
		// register permission service
		Sponge.getServiceManager().setProvider(this, PermissionService.class, this);
		
		
		// register commands
		Sponge.getCommandManager().register(this, CommandSpec.builder()
				.description(Text.of("SimplePermissions User Command"))
				.arguments(GenericArguments.user(Text.of("user")), GenericArguments.optional(Utils.buildChoices("choice", "add", "remove", "setgroup", "addgroup", "removegroup", "option", "test")), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("param"))))
				.permission("simplepermissions.manage")
				.executor(new UserCommand(this)).build(), "puser");
		
		Sponge.getCommandManager().register(this, CommandSpec.builder()
				.description(Text.of("SimplePermissions Group Command"))
				.arguments(GenericArguments.string(Text.of("group")), GenericArguments.optional(Utils.buildChoices("choice", "create", "delete", "add", "remove", "parent", "weight", "option", "test")), GenericArguments.optionalWeak(GenericArguments.integer(Text.of("weight"))), GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("param"))))
				.permission("simplepermissions.manage")
				.executor(new GroupCommand(this)).build(), "pgroup");
		
		Sponge.getCommandManager().register(this, CommandSpec.builder()
				.description(Text.of("SimplePermissions Groups Command"))
				.permission("simplepermissions.manage")
				.executor(new GroupsCommand(this)).build(), "pgroups");
		
		Sponge.getCommandManager().register(this, CommandSpec.builder()
				.description(Text.of("SimplePermissions Test Command"))
				.arguments(GenericArguments.string(Text.of("permission")))
				.executor(new TestCommand(this)).build(), "ptest");
		
		Sponge.getCommandManager().register(this, CommandSpec.builder()
				.description(Text.of("SimplePermissions Debug Command"))
				.permission("simplepermissions.manage")
				.arguments(GenericArguments.bool(Text.of("truefalse")))
				.executor(new DebugCommand(this)).build(), "pdebug");
		
		loaded = true;
	}
	
	@Listener
	public void onGameReload(GameReloadEvent event) throws IOException, ObjectMappingException {
		loaded = false;
		this.loadData();
		loaded = true;
	}
	
	@Listener
	public void onGameStopping(GameStoppingEvent event) throws Exception {
		if (loaded) {
			this.saveData();
		}
	}

	@Override
	public void registerContextCalculator(ContextCalculator<Subject> calculator) {
		// nope
	}

	@Override
	public SubjectCollection getUserSubjects() {
		return users;
	}

	@Override
	public SubjectCollection getGroupSubjects() {
		return groups;
	}
	
	// SpongeAPI 4 Override
	public SubjectData getDefaultData() {
		return groups.get("default").getSubjectData();
	}

	// SpongeAPI 5 Override
	public Subject getDefaults() {
		return groups.get("default");
	}

	@Override
	public SubjectCollection getSubjects(String identifier) {
		if (this.debug) {
			this.logger().info("Requested subject collection '"+identifier+"' by "+Utils.getCaller());
		}
		if (identifier.equals(PermissionService.SUBJECTS_USER)) {
			return this.getUserSubjects();
		} else if (identifier.equals(PermissionService.SUBJECTS_GROUP) || identifier.equals("default")) {
			return this.getGroupSubjects();
		}
		
		SubjectCollection collection = knownSubjectsMap.get(identifier);
		if (collection==null) {
			if (this.debug) {
				this.logger().info("Creating new custom subject collection "+identifier);
			}
			collection = new SimpleSubjectCollection(identifier);
			knownSubjectsMap.put(identifier, collection);
			customSubjectsMap.put(identifier, collection);
		}
		return collection;
	}

	@Override
	public Map<String, SubjectCollection> getKnownSubjects() {
		return knownSubjectsMap;
	}

	@Override
	public Optional<Builder> newDescriptionBuilder(Object plugin) {
		return Optional.empty(); // TODO
	}

	@Override
	public Optional<PermissionDescription> getDescription(String permission) {
		return Optional.empty(); // TODO
	}

	@Override
	public Collection<PermissionDescription> getDescriptions() {
		return Collections.emptyList(); // TODO
	}

	synchronized public void loadData() throws IOException, ObjectMappingException {
		privateConfigDir.toFile().mkdirs();
		if (privateConfigDir.resolve("groups.conf").toFile().exists()) {
			ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(privateConfigDir.resolve("groups.conf")).build();
			ConfigurationNode rootNode = loader.load();
			GroupSubjectCollection groups = rootNode.getValue(TypeToken.of(GroupSubjectCollection.class));
			if (groups!=null) {
				this.groups = groups;
				logger.info("Loaded "+groups.size()+" groups");
			} else {
				logger.warn("Couldn't read groups permission file");
			}
			knownSubjectsMap.put(PermissionService.SUBJECTS_GROUP, this.getGroupSubjects());
		}
		
		if (privateConfigDir.resolve("users.conf").toFile().exists()) {
			ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(privateConfigDir.resolve("users.conf")).build();
			ConfigurationNode rootNode = loader.load();
			UserSubjectCollection users = rootNode.getValue(TypeToken.of(UserSubjectCollection.class));
			if (users!=null) {
				this.users = users;
				logger.info("Loaded "+users.size()+" users");
			} else {
				logger.warn("Couldn't read users permission file");
			}
			knownSubjectsMap.put(PermissionService.SUBJECTS_USER, this.getUserSubjects());
		}
		
		File customSubjectsDir = privateConfigDir.resolve("CustomSubjects").toFile();
		customSubjectsDir.mkdirs();
		for (File file : customSubjectsDir.listFiles()) {
			ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(file.toPath()).build();
			ConfigurationNode rootNode = loader.load();
			String identifier = rootNode.getNode("identifier").getString();
			if (identifier!=null) {
				SimpleSubjectCollection collection = rootNode.getNode("data").getValue(TypeToken.of(SimpleSubjectCollection.class));
				if (collection!=null) {
					knownSubjectsMap.put(identifier, collection);
					customSubjectsMap.put(identifier, collection);
					logger.info("Loaded "+collection.size()+" "+identifier);
				} else {
					logger.warn("Couldn't read "+identifier+" permission file");
				}
			} else {
				logger.warn("Couldn't read "+file.getName()+" permission file");
			}
		}
	}
	
	synchronized public void saveData() throws IOException, ObjectMappingException {
		privateConfigDir.toFile().mkdirs();
		// users
		ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(privateConfigDir.resolve("users.conf")).build();
		ConfigurationNode node = loader.createEmptyNode(ConfigurationOptions.defaults());
		node.setValue(TypeToken.of(UserSubjectCollection.class), this.users);
		loader.save(node);
		
		// groups
		loader = HoconConfigurationLoader.builder().setPath(privateConfigDir.resolve("groups.conf")).build();
		node = loader.createEmptyNode(ConfigurationOptions.defaults());
		node.setValue(TypeToken.of(GroupSubjectCollection.class), this.groups);
		loader.save(node);
		
		// other identifiers
		privateConfigDir.resolve("CustomSubjects").toFile().mkdirs();
		for (Entry<String,SubjectCollection> e : this.customSubjectsMap.entrySet()) {
			loader = HoconConfigurationLoader.builder().setPath(privateConfigDir.resolve("CustomSubjects"+File.separator+e.getKey()+".conf")).build();
			node = loader.createEmptyNode(ConfigurationOptions.defaults());
			node.getNode("identifier").setValue(e.getKey());
			node.getNode("data").setValue(TypeToken.of(SimpleSubjectCollection.class), (SimpleSubjectCollection) e.getValue());
			loader.save(node);
		}
	}
	
	/**
	 * Instance for SimpleSpongePermissions.
	 * @return The SimpleSpongePermissions instance
	 */
	public static SimplePermissions instance() {
		return instance;
	}
	
    /**
     * Set a permission to a given value for the specified user.
     * Setting value as {@link Tristate#UNDEFINED} unsets the permission.
     *
     * @param user The user
     * @param permission The permission to set
     * @param value The value to set this permission to
     * @return Whether the operation was successful
     */
	public boolean setPermission(User user, String permission, Tristate value) {
		return instance.getUserSubjects().get(user.getIdentifier()).getSubjectData().setPermission(null, permission, value);
	}
	
	/**
     * Set a permission to a given value for the specified group.
     * Setting value as {@link Tristate#UNDEFINED} unsets the permission.
     *
     * @param groupName The group name
     * @param permission The permission to set
     * @param value The value to set this permission to
     * @return Whether the operation was successful
     */
	public boolean setGroupPermission(String groupName, String permission, Tristate value) {
		return this.setGroupPermission((GroupSubject) instance.getGroupSubjects().get(groupName), permission, value);
	}
	
	/**
     * Set a permission to a given value for the specified group.
     * Setting value as {@link Tristate#UNDEFINED} unsets the permission.
     *
     * @param groupName The group name
     * @param permission The permission to set
     * @param value The value to set this permission to
     * @return Whether the operation was successful
     */
	public boolean setGroupPermission(GroupSubject group, String permission, Tristate value) {
		return group.getSubjectData().setPermission(null, permission, value);
	}
	

	/** 
	 * Add user to the specified group
	 * @param user The user
	 * @param groupName The group name
	 * @return Whether the operation was successful
	 * @throws IllegalArgumentException If the group doesn't exist
	 * @throws IOException If there was an issue with the permissions file
	 * @throws ObjectMappingException If Kai is human
	 */
	public boolean addUserToGroup(User user, String groupName) throws IllegalArgumentException, IOException, ObjectMappingException {
		if (groupName.equalsIgnoreCase("default")) {
			throw new IllegalArgumentException("You cannot add players to the default group!");
		}
		if (!this.getGroupSubjects().hasRegistered(groupName)) {
			throw new IllegalArgumentException("The specified group doesn't exist!");
		}
		
		GroupSubject gs = (GroupSubject) this.getGroupSubjects().get(groupName);
		UserSubject us = (UserSubject) this.getUserSubjects().get(user.getIdentifier());
		us.getGroups().add(gs);
		this.saveData();
		return true;
	}
	
	/** 
	 * Set user to the specified group. This will remove the user from any other group.
	 * @param user The user
	 * @param groupName The group name
	 * @return Whether the operation was successful
	 * @throws IllegalArgumentException If the group doesn't exist
	 * @throws IOException If there was an issue with the permissions file
	 * @throws ObjectMappingException If Kai is human
	 */
	public boolean setUserToGroup(User user, String groupName) throws IllegalArgumentException, IOException, ObjectMappingException {
		if (groupName.equalsIgnoreCase("default")) {
			throw new IllegalArgumentException("You cannot set players to the default group!");
		}
		if (!this.getGroupSubjects().hasRegistered(groupName)) {
			throw new IllegalArgumentException("The specified group doesn't exist!");
		}
		
		GroupSubject gs = (GroupSubject) this.getGroupSubjects().get(groupName);
		UserSubject us = (UserSubject) this.getUserSubjects().get(user.getIdentifier());
		us.getGroups().clear();
		us.getGroups().add(gs);
		this.saveData();
		return true;
	}
	
	/** 
	 * Remove user from the specified group
	 * @param user The user
	 * @param groupName The group name
	 * @return Whether the operation was successful
	 * @throws IllegalArgumentException If the group doesn't exist
	 * @throws IOException If there was an issue with the permissions file
	 * @throws ObjectMappingException - If Kai is human
	 */
	public boolean removeUserFromGroup(User user, String groupName) throws IllegalArgumentException, IOException, ObjectMappingException {
		if (!this.getGroupSubjects().hasRegistered(groupName)) {
			throw new IllegalArgumentException("The specified group doesn't exist!");
		}
		
		GroupSubject gs = (GroupSubject) this.getGroupSubjects().get(groupName);
		UserSubject us = (UserSubject) this.getUserSubjects().get(user.getIdentifier());
		us.getGroups().remove(gs);
		this.saveData();
		return true;
	}
	
	public boolean setOption(Subject subject, String key, String value) {
		subject.getSubjectData().setOption(SubjectData.GLOBAL_CONTEXT, key, value);
		try {
			this.saveData();
		} catch (IOException | ObjectMappingException e) {
			e.printStackTrace();
		}
		return true;
	}

	public Logger logger() {
		return logger;
	}
}
