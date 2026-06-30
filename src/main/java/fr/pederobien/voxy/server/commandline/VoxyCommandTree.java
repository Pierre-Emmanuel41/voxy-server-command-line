package fr.pederobien.voxy.server.commandline;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import fr.pederobien.commandtree.impl.NodeHelper;
import fr.pederobien.commandtree.impl.Tree;
import fr.pederobien.commandtree.interfaces.INode;
import fr.pederobien.commandtree.interfaces.INodeBuilder;
import fr.pederobien.commandtree.interfaces.IResult;
import fr.pederobien.commandtree.interfaces.ITree;
import fr.pederobien.communication.impl.layer.AesSafeLayerInitializer;
import fr.pederobien.communication.impl.layer.SimpleCertificate;
import fr.pederobien.voxy.server.impl.VoxyServerFactory;
import fr.pederobien.voxy.server.impl.config.VoxyServerConfig;
import fr.pederobien.voxy.server.interfaces.IVoxyPlayer;
import fr.pederobien.voxy.server.interfaces.IVoxyRoom;
import fr.pederobien.voxy.server.interfaces.IVoxyServer;

public class VoxyCommandTree {
	private static final String CREATE = "create";
	private static final String OPEN = "open";
	private static final String CLOSE = "close";
	private static final String DISPOSE = "dispose";
	private static final String ADD = "add";
	private static final String REMOVE = "remove";
	private static final String SET = "set";
	private static final String NAME = "name";
	private static final String PLAYBACK = "playback";
	private static final String LIST = "list";
	private static final String ROOM = "room";
	private static final String PLAYER = "player";
	private static final String MUTE = "mute";
	private static final String COORDINATES = "coordinates";
	private static final String SPHERE = "sphere";
	private static final String RADIUS = "radius";
	private static final String ENABLE = "enable";
	private final ITree<IVoxyServer> tree;

	/**
	 * Creates a command tree to interact with a voxy server
	 */
	public VoxyCommandTree() {
		tree = new Tree<IVoxyServer>();

		INodeBuilder<IVoxyServer> builder;

		// Create -------------------------------------------------------------
		builder = tree.getNodeBuilder(CREATE, "To create a new server");
		builder.withAvailability(server -> server == null || server.isDisposed());
		builder.withExecution((tree, args) -> create(tree, args));
		tree.add(builder.build());

		// Open ---------------------------------------------------------------
		builder = tree.getNodeBuilder(OPEN, "To open a server");
		builder.withAvailability(server -> server != null && !server.isDisposed());
		builder.withExecution((tree, args) -> open(tree, args));
		tree.add(builder.build());

		// Close --------------------------------------------------------------
		builder = tree.getNodeBuilder(CLOSE, "To close a server");
		builder.withAvailability(server -> server != null && !server.isDisposed());
		builder.withExecution((tree, args) -> close(tree, args));
		tree.add(builder.build());

		// Dispose ------------------------------------------------------------
		builder = tree.getNodeBuilder(DISPOSE, "To dispose a server");
		builder.withAvailability(server -> server != null && !server.isDisposed());
		builder.withExecution((tree, args) -> dispose(tree, args));
		tree.add(builder.build());

		// Add ----------------------------------------------------------------
		builder = tree.getNodeBuilder(ADD, "To add a room to the server or a player to a room");
		builder.withAvailability(server -> server != null);
		INode<IVoxyServer> add = builder.build();
		tree.add(add);

		// Add Room -----------------------------------------------------------
		builder = tree.getNodeBuilder(ROOM, "To add a room to the server");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> addRoom(tree, args));
		add.add(builder.build());

		// Add Player ---------------------------------------------------------
		builder = tree.getNodeBuilder(PLAYER, "To add a player to a room");
		builder.withAvailability(server -> server != null && server.getRooms().size() > 0 && server.getPlayers().size() > 0);
		builder.withExecution((tree, args) -> addPlayer(tree, args));
		add.add(builder.build());

		// Remove -------------------------------------------------------------
		builder = tree.getNodeBuilder(REMOVE, "To remove a room from the server or a player from a room");
		builder.withAvailability(server -> server != null);
		INode<IVoxyServer> remove = builder.build();
		tree.add(remove);

		// Remove Room --------------------------------------------------------
		builder = tree.getNodeBuilder(ROOM, "To remove a room from the server");
		builder.withAvailability(server -> server != null && server.getRooms().size() > 0);
		builder.withExecution((tree, args) -> removeRoom(tree, args));
		remove.add(builder.build());

