package cnos;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;

public class Client {

    private MulticastSocket socket;
    private DatagramPacket packet;
    private final String host;
    private final int port;
    
    private static boolean cont = true;
    
    private boolean firstResponse;
    private InetAddress unicastHost;
    private int unicastPort = 0;
    
    // Storing best 3
    private ArrayList<Message> messageArray;
    private ArrayList<DatagramPacket> packetArray = new ArrayList<>();
    
    // Client constructor
    public Client(String host, int port){
        this.host = host;
        this.port = port;
        this.firstResponse = true;
    }
    
    // Send packets
    public void sendPacket(){
        
        // Data
        byte[] data = new byte[48];
        
        // Create message
        Message msg = new Message();
        msg.li = 0;
        msg.vn = 1;
        msg.mode = 3;
        msg.reference_identifier = "LOCL".getBytes();
        msg.transmit_timestamp = SntpTimestamp.getTimeNow();
        
        // Get the message as bytes and store it in data
        data = msg.getMessageBytes();
        
        // Try and create packet
        try{
            packet = new DatagramPacket(data, data.length, InetAddress.getByName(this.host), this.port);
            
            // Try and send packet
            try{
                socket.send(packet);
            }catch(IOException e){
                System.out.println("Failed to send packet: " + e);
            }
        }catch(UnknownHostException e){
            System.out.println("Failed to create packet: " + e);
        }
        
        System.out.println("Sent packet");
    }
    
    // Receieve Packet
    public void receievePacket(){
        
        byte data[] = new byte[48];
        
        // Setup datagram packet to receieve
        packet = new DatagramPacket(data, data.length);
        
        // Set socket timeout
        try{ socket.setSoTimeout(10); } catch(Exception e){ System.out.println("Timeout"); };
        
        // Wait to receieve packet
        try{
            socket.receive(packet);
            packetArray.add(packet);

        }catch(IOException e){
            //System.out.println("Failed to receieve packet: " + e);
            cont = false;
        }
        
        if(cont){
        
            // Set subsequent unicast server
            if(this.firstResponse){
                setUnicastServer(packet);
                System.out.println("Setting once");
            }

            this.firstResponse = false;

            Message msg = new Message(packet.getData());
            msg.calculateDestinationDelayOffset();

        }
    }
    
    public void mitigatePackets(){
        
        messageArray = new ArrayList<>();
        
        // Loop through arraylist
        for(DatagramPacket p : packetArray){
            // Add to message array
            Message mes = new Message(p.getData());
            mes.calculateDestinationDelayOffset();
            messageArray.add(mes);
        }
        
        // For all messages
        for (int i = 1; i < messageArray.size(); i++) {

            Message current = messageArray.get(i);
            double value = messageArray.get(i).delay;

            int toCompare = i;
            
            System.out.println("i: " + i + " delay: " + messageArray.get(i).delay);

            while (toCompare > 0 && messageArray.get(toCompare - 1).delay > value) {
                messageArray.set(toCompare, messageArray.get(toCompare - 1));
                toCompare = toCompare - 1;
            }
            
            messageArray.set(toCompare, current);
        }
    }
    
    public void setUnicastServer(DatagramPacket p){
        this.unicastHost = p.getAddress();
        this.unicastPort = p.getPort();
    }
    // Try and create socket
    public void setupSocket() {
        try{
            this.socket = new MulticastSocket();
        }catch(IOException e){
            System.out.println("Could not create socket: " + e);
        }
    }
    
    public void closeSocket(){
        socket.close();
    }
    
    // Display ntp message
    public void displayMessage(){
        int breakCount = 0;
        for(Message msg : messageArray){
            long offset = (long)(msg.offset * 1000);
            long clientT = System.currentTimeMillis();
            long serverT = (long)(clientT + offset);
            System.out.println("Offset: " + offset + "ms");
            System.out.printf("Delay: %f \n", (double)msg.delay);
            System.out.println("Client: " + new Date(clientT).toString());
            System.out.println("Server: " + new Date(serverT).toString());

            // Response from server
            System.out.println("SNTP Server Response: ");
            System.out.println("LI: " + msg.li + " VN: " + msg.vn + " Mode: " + msg.mode + " Stratum: " + msg.stratum + " Poll: " + msg.poll + " Precision: " + msg.precision);
            System.out.printf("\nRoot Delay: %f\n", msg.root_delay);
            System.out.printf("\nRoot Dispersion: %f\n", msg.root_dispersion);
            System.out.printf("   Reference Timestamp %f ", msg.reference_timestamp.getCurrentEpoch());
            System.out.printf("\n   Originate Timestamp: %f ", msg.originate_timestamp.getCurrentEpoch());
            System.out.printf("\n     Receive Timestamp: %f ", msg.receive_timestamp.getCurrentEpoch());
            System.out.printf("\n    Transmit Timestamp: %f ", msg.transmit_timestamp.getCurrentEpoch());
            System.out.printf("\n Destination Timestamp: %f ", msg.destination_timestamp.getCurrentEpoch());

            // Realtime
            System.out.println("\nReal Time: ");
            System.out.println("  Reference: " + new Date((long)msg.reference_timestamp.getCurrentEpoch() * 1000).toString());
            System.out.println("  Originate: " + new Date((long)msg.originate_timestamp.getCurrentEpoch() * 1000).toString());
            System.out.println("    Receive: " + new Date((long)msg.receive_timestamp.getCurrentEpoch() * 1000).toString());
            System.out.println("   Transmit: " + new Date((long)msg.transmit_timestamp.getCurrentEpoch() * 1000).toString());
            System.out.println("Destination: " + new Date((long)msg.destination_timestamp.getCurrentEpoch() * 1000).toString());
            breakCount++;
            if(breakCount == 3) break;
        }
    }

    public static void main(String args[]) {
        System.out.println("Client:");
        Client client = new Client("225.4.5.6", 3456);
        client.setupSocket();
        client.sendPacket();
        
        while(cont){
            client.receievePacket();
        }
        
        client.mitigatePackets();
        client.closeSocket();
        
        // Display server responses
        client.displayMessage();
    }
}
