/**
 * @file OtaTcp.java
 * @author Kwon Taeyoung (xlink69@gmail.com)
 * @brief ESP32 TCP OTA
 * @version 1.0
 * @date 2024-01-13
 */

import java.io.*;
import java.net.Socket;

public class OtaTcp implements Runnable {
    String filename = "esp32-firmware.bin";
    String serverIp = "192.168.4.1";
    int serverPort = 12222;
    byte[] bytesData;

    public OtaTcp(String target_ip, String filename) {
        this.serverIp = target_ip;
        this.filename = filename;
    }

    @Override
    public void run() {
        try {
            FileInputStream inStream = new FileInputStream(filename);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            inStream.transferTo(byteArrayOutputStream);
            bytesData = byteArrayOutputStream.toByteArray();
            inStream.close();
            byteArrayOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info(String.format("Connecting to %s:%d...", serverIp, serverPort));
        Socket socket = null;
        try {
            socket = new Socket(serverIp,serverPort);
        } catch (IOException e) {
            log.error(String.format("%s socket creation ERROR", serverIp));
            return;
        }

        try {
            log.info(String.format("%s Connected", serverIp));

            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();

            byte[] out = "ota".getBytes();
            os.write(out);
            os.flush();
            log.info(String.format("Send OTA message to %s", serverIp));

            log.info(String.format("Waiting for ACK message from %s", serverIp));
            byte[] ret = is.readNBytes(4);
            if(ret == null)
            {
                log.warn(String.format("Read ACK ERROR from %s\n", serverIp));
                socket.close();
                return;
            }
            if(ret.length != 4)
            {
                log.error(String.format("ACK length ERROR : %d, %s\n", ret.length, serverIp));
                socket.close();
                return;
            }
            if(ret[0] != (byte)'A' || ret[1] != (byte)'C' || ret[2] != (byte)'K' || ret[3] != 0)
            {
                log.error(String.format("ACK message ERROR : %02X, %02X, %02X, %02X, %s\n",
                        ret[0], ret[1], ret[2], ret[3], serverIp));
                socket.close();
                return;
            }

            log.info(String.format("Start transmitting to %s\n", serverIp));

            int packetLen = 4096;
            int wrLen, leftLen, writtenLen;
            leftLen = bytesData.length;
            writtenLen = 0;

            while(leftLen > 0)
            {
                if(leftLen > packetLen) wrLen = packetLen;
                else wrLen = leftLen;

                os.write(bytesData, writtenLen, wrLen);
                os.flush();

                writtenLen += wrLen;
                leftLen -= wrLen;

                System.out.printf("%s : %d / %d, %d%%\n", serverIp, writtenLen, bytesData.length,
                        writtenLen * 100 / bytesData.length);
            }

            os.close();
            System.out.printf("\n\n%s : Transmit complete\n", serverIp);
            socket.close();
            return;
        } catch (IOException e) {
            log.warn(e.getMessage());
        }

        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.printf("\n\n%s : Transmit ERROR, Quit\n", serverIp);
    }

    /* Thread 사용하지 않고 직접 실행 */
    public void process() throws IOException {

        try {
            FileInputStream inStream = new FileInputStream(filename);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            inStream.transferTo(byteArrayOutputStream);
            bytesData = byteArrayOutputStream.toByteArray();
            inStream.close();
            byteArrayOutputStream.close();

        } catch (IOException e) {
            System.out.print(e.getMessage());
            return;
        }

        log.info(String.format("Connecting to %s:%d...", serverIp, serverPort));
        Socket socket = null;
        try {
            socket = new Socket(serverIp,serverPort);
        } catch (IOException e) {
            log.error(String.format("%s socket creation ERROR", serverIp));
            return;
        }

        try {
            log.info(String.format("%s Connected", serverIp));

            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();

            byte[] out = "ota".getBytes();
            os.write(out);
            os.flush();
            log.info(String.format("Send OTA message to %s", serverIp));

            log.info(String.format("Waiting for ACK message from %s", serverIp));
            byte[] ret = is.readNBytes(4);
            if(ret == null)
            {
                log.warn(String.format("Read ACK ERROR from %s\n", serverIp));
                socket.close();
                return;
            }
            if(ret.length != 4)
            {
                log.error(String.format("ACK length ERROR : %d, %s\n", ret.length, serverIp));
                socket.close();
                return;
            }
            if(ret[0] != (byte)'A' || ret[1] != (byte)'C' || ret[2] != (byte)'K' || ret[3] != 0)
            {
                log.error(String.format("ACK message ERROR : %02X, %02X, %02X, %02X, %s\n",
                        ret[0], ret[1], ret[2], ret[3], serverIp));
                socket.close();
                return;
            }

            log.info(String.format("Start transmitting to %s\n", serverIp));

            int packetLen = 4096;
            int wrLen, leftLen, writtenLen;
            leftLen = bytesData.length;
            writtenLen = 0;

            while(leftLen > 0)
            {
                if(leftLen > packetLen) wrLen = packetLen;
                else wrLen = leftLen;

                os.write(bytesData, writtenLen, wrLen);
                os.flush();

                writtenLen += wrLen;
                leftLen -= wrLen;

                System.out.printf("%s : %d / %d, %d%%\n", serverIp, writtenLen, bytesData.length,
                        writtenLen * 100 / bytesData.length);
            }

            os.close();
            System.out.printf("\n\n%s : Transmit complete\n", serverIp);
            socket.close();
            return;
        } catch (IOException e) {
            log.warn(e.getMessage());
        }

        socket.close();
        System.out.printf("\n\n%s : Transmit ERROR, Quit\n", serverIp);
    }
}
