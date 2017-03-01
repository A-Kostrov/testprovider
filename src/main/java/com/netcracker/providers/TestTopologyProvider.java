/*
 * Copyright 2017-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netcracker.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.felix.scr.annotations.*;
import org.onlab.packet.ChassisId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.*;
import org.onosproject.net.device.*;
import org.onosproject.net.flow.*;
import org.onosproject.net.link.*;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component(immediate = true)
public class TestTopologyProvider extends AbstractProvider implements LinkProvider, DeviceProvider, FlowRuleProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CFG_FILE_NAME = "config.json";
    private static final String CFG_PATH = "/home/sdn/Applications/config/";

    private static final Integer NUMBER_OF_PORTS = 5;

    protected static final String PROVIDER_SCHEME = "testprovider";
    protected static final String PROVIDER_ID = "com.netcracker.providers";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry linkProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleProviderRegistry flowRuleProviderRegistry;

    protected FlowRuleProviderService flowRuleProviderService;
    protected DeviceProviderService deviceProviderService;
    protected LinkProviderService linkProviderService;

    protected final Map<DeviceId, DeviceDescription> devDesc = new HashMap<>();
    protected final Map<DeviceId, List<PortDescription>> devPort = new HashMap<>();

    protected final Map<ConnectPoint, ConnectPoint> links = new HashMap<>();
    protected final Map<ConnectPoint, ConnectPoint> downLinks = new HashMap<>();

    public TestTopologyProvider() {
        super(new ProviderId(PROVIDER_SCHEME, PROVIDER_ID));
    }

    @Activate
    protected void activate() {
        log.info("Test-provider started");

        deviceProviderService = deviceProviderRegistry.register(this);
        linkProviderService = linkProviderRegistry.register(this);
        flowRuleProviderService = flowRuleProviderRegistry.register(this);

        try {
            parseConfig(new File(CFG_PATH + CFG_FILE_NAME));
        } catch (IOException ex) {
            log.error("Error reading config file");
        }
    }


    @Deactivate
    protected void deactivate() {
        log.info("Test-provider stopped");
        deviceProviderRegistry.unregister(this);
        deviceProviderService = null;
        linkProviderRegistry.unregister(this);
        linkProviderService = null;
        flowRuleProviderRegistry.unregister(this);
        flowRuleProviderService = null;
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {

    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole mastershipRole) {

    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        return true;
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean enable) {
        deviceProviderService.portStatusChanged(deviceId, new DefaultPortDescription(portNumber, enable));

        ConnectPoint connectPoint = ConnectPoint.deviceConnectPoint(deviceId + "/" + portNumber);

        if(enable && downLinks.containsKey(connectPoint)
                  && !downLinks.containsKey(downLinks.get(connectPoint))) {
            upLink(connectPoint, links.get(connectPoint));
            downLinks.remove(connectPoint);
        } else if(!enable && links.containsKey(connectPoint)) {
            downLinks.put(connectPoint, links.get(connectPoint));
        }
    }

    protected void upLink(ConnectPoint src, ConnectPoint dst) {
        linkProviderService.linkDetected(new DefaultLinkDescription(src, dst, Link.Type.DIRECT));
        linkProviderService.linkDetected(new DefaultLinkDescription(dst, src, Link.Type.DIRECT));
        links.put(src, dst);
        links.put(dst, src);
    }

    protected void parseConfig(File configFile) throws IOException {
        ObjectNode jsonConfig = (ObjectNode) new ObjectMapper().readTree(configFile);
        JsonNode jsonDevices = jsonConfig.get("devices");
        JsonNode jsonLinks = jsonConfig.get("links");

        setDevices(jsonDevices);
        setPorts();
        setLinks(jsonLinks);
    }

    protected void setPorts(){
        List<PortDescription> ports = new LinkedList<>();
        for(int i = 1; i < NUMBER_OF_PORTS; i++)
            ports.add(new DefaultPortDescription(PortNumber.portNumber(i), true));

        for(DeviceId id : devDesc.keySet()) {
            deviceProviderService.updatePorts(id, ports);
            devPort.put(id, ports);
        }
    }

    protected void setDevices(JsonNode jsonDevices){
        if(jsonDevices == null)
            return;
        Iterator<Map.Entry<String, JsonNode>> jsonDevicesIterator = jsonDevices.fields();

        while(jsonDevicesIterator.hasNext()) {
            Map.Entry<String, JsonNode> device = jsonDevicesIterator.next();
            DeviceId id = DeviceId.deviceId(device.getKey());
            DeviceDescription deviceDescription = new DefaultDeviceDescription(id.uri(), Device.Type.SWITCH,
                    "test","","","", new ChassisId(1));

            devDesc.put(id, deviceDescription);
            deviceProviderService.deviceConnected(id, deviceDescription);
        }
    }

    protected void setLinks(JsonNode jsonLinks){
        if(jsonLinks == null)
            return;
        Iterator<Map.Entry<String, JsonNode>> jsonLinksIterator = jsonLinks.fields();

        while(jsonLinksIterator.hasNext()) {
            Map.Entry<String, JsonNode> link = jsonLinksIterator.next();
            ConnectPoint src = ConnectPoint.deviceConnectPoint(link.getKey().split("-")[0]);
            ConnectPoint dst = ConnectPoint.deviceConnectPoint(link.getKey().split("-")[1]);
            upLink(src, dst);
        }
    }

    @Override
    public void applyFlowRule(FlowRule... flowRules) {

    }

    @Override
    public void removeFlowRule(FlowRule... flowRules) {

    }

    @Override
    public void removeRulesById(ApplicationId id, FlowRule... flowRules) {

    }

    @Override
    public void executeBatch(FlowRuleBatchOperation batch) {

    }
}
