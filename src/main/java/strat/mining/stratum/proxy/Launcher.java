package strat.mining.stratum.proxy;

import java.util.List;

import org.kohsuke.args4j.CmdLineException;

import strat.mining.stratum.proxy.cli.CommandLineOptions;
import strat.mining.stratum.proxy.pool.Pool;

public class Launcher {

	public static void main(String[] args) {

		CommandLineOptions cliParser = new CommandLineOptions();
		try {
			cliParser.parseArguments(args);
			
			List<Pool> pools = cliParser.getPools();
			for(Pool pool : pools) {
				pool.
			}
			
			
		} catch (CmdLineException e) {
			e.printStackTrace();
		}

	}
}
