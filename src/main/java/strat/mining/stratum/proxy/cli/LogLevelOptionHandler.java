package strat.mining.stratum.proxy.cli;

import org.apache.log4j.Level;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

public class LogLevelOptionHandler extends OneArgumentOptionHandler<Level> {

	public LogLevelOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Level> setter) {
		super(parser, option, setter);
	}

	@Override
	protected Level parse(String argument) throws NumberFormatException, CmdLineException {
		Level level = null;

		if (argument.equalsIgnoreCase("off")) {
			level = Level.OFF;
		} else if (argument.equalsIgnoreCase("fatal")) {
			level = Level.FATAL;
		} else if (argument.equalsIgnoreCase("error")) {
			level = Level.ERROR;
		} else if (argument.equalsIgnoreCase("warn")) {
			level = Level.WARN;
		} else if (argument.equalsIgnoreCase("info")) {
			level = Level.INFO;
		} else if (argument.equalsIgnoreCase("debug")) {
			level = Level.DEBUG;
		} else if (argument.equalsIgnoreCase("trace")) {
			level = Level.TRACE;
		}

		return level;
	}

}