		// Remove Player ------------------------------------------------------
		builder = tree.getNodeBuilder(PLAYER, "To remove a player from a room");
		builder.withAvailability(server -> server != null && server.getRooms().size() > 0 && server.getPlayers().size() > 0);
		builder.withExecution((tree, args) -> removePlayer(tree, args));
		remove.add(builder.build());

		// Set ----------------------------------------------------------------
		builder = tree.getNodeBuilder(SET, "To modify the properties of a room or of a player");
		builder.withAvailability(server -> server != null);
		INode<IVoxyServer> set = builder.build();
		tree.add(set);

		// Set Room -----------------------------------------------------------
		builder = tree.getNodeBuilder(ROOM, "To modify the properties of a room");
		builder.withAvailability(server -> server != null && server.getRooms().size() > 0);
		INode<IVoxyServer> room = builder.build();
		set.add(room);

		// Set Room Name ------------------------------------------------------
		builder = tree.getNodeBuilder(NAME, "To modify the name of a room");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> setRoomName(tree, args));
		room.add(builder.build());

		// Set Room Playback --------------------------------------------------
		builder = tree.getNodeBuilder(PLAYBACK, "To enable/disable the playback on a room");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> setPlayback(tree, args));
		room.add(builder.build());

		// Set Player ---------------------------------------------------------
		builder = tree.getNodeBuilder(PLAYER, "To modify player's properties");
		builder.withAvailability(server -> server != null && !server.getPlayers().isEmpty());
		INode<IVoxyServer> player = builder.build();
		set.add(player);

		// Set Player Mute ----------------------------------------------------
		builder = tree.getNodeBuilder(MUTE, "To mute or unmute a player");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> setMute(tree, args));
		player.add(builder.build());

		// Set Player Coordinates ---------------------------------------------
		builder = tree.getNodeBuilder(COORDINATES, "To set the coordinates of a player");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> setCoordinates(tree, args));
		player.add(builder.build());

		// Set Player Sphere --------------------------------------------------
		builder = tree.getNodeBuilder(SPHERE, "To modify player's sound sphere properties");
		builder.withAvailability(server -> server != null);
		INode<IVoxyServer> sphere = builder.build();
		player.add(sphere);

		// Set Player Sphere radius -------------------------------------------
		builder = tree.getNodeBuilder(RADIUS, "To set the radius of a player's sound sphere");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> setSphereRadius(tree, args));
		sphere.add(builder.build());

		// Set Player Sphere enable -------------------------------------------
		builder = tree.getNodeBuilder(ENABLE, "To enable or disable player's sound sphere");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> setSphereEnable(tree, args));
		sphere.add(builder.build());

		// List ---------------------------------------------------------------
		builder = tree.getNodeBuilder(LIST, "To list each room registered on the server");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> list(tree, args));
		tree.add(builder.build());
	}

	/**
	 * @return The tree to interact with a voxy server.
	 */
	public ITree<IVoxyServer> getTree() {
		return tree;
	}

	private IResult create(ITree<IVoxyServer> tree, String[] args) {
		if (args.length < 2)
			return NodeHelper.result(false, "The server's name or the server's port number is missing");

		if (tree.getSeed() != null && !tree.getSeed().isDisposed()) {
			tree.getSeed().close();
			tree.getSeed().dispose();
		}

		String name = args[0];

		if (!NodeHelper.isStrictInt(args[1]))
			return NodeHelper.result(false, "The server's port number shall be of type int");

		int port = NodeHelper.parseInt(args[1]);

		VoxyServerConfig config = VoxyServerFactory.createConfig(name, "*", port);
		config.getTcpConfig().setLayerInitializer(() -> new AesSafeLayerInitializer(new SimpleCertificate()));
		config.getUdpConfig().setLayerInitializer(() -> new AesSafeLayerInitializer(new SimpleCertificate()));
		config.getUdpConfig().setMin(40000);
		config.getUdpConfig().setMax(50000);

		tree.setSeed(VoxyServerFactory.createServer(config));
		return NodeHelper.result(true, "Server \"%s\" created successfully", tree.getSeed().getName());
	}

	private IResult open(ITree<IVoxyServer> tree, String[] args) {
		if (tree.getSeed().isOpened())
			return NodeHelper.result(true, "Server \"%s\" is already opened", tree.getSeed().getName());

		if (tree.getSeed().open())
			return NodeHelper.result(true, "Server \"%s\" opened successfully", tree.getSeed().getName());
		else
			return NodeHelper.result(false, "Server \"%s\" could not be opened", tree.getSeed().getName());
	}

	private IResult close(ITree<IVoxyServer> tree, String[] args) {
		if (!tree.getSeed().isOpened())
			return NodeHelper.result(true, "Server \"%s\" is already closed", tree.getSeed().getName());

		if (tree.getSeed().close())
			return NodeHelper.result(true, "Server \"%s\" closed successfully", tree.getSeed().getName());
		else
			return NodeHelper.result(false, "Server \"%s\" could not be closed", tree.getSeed().getName());
	}

	private IResult dispose(ITree<IVoxyServer> tree, String[] args) {
		if (tree.getSeed().dispose())
			return NodeHelper.result(true, "Server \"%s\" disposed successfully", tree.getSeed().getName());
		else
			return NodeHelper.result(false, "Server \"%s\" could not be disposed", tree.getSeed().getName());
	}

	private IResult addRoom(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 0)
			return NodeHelper.result(false, "The room's name is missing");

		String name = args[0];
		Optional<IVoxyRoom> optional = tree.getSeed().getRooms().get(name);

		if (!optional.isEmpty())
			return NodeHelper.result(false, "The room \"%s\" is already registered on server \"%s\"", name, tree.getSeed().getName());

		int port = 0;
		if (1 < args.length) {
			if (NodeHelper.isStrictInt(args[1]))
				return NodeHelper.result(false, "The room's port number cannot be parsed, it shall be an integer");

			port = NodeHelper.parseInt(args[1]);
		}

		if (!tree.getSeed().getRooms().add(name, port, tree.getSeed()))
			return NodeHelper.result(false, "An external plugin cancelled the adding of room %s from server \"%s\"", name, tree.getSeed().getName());

		return NodeHelper.result(true, "The room \"%s\" has been added to server \"%s\"", name, tree.getSeed().getName());
	}

	private IResult removeRoom(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 0)
			return NodeHelper.result(false, "The room's name is missing");

		String name = args[0];
		Optional<IVoxyRoom> optional = tree.getSeed().getRooms().get(name);

		if (optional.isEmpty())
			return NodeHelper.result(false, "The room \"%s\" is not registered on server %s", name, tree.getSeed().getName());

		if (!tree.getSeed().getRooms().remove(optional.get().getName(), tree.getSeed()))
			return NodeHelper.result(false, "An external plugin cancelled the removing of room %s from server \"%\"", name, tree.getSeed().getName());

		return NodeHelper.result(true, "The room \"%s\" has been removed from server \"%s\"", name, tree.getSeed().getName());
	}

	private IResult setRoomName(ITree<IVoxyServer> tree, String[] args) {
		if (args.length < 2)
			return NodeHelper.result(false, "The name of the room to rename or the room's new name is missing");

		String oldName = args[0];
		Optional<IVoxyRoom> toRename = tree.getSeed().getRooms().get(oldName);

		if (toRename.isEmpty())
			return NodeHelper.result(false, "The room \"%s\" is not registered on server \"%s\"", oldName, tree.getSeed().getName());

		String newName = args[1];
		Optional<IVoxyRoom> exist = tree.getSeed().getRooms().get(newName);

		if (!exist.isEmpty())
			return NodeHelper.result(false, "The room \"%s\" is already registered on server \"%s\"", newName, tree.getSeed().getName());

		if (!toRename.get().setName(newName, tree.getSeed()))
			return NodeHelper.result(false, "An external plugin cancelled the renaming of room \"%s\" as \"%s\" on server \"%s\"", oldName, newName,
					tree.getSeed().getName());

		return NodeHelper.result(true, "The room \"%s\" has been renamed as \"%s\" on server \"%s\"", oldName, newName, tree.getSeed().getName());
	}

	private IResult setPlayback(ITree<IVoxyServer> tree, String[] args) {
		if (args.length < 2)
			return NodeHelper.result(false, "The name of the room to modify or the playback status is missing");

		Optional<IVoxyRoom> room = tree.getSeed().getRooms().get(args[0]);
		if (room.isEmpty())
			return NodeHelper.result(false, "The room \"%s\" is not registered on server \"%s\"", args[0], tree.getSeed().getName());

		if (!NodeHelper.isStrictBool(args[1]))
			return NodeHelper.result(false, "The playback state cannot be parsed, it shall be \"true\" or \"false\", case ignored");

		boolean playback = NodeHelper.parseBool(args[1]);
		room.get().setPlayBack(playback);
		return NodeHelper.result(true, "The playback is %s on room \"%s\"", playback ? "enabled" : "disabled", room.get().getName());
	}

	private IResult list(ITree<IVoxyServer> tree, String[] args) {
		if (tree.getSeed().getRooms().size() == 0)
			return NodeHelper.result(true, "The server \"%s\" does not have any room", tree.getSeed().getName());

		List<IVoxyRoom> rooms = tree.getSeed().getRooms().toList();
		IResult result = NodeHelper.result(true, "");

		for (IVoxyRoom room : rooms) {
			StringJoiner joiner = new StringJoiner(", ", "[", "]");
			if (room.getPlayers().size() == 0)
				joiner.add("no player");
			else
				for (IVoxyPlayer player : room.getPlayers().toList())
					joiner.add(player.getName());

			result.getFeedbacks().add(String.format("%s: %s", room.getName(), joiner));
		}

		return result;
	}

	private IResult addPlayer(ITree<IVoxyServer> tree, String[] args) {
		if (args.length < 2)
			return NodeHelper.result(false, "The room's name or the player's name is missing");

		Optional<IVoxyRoom> room = tree.getSeed().getRooms().get(args[0]);
		if (!room.isPresent())
			return NodeHelper.result(false, "The room \"%s\" is not registered on server \"%s\"", args[0], tree.getSeed().getName());

		Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[1]);
		if (!player.isPresent())
			return NodeHelper.result(false, "The player \"%s\" is not registered on server \"%s\"", args[1], tree.getSeed().getName());

		Optional<IVoxyPlayer> registered = room.get().getPlayers().get(player.get().getName());
		if (registered.isPresent())
			return NodeHelper.result(false, "The player \"%s\" is already registered in room %s", player.get().getName(), room.get().getName());

		if (!room.get().getPlayers().add(player.get().getName(), tree.getSeed()))
			return NodeHelper.result(false, "An external plugin cancelled the adding of player \"%s\" to room \"%s\"", player.get().getName(), room.get().getName());

		return NodeHelper.result(true, "The player \"%s\" has been added to room \"%s\"", player.get().getName(), room.get().getName());
	}

	private IResult removePlayer(ITree<IVoxyServer> tree, String[] args) {
		if (args.length < 2)
			return NodeHelper.result(false, "The room's name or the player's name is missing");

		Optional<IVoxyRoom> room = tree.getSeed().getRooms().get(args[0]);
		if (!room.isPresent())
			return NodeHelper.result(false, "The room \"%s\" is not registered on server \"%s\"", args[0], tree.getSeed().getName());

		Optional<IVoxyPlayer> player = room.get().getPlayers().get(args[1]);
		if (!player.isPresent())
			return NodeHelper.result(false, "The player \"%s\" is no registered in room \"%s\"", args[1], args[0]);

		if (!room.get().getPlayers().remove(player.get().getName()))
			return NodeHelper.result(false, "An external plugin cancelled the removing of player \"%s\" from room \"%s\"", player.get().getName(), room.get().getName());

		return NodeHelper.result(true, "The player \"%s\" has been removed from room \"%s\"", player.get().getName(), room.get().getName());
	}

	private IResult setMute(ITree<IVoxyServer> tree, String[] args) {
		if (args.length < 2)
			return NodeHelper.result(false, "The player's name or the mute status is missing");

		Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[0]);
		if (!player.isPresent())
			return NodeHelper.result(false, "The player \"%s\" is not registered on server \"%s\"", args[0], tree.getSeed().getName());

		Optional<IVoxyRoom> room = tree.getSeed().getRooms().getRoomByPlayerName(player.get().getName());
		if (!room.isPresent())
			return NodeHelper.result(false, "The player \"%s\" is not registered in a room", player.get().getName());

		if (!NodeHelper.isStrictBool(args[1]))
			return NodeHelper.result(false, "The mute status cannot be parsed, it shall be \"true\" or \"false\", case ignored");

		boolean isMute = NodeHelper.parseBool(args[1]);
		if (player.get().isMute() == isMute)
			return NodeHelper.result(true, "The player \"%s\" is already %s", player.get().getName(), isMute ? "muted" : "unmuted");

		if (player.get().setMute(isMute, tree.getSeed()))
			return NodeHelper.result(false, "An external plugin cancelled the change of the mute status of player \"%s\"", player.get().getName());

		return NodeHelper.result(true, "The player \"%s\" is %s", player.get().getName(), isMute ? "muted" : "unmuted");
	}

	private IResult setCoordinates(ITree<IVoxyServer> tree, String[] args) {
		if (args.length < 7)
			return NodeHelper.result(false, "The player's name or one coordinates (x, y, z, yaw, pitch, roll) is missing");

		Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[0]);
		if (!player.isPresent())
			return NodeHelper.result(false, "The player \"%s\" is not registered on server \"%s\"", args[0], tree.getSeed().getName());

		if (!NodeHelper.isStrictDouble(args[1]))
			return NodeHelper.result(false, "The x value cannot be parsed, it shall be a decimal value with \".\" as separator");

		double x = NodeHelper.parseDouble(args[1]);

		if (!NodeHelper.isStrictDouble(args[2]))
			return NodeHelper.result(false, "The y value cannot be parsed, it shall be a decimal value with \".\" as separator");

		double y = NodeHelper.parseDouble(args[2]);

		if (!NodeHelper.isStrictDouble(args[3]))
			return NodeHelper.result(false, "The z value cannot be parsed, it shall be a decimal value with \".\" as separator");

		double z = NodeHelper.parseDouble(args[3]);

		if (!NodeHelper.isStrictDouble(args[4]))
			return NodeHelper.result(false, "The yaw value cannot be parsed, it shall be a decimal value with \".\" as separator");

		double yaw = NodeHelper.parseDouble(args[4]);

		if (!NodeHelper.isStrictDouble(args[5]))
			return NodeHelper.result(false, "The pitch value cannot be parsed, it shall be a decimal value with \".\" as separator");

		double pitch = NodeHelper.parseDouble(args[5]);

		if (!NodeHelper.isStrictDouble(args[6]))
			return NodeHelper.result(false, "The roll value cannot be parsed, it shall be a decimal value with \".\" as separator");

		double roll = NodeHelper.parseDouble(args[6]);

		player.get().getCoordinates().update(x, y, z, yaw, pitch, roll);
		return NodeHelper.result(true, "%s's coordinates updated: [%s, %s, %s, %s, %s, %s]", player.get().getName(), x, y, z, yaw, pitch, roll);
	}

	private IResult setSphereRadius(ITree<IVoxyServer> tree, String[] args) {
		if (args.length < 4)
			return NodeHelper.result(false, "The player's name or one radius (x-radius, y-radius, z-radius) is missing");

		Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[0]);
		if (!player.isPresent())
			return NodeHelper.result(false, "The player \"%s\" is not registered on server \"%s\"", args[0], tree.getSeed().getName());

		if (!NodeHelper.isStrictDouble(args[1]))
			return NodeHelper.result(false, "The x-radius value cannot be parsed, it shall be a decimal value with \".\" as separator");

		double xRadius = NodeHelper.parseDouble(args[1]);

		if (!NodeHelper.isStrictDouble(args[2]))
			return NodeHelper.result(false, "The y-radius value cannot be parsed, it shall be a decimal value with \".\" as separator");

		double yRadius = NodeHelper.parseDouble(args[2]);

		if (!NodeHelper.isStrictDouble(args[3]))
			return NodeHelper.result(false, "The z-radius value cannot be parsed, it shall be a decimal value with \".\" as separator");

		double zRadius = NodeHelper.parseDouble(args[3]);

		player.get().getSoundSphere().setRadius(xRadius, yRadius, zRadius);
		return NodeHelper.result(true, "%s's sphere radius updated: [%s, %s, %s]", player.get().getName(), xRadius, yRadius, zRadius);
	}

	private IResult setSphereEnable(ITree<IVoxyServer> tree, String[] args) {
		if (args.length < 2)
			return NodeHelper.result(false, "The player's name or the enable state is missing");

		Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[0]);
		if (!player.isPresent())
			return NodeHelper.result(false, "The player \"%s\" is not registered on server \"%s\"", args[0], tree.getSeed().getName());

		if (!NodeHelper.isStrictBool(args[1]))
			return NodeHelper.result(false, "The enable status cannot be parsed, it shall be \"true\" or \"false\", case ignored");

		boolean isEnabled = NodeHelper.parseBool(args[1]);
		player.get().getSoundSphere().setEnabled(isEnabled);
		return NodeHelper.result(true, "The sound sphere of player \"%s\" is %s", player.get().getName(), isEnabled ? "enabled" : "disabled");
	}
}
