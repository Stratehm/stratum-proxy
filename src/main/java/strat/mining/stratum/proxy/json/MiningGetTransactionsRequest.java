/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014-2015  Stratehm (stratehm@hotmail.com)
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
package strat.mining.stratum.proxy.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MiningGetTransactionsRequest extends JsonRpcRequest {

    public static final String METHOD_NAME = "mining.get_transactions";

    @JsonIgnore
    private String jobId;

    public MiningGetTransactionsRequest() {
        super(METHOD_NAME);
    }

    public MiningGetTransactionsRequest(JsonRpcRequest request) {
        super(request);
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public List<Object> getParams() {
        if (super.getParams() == null) {
            ArrayList<Object> params = new ArrayList<Object>();
            super.setParams(params);
            params.add(jobId);
        }
        return super.getParams();
    }

    @Override
    public void setParams(List<Object> params) {
        super.setParams(params);
        if (params != null) {
            jobId = (String) params.get(0);
        }
    }

}
