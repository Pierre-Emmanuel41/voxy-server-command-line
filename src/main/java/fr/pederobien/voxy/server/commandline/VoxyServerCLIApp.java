package fr.pederobien.voxy.server.commandline;

import fr.pederobien.commandtree.impl.CLI;
import fr.pederobien.utils.event.Logger;

public class VoxyServerCLIApp {

	public static void main(String[] args) {
		Logger.instance().colorized(true).debug(true);

		Runnable cli = CLI.simpleInterface("voxy>", arg -> arg.equals("exit"), new VoxyCommandTree().getTree());
		cli.run();
	}
}
