package strat.mining.stratum.proxy.rest.dto;

public class HashrateDTO {

	private Long acceptedHashrate;
	private Long rejectedHashrate;
	private Long captureTimeUTC;

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

	public Long getCaptureTimeUTC() {
		return captureTimeUTC;
	}

	public void setCaptureTimeUTC(Long captureTimeUTC) {
		this.captureTimeUTC = captureTimeUTC;
	}

}
