package com.tuempresa.nms.core.ports;

import com.tuempresa.nms.core.domain.PollPlan;
import com.tuempresa.nms.infrastructure.snmp.SnmpResponse;

public interface SnmpClient {
    SnmpResponse execute(PollPlan plan, String ip);
    String getString(String ip, String oid);
}
