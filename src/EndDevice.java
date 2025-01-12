import java.util.HashMap;
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
    private CommunicationMode communicationMode;
    private int m = 0 ; //number of collisions
    private final int maxRetransmissions = 4 ;
    private int cw; // contention window

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
        setM(0);
        this.communicationMode = CommunicationMode.LBT;
        if(getPacketToSend()==null){
            sendPacket(0);
        }
        calculateEnergyConsumption();
        if (!channel.isSimulationStarted()) {
            channel.startSimulation();
        }

    }

    public void transmitInTimeslot(){
        this.communicationMode = CommunicationMode.TDMA;
        // compute the start of its time slot
        int timeslot = (int) (wubArrivalTime + (calculateTimeslot()) * (super.getId() - 1));
        //System.out.println("timeslot start " + timeslot + " for end device " + this.getId());
        sendPacket(timeslot);
        calculateEnergyConsumption();
    }
    public void transmitToTarget(){
        this.communicationMode = CommunicationMode.UNICAST;
        // starts the transmission directly
        int timeslot = wubArrivalTime;
        sendPacket(timeslot);
        calculateEnergyConsumption();
    }
    public void randomBackoff(int numOfEds){

        int requiredTimeslots = super.getRequiredTimeslots();
        Random random = new Random();
        internalClock = 2 * requiredTimeslots;

        if(numOfEds==1){
           channel.addEndDevice(this);
        } else if(packetToSend!=null) {
            if(loraSettings.getSpreadingFactor()==12) {
                internalClock += random.nextInt(2000);
                channel.addEndDevice(this);
            }else if(loraSettings.getSpreadingFactor()==9){
                internalClock += random.nextInt(500);
                channel.addEndDevice(this);
            }else if (loraSettings.getSpreadingFactor()==7){
                internalClock += random.nextInt(150);
                channel.addEndDevice(this);
            }
        }
    }
    public int binaryExponentialBackOff(){
        m ++;
        if(m>maxRetransmissions){
            System.out.println("Drop packet. Node " + this.getId());
//            this.m = maxRetransmissions-1;
            return -1;
        }
        cw = (int) Math.pow(2,m) - 1 ;
        Random random = new Random();
        int randomNumber = random.nextInt(cw  + 1)  ; // (max - min + 1) + min --> (max - 0 + 1 ) + 0
        System.out.println("random number " + randomNumber);
        int timeslot = calculateTimeslot();
        System.out.println("m = " + m + ", cw " + cw + ", random " + randomNumber);
        return timeslot * randomNumber;
    }

    public void sendPacket(int timeslot){
        if(packetToSend==null){
//            System.out.println("The node with id " + getId() + " has no data to transmit in this cycle.");
            sink.receivePacket(null,timeslot,getId());
        }else{
            sink.receivePacket(packetToSend,timeslot,getId());
        }
    }

    public void calculateEnergyConsumption(){
        resetEnergyConsumption();

        //sleep mode --> mode 3;
        setMode(3);
        int sleepModeTime = getRequiredTimeslots();
        addEnergyConsumption(calculateEnergyConsumptions(sleepModeTime,mode));

        //wake up receiver
        setMode(4);
        this.tReceive = wubDuration;
        addEnergyConsumption(calculateEnergyConsumptions(wubDuration,mode));

        if(communicationMode == CommunicationMode.TDMA || communicationMode == CommunicationMode.UNICAST) {
            //transmitting mode
            if (packetToSend != null) {
                setMode(2);
                this.tTransmit = super.calculateTimeslot();
                addEnergyConsumption(calculateEnergyConsumptions(tTransmit, mode));
            } else {
                this.tTransmit = 0;
            }
        }
    }
    public Packet getPacketToSend() { return packetToSend; }
    public void setPacketToSend(Packet packetToSend) { this.packetToSend = packetToSend; }
    public int getLostPackets(){ return lostPackets; }
    public Integer getInternalClock(){ return internalClock ; }

    @Override
    public int compareTo(EndDevice other) {
        return Integer.compare(this.internalClock, other.internalClock); // Σύγκριση με βάση το internalClock
    }
    public void setLostPackets(int lostPackets){
        this.lostPackets = lostPackets ;
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

    public void addtTransmit(int tTransmit) {
        mode = 2;
        this.tTransmit += tTransmit;
        this.addEnergyConsumption(calculateEnergyConsumptions(tTransmit, mode));
    }

    public void addtReceive(int tReceive) {
        mode = 1;
        this.tReceive += tReceive;
        this.addEnergyConsumption(calculateEnergyConsumptions(tReceive, mode));
    }
    public int getSpreadingFactor(){ return loraSettings.getSpreadingFactor();}
    public void resetEndDevice(){
        tTransmit=0;
        tReceive=0;
        latency=0;
    }
    public double calculateEnergy(int amount, int mode){
        return calculateEnergyConsumptions(amount,mode);
    }

    public int getM() {
        return m;
    }

    public void setM(int m) {
        this.m = m;
    }
    public int getCw() {
        return cw;
    }

    public void setCw(int cw) {
        this.cw = cw;
    }
}
