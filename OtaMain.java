/**
 * @file OtaMain.java
 * @author Kwon Taeyoung (xlink69@gmail.com)
 * @brief ESP32 TCP OTA
 * @version 1.0
 * @date 2024-01-13
 */

import java.io.IOException;
import java.net.*;
import java.util.*;

import static java.lang.System.exit;

public class OtaMain {
    static final int BROADCAST_PORT = 13333;
    static final int OTA_PORT = 12222;
    static final String FIRMWARE_FILENAME = "esp32-firmware.bin";
    static final int BROADCAST_RCV_TIMEOUT = 2000;
    static final int MAX_NET_INFO = 10;
    static final int MAC_OS = 1;
    static final int WINDOWS_OS = 2;
    static final int LINUX_OS = 3;
    static final int UNKNOWN_OS = 4;
    static int os_type;
    static NetInfo[] netInfo;
    static int number_of_net = 0;
    static int select_interface;
    static List<String> target_ips;
    static String ota_filename;

    static class NetInfo {
        NetworkInterface networkInterface;
        InetAddress inetAddress;
    }

    static void identify_os() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("mac")) {
            log.info("Mac OS");
            os_type = MAC_OS;
        } else if (osName.contains("win")) {
            log.info("Windows OS");
            os_type = WINDOWS_OS;
        } else if (osName.contains("nux") || osName.contains("nix") || osName.contains("aix")) {
            log.info("Linux/Unix OS");
            os_type = LINUX_OS;
        } else {
            log.info("Unknown OS: " + osName);
            os_type = UNKNOWN_OS;
        }
    }

    static void FindTargets() throws IOException, InterruptedException {
        // Get local IP address
        byte[] ipBytes = netInfo[select_interface-1].inetAddress.getAddress();

        // Modify the last byte to 255 for C class broadcast
        ipBytes[3] = (byte) 255;
        InetAddress broadcastAddress = InetAddress.getByAddress(ipBytes);

        // Broadcast message
        String message = "REQUEST IP";
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(BROADCAST_RCV_TIMEOUT);

        int target_num;
        List<String> clientResponses = new ArrayList<>();

        while(true) {
            clientResponses.clear();
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, BROADCAST_PORT);
            socket.send(packet);
            log.info(String.format("Broadcast \"REQUEST IP\" sent to %s:%d",
                    broadcastAddress.getHostAddress(), BROADCAST_PORT));

            buffer = new byte[2048];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

            while (true) {
                try {
                    socket.receive(responsePacket);
                } catch (SocketTimeoutException e) {
                    break;
                }
                String response = new String(buffer, 0, responsePacket.getLength());
                clientResponses.add(response);
                log.info("Received response: " + response);
            }

            log.info("All responses received:");


            System.out.println("(0) Rescan");
            for (int i = 0; i < clientResponses.size(); i++) {
                System.out.printf("(%d) %s\n", i + 1, clientResponses.get(i));
            }
            System.out.printf("(%d) All targets\n", clientResponses.size() + 1);
            System.out.printf("Select target IP(0-%d) : ", clientResponses.size()+1);

            while(true) {
                try {
                    target_num = new Scanner(System.in).nextInt();
                    if(target_num == 0) {
                        log.info("Rescan targets");
                        break;
                    }
                    if (target_num > clientResponses.size()+1) {
                        log.info(String.format("Invalid selection : %d", target_num));
                        Thread.sleep(100);
                        System.out.printf("Select target IP(0-%d) : ", clientResponses.size()+1);
                    } else break;
                } catch(InputMismatchException e1) {
                    log.info("Input number only");
                    Thread.sleep(100);
                    System.out.printf("Select target IP(0-%d) : ", clientResponses.size()+1);
                } catch(NoSuchElementException e2) {
                    log.info("NoSuchElementException");
                }
            }

            if(target_num != 0) break;
        }

        socket.close();

        target_ips = new ArrayList<>();
        if(target_num <= clientResponses.size()) {
            target_ips.add(clientResponses.get(target_num-1));
        } else {
            target_ips.addAll(clientResponses);
        }
    }

    static void OtaProcess() throws InterruptedException, IOException {
        if(target_ips.isEmpty()) {
            log.info("NO target IP, Quit");
            exit(-1);
        }

        OtaTcp[] otaTcps = new OtaTcp[target_ips.size()];
        Thread[] otaThread = new Thread[target_ips.size()];

        for(int i=0;i<target_ips.size();i++) {
            // Thread 로 OTA 동시 실행
            otaTcps[i] = new OtaTcp(target_ips.get(i), ota_filename);
            otaThread[i] = new Thread(otaTcps[i]);
            otaThread[i].start();

            // Thread 없이 순차적으로 실행
            // otaTcps[i].process();
        }

        for(int i=0;i<target_ips.size();i++) {
            otaThread[i].join();
        }

        log.info("All OTA threads finished");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        netInfo = new NetInfo[MAX_NET_INFO];

        identify_os();
        if(args.length == 0) ota_filename = FIRMWARE_FILENAME;
        else ota_filename = args[0];

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            // Filters out 127.0.0.1 and inactive interfaces
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress inetAddress = addresses.nextElement();
                if (inetAddress instanceof Inet4Address) {
                    if(number_of_net < MAX_NET_INFO) {
                        netInfo[number_of_net] = new NetInfo();
                        netInfo[number_of_net].networkInterface = networkInterface;
                        netInfo[number_of_net].inetAddress = inetAddress;
                        number_of_net += 1;
                    }
                }
            }
        }

        log.info(String.format("Number of network interfaces : %d", number_of_net));
        if(number_of_net <= 0) {
            log.error("No available network interface");
            exit(-1);
        }

        if(number_of_net == 1) {
            select_interface = 1;
            log.info(String.format("Interface : %s, IP : %s selected",
                    netInfo[0].networkInterface.getDisplayName(), netInfo[0].inetAddress.getHostAddress()));
        } else {
            System.out.println("(0) Quit");
            for (int i = 0; i < number_of_net; i++) {
                System.out.printf("(%d) Interface : %s, IP : %s\n", i + 1,
                        netInfo[i].networkInterface.getDisplayName(), netInfo[i].inetAddress.getHostAddress());
            }

            System.out.printf("Select interface(0-%d) : ", number_of_net);

            while(true) {
                try {
                    select_interface = new Scanner(System.in).nextInt();
                    if (select_interface < 0 || select_interface > number_of_net) {
                        log.info(String.format("Invalid selection : %d", select_interface));
                        Thread.sleep(100);
                        System.out.printf("Select interface(1-%d) : ", number_of_net);
                    } else break;
                } catch(InputMismatchException e1) {
                    log.info("Input number only");
                    Thread.sleep(100);
                    System.out.printf("Select interface(1-%d) : ", number_of_net);
                }
            }
        }

        if(select_interface == 0) exit(0);

        FindTargets();
        OtaProcess();
    }
}
