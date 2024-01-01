# Documentation

<hr>

- [Methods](#methods)
  - [openPortTCP](#openporttcp)
  - [openPortUDP](#openportudp)
  - [closePortTCP](#closeporttcp)
  - [closePortUDP](#closeportudp)
  - [isMappedTCP](#ismappedtcp)
  - [isMappedUDP](#ismappedudp)
  - [getExternalIP](#getexternalip)
  - [getLocalIP](#getlocalip)
  - [getDefaultGatewayIP](#getdefaultgatewayip)
  - [getPortMappings](#getportmappings)
- [Examples](#examples)
  1. [Open 23456 TCP port](#open-23456-tcp-port)
  2. [Open 23456 UDP port](#open-23456-udp-port)
  3. [Open 23456 TCP port with name 'Ultimate port (TCP 23456)' for 3 minute](#open-23456-tcp-port-with-name-ultimate-port-tcp-23456-for-3-minute)
  4. [Open 23456 TCP port for 3 minute with repeat timer (Recommend)](#open-23456-tcp-port-for-3-minute-with-repeat-timer-recommend)
  5. [Check if port TCP 23333 is open](#check-if-port-tcp-23333-is-open)
  6. [Get a description of all port mappings](#get-a-description-of-all-port-mappings)
- [Attentions](#attention)
  1. [Duration '0'](#1-duration-0)

<hr>

## Methods
### `openPortTCP`

Opens a TCP port on the gateway. Return true, if operation was successful

#### Parameters
- `port` - TCP port (0-65535)
- `name` - UPnP rule description (if value is not set, will default name)
- `duration` - duration rule in seconds (0 if is not set)([Read this!](#1-duration-0))

### `openPortUDP`

Opens a UDP port on the gateway. Return true, if operation was successful

#### Parameters
- `port` - UDP port (0-65535)
- `name` - UPnP rule description (if value is not set, will default name)
- `duration` - duration rule in seconds (0 if is not set)([Read this!](#1-duration-0))

### `closePortTCP`

Closes a TCP port on the gateway. Return true, if operation was successful. _Most gateways seem to refuse to do this_

#### Parameters
- `port` - TCP port (0-65535)

### `closePortUDP`

Closes a UDP port on the gateway. Return true, if operation was successful. _Most gateways seem to refuse to do this_

#### Parameters
- `port` - UDP port (0-65535)

### `isMappedTCP`

Checks if a TCP port is mapped. Return true if the port is mapped

#### Parameters
- `port` - TCP port (0-65535)

### `isMappedUDP`

Checks if a UDP port is mapped. Return true if the port is mapped

#### Parameters
- `port` - UDP port (0-65535)

### `getExternalIP`

Gets the external IP address of the default gateway. Return external IP address as string, or null if not available

### `getLocalIP`

Gets the internal IP address of this machine. Return internal IP address as string, or null if not available

### `getDefaultGatewayIP`

Gets the IP address of the router. Return internal IP address as string, or null if not available

### `getPortMappings`

Gets the port mappings of the router. Recommended to use this method with caution due to its labor-intensive nature. Return Set port mappings as PortMappingEntity, or empty if not available

<hr>

## Examples

### Open 23456 TCP port
```java
    import com.dosse.upnp.UPnP;

    public class Main {
        public static void main(String[] args) {
            UPnP.openPortTCP(23456);
        }
    }
```

### Open 23456 UDP port
```java
    import com.dosse.upnp.UPnP;

    public class Main {
        public static void main(String[] args) {
            UPnP.openPortUDP(23456);
        }
    }
```

### Open 23456 TCP port with name 'Ultimate port (TCP 23456)' for 3 minute
```java
    import com.dosse.upnp.UPnP;

    public class Main {
        public static void main(String[] args) {
            UPnP.openPortTCP(23456, "Ultimate port (TCP 23456)", 3 * 60);
        }
    }
```

### Open 23456 TCP port for 3 minute with repeat timer (Recommend)
```java
    import com.dosse.upnp.UPnP;
    import java.util.Timer;
    import java.util.TimerTask;
    
    public class Main {
        public static void main(String[] args) {
            int duration = 30;
            TimerTask task = new TimerTask() {
                public void run() { UPnP.openPortTCP(23456, duration); }
            };
            new Timer().scheduleAtFixedRate(task, 0, duration - 10L); // -10 - time margin to keep the port open
        }
    }
```

### Check if port TCP 23333 is open
```java
    import com.dosse.upnp.UPnP;
    
    public class Main {
        public static void main(String[] args) {
            int port = 23333;
            if (UPnP.isMappedTCP(port)) {
                System.out.printf("Port %s is opened%n", port);
            } else {
                System.out.printf("Port %s is closed or unavailable%n", port);
            }
        }
    }
```

### Get a description of all port mappings
```java
    import com.dosse.upnp.UPnP;
    
    public class Main {
        public static void main(String[] args) {
            UPnP.getPortMappings().forEach(portMappingEntity -> System.out.println(portMappingEntity.getDescription()));
        }
    }
```

<hr>

## Attention
### 1. Duration '0'
Be careful, if you open a port indefinitely and do not close it during the program (or when the program crashes), the port will remain open until you reboot the router/UPnP or close the port from the program
