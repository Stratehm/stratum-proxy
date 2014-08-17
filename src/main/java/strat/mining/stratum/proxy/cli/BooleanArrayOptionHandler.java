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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.util.HashMap;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class BooleanArrayOptionHandler extends OptionHandler<Boolean> {

	// same values as BooleanOptionHandler
	private static final Map<String, Boolean> ACCEPTABLE_VALUES;

	static {
		// I like the trick in BooleanOptionHandler but find this explicit map
		// more readable
		ACCEPTABLE_VALUES = new HashMap<String, Boolean>();
		ACCEPTABLE_VALUES.put("true", TRUE);
		ACCEPTABLE_VALUES.put("on", TRUE);
		ACCEPTABLE_VALUES.put("yes", TRUE);
		ACCEPTABLE_VALUES.put("1", TRUE);
		ACCEPTABLE_VALUES.put("false", FALSE);
		ACCEPTABLE_VALUES.put("off", FALSE);
		ACCEPTABLE_VALUES.put("no", FALSE);
		ACCEPTABLE_VALUES.put("0", FALSE);
	}

	public BooleanArrayOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Boolean> setter) {
		super(parser, option, setter);
	}

	@Override
	public String getDefaultMetaVariable() {
		return "BOOL[]";
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
				setter.addValue(getBoolean(p));
			}
		}

		return counter;
	}

	private Boolean getBoolean(String parameter) throws CmdLineException {
		String valueStr = parameter.toLowerCase();
		if (!ACCEPTABLE_VALUES.containsKey(valueStr)) {
			throw new CmdLineException(owner, Messages.ILLEGAL_BOOLEAN.format(valueStr));
		}
		return ACCEPTABLE_VALUES.get(valueStr);
	}
}
