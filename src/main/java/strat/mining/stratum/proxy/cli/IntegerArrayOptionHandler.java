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

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class IntegerArrayOptionHandler extends OptionHandler<Integer> {

	public IntegerArrayOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Integer> setter) {
		super(parser, option, setter);
	}

	@Override
	public String getDefaultMetaVariable() {
		return "INTEGER[]";
	}

	@Override
	public int parseArguments(Parameters params) throws CmdLineException {
		int counter = 0;
		for (; counter < params.size(); counter++) {
			String param = params.getParameter(counter);

			if (param.startsWith("-")) {
				break;
			}

			for (String p : param.split(" ")) {
				setter.addValue(Integer.parseInt(p));
			}
		}

		return counter;
	}

}
