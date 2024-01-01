/*
 * Copyright (C) 2015 Federico Dossena (adolfintel.com).
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.dosse.upnp;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;

/**
 * @author Federico
 */
class Gateway {

    private final Inet4Address iface;
    private final InetAddress routerIp;

    private String serviceType = null;
    private String controlURL = null;

    public Gateway(byte[] data, Inet4Address ip, InetAddress gatewayIp) throws RuntimeException, ParserConfigurationException, IOException, SAXException {
        iface = ip;
        routerIp = gatewayIp;
        String location = null;
        StringTokenizer st = new StringTokenizer(new String(data), "\n");
        while (st.hasMoreTokens()) {
            String s = st.nextToken().trim();
            if (s.isEmpty() || s.startsWith("HTTP/1.") || s.startsWith("NOTIFY *")) {
                continue;
            }
            String name = s.substring(0, s.indexOf(':')), val = s.length() >= name.length() ? s.substring(name.length() + 1).trim() : null;
            if (name.equalsIgnoreCase("location")) {
                location = val;
            }
        }
        if (location == null) {
            throw new RuntimeException("Unsupported Gateway");
        }
        Document d;
        d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(location);
        NodeList services = d.getElementsByTagName("service");
        for (int i = 0; i < services.getLength(); i++) {
            Node service = services.item(i);
            NodeList n = service.getChildNodes();
            serviceType = null;
            controlURL = null;
            for (int j = 0; j < n.getLength(); j++) {
                Node x = n.item(j);
                if (x.getNodeName().trim().equalsIgnoreCase("serviceType")) {
                    serviceType = x.getFirstChild().getNodeValue();
                } else if (x.getNodeName().trim().equalsIgnoreCase("controlURL")) {
                    controlURL = x.getFirstChild().getNodeValue();
                }
            }
            if (serviceType == null || controlURL == null) {
                continue;
            }
            if (serviceType.trim().toLowerCase().contains(":wanipconnection:") || serviceType.trim().toLowerCase().contains(":wanpppconnection:")) {
                this.serviceType = serviceType.trim();
                this.controlURL = controlURL.trim();
            }
        }
        if (controlURL == null) {
            throw new RuntimeException("Unsupported Gateway");
        }
        int slash = location.indexOf("/", 7); //finds first slash after http://
        if (slash == -1) {
            throw new RuntimeException("Unsupported Gateway");
        }
        location = location.substring(0, slash);
        if (!controlURL.startsWith("/")) {
            controlURL = "/" + controlURL;
        }
        controlURL = location + controlURL;
    }

    private static void assertPort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port");
        }
    }

    private Map<String, String> command(String action, Map<String, String> params) throws Exception {
        Map<String, String> ret = new HashMap<String, String>();
        StringBuilder soap = new StringBuilder("<?xml version=\"1.0\"?>\r\n" + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
                + "<SOAP-ENV:Body>"
                + "<m:" + action + " xmlns:m=\"" + serviceType + "\">");
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                soap.append("<").append(entry.getKey()).append(">").append(entry.getValue()).append("</").append(entry.getKey()).append(">");
            }
        }
        soap.append("</m:").append(action).append("></SOAP-ENV:Body></SOAP-ENV:Envelope>");
        byte[] req = soap.toString().getBytes();
        HttpURLConnection conn = (HttpURLConnection) new URL(controlURL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/xml");
        conn.setRequestProperty("SOAPAction", "\"" + serviceType + "#" + action + "\"");
        conn.setRequestProperty("Connection", "Close");
        conn.setRequestProperty("Content-Length", "" + req.length);
        conn.getOutputStream().write(req);
        Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
        NodeIterator iter = ((DocumentTraversal) d).createNodeIterator(d.getDocumentElement(), NodeFilter.SHOW_ELEMENT, null, true);
        Node n;
        while ((n = iter.nextNode()) != null) {
            try {
                if (n.getFirstChild().getNodeType() == Node.TEXT_NODE) {
                    ret.put(n.getNodeName(), n.getTextContent());
                }
            } catch (Throwable t) {
            }
        }
        conn.disconnect();
        return ret;
    }

    public String getGatewayIP() {
        return routerIp.getHostAddress();
    }

    public String getLocalIP() {
        return iface.getHostAddress();
    }


    public String getExternalIP() {
        try {
            Map<String, String> r = command("GetExternalIPAddress", null);
            return r.get("NewExternalIPAddress");
        } catch (Throwable t) {
            return null;
        }
    }

    public boolean openCommonPort(int port, boolean udp, String name, int duration) {
        assertPort(port);
        Map<String, String> params = new HashMap<String, String>();
        params.put("NewRemoteHost", "");
        params.put("NewProtocol", udp ? "UDP" : "TCP");
        params.put("NewInternalClient", iface.getHostAddress());
        params.put("NewExternalPort", "" + port);
        params.put("NewInternalPort", "" + port);
        params.put("NewEnabled", "1");
        params.put("NewPortMappingDescription", name);
        params.put("NewLeaseDuration", "" + duration);
        try {
            Map<String, String> r = command("AddPortMapping", params);
            return r.get("errorCode") == null;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean closePort(int port, boolean udp) {
        assertPort(port);
        Map<String, String> params = new HashMap<String, String>();
        params.put("NewRemoteHost", "");
        params.put("NewProtocol", udp ? "UDP" : "TCP");
        params.put("NewExternalPort", "" + port);
        try {
            command("DeletePortMapping", params);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isMapped(int port, boolean udp) {
        assertPort(port);
        Map<String, String> params = new HashMap<String, String>();
        params.put("NewRemoteHost", "");
        params.put("NewProtocol", udp ? "UDP" : "TCP");
        params.put("NewExternalPort", "" + port);
        try {
            Map<String, String> r = command("GetSpecificPortMappingEntry", params);
            if (r.get("errorCode") != null) {
                throw new Exception();
            }
            return r.get("NewInternalPort") != null;
        } catch (Exception ex) {
            return false;
        }
    }


    public Set<PortMappingEntity> getPortMappings() {
        Set<PortMappingEntity> response = new HashSet<PortMappingEntity>();
        for (int i = 0; i < 1000; i++) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("NewPortMappingIndex", "" + i);
            try {
                PortMappingEntity entity = PortMappingEntity.fromResponse(command("GetGenericPortMappingEntry", params));
                response.add(entity);
            } catch (Exception ex) {
                return response;
            }
        }
        return response;
    }

}
