package cnos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Testing {

    public static void main(String[] args) {
        float f = (float) 0.5;
        System.out.println("F: " + f);
        
        byte[] temp = floatToBytes(f);
        float fConverted = bytesToFloat(temp);
        System.out.printf("Converted: %f", fConverted);
        
    }

    public static float bytesToFloat(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getFloat();
    }
    
    public static byte [] floatToBytes (float value) {  
         return ByteBuffer.allocate(4).putFloat(value).array();
    }
}
