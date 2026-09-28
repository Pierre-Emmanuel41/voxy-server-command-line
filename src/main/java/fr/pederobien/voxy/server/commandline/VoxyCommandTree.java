package fr.pederobien.voxy.server.commandline;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Stream;

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
import fr.pederobien.voxy.server.interfaces.IVoxyEffect;
import fr.pederobien.voxy.server.interfaces.IVoxyEffect.ParameterDescription;
import fr.pederobien.voxy.server.interfaces.IVoxyPlayer;
import fr.pederobien.voxy.server.interfaces.IVoxyRoom;
import fr.pederobien.voxy.server.interfaces.IVoxyServer;

public class VoxyCommandTree {
	/**
	 * Specific flag to perform an action on all objects (players, rooms etc...)
	 */
	private static final String ALL = "all";

	private static final String LOAD = "load";
	private static final String DEFAULT = "default";
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
	private static final String EFFECT = "effect";
	private final ITree<IVoxyServer> tree;

	/**
	 * Creates a command tree to interact with a voxy server
	 */
	public VoxyCommandTree() {
		tree = new Tree<IVoxyServer>();

		INodeBuilder<IVoxyServer> builder;

		// Load ---------------------------------------------------------------
		builder = tree.getNodeBuilder(LOAD, "To load a voxy server configuration");
		builder.withAvailability(server -> server == null);
		INode<IVoxyServer> load = builder.build();
		tree.add(load);

		// Load Default -------------------------------------------------------
		builder = tree.getNodeBuilder(DEFAULT, "To load the default configuration of a voxy server");
		builder.withAvailability(server -> server == null);
		builder.withExecution((tree, args) -> loadDefault(tree, args));
		load.add(builder.build());

		// Create -------------------------------------------------------------
		builder = tree.getNodeBuilder(CREATE, "To create a new server");
		builder.withAvailability(server -> server == null);
		builder.withExecution((tree, args) -> create(tree, args));
		builder.withCompletions((tree, args) -> createCompletions(tree, args));
		tree.add(builder.build());

		// Open ---------------------------------------------------------------
		builder = tree.getNodeBuilder(OPEN, "To open a server");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> open(tree, args));
		tree.add(builder.build());

