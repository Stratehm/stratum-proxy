package strat.mining.stratum.proxy.pool;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Quota {

    private Pool pool;

    private Integer quota;
}
