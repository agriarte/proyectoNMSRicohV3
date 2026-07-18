package com.tuempresa.nms.infrastructure.snmp;

import com.tuempresa.nms.core.domain.PollPlan;
import com.tuempresa.nms.core.domain.TableWalk;
import com.tuempresa.nms.core.ports.SnmpClient;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;

@Component
@SuppressWarnings("unchecked")
public class Snmp4jClient implements SnmpClient {

    private final String community;
    private final int timeout;
    private final int retries;

    private Snmp snmp;

    public Snmp4jClient(
            @Value("${nms.snmp.community:public}") String community,
            @Value("${nms.snmp.timeout:3000}") int timeout,
            @Value("${nms.snmp.retries:2}") int retries) {
        this.community = community;
        this.timeout = timeout;
        this.retries = retries;
    }

    @PostConstruct
    public void init() throws IOException {
        TransportMapping<? extends Address> transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);
        transport.listen();
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (snmp != null) {
                snmp.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public String getString(String ip, String oid) {
        try {
            PDU pdu = new PDU();
            pdu.setType(PDU.GET);
            pdu.add(new VariableBinding(new OID(oid)));

            CommunityTarget<Address> target = createTarget(ip);
            ResponseEvent<Address> event = snmp.send(pdu, target);

            if (event == null || event.getResponse() == null) {
                return null;
            }

            PDU response = event.getResponse();
            if (response.size() == 0) {
                return null;
            }

            VariableBinding vb = response.get(0);
            if (vb == null || vb.getVariable() instanceof Null) {
                return null;
            }

            return vb.getVariable().toString();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public SnmpResponse execute(PollPlan plan, String ip) {
        SnmpResponse response = new SnmpResponse();

        // 1. GETs escalares
        if (!plan.scalarOids().isEmpty()) {
            try {
                PDU pdu = new PDU();
                pdu.setType(PDU.GET);
                for (String oid : plan.scalarOids()) {
                    pdu.add(new VariableBinding(new OID(oid)));
                }

                CommunityTarget<Address> target = createTarget(ip);
                ResponseEvent<Address> event = snmp.send(pdu, target);

                if (event != null && event.getResponse() != null) {
                    PDU resp = event.getResponse();
                    for (VariableBinding vb : resp.getVariableBindings()) {
                        String oid = vb.getOid().toString();
                        String val = vb.getVariable() instanceof Null ? null : vb.getVariable().toString();
                        response.putScalar(oid, val);
                    }
                }
            } catch (IOException e) {
                // Continuar con lo que tengamos
            }
        }

        // 2. WALKs de tablas
        for (TableWalk walk : plan.tableWalks()) {
            List<SnmpResponse.TableRow> rows = walkTable(ip, walk.baseOid());
            response.putTableRows(walk.baseOid(), rows);
        }

        return response;
    }

    private List<SnmpResponse.TableRow> walkTable(String ip, String baseOid) {
        List<SnmpResponse.TableRow> rows = new ArrayList<>();
        OID root = new OID(baseOid);
        OID current = new OID(baseOid);
        int maxIterations = 1000;
        int iterations = 0;

        try {
            while (iterations < maxIterations) {
                iterations++;

                PDU pdu = new PDU();
                pdu.setType(PDU.GETNEXT);
                pdu.add(new VariableBinding(current));

                CommunityTarget<Address> target = createTarget(ip);
                ResponseEvent<Address> event = snmp.send(pdu, target);

                if (event == null || event.getResponse() == null) {
                    break;
                }

                PDU resp = event.getResponse();
                if (resp.size() == 0) {
                    break;
                }

                VariableBinding vb = resp.get(0);
                if (vb == null) {
                    break;
                }

                OID nextOid = vb.getOid();
                if (!nextOid.startsWith(root)) {
                    break;
                }

                Map<String, String> cols = new HashMap<>();
                cols.put("oid", nextOid.toString());
                cols.put("value", vb.getVariable().toString());
                rows.add(new SnmpResponse.TableRow(cols));

                current = nextOid;
            }
        } catch (IOException e) {
            // Fin del walk
        }

        return rows;
    }

    private CommunityTarget<Address> createTarget(String ip) {
        CommunityTarget<Address> target = new CommunityTarget<>();
        target.setCommunity(new OctetString(community));
        target.setAddress(new UdpAddress(ip + "/161"));
        target.setVersion(SnmpConstants.version2c);
        target.setTimeout(timeout);
        target.setRetries(retries);
        return target;
    }
}