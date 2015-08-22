package cnos;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ReferenceClock {

    // Reference clock information
    private DatagramSocket socket;
    private DatagramPacket packet;
    private final String host;
    private final int port;
    public Message reference_clock = new Message();

    public ReferenceClock(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setupSocket() {
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("Reference Clock - Could not create socket: " + e);
        }
    }

    public void sendPacket() {
        // Data
        byte[] data = new byte[48];

        // Create message
        Message msg = new Message();
        reference_clock.li = 0;
        reference_clock.vn = 1;
        reference_clock.mode = 3;
        reference_clock.transmit_timestamp = SntpTimestamp.getTimeNow();

        // Get the message as bytes and store it in data
        data = reference_clock.getMessageBytes();

        // Try and create packet
        try {
            packet = new DatagramPacket(data, data.length, InetAddress.getByName(this.host), this.port);

            // Try and send packet
            try {
                socket.send(packet);
            } catch (IOException e) {
                System.out.println("Reference Clock - Failed to send packet: " + e);
            }
        } catch (UnknownHostException e) {
            System.out.println("Reference Clock - Failed to create packet: " + e);
        }

        System.out.println("Reference Clock - Sent packet");
    }

    public void receievePacket() {
        byte data[] = new byte[48];

        // Setup datagram packet to receieve
        packet = new DatagramPacket(data, data.length);

        // Wait to receieve packet
        try {
            socket.receive(packet);

        } catch (IOException e) {
            System.out.println("Reference Clock - Failed to receieve packet: " + e);
        }

       reference_clock = new Message(packet.getData());
       reference_clock.destination_timestamp = SntpTimestamp.getTimeNow();
       
       reference_clock.root_delay = (float) ((reference_clock.destination_timestamp.time - reference_clock.originate_timestamp.time) - (reference_clock.transmit_timestamp.time - reference_clock.receive_timestamp.time));
       System.out.println("ReferenceClock: " + reference_clock.root_delay);
       System.out.println("Reference Clock: Got packet");

    }

    public void closeSocket() {
        socket.close();
    }
}
