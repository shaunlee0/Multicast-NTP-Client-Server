package cnos;

public class SntpTimestamp {

    public double time;
    private final long time_correction = 2208988800l;
    private final double max_time = 4294967296.0;

    // Init
    public SntpTimestamp() {
        this.time = 0.0;
    }

    // Pack timestamps to be sent to server
    // Unpack response from server for timestamp
    public SntpTimestamp(byte[] data, int start, int finish) {
        
        double tempTime = 0.0;

        // Go through each array element in data
        for (int i = start; i < finish; i++) {
            int u = Byte.toUnsignedInt(data[i]);
            tempTime = 256.0 * tempTime + u;
        }

        
        // Set this timestamps time
        this.time = tempTime / max_time;
    }

    public SntpTimestamp(long timeNow) {
        double t = ((double) timeNow) / 1000.0;
        this.time = t + time_correction;
    }

    public static SntpTimestamp getTimeNow() {
        long timeNow = System.currentTimeMillis();

        return new SntpTimestamp(timeNow);
    }
    
    public byte[] getTimestampBytes() {
        byte[] data = new byte[8];

        double timeAsDouble = this.time / max_time;
        int byteValue = 0;
        
        for(int i = 0; i < data.length; i++){
            byteValue = (int)(timeAsDouble *= 256.0);
            
            // Set byte value to maximum byte value
            if(byteValue >= 256){
                byteValue = 255;
            }
            
            // Store the byteValue
            data[i] = (byte)byteValue;
            
            // Take off the time we just added to data
            timeAsDouble -= byteValue;
        }

        return data;
    }

    public void toCurrentEpoch() {
        this.time -= time_correction;
    }
    
    public double getCurrentEpoch(){
        
        return this.time - time_correction;
    }
}
