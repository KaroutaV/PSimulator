public class Packet {
    private long endDeviceID;
    private long generatedTime;
    private static int globalSequenceNumber = 0;
    private int sequenceNumber;

    public Packet(long id, long generatedTime) {
        this.endDeviceID = id;
        this.generatedTime=generatedTime;
    }

    public long getEndDeviceID() {
        return endDeviceID;
    }
    public void setEndDeviceID(long endDeviceID) {
        this.endDeviceID = endDeviceID;
    }

    public long getGeneratedTime() {
        return generatedTime;
    }

    public void setGeneratedTime(long generatedTime) {
        this.generatedTime = generatedTime;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }
    public void setSequenceNumber(){
        this.sequenceNumber = ++globalSequenceNumber;
    }

    public String toString() {
        return "The node with ID " + endDeviceID + " sent the packet with sequence number "
                + sequenceNumber + " that was generated at time " + generatedTime + " ms.";
    }
}