		// Close --------------------------------------------------------------
		builder = tree.getNodeBuilder(CLOSE, "To close a server");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> close(tree, args));
		tree.add(builder.build());

		// Dispose ------------------------------------------------------------
		builder = tree.getNodeBuilder(DISPOSE, "To dispose a server");
		builder.withAvailability(server -> server != null);
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
		builder.withCompletions((tree, args) -> addRoomCompletions(tree, args));
		add.add(builder.build());

		// Add Player ---------------------------------------------------------
		builder = tree.getNodeBuilder(PLAYER, "To add a player to a room");
		builder.withAvailability(server -> server != null && server.getRooms().size() > 0 && server.getPlayers().size() > 0);
		builder.withExecution((tree, args) -> addPlayer(tree, args));
		builder.withCompletions((tree, args) -> addPlayerCompletions(tree, args));
		add.add(builder.build());

		// Add Effect ---------------------------------------------------------
		builder = tree.getNodeBuilder(EFFECT, "To add an effect to apply on the audio stream of a player");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> addEffect(tree, args));
		builder.withCompletions((tree, args) -> addEffectCompletions(tree, args));
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
		builder.withCompletions((tree, args) -> removeRoomCompletions(tree, args));
		remove.add(builder.build());

		// Remove Player ------------------------------------------------------
		builder = tree.getNodeBuilder(PLAYER, "To remove a player from a room");
		builder.withAvailability(server -> server != null && server.getRooms().size() > 0 && server.getPlayers().size() > 0);
		builder.withExecution((tree, args) -> removePlayer(tree, args));
		builder.withCompletions((tree, args) -> removePlayerCompletions(tree, args));
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
		builder.withCompletions((tree, args) -> setRoomNameCompletions(tree, args));
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
		builder.withCompletions((tree, args) -> setMuteCompletions(tree, args));
		player.add(builder.build());

		// Set Player Playback ------------------------------------------------
		builder = tree.getNodeBuilder(PLAYBACK, "To enable/disable the playback of a player");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> setPlayback(tree, args));
		builder.withCompletions((tree, args) -> setPlaybackCompletions(tree, args));
		player.add(builder.build());

		// Set Player Coordinates ---------------------------------------------
		builder = tree.getNodeBuilder(COORDINATES, "To set the coordinates of a player");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> setCoordinates(tree, args));
		builder.withCompletions((tree, args) -> setCoordinatesCompletions(tree, args));
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
		builder.withCompletions((tree, args) -> setSphereRadiusCompletions(tree, args));
		sphere.add(builder.build());

		// Set Player Sphere enable -------------------------------------------
		builder = tree.getNodeBuilder(ENABLE, "To enable or disable player's sound sphere");
		builder.withAvailability(server -> server != null);
		builder.withExecution((tree, args) -> setSphereEnable(tree, args));
		builder.withCompletions((tree, args) -> setSphereEnableCompletions(tree, args));
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

	private IResult loadDefault(ITree<IVoxyServer> tree, String[] args) {
		if (tree.getSeed() != null && !tree.getSeed().isDisposed()) {
			tree.getSeed().close();
			tree.getSeed().dispose();
		}

		String name = "VoxyServer";
		int port = 0;
		VoxyServerConfig config = VoxyServerFactory.createConfig(name, "*", port);
		config.getTcpConfig().setLayerInitializer(() -> new AesSafeLayerInitializer(new SimpleCertificate()));
		config.getUdpConfig().setLayerInitializer(() -> new AesSafeLayerInitializer(new SimpleCertificate()));
		config.getUdpConfig().setMin(40000);
		config.getUdpConfig().setMax(50000);

		tree.setSeed(VoxyServerFactory.createServer(config));
		return NodeHelper.result(true, "\"%s\" server created successfully", tree.getSeed().getName());
	}

	// Create -----------------------------------------------------------------
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
		return NodeHelper.result(true, "\"%s\" server created successfully", tree.getSeed().getName());
	}

	private List<String> createCompletions(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 1)
			return NodeHelper.asList("<serverName:String>");
		if (args.length == 2)
			return NodeHelper.asList("<serverPort:Int>");

		return NodeHelper.emptyList();
	}

	// Open -------------------------------------------------------------------
	private IResult open(ITree<IVoxyServer> tree, String[] args) {
		if (tree.getSeed().isOpened())
			return NodeHelper.result(true, "Server \"%s\" is already opened", tree.getSeed().getName());

		if (tree.getSeed().open()) {
			String format = "Server \"%s\" opened successfully on port %s";
			return NodeHelper.result(true, format, tree.getSeed().getName(), tree.getSeed().getPort());
		} else
			return NodeHelper.result(false, "Server \"%s\" could not be opened", tree.getSeed().getName());
	}

	// Close ------------------------------------------------------------------
	private IResult close(ITree<IVoxyServer> tree, String[] args) {
		if (!tree.getSeed().isOpened())
			return NodeHelper.result(true, "Server \"%s\" is already closed", tree.getSeed().getName());

		if (tree.getSeed().close())
			return NodeHelper.result(true, "Server \"%s\" closed successfully", tree.getSeed().getName());
		else
			return NodeHelper.result(false, "Server \"%s\" could not be closed", tree.getSeed().getName());
	}

	// Dispose ----------------------------------------------------------------
	private IResult dispose(ITree<IVoxyServer> tree, String[] args) {
		String name = tree.getSeed().getName();

		if (tree.getSeed().dispose()) {
			tree.setSeed(null);
			return NodeHelper.result(true, "Server \"%s\" disposed successfully", name);
		} else
			return NodeHelper.result(false, "Server \"%s\" could not be disposed, try to close the server first", name);
	}

	// Add Room ---------------------------------------------------------------
	private IResult addRoom(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 0)
			return NodeHelper.result(false, "The room's name is missing");

		String name = args[0];
		Optional<IVoxyRoom> optional = tree.getSeed().getRooms().get(name);

		if (!optional.isEmpty())
			return NodeHelper.result(false, "The room \"%s\" is already registered on server \"%s\"", name, tree.getSeed().getName());

		int port = 0;
		if (1 < args.length) {
			if (!NodeHelper.isStrictInt(args[1]))
				return NodeHelper.result(false, "The room's port number cannot be parsed, it shall be an integer");

			port = NodeHelper.parseInt(args[1]);
		}

		if (!tree.getSeed().getRooms().add(name, port, tree.getSeed()))
			return NodeHelper.result(false, "An external plugin cancelled the adding of room %s from server \"%s\"", name, tree.getSeed().getName());

		return NodeHelper.result(true, "The room \"%s\" has been added to server \"%s\"", name, tree.getSeed().getName());
	}

	private List<String> addRoomCompletions(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 1)
			return NodeHelper.asList("<roomName:String>");

		if (args.length == 2) {
			String name = args[0];
			// Checking if room already exists
			if (tree.getSeed().getRooms().get(name).isPresent())
				return NodeHelper.asList(String.format("Room \"%s\" already registered", name));

			return NodeHelper.asList("<roomPort:Int [Optional]>");
		}

		return NodeHelper.emptyList();
	}

	// Add Player -------------------------------------------------------------
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

	private List<String> addPlayerCompletions(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 1) {
			Stream<String> rooms = tree.getSeed().getRooms().toList().stream().map(room -> room.getName());
			return NodeHelper.filter(rooms, args);
		}

		if (args.length == 2) {
			String name = args[0];
			Optional<IVoxyRoom> opt = tree.getSeed().getRooms().get(name);
			if (opt.isEmpty())
				return NodeHelper.asList(String.format("Room \"%s\" is not registered", name));

			Stream<IVoxyPlayer> players = tree.getSeed().getPlayers().stream().filter(player -> player.getRoom() == null);
			return NodeHelper.filter(players.map(player -> player.getName()), args);
		}

		return NodeHelper.asList();
	}

	// Add Effect -------------------------------------------------------------
	private IResult addEffect(ITree<IVoxyServer> tree, String[] args) {
		if (args.length < 3)
			return NodeHelper.result(false, "The name of the speaking player, of the listening player or of the effect is missing");

		Optional<IVoxyPlayer> speaking = tree.getSeed().getPlayerByName(args[0]);
		if (speaking.isEmpty())
			return NodeHelper.result(false, "The player \"%s\" is not registered on the \"%\" server", args[0], tree.getSeed().getName());

		if (speaking.get().getRoom() == null)
			return NodeHelper.result(false, "The player \"%s\" is not registered in a room", args[0]);

		Optional<IVoxyPlayer> listening = tree.getSeed().getPlayerByName(args[1]);
		if (listening.isEmpty())
			return NodeHelper.result(false, "The player \"%s\" is not registered on the \"%\" server", args[1], tree.getSeed().getName());

		if (listening.get().getRoom() == null)
			return NodeHelper.result(false, "The player \"%s\" is not registered in a room", args[0]);

		if (!speaking.get().getRoom().getName().equals(listening.get().getRoom().getName()))
			return NodeHelper.result(false, "\"%s\" and \"%s\" are not in the same room", args[0], args[1]);

		IVoxyEffect effect = tree.getSeed().getEffectManager().get(args[2]);
		if (effect == null)
			return NodeHelper.result(false, "There is no effect registered for the name \"%s\"", args[2]);

		// Extracting parameter name and value
		List<String> names = effect.getNames();
		int offset = 3;
		String[] paramAndVal = NodeHelper.extract(args, offset);

		// Checking that all parameters are set
		if (paramAndVal.length != names.size() * 2)
			return NodeHelper.result(false, "A value is expected for each parameter: %s", names.toString());

		for (int i = 0; i < paramAndVal.length; i += 2) {
			if (!effect.update(paramAndVal[i], paramAndVal[i + 1]))
				return NodeHelper.result(false, "Cannot update the value of parameter \"%s\" (found %s)", paramAndVal[i], paramAndVal[i + 1]);
		}

		listening.get().addEffect(speaking.get(), 0, effect);

		String listener = listening.get().getName();
		String speaker = speaking.get().getName();
		return NodeHelper.result(true, "Player \"%s\" will hear player \"%s\" with the following effect: %s", listener, speaker, effect);
	}

	private List<String> addEffectCompletions(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 0)
			return NodeHelper.emptyList();

		// Speaking player
		if (args.length == 1) {
			// The name of each players registered in a room
			Stream<String> stream = tree.getSeed().getPlayers().stream().filter(player -> player.getRoom() != null).map(player -> player.getName());
			return NodeHelper.filter(stream, args);
		}

		// Listening player
		if (args.length == 2) {
			// The name of each players registered in a room
			Stream<String> stream = tree.getSeed().getPlayers().stream().filter(player -> player.getRoom() != null).map(player -> player.getName());
			return NodeHelper.filter(stream, args);
		}

		// Effect name
		if (args.length == 3) {
			// The name of each supported effect
			Stream<String> stream = tree.getSeed().getEffectManager().getEffects().stream().map(effect -> effect.getName());
			return NodeHelper.filter(stream, args);
		}

		// Defines the value of each parameter of the effect if the effect is registered
		IVoxyEffect effect = tree.getSeed().getEffectManager().get(args[2]);
		if (effect == null)
			return NodeHelper.emptyList();

		List<String> names = effect.getNames();
		int offset = 3;
		String[] paramAndVal = NodeHelper.extract(args, offset);

		// Expecting parameter name
		if (paramAndVal.length % 2 == 1) {

			// Removing parameters already registered
			for (int i = 0; i < paramAndVal.length - 1; i += 2) {

				// If parameter cannot be removed, then wrong parameter name
				if (!names.remove(paramAndVal[i]))
					return NodeHelper.emptyList();
				else if (!effect.update(paramAndVal[i], paramAndVal[i + 1]))
					return NodeHelper.emptyList();
			}

			return NodeHelper.filter(names.stream(), args);
		}

		// Expecting parameter value
		else {
			String parameter = paramAndVal[0];
			for (int i = 0; i < paramAndVal.length - 1; i += 2) {
				// Getting parameter name, parsing parameter value
				parameter = paramAndVal[i];

				// Parameter value defined but update failed
				if (!paramAndVal[i + 1].isEmpty() && !effect.update(parameter, paramAndVal[i + 1]))
					return NodeHelper.emptyList();
			}

			ParameterDescription description = effect.getDescription(parameter);
			if (description == null)
				return NodeHelper.emptyList();

			String name = description.getName();
			String datatype = description.getValueDataType().getSimpleName();
			String completion;
			if (description.getUnit() != null)
				completion = String.format("<%s:%s[%s] - %s>", name, datatype, description.getUnit(), description.getConstraints());
			else
				completion = String.format("<%s:%s - %s>", name, datatype, description.getConstraints());

			return NodeHelper.asList(completion);
		}
	}

	// Remove Room ------------------------------------------------------------
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

	private List<String> removeRoomCompletions(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 1) {
			// The name of each room registered on the server
			Stream<String> rooms = tree.getSeed().getRooms().toList().stream().map(room -> room.getName());
			return NodeHelper.filter(rooms, args);
		}

		return NodeHelper.emptyList();
	}

	// Remove Player ----------------------------------------------------------
	private IResult removePlayer(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 0)
			return NodeHelper.result(false, "The player's name is missing");

		Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[0]);
		if (!player.isPresent())
			return NodeHelper.result(false, "The player \"%s\" is no registered on the \"%s\" server", args[1], tree.getSeed().getName());

		if (player.get().getRoom() == null)
			return NodeHelper.result(false, "The player \"%s\" is not registered in a room", player.get().getName());

		IVoxyRoom room = player.get().getRoom();
		if (!room.getPlayers().remove(args[0]))
			return NodeHelper.result(false, "An external plugin cancelled the removing of player \"%s\" from room \"%s\"", player.get().getName(), room.getName());

		return NodeHelper.result(true, "The player \"%s\" has been removed from room \"%s\"", player.get().getName(), room.getName());
	}

	private List<String> removePlayerCompletions(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 1) {
			// The name of each players registered in a room
			Stream<String> stream = tree.getSeed().getPlayers().stream().filter(player -> player.getRoom() != null).map(player -> player.getName());
			return NodeHelper.filter(stream, args);
		}

		return NodeHelper.emptyList();
	}

	// Set Room Name ----------------------------------------------------------
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

	private List<String> setRoomNameCompletions(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 1) {
			// The name of each room registered on the server
			Stream<String> rooms = tree.getSeed().getRooms().toList().stream().map(room -> room.getName());
			return NodeHelper.filter(rooms, args);
		}

		if (args.length == 2) {
			Optional<IVoxyRoom> room = tree.getSeed().getRooms().get(args[1]);
			if (room.isPresent())
				return NodeHelper.asList(String.format("The room \"%s\" is already registered", room.get().getName()));

			return NodeHelper.asList("<newName:String>");
		}

		return NodeHelper.emptyList();
	}

	// Set Player Mute --------------------------------------------------------
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

		if (!player.get().setMute(isMute, tree.getSeed()))
			return NodeHelper.result(false, "An external plugin cancelled the change of the mute status of player \"%s\"", player.get().getName());

		return NodeHelper.result(true, "The player \"%s\" is %s", player.get().getName(), isMute ? "muted" : "unmuted");
	}

	private List<String> setMuteCompletions(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 1) {
			// The name of each players registered in a room
			Stream<String> stream = tree.getSeed().getPlayers().stream().filter(player -> player.getRoom() != null).map(player -> player.getName());
			return NodeHelper.filter(stream, args);
		}

		if (args.length == 2 && !args[0].isEmpty()) {
			Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[0]);
			if (player.isEmpty())
				return NodeHelper.asList(String.format("The player \"%s\" is not registered in a room", args[0]));

			return NodeHelper.asList(player.get().isMute() ? "False" : "True");
		}

		return NodeHelper.emptyList();
	}

	// Set Player Playback ----------------------------------------------------
	private IResult setPlayback(ITree<IVoxyServer> tree, String[] args) {
		if (args.length < 2)
			return NodeHelper.result(false, "The name of the player or the playback status is missing");

		Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[0]);
		if (player.isEmpty())
			return NodeHelper.result(false, "The player \"%s\" is not registered on server \"%s\"", args[0], tree.getSeed().getName());

		if (!NodeHelper.isStrictBool(args[1]))
			return NodeHelper.result(false, "The playback state cannot be parsed, it shall be \"true\" or \"false\", case ignored");

		if (!player.get().setPlayback(NodeHelper.parseBool(args[1]), tree.getSeed()))
			return NodeHelper.result(false, "An external plugin cancelled the change of the playback status of player \"%s\"", player.get().getName());

		return NodeHelper.result(true, "The playback is %s for player \"%s\"", player.get().isPlayback() ? "enabled" : "disabled", player.get().getName());
	}

	private List<String> setPlaybackCompletions(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 1) {
			// The name of each players registered in a room
			Stream<String> stream = tree.getSeed().getPlayers().stream().filter(player -> player.getRoom() != null).map(player -> player.getName());
			return NodeHelper.filter(stream, args);
		}

		if (args.length == 2 && !args[0].isEmpty()) {
			Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[0]);
			if (player.isEmpty())
				return NodeHelper.asList(String.format("The player \"%s\" is not registered in a room", args[0]));

			return NodeHelper.asList(player.get().isPlayback() ? "False" : "True");
		}

		return NodeHelper.emptyList();
	}

	// Set Player Coordinates -------------------------------------------------
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

	private List<String> setCoordinatesCompletions(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 1) {
			// The name of each players registered on the server
			Stream<String> stream = tree.getSeed().getPlayers().stream().map(player -> player.getName());
			return NodeHelper.filter(stream, args);
		}

		if (args.length == 2) {
			Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[0]);
			if (player.isEmpty())
				return NodeHelper.asList(String.format("The player \"%s\" is not registered on the server", args[0]));

			return NodeHelper.asList("<x:Int>");
		}

		if (args.length == 3)
			return NodeHelper.isStrictInt(args[1]) ? NodeHelper.asList("<y:Int>") : NodeHelper.emptyList();

		if (args.length == 4)
			return NodeHelper.isStrictInt(args[2]) ? NodeHelper.asList("<z:Int>") : NodeHelper.emptyList();

		if (args.length == 5)
			return NodeHelper.isStrictInt(args[3]) ? NodeHelper.asList("<yaw:Double>") : NodeHelper.emptyList();

		if (args.length == 6)
			return NodeHelper.isStrictDouble(args[4]) ? NodeHelper.asList("<pitch:Double>") : NodeHelper.emptyList();

		if (args.length == 7)
			return NodeHelper.isStrictDouble(args[5]) ? NodeHelper.asList("<roll:Double>") : NodeHelper.emptyList();

		return NodeHelper.emptyList();
	}

	// Set Player Sphere Radius -----------------------------------------------
	private IResult setSphereRadius(ITree<IVoxyServer> tree, String[] args) {
		if (args.length < 4)
			return NodeHelper.result(false, "The player's name or one radius (x-radius, y-radius, z-radius) is missing");

		List<IVoxyPlayer> players = NodeHelper.emptyList();
		if (args[0].equalsIgnoreCase(ALL))
			players.addAll(tree.getSeed().getPlayers());
		else {
			Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[0]);
			if (!player.isPresent())
				return NodeHelper.result(false, "The player \"%s\" is not registered on server \"%s\"", args[0], tree.getSeed().getName());

			players.add(player.get());
		}
		if (!NodeHelper.isStrictDouble(args[1]))
			return NodeHelper.result(false, "The x-radius value cannot be parsed, it shall be a decimal value with \".\" as separator");

		double xRadius = NodeHelper.parseDouble(args[1]);

		if (!NodeHelper.isStrictDouble(args[2]))
			return NodeHelper.result(false, "The y-radius value cannot be parsed, it shall be a decimal value with \".\" as separator");

		double yRadius = NodeHelper.parseDouble(args[2]);

		if (!NodeHelper.isStrictDouble(args[3]))
			return NodeHelper.result(false, "The z-radius value cannot be parsed, it shall be a decimal value with \".\" as separator");

		double zRadius = NodeHelper.parseDouble(args[3]);

		StringJoiner joiner = new StringJoiner(",");
		for (IVoxyPlayer player : players) {
			joiner.add(player.getName());
			player.getSoundSphere().setRadius(xRadius, yRadius, zRadius);
		}
		return NodeHelper.result(true, "Sphere radius of %s updated: [%s, %s, %s]", joiner, xRadius, yRadius, zRadius);
	}

	private List<String> setSphereRadiusCompletions(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 1) {
			// The name of each players registered on the server + "all"
			Stream<String> stream = NodeHelper.concat(tree.getSeed().getPlayers().stream().map(player -> player.getName()), Stream.of(ALL));
			return NodeHelper.filter(stream, args);
		}

		if (args.length == 2) {
			if (!args[0].equalsIgnoreCase(ALL)) {
				Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[0]);
				if (player.isEmpty())
					return NodeHelper.asList(String.format("The player \"%s\" is not registered on the server", args[0]));
			}

			return NodeHelper.asList("<xRadius:Double>");
		}

		if (args.length == 3)
			return NodeHelper.isStrictDouble(args[1]) ? NodeHelper.asList("<yRadius:Double>") : NodeHelper.emptyList();

		if (args.length == 4)
			return NodeHelper.isStrictDouble(args[2]) ? NodeHelper.asList("<zRadius:Double>") : NodeHelper.emptyList();

		return NodeHelper.emptyList();
	}

	// Set Player Sphere Enable -----------------------------------------------
	private IResult setSphereEnable(ITree<IVoxyServer> tree, String[] args) {
		if (args.length < 2)
			return NodeHelper.result(false, "The player's name or the enable state is missing");

		List<IVoxyPlayer> players = NodeHelper.emptyList();
		if (args[0].equals(ALL))
			players.addAll(tree.getSeed().getPlayers());
		else {
			Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[0]);
			if (!player.isPresent())
				return NodeHelper.result(false, "The player \"%s\" is not registered on server \"%s\"", args[0], tree.getSeed().getName());

			players.add(player.get());
		}

		if (!NodeHelper.isStrictBool(args[1]))
			return NodeHelper.result(false, "The enable status cannot be parsed, it shall be \"true\" or \"false\", case ignored");

		boolean isEnabled = NodeHelper.parseBool(args[1]);

		StringJoiner joiner = new StringJoiner(",");
		for (IVoxyPlayer player : players) {
			joiner.add(player.getName());
			player.getSoundSphere().setEnabled(isEnabled);
		}

		return NodeHelper.result(true, "The sound sphere of player(s) \"%s\" is %s", joiner, isEnabled ? "enabled" : "disabled");
	}

	private List<String> setSphereEnableCompletions(ITree<IVoxyServer> tree, String[] args) {
		if (args.length == 1) {
			// The name of each players registered on the server
			Stream<String> stream = NodeHelper.concat(tree.getSeed().getPlayers().stream().map(player -> player.getName()), Stream.of(ALL));
			return NodeHelper.filter(stream, args);
		}

		if (args.length == 2) {
			if (!args[0].equalsIgnoreCase(ALL)) {
				Optional<IVoxyPlayer> player = tree.getSeed().getPlayerByName(args[0]);
				if (player.isEmpty())
					return NodeHelper.asList(String.format("The player\"%s\" is not registered on the server", args[0]));

				return NodeHelper.asList(player.get().getSoundSphere().isEnabled() ? "False" : "True");
			} else
				return NodeHelper.asList("True", "False");
		}

		return NodeHelper.emptyList();
	}

	// List -------------------------------------------------------------------
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
}
