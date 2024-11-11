import java.util.Random;

public class EndDevice extends Node implements Comparable<EndDevice> {

    private int internalClock;
    private Packet packetToSend ;
    private Sink sink;
    private Channel channel;
    private int lostPackets;
    private int latency ;

    public EndDevice(long endDeviceID, LoraSettings loraSettings){
        super(endDeviceID,loraSettings);
    }
    public EndDevice(long endDeviceID, LoraSettings loraSettings,Channel channel, int clock){
        super(endDeviceID,loraSettings);
        this.channel=channel;
        setClock(clock);
        lostPackets = 0 ;
        latency = 0 ;
    }
    public void setSink(Sink sink){ this.sink = sink; }

    public void receivePacket(int numOfNodes){
        // compute the start of its time slot
        int timeslot = (int) (wubArrivalTime + (getTimeOnAir() + GUARDTIME) * (super.getId() - 1 ));
        //System.out.println("timeslot start " + timeslot + " for end device " + this.getId());
        sendPacket(timeslot);
        calculateEnergyConsumption(numOfNodes);
    }

    public void sendPacket(int timeslot){
        if(packetToSend==null){
            System.out.println("The node with id " + getId() + " has no data to transmit in this cycle.");
            sink.receivePacket(null,timeslot,getId());
        }else{
            sink.receivePacket(packetToSend,timeslot,getId());
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
        if(packetToSend!=null){
            setMode(2);
            addEnergyConsumption(calculateEnergyConsumptions(super.getTimeOnAir(),mode));
        }
    }

    public void receiveUnicastBeacon(){
        // starts the transmission directly
        int timeslot = wubArrivalTime;
        sendPacket(timeslot);

        calculateEnergyConsumption(1);
    }
    public Packet getPacketToSend() { return packetToSend; }
    public void setPacketToSend(Packet packetToSend) { this.packetToSend = packetToSend; }
    public int getLostPackets(){ return lostPackets; }
    public Integer getInternalClock(){ return internalClock ; }

    public void generateRandomTimeToSent() {
        //den bazw sthn oura nodes ta opoia den exoun na metadosoyn paketa
        if(packetToSend != null){
//            //tyxaia kathusterisi gia na mhn ksekinane ola ta nhmata mazi
//            internalClock = clock;
//            Random random = new Random();
//            int r = random.nextInt(2000);
//
//            internalClock += r;
            //prosteto to ed sto queue tou channel

            // to bazw na kseikina molis kanei sensing
           internalClock = clock;
           internalClock += (int) getPacketToSend().getGeneratedTime();
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


}
