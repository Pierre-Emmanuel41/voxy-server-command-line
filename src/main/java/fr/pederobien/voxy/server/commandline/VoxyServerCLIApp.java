package fr.pederobien.voxy.server.commandline;

import fr.pederobien.commandtree.impl.CLI;
import fr.pederobien.utils.event.Logger;

public class VoxyServerCLIApp {

	public static void main(String[] args) {
		Logger logger = Logger.instance();
		logger.colorized(true);

		if (args.length > 0 && args[0].equals("-d"))
			logger.debug(true);

		Runnable cli = CLI.simpleInterface("voxy>", arg -> arg.equals("exit"), new VoxyCommandTree().getTree());
		cli.run();
	}
}
