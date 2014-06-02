package strat.mining.stratum.proxy.network;

import java.net.InetAddress;

public interface Connection {

	/**
	 * Close the connection
	 */
	public void close();

	/**
	 * Return the connection name.
	 * 
	 * @return
	 */
	public String getConnectionName();

	/**
	 * Return the remote address.
	 * 
	 * @return
	 */
	public InetAddress getRemoteAddress();

	/**
	 * Return the remote port.
	 * 
	 * @return
	 */
	public Integer getRemotePort();

}
