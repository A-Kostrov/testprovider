package com.netcracker.providers;

import static org.easymock.EasyMock.createMock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProviderService;

import java.io.File;

import static org.junit.Assert.*;

public class TestTopologyProviderTest {
    private static final String TEST_CFG_FILE = "testconfig.json";
    private TestTopologyProvider testTopologyProvider = new TestTopologyProvider();
    private File file;
    ObjectNode jsonConfig;

    @Before
    public void setUp() throws Exception {
        testTopologyProvider.deviceProviderService = createMock(DeviceProviderService.class);
        testTopologyProvider.linkProviderService = createMock(LinkProviderService.class);
        file = new File(TEST_CFG_FILE);
        jsonConfig = (ObjectNode) new ObjectMapper().readTree(file);
    }

    @Test
    public void setDevicesTest() {
        testTopologyProvider.setDevices(jsonConfig.get("devices"));
        assertTrue(testTopologyProvider.devDesc.containsKey(DeviceId.deviceId("testprovider:a1")));
    }

    @Test
    public void setDevicesFromNullNode() {
        testTopologyProvider.setDevices(null);
        assertTrue(testTopologyProvider.devDesc.isEmpty());
    }

    @Test
    public void setPortsWithoutDevicesTest() {
        testTopologyProvider.setDevices(null);
        testTopologyProvider.setPorts();
        assertTrue(testTopologyProvider.devPort.isEmpty());
    }

    @Test
    public void setPortsTest() {
        testTopologyProvider.setDevices(jsonConfig.get("devices"));
        testTopologyProvider.setPorts();
        assertFalse(testTopologyProvider.devPort.isEmpty());
    }

    @Test
    public void setUpLink() {
        testTopologyProvider.setDevices(jsonConfig.get("devices"));
        testTopologyProvider.setPorts();
		ConnectPoint src = ConnectPoint.deviceConnectPoint("testprovider:a1/1");
        ConnectPoint dst = ConnectPoint.deviceConnectPoint("testprovider:a2/1");
        testTopologyProvider.upLink(src, dst);
        assertTrue(testTopologyProvider.links.containsKey(src));
        assertTrue(testTopologyProvider.links.containsKey(dst));
    }

    @Test
    public void setLinksTest() {
        testTopologyProvider.setDevices(jsonConfig.get("devices"));
        testTopologyProvider.setPorts();
        testTopologyProvider.setLinks(jsonConfig.get("links"));
        assertTrue(testTopologyProvider.links.containsKey(ConnectPoint.deviceConnectPoint("testprovider:a1/1")));
        assertEquals(testTopologyProvider.links.get(ConnectPoint.deviceConnectPoint("testprovider:a1/1"))
                     , ConnectPoint.deviceConnectPoint("testprovider:a2/1"));
    }

    @Test
    public void setLinksFromNullNode() {
        testTopologyProvider.setDevices(jsonConfig.get("devices"));
        testTopologyProvider.setPorts();
        testTopologyProvider.setLinks(null);
        assertTrue(testTopologyProvider.links.isEmpty());
    }

    @Test
    public void parseConfigTest() throws Exception{
        testTopologyProvider.parseConfig(file);
        assertFalse(testTopologyProvider.devDesc.isEmpty());
        assertFalse(testTopologyProvider.links.isEmpty());
        assertFalse(testTopologyProvider.devPort.isEmpty());
    }

}