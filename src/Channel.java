import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;

public class Channel {
    private PriorityQueue<EndDevice> priorityQueue;
    private int tPreample;
    public boolean isOccupied ;
    private int occupiedClockTimestamp;
    private int clock;
    private Sink sink ;
    public static int lostPackets = 0;
    public boolean isStarted = false;
    private boolean flag ;
    public Channel(int tPreamble){
        this.tPreample = tPreamble;
        priorityQueue = new PriorityQueue<>();
        this.isOccupied = false;
        flag = true;
    }
    public void setSink(Sink sink){
        this.sink = sink;
    }

    public void addEndDevice(EndDevice endDevice){
        priorityQueue.add(endDevice);
    }
    public void startSimulation(){
        isStarted = true;
        while (!priorityQueue.isEmpty()) {
            EndDevice endDevice = priorityQueue.poll(); // takes from the queue the ed that transmits

            System.out.println("The end device " + endDevice.getId() + " is using CAD mode to check for channel occupancy at "
                    + endDevice.getInternalClock() + " ms.");
            clock = (int) Math.ceil(endDevice.getCADduration());
//            System.out.println("cad duration : " + clock);
            endDevice.addtReceive(clock);

            if (isFree()) { //if the channel is free
                //occupiedClockTimestamp = time required for the cad operation + the time the ed starts the transmission

                this.occupiedClockTimestamp = clock + endDevice.getInternalClock();
                clock = occupiedClockTimestamp;
                System.out.println(" The channel is not busy. The end device " + endDevice.getId() +
                        " occupies the channel at "  + clock + " ms ");
                occupy();

                if(priorityQueue.peek() !=null) {
                    //simulate the transmission of the packet preamble to detect any transmission attempts from other ed
                    //if another ed tries to transmit it finds the channel busy and backoff

                    sendPreambleOfPacket(endDevice);
                    if(priorityQueue.peek() !=null) {
                        sendWholePacket(endDevice);
                    }else{
                        clock += endDevice.calculateTimeslot();
                        endDevice.addtTransmit(endDevice.getTimeOnAir());
                        endDevice.setM(0);
                        sink.receivePacket(endDevice.getPacketToSend(),clock-endDevice.calculateTimeslot(),endDevice.getId());
                        endDevice.addLatency(clock);
                        this.isOccupied = false;
                    }
                }else{
                    //if there is no other ed to transmit
                    clock += endDevice.calculateTimeslot();
                    endDevice.addtTransmit(endDevice.getTimeOnAir());
                    endDevice.setM(0);
                    sink.receivePacket(endDevice.getPacketToSend(),clock-endDevice.calculateTimeslot(),endDevice.getId());
                    endDevice.addLatency(clock);
                    this.isOccupied = false;
                }
            }
        }
        isStarted=false;
    }
    //kalytero onoma
    public void sendPreambleOfPacket(EndDevice endDevice){
        while(clock < priorityQueue.peek().getInternalClock() && clock < occupiedClockTimestamp + tPreample ){
            this.clock ++;
        }
        if (clock >= priorityQueue.peek().getInternalClock()){
            //running this code block if other ed tries to transmit while sending the preamble part.
            //the ed that wants to transmit finds the channel busy and backs off
            EndDevice deviceToUpdate = priorityQueue.poll();

            System.out.println("The end device " + deviceToUpdate.getId() + " is using the CAD mode to check for channel occupancy at time "
                    + clock + " ms.");

            int cadDuration = (int) Math.ceil(deviceToUpdate.getCADduration());
            int backoff = deviceToUpdate.binaryExponentialBackOff();
            if(backoff<0){
                lostPackets++;
//                flag = false;
            }else {
                deviceToUpdate.setInternalClock(deviceToUpdate.getInternalClock() + backoff /*+ cadDuration*/);
                deviceToUpdate.addtReceive(cadDuration);
                priorityQueue.add(deviceToUpdate);
                System.out.println("The channel is busy from node " + endDevice.getId() + " at time " + clock);
                System.out.println("node : " + deviceToUpdate.getId() + " -> backoff " + backoff);
            }
            if(priorityQueue.peek()!=null) {
//                flag = true;
                sendPreambleOfPacket(endDevice);
            }
        }
//        else if(clock== occupiedClockTimestamp + tPreample){
//            System.out.println("end of preamble");
//        }
    }
    public void sendWholePacket(EndDevice endDevice){
        //completion of transmission
        int transmissionEnds = occupiedClockTimestamp + endDevice.getTimeOnAir();
        while (clock < priorityQueue.peek().getInternalClock() && clock < transmissionEnds){
            clock ++ ;
        }
        if( clock == transmissionEnds){
            sink.receivePacket(endDevice.getPacketToSend(),occupiedClockTimestamp,endDevice.getId());
//            System.out.println(endDevice.getPacketToSend() + " transmission ends at " + clock);
            endDevice.addtTransmit(endDevice.getTimeOnAir());
            endDevice.addLatency(clock);
            endDevice.setM(0);
            isOccupied = false;
        }else if(clock >= priorityQueue.peek().getInternalClock()){
            EndDevice colEndDevice = priorityQueue.poll();
            System.out.println("The end device " + colEndDevice.getId() + " is using CAD mode to check for channel occupancy at "
                    + clock + " ms." );

            int cadDuration = (int) Math.ceil(colEndDevice.getCADduration());
            colEndDevice.addtReceive(cadDuration);
            colEndDevice.addtTransmit(colEndDevice.getTimeOnAir());
            int transmissionTime = cadDuration + colEndDevice.getInternalClock();

            System.out.println("the end device " + colEndDevice.getId() + " starts transmitting at time " + transmissionTime +
                    " but end device " + endDevice.getId() + " occupied the channel at " + (clock + cadDuration) + " ms " +
                    "and ended at " + transmissionEnds + " ms.");
            System.out.println(" Collision detected! Packet from end device : " + colEndDevice.getId() + " lost." );
            lostPackets ++;
            if(priorityQueue.peek()!=null) {
                sendWholePacket(endDevice);
            }else{
                while (clock < occupiedClockTimestamp + endDevice.calculateTimeslot()){
                    clock ++ ;
                }
                endDevice.addtTransmit(endDevice.getTimeOnAir());
                endDevice.addLatency(clock);
                sink.receivePacket(endDevice.getPacketToSend(),occupiedClockTimestamp,endDevice.getId());
                endDevice.setM(0);
                this.isOccupied = false;
            }
        }
    }
    public boolean isFree(){
        return !isOccupied;
    }
    public void occupy(){ this.isOccupied = true; }

    public PriorityQueue<EndDevice> getPriorityQueue() {
        return priorityQueue;
    }

    public void setPriorityQueue(PriorityQueue<EndDevice> priorityQueue) {
        this.priorityQueue = priorityQueue;
    }
    public int getClock(){return clock;}

    public boolean isSimulationStarted() {
        return isStarted;
    }
}
