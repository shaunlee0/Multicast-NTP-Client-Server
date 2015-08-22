/*

       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9  0  1
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |LI | VN  |Mode |    Stratum    |     Poll      |   Precision    |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                          Root  Delay                           |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                       Root  Dispersion                         |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                     Reference Identifier                       |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                                                                |
      |                    Reference Timestamp (64)                    |
      |                                                                |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                                                                |
      |                    Originate Timestamp (64)                    |
      |                                                                |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                                                                |
      |                     Receive Timestamp (64)                     |
      |                                                                |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                                                                |
      |                     Transmit Timestamp (64)                    |
      |                                                                |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                 Key Identifier (optional) (32)                 |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                                                                |
      |                                                                |
      |                 Message Digest (optional) (128)                |
      |                                                                |
      |                                                                |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

*/
package cnos;

import java.nio.ByteBuffer;


public final class Message {
    
    public byte li;
    public byte vn;
    public byte mode;
    public byte stratum;
    public byte poll;
    public double precision;
    public float root_delay;
    public float root_dispersion;
    public byte[] reference_identifier = new byte[4];
    
    
    // Timestamps
    public SntpTimestamp reference_timestamp;   // Time the system clock was last set (server side)
    public SntpTimestamp originate_timestamp;   // Set by server - from the first transmit time
    public SntpTimestamp receive_timestamp;     // Set by server - When request is receieved
    public SntpTimestamp transmit_timestamp;    // Set by client - sent to the server. Set by server in reply (actual time)
    public SntpTimestamp destination_timestamp; // When final reply is receieved by the client
    
    // Info
    public double delay;
    public double offset;
    
    // For time correction
    //public long time_correction = 2208988800l;
    
    public Message(){
        this.reference_timestamp = new SntpTimestamp();
        this.originate_timestamp = new SntpTimestamp();
        this.receive_timestamp = new SntpTimestamp();
        this.destination_timestamp = new SntpTimestamp();
    }
    
    public Message(byte[] data){

        this.li = (byte)((data[0] >> 6) & 0x03);
        this.vn = (byte)((data[0] >> 3) & 0x07);
        this.mode = (byte)((data[0]) & 0x07);
        this.stratum = (byte)(data[1]);
        this.poll = (byte)(data[2]);
        this.precision = Byte.toUnsignedInt(data[3]);
        
        // Root Delay
        byte[] tempRD = new byte[4];
        System.arraycopy(data, 4, tempRD, 0, 4);
        this.root_delay = bytesToFloat(tempRD);
        
        // Root Dispersion
        byte[] tempRDis = new byte[4];
        System.arraycopy(data, 8, tempRDis, 0, 4);
        this.root_dispersion = bytesToFloat(tempRDis);
        
        //this.root_dispersion = convertData(data, 8, 12);
        this.reference_identifier[0] = data[12];
        this.reference_identifier[1] = data[13];
        this.reference_identifier[2] = data[14];
        this.reference_identifier[3] = data[15];
        
        // Unpack timestamps from server
        this.reference_timestamp = new SntpTimestamp(data, 16, 24);
        this.originate_timestamp = new SntpTimestamp(data, 24, 32);
        this.receive_timestamp = new SntpTimestamp(data, 32, 40);
        this.transmit_timestamp = new SntpTimestamp(data, 40, 48);
  }
    
    public void calculateDestinationDelayOffset(){
        this.destination_timestamp = SntpTimestamp.getTimeNow();
        this.delay = (this.destination_timestamp.time - this.originate_timestamp.time) - (this.transmit_timestamp.time - this.receive_timestamp.time);
        this.offset = ((this.receive_timestamp.time - this.originate_timestamp.time) + (this.transmit_timestamp.time - this.destination_timestamp.time)) / 2;
    }
    
    public byte[] getMessageBytes(){

        byte[] data = new byte[48];
        
        // Pack li, vn & mode
        data[0] = (byte)((this.li << 6) & 0xC0 | (this.vn << 3) & 0x38 | (this.mode & 0x07));
        data[1] = (byte)(this.stratum); // Stratum
        data[2] = (byte)(this.poll); // Poll
        data[3] = (byte)(this.precision); // Precision
        
        // Root Delay
        byte[] tempBytesRootDelay = floatToBytes(this.root_delay);
        System.arraycopy(tempBytesRootDelay, 0, data, 4, tempBytesRootDelay.length);
        
        // Root Dispersion
        byte[] tempBytesRootDis = floatToBytes(this.root_dispersion);
        System.arraycopy(tempBytesRootDis, 0, data, 8, tempBytesRootDis.length);
       
        byte[] tempBytesReference = this.reference_timestamp.getTimestampBytes();
        byte[] tempBytesOrginate = this.originate_timestamp.getTimestampBytes();
        byte[] tempBytesRecieve = this.receive_timestamp.getTimestampBytes();
        byte[] tempBytesTransmit = this.transmit_timestamp.getTimestampBytes();
        
        // Copy transmit time to data
        System.arraycopy(tempBytesReference, 0, data, 16, tempBytesReference.length);
        System.arraycopy(tempBytesOrginate, 0, data, 24, tempBytesOrginate.length);
        System.arraycopy(tempBytesRecieve, 0, data, 32, tempBytesRecieve.length);
        System.arraycopy(tempBytesTransmit, 0, data, 40, tempBytesTransmit.length);
        
        return data;
    }
    
    public static float bytesToFloat(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getFloat();
    }
    
    public static byte [] floatToBytes (float value) {  
         return ByteBuffer.allocate(4).putFloat(value).array();
    }
}
