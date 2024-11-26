import java.util.Random;

public class EndDevice extends Node implements Comparable<EndDevice> {

    private int internalClock;
    private Packet packetToSend ;
    private Sink sink;
    private Channel channel;
    private int lostPackets;
    private int latency ;
    private int tTransmit;
    private int tReceive;


    public EndDevice(long endDeviceID, LoraSettings loraSettings){
        super(endDeviceID,loraSettings);
    }
    public EndDevice(long endDeviceID, LoraSettings loraSettings,Channel channel, int clock){
        super(endDeviceID,loraSettings);
        this.channel=channel;
        setClock(clock);
        lostPackets = 0 ;
        latency = 0 ;
        this.tTransmit = 0;
        this.tReceive = 0 ;
    }
    public void setSink(Sink sink){ this.sink = sink; }

    public void receiveBeacon(CommunicationMode communicationMode){
        switch (communicationMode){
            case LBT -> listenBeforeTalk();
            case TDMA -> transmitInTimeslot();
            case UNICAST -> transmitToTarget();
        }
    }

    public void listenBeforeTalk(){
        startTransmission();
    }
    public void transmitInTimeslot(){
        // compute the start of its time slot
        int timeslot = (int) (wubArrivalTime + (getTimeOnAir() + GUARDTIME) * (super.getId() - 1));
        //System.out.println("timeslot start " + timeslot + " for end device " + this.getId());
        sendPacket(timeslot);
        calculateEnergyConsumption();
    }
    public void transmitToTarget(){
        // starts the transmission directly
        int timeslot = wubArrivalTime;
        sendPacket(timeslot);
        calculateEnergyConsumption();
    }

    public void sendPacket(int timeslot){
        if(packetToSend==null){
            System.out.println("The node with id " + getId() + " has no data to transmit in this cycle.");
            sink.receivePacket(null,timeslot,getId());
        }else{
            sink.receivePacket(packetToSend,timeslot,getId());
        }
    }

    public void calculateEnergyConsumption(){
        resetEnergyConsumption();

        //sleep mode mode 3;
        setMode(3);
        int sleepModeTime = 2 * loraSettings.calculateTimeOnAir(2,1);
        addEnergyConsumption(calculateEnergyConsumptions(sleepModeTime,mode));

        //wake up receiver
        setMode(4);
        // o xronos pou einai se receiving mode
        this.tReceive = wubArrivalTime;
        addEnergyConsumption(calculateEnergyConsumptions(wubArrivalTime,mode));

        //transmiting mode
        if(packetToSend!=null){
            setMode(2);
            this.tTransmit = super.getTimeOnAir();
            addEnergyConsumption(calculateEnergyConsumptions(tTransmit,mode));
        }else {
            this.tTransmit = 0 ;
        }

    }
    public Packet getPacketToSend() { return packetToSend; }
    public void setPacketToSend(Packet packetToSend) { this.packetToSend = packetToSend; }
    public int getLostPackets(){ return lostPackets; }
    public Integer getInternalClock(){ return internalClock ; }

    public void startTransmission() {
        if(packetToSend != null){
            internalClock = clock + wubArrivalTime + (int) getPacketToSend().getGeneratedTime();
            channel.addEndDevice(this);
        }else{
            System.out.println("node with id " + getId() + " has not packet to send.");
        }
    }
    @Override
    public int compareTo(EndDevice other) {
        return Integer.compare(this.internalClock, other.internalClock); // Σύγκριση με βάση το internalClock
    }
    public Channel getChannel(){ return channel; }
    public void setInternalClock(int internalClock){this.internalClock = internalClock;}

    public int getLatency() {
        return latency;
    }

    public void setLatency(int latency) {
        this.latency = latency;
    }
    public void addLatency(int latency) {
        this.latency += latency;
    }
    public double getCADduration() { return loraSettings.cadDuration();}

    public int gettTransmit() {
        return tTransmit;
    }

    public int gettReceive() {
        return tReceive;
    }

    public void settTransmit(int tTransmit) {
        this.tTransmit = tTransmit;
    }

    public void settReceive(int tReceive) {
        this.tReceive = tReceive;
    }
    public int getSpreadingFactor(){ return loraSettings.getSpreadingFactor();}

}
