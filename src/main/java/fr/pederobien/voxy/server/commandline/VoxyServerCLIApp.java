package fr.pederobien.voxy.server.commandline;

import fr.pederobien.commandtree.impl.CLI;
import fr.pederobien.utils.event.Logger;

public class VoxyServerCLIApp {

	public static void main(String[] args) {
		Logger.setPrintInColor(true);
		parseArgs(args);

		Runnable cli = CLI.simpleInterface("voxy>", arg -> arg.equals("exit"), new VoxyCommandTree().getTree());
		cli.run();
	}

	private static void parseArgs(String[] args) {
		int debugLevel = Integer.MAX_VALUE;

		for (int i = 0; i < args.length; /* Managed inside the loop */) {
			// Debug
			if (args[i].equals("-d")) {
				i++;
				try {
					debugLevel = Integer.parseInt(args[i]);
					i++;
				} catch (Exception e) {
					Logger.warning("Minimum debug level could not be parsed, setting to maximum value (ie: no debug output)");
					Logger.setPrintDebugLevel(Integer.MAX_VALUE);
				}

				continue;
			}

			// Event
			if (args[i].equals("-e")) {
				Logger.setPrintEvent(true);
				Logger.info("Internal events will be printed");
				i++;
			}
		}

		Logger.setPrintDebugLevel(debugLevel);

		if (debugLevel != Integer.MAX_VALUE)
			Logger.info("Minimum debug level is %s", debugLevel);
	}
}
