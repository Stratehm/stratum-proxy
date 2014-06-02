package strat.mining.stratum.proxy.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GetworkResponse {

	private String midstate;
	private String data;
	private String hash1;
	private String target;

	public String getMidstate() {
		return midstate;
	}

	public void setMidstate(String midstate) {
		this.midstate = midstate;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getHash1() {
		return hash1;
	}

	public void setHash1(String hash1) {
		this.hash1 = hash1;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

}
