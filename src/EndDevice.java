public class EndDevice extends Node{

    private Packet packetToSent ;
    private Sink sink;
    public EndDevice(long endDeviceID, LoraSettings loraSettings){
        super(endDeviceID,loraSettings);
    }
    public void setSink(Sink sink){ this.sink = sink; }

    public void receivePacket(int numOfNodes){
        // calculation of the timeslot to start transmitting
        int timeslot = (int) (wubArrivalTime + (getTimeOnAir() + GUARDTIME) * super.getId());
        sendPacket(timeslot);

        calculateEnergyConsumption(numOfNodes);
    }

    public void sendPacket(int timeslot){
        if(packetToSent==null){
            System.out.println("The node with id " + getId() + " has no data to transmit in this cycle.");
            sink.receivePacket(null,timeslot,getId());
        }else{
            sink.receivePacket(packetToSent,timeslot,getId());
        }
    }

    public void calculateEnergyConsumption(int numOfNodes){
        resetEnergyConsumption();

        //sleep mode mode 3;
        setMode(3);
        int sleepModeTime = 2 * loraSettings.calculateTimeOnAir(2,1);
        addEnergyConsumption(calculateEnergyConsumptions(sleepModeTime,mode));

        //wake up receiver
        setMode(4);
        addEnergyConsumption(calculateEnergyConsumptions(wubArrivalTime,mode));


        //transmiting mode
        //if(packetToSent!=null){
            setMode(2);
            addEnergyConsumption(calculateEnergyConsumptions(getTimeOnAir(),mode));
        //}
    }

    public void receiveUnicastBeacon(){
        // calculation of the timeslot to start transmitting
        int timeslot = (int) (wubArrivalTime + (getTimeOnAir() + GUARDTIME));
        sendPacket(timeslot);

        calculateEnergyConsumption(1);
    }
    public Packet getPacketToSent() { return packetToSent; }
    public void setPacketToSent(Packet packetToSent) { this.packetToSent = packetToSent; }
}
