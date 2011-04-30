package gsn.wrappers.tinyos;

public class SensorScope2Packet {
    public byte[] bytes;
    long timestamp;

    public String toString() {

        StringBuilder hex_sb = new StringBuilder();

        hex_sb.append(timestamp).append(" : ");

        for (int i = 0; i < bytes.length; i++) {
            hex_sb.append(String.format("%02x", bytes[i])).append(" ");
        }

        return hex_sb.toString() + " (" + String.format("%2d", bytes.length) + ")";
    }

    public SensorScope2Packet(long timestamp, byte[] bytes) {
        this.timestamp = timestamp;
        this.bytes = bytes;
    }
}
