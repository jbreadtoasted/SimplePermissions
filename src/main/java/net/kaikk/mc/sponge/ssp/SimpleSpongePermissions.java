package net.kaikk.mc.sponge.ssp;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionDescription.Builder;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;

import net.kaikk.mc.sponge.ssp.commands.GroupCommand;
import net.kaikk.mc.sponge.ssp.commands.GroupsCommand;
import net.kaikk.mc.sponge.ssp.commands.ReloadCommand;
import net.kaikk.mc.sponge.ssp.commands.TestCommand;
import net.kaikk.mc.sponge.ssp.commands.UserCommand;
import net.kaikk.mc.sponge.ssp.subject.GroupSubject;
import net.kaikk.mc.sponge.ssp.subject.GroupSubjectCollection;
import net.kaikk.mc.sponge.ssp.subject.SimpleSubjectCollection;
import net.kaikk.mc.sponge.ssp.subject.UserSubject;
import net.kaikk.mc.sponge.ssp.subject.UserSubjectCollection;
import net.kaikk.mc.sponge.ssp.subject.serializer.GroupSubjectCollectionSerializer;
import net.kaikk.mc.sponge.ssp.subject.serializer.SimpleSubjectCollectionSerializer;
import net.kaikk.mc.sponge.ssp.subject.serializer.UserSubjectCollectionSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

@Plugin(id="simplespongepermissions", name = "SimpleSpongePermissions", version = "0.9", description = "A very simple basic permissions plugin")
public class SimpleSpongePermissions implements PermissionService {
	private static SimpleSpongePermissions instance;
	
	private UserSubjectCollection users = new UserSubjectCollection();
	private GroupSubjectCollection groups = new GroupSubjectCollection();
	private Map<String, SubjectCollection> knownSubjectsMap = new HashMap<String,SubjectCollection>();
	
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path privateConfigDir;
	
	public SimpleSpongePermissions() {
		instance = this;
		knownSubjectsMap.put(PermissionService.SUBJECTS_USER, this.getUserSubjects());
		knownSubjectsMap.put(PermissionService.SUBJECTS_GROUP, this.getGroupSubjects());
	}
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) throws Exception {
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
				.description(Text.of("SimpleSpongePermissions User Command"))
				.arguments(GenericArguments.user(Text.of("user")), GenericArguments.optional(Utils.buildChoices("choice", "add", "remove", "setgroup", "addgroup", "removegroup", "test")), GenericArguments.optional(GenericArguments.string(Text.of("param"))))
				.permission("ssp.manage")
				.executor(new UserCommand(this)).build(), "puser");
		
		Sponge.getCommandManager().register(this, CommandSpec.builder()
				.description(Text.of("SimpleSpongePermissions Group Command"))
				.arguments(GenericArguments.string(Text.of("group")), GenericArguments.optional(Utils.buildChoices("choice", "create", "delete", "add", "remove", "parent", "weight", "test")), GenericArguments.optionalWeak(GenericArguments.integer(Text.of("weight"))), GenericArguments.optional(GenericArguments.string(Text.of("param"))))
				.permission("ssp.manage")
				.executor(new GroupCommand(this)).build(), "pgroup");
		
		Sponge.getCommandManager().register(this, CommandSpec.builder()
				.description(Text.of("SimpleSpongePermissions Groups Command"))
				.permission("ssp.manage")
				.executor(new GroupsCommand(this)).build(), "pgroups");
		
		Sponge.getCommandManager().register(this, CommandSpec.builder()
				.description(Text.of("SimpleSpongePermissions Reload Command"))
				.permission("ssp.manage")
				.executor(new ReloadCommand(this)).build(), "preload");
		
		Sponge.getCommandManager().register(this, CommandSpec.builder()
				.description(Text.of("SimpleSpongePermissions Test Command"))
				.arguments(GenericArguments.string(Text.of("permission")))
				.executor(new TestCommand(this)).build(), "ptest");
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

	@Override
	public Subject getDefaults() {
		return groups.get("default");
	}

	@Override
	public SubjectCollection getSubjects(String identifier) {
		if (identifier.equals(PermissionService.SUBJECTS_USER)) {
			return this.getUserSubjects();
		} else if (identifier.equals(PermissionService.SUBJECTS_GROUP)) {
			return this.getGroupSubjects();
		}
		throw new UnsupportedOperationException("Identifier "+identifier+" is not implemented yet.");
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

	public void loadData() throws IOException, ObjectMappingException {
		privateConfigDir.toFile().mkdirs();
		if (privateConfigDir.resolve("groups.conf").toFile().exists()) {
			ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(privateConfigDir.resolve("groups.conf")).build();
			ConfigurationNode rootNode = loader.load();
			this.groups = rootNode.getValue(TypeToken.of(GroupSubjectCollection.class), this.groups);
		}
		
		if (privateConfigDir.resolve("users.conf").toFile().exists()) {
			ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(privateConfigDir.resolve("users.conf")).build();
			ConfigurationNode rootNode = loader.load();
			this.users = rootNode.getValue(TypeToken.of(UserSubjectCollection.class), this.users);
		}
	}
	
	public void saveData() throws IOException, ObjectMappingException {
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
	}
	
	/**
	 * Instance for SimpleSpongePermissions.
	 * @return The SimpleSpongePermissions instance
	 */
	public static SimpleSpongePermissions instance() {
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
}
