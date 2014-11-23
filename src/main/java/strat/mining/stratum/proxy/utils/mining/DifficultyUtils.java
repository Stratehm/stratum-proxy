package strat.mining.stratum.proxy.utils.mining;

import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.worker.GetworkJobTemplate;
import strat.mining.stratum.proxy.worker.GetworkJobTemplate.GetworkRequestResult;

public final class DifficultyUtils {

	/**
	 * Return the real share difficulty of the share with the given parameters.
	 * 
	 * @param currentJobTemplate
	 *            the current job the share has been found on.
	 * @param extranonce1Tail
	 *            the tail of the extranonce1 of the worker connection
	 * @param extranonce2
	 *            the submitted extranonce2 (should contains the above
	 *            extranonce1Tail)
	 * @param ntime
	 *            the submitted ntime
	 * @param nonce
	 *            the submitted nonce
	 * @return
	 */
	public static Double getRealShareDifficulty(GetworkJobTemplate currentJobTemplate, String extranonce1Tail, String extranonce2, String ntime,
			String nonce) {
		GetworkJobTemplate cloned = new GetworkJobTemplate(currentJobTemplate);
		cloned.setTime(ntime);
		cloned.setNonce(nonce);
		GetworkRequestResult jobResult = cloned.getData(extranonce2.replaceFirst(extranonce1Tail, ""));
		String blockHeader = jobResult.getData();
		Double realDifficulty = 0d;
		if (ConfigurationManager.getInstance().isScrypt()) {
			realDifficulty = ScryptHashingUtils.getRealShareDifficulty(blockHeader);
		} else {
			realDifficulty = SHA256HashingUtils.getRealShareDifficulty(blockHeader);
		}
		return realDifficulty;
	}

}
