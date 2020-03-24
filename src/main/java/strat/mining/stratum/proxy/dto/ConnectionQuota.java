package strat.mining.stratum.proxy.dto;

import lombok.Builder;
import lombok.Data;
import strat.mining.stratum.proxy.pool.Quota;

import java.time.LocalDateTime;

@Data
@Builder
public class ConnectionQuota {

    private Quota quota;

    private LocalDateTime date;

    private int position;
}
