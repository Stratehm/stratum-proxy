package strat.mining.stratum.proxy.database.model;


public class HashrateModel {

	private String name;
	private Long acceptedHashrate;
	private Long rejectedHashrate;
	private Long captureTime;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getAcceptedHashrate() {
		return acceptedHashrate;
	}

	public void setAcceptedHashrate(Long acceptedHashrate) {
		this.acceptedHashrate = acceptedHashrate;
	}

	public Long getRejectedHashrate() {
		return rejectedHashrate;
	}

	public void setRejectedHashrate(Long rejectedHashrate) {
		this.rejectedHashrate = rejectedHashrate;
	}

	public Long getCaptureTime() {
		return captureTime;
	}

	public void setCaptureTime(Long captureTime) {
		this.captureTime = captureTime;
	}

}
