/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014  Stratehm (stratehm@hotmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with multipool-stats-backend. If not, see <http://www.gnu.org/licenses/>.
 */
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
