package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientReconnectNotification extends JsonRpcNotification {

	public static final String METHOD_NAME = "client.reconnect";

	private String host;
	private Integer port;

	public ClientReconnectNotification() {
		super(METHOD_NAME);
	}

	public ClientReconnectNotification(JsonRpcNotification notification) {
		super(notification);
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	@Override
	public List<Object> getParams() {
		if (super.getParams() == null) {
			List<Object> params = new ArrayList<Object>();
			super.setParams(params);
			params.add(host);
			params.add(port);
		}
		return super.getParams();
	}

	@Override
	public void setParams(List<Object> params) {
		super.setParams(params);
		if (params != null) {
			host = getParamsObjectAtIndex(0);
			port = getParamsObjectAtIndex(1) != null ? ((Number) getParamsObjectAtIndex(1)).intValue() : null;
		}
	}

}
