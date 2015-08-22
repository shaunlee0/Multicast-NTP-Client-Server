package cnos;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Server {

    // Manycast vars
    private final String manycastHost;
    private final int manycastPort;
    MulticastSocket multicastSocket;
    InetAddress group;

    // Message handling
    DatagramPacket packet;
    private Message msg;
    private static Message referenceMessage;

    private final int serverMode = 1;

    public Server(String manycastHost, int manycastPort) {
        this.manycastHost = manycastHost;
        this.manycastPort = manycastPort;
        
        try{
            this.group = InetAddress.getByName(this.manycastHost);
        }catch(UnknownHostException e){
            System.out.println("Error creating group: " + e);
        }
    }

    // Try and create socket
    public void setupSocket() {
        System.out.println("Seting up multicast socket.");
        try {
            this.multicastSocket = new MulticastSocket(this.manycastPort);
            this.multicastSocket.joinGroup(this.group);
        } catch (IOException e) {
            System.out.println("Could not create socket: " + e);
        }

    }

    public void receievePacket() {
        System.out.println("Receiving packets.");
        
        byte[] data = new byte[48];
        
        // Setup datagram packet ro recieve
        packet = new DatagramPacket(data, data.length);
        
        // Wait to recieve packets
        try{
            multicastSocket.receive(packet);
        }catch(Exception e){
            System.out.println("Error recieving packet: " + e);
        }

        msg = new Message(packet.getData());
        
        System.out.println("Recieved Packet");
    }

    public void setupMessage() {
        System.out.println("Setting up message.");
        
        msg.stratum = 1;
        msg.mode = 4;
        msg.root_delay = referenceMessage.root_delay;
        System.out.println("Ref: " + referenceMessage.root_delay);
        msg.reference_timestamp.time = referenceMessage.transmit_timestamp.time;
        System.out.println("Trans: " + referenceMessage.transmit_timestamp.time);
        msg.originate_timestamp.time = msg.transmit_timestamp.time;
        msg.receive_timestamp = SntpTimestamp.getTimeNow();
        msg.transmit_timestamp = SntpTimestamp.getTimeNow();

        //displayMessage(msg);
    }

    public void sendResponse() {
        System.out.println("Sending response.");
        // Data
        byte[] data = new byte[48];
        
        // Setup empty socket
        try{
            multicastSocket = new MulticastSocket();
        }catch(Exception e){
            System.out.println("Error recreating socket: " + e);
        }
        
        // Get the message as bytes and store it in data
        data = msg.getMessageBytes();
        
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
        
        try{
            multicastSocket.setTimeToLive(255);
            multicastSocket.send(sendPacket);
        }catch(IOException e){
            System.out.println("Failed to send packet: " + e);
        }
        
        System.out.println("Sent packet");
    }

    public void closeSocket() {
        multicastSocket.close();
    }

    public void displayMessage(Message msg) {
        System.out.println("SNTP Client Request: ");
        System.out.println("LI: " + msg.li + " VN: " + msg.vn + " Mode: " + msg.mode + " Stratum: " + msg.stratum + " Poll: " + msg.poll + " Precision: " + msg.precision);
        System.out.println("   Reference Timestamp: " + (long) msg.reference_timestamp.getCurrentEpoch());
        System.out.println("   Originate Timestamp: " + (long) msg.originate_timestamp.getCurrentEpoch());
        System.out.println("     Receive Timestamp: " + (long) msg.receive_timestamp.getCurrentEpoch());
        System.out.println("    Transmit Timestamp: " + (long) msg.transmit_timestamp.getCurrentEpoch());

        // Realtime
        System.out.println("Real Time: ");
        System.out.println("  Reference Timestamp: " + new Date((long) msg.reference_timestamp.time * 1000).toString());
        System.out.println("  Originate Timestamp: " + new Date((long) msg.originate_timestamp.time * 1000).toString());
        System.out.println("    Receive Timestamp: " + new Date((long) msg.receive_timestamp.time * 1000).toString());
        System.out.println("   Transmit Timestamp: " + new Date((long) msg.transmit_timestamp.time * 1000).toString());

    }

    public static void main(String[] args) {
        System.out.println("Manycast Server:");
        Server server = new Server("225.4.5.6", 3456);

        Timer timer = new Timer();
        // Get reference time every time minutes
        timer.schedule(new TimerTask() {
            public void run() {
                System.out.println("Setting reference time.");
                ReferenceClock ref = new ReferenceClock("2.pool.ntp.org", 123);
                ref.setupSocket();
                ref.sendPacket();
                ref.receievePacket();
                ref.closeSocket();

                // Set recieve messag to required info
                referenceMessage = new Message(ref.reference_clock.getMessageBytes());
                referenceMessage.root_delay = ref.reference_clock.root_delay;
            }
        }, 0, 60 * 10000);

        // Manycast mode
        while (true) {
            server.setupSocket();
            server.receievePacket();
            server.setupMessage();
            server.sendResponse();
            server.closeSocket();
        }
    }
}
