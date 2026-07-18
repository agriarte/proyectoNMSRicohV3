package com.tuempresa.nms.core.ports;

import com.tuempresa.nms.core.domain.DeviceReading;
import com.tuempresa.nms.core.domain.PollPlan;
import com.tuempresa.nms.infrastructure.snmp.SnmpResponse;

public interface PrinterDriver {
    boolean supports(String sysDescrOrModelName);
    PollPlan planFor(String modelName);
    DeviceReading parse(SnmpResponse raw, String ip);
}
