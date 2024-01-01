package com.dosse.upnp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class PortMappingEntity {

    private final InetAddress internalClient;
    private final Protocol protocol;
    private final boolean enable;
    private final int leaseDuration;
    private final int internalPort;
    private final int externalPort;
    private final String portMappingDescription;


    public static PortMappingEntity fromResponse(Map<String, String> response) {
        InetAddress internalClient;
        try {
            internalClient = InetAddress.getByName(response.get("NewInternalClient"));


        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Can't represent internalClient string ip as ip");
        }
        Protocol protocol = response.get("NewProtocol").equalsIgnoreCase("tcp") ? Protocol.TCP : Protocol.UDP;
        boolean enable = response.get("NewEnabled").equals("1");

        int leaseDuration;
        try {
            leaseDuration = Integer.parseInt(response.get("NewLeaseDuration"));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Can't represent leaseDuration as integer");
        }

        int internalPort;
        try {
            internalPort = Integer.parseInt(response.get("NewInternalPort"));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Can't represent internalPort as integer");
        }

        int externalPort;
        try {
            externalPort = Integer.parseInt(response.get("NewExternalPort"));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Can't represent externalPort as integer");
        }

        String portMappingDescription = response.get("NewPortMappingDescription");

        return new PortMappingEntity(internalClient, protocol, enable, leaseDuration, internalPort, externalPort, portMappingDescription);

    }

    public PortMappingEntity(InetAddress internalClient, Protocol protocol, boolean enable, int leaseDuration, int internalPort, int externalPort, String portMappingDescription) {
        this.internalClient = internalClient;
        this.protocol = protocol;
        this.enable = enable;
        this.leaseDuration = leaseDuration;
        this.internalPort = internalPort;
        this.externalPort = externalPort;
        this.portMappingDescription = portMappingDescription;
    }


    public boolean isEnabled() {
        return enable;
    }

    public String getDescription() {
        return portMappingDescription;
    }

    public int getInternalPort() {
        return internalPort;
    }

    public int getExternalPort() {
        return externalPort;
    }

    @Override
    public String toString() {
        return "PortMappingEntity{internalClient=%s, protocol=%s, enable=%s, leaseDuration=%s, internalPort=%s, externalPort=%s, portMappingDescription=%s}"
                .replaceFirst("%s", this.internalClient.getHostAddress())
                .replaceFirst("%s", this.protocol.name())
                .replaceFirst("%s", this.enable ? "1" : "0")
                .replaceFirst("%s", String.valueOf(this.leaseDuration))
                .replaceFirst("%s", String.valueOf(this.internalPort))
                .replaceFirst("%s", String.valueOf(this.externalPort))
                .replaceFirst("%s", this.portMappingDescription);
    }

    public enum Protocol {
        UDP,
        TCP
    }
}