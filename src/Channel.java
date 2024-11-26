import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class Channel {
    private PriorityQueue<EndDevice> priorityQueue;
    private int tPreample;
    private boolean isOccupied ;
    private int occupiedClockTimestamp;
    private int clock;
    private Random backoff;
    public static int lostPackets = 0;
    public Channel(int tPreamble){
        this.tPreample = tPreamble;
        priorityQueue = new PriorityQueue<>();
        this.isOccupied = false;
        backoff = new Random();
    }

    public void addEndDevice(EndDevice endDevice){
        priorityQueue.add(endDevice);
    }
    public void startSimulation(){
        while (!priorityQueue.isEmpty()) {
            EndDevice endDevice = priorityQueue.poll(); // pairnei to proto kombo apo thn oura

            System.out.println("The end device " + endDevice.getId() + " is using CAD mode to check for channel occupancy.");
            clock = (int) Math.ceil(endDevice.getCADduration());
            endDevice.addLatency(clock);
//            System.out.println("cad duration : " + clock);
            endDevice.settReceive(clock);

            if (isFree()) { //an to kanali eleuthero
                this.occupiedClockTimestamp = clock + endDevice.getInternalClock();
                clock = occupiedClockTimestamp;
                System.out.println(" The channel is not busy. The end device " + endDevice.getId() +
                        " occupies the channel at "  + clock + " ms ");
                occupy();

                if(priorityQueue.peek() !=null) {
                    sendPreambleOfPacket(endDevice); // edw stelnei to preamble kai mexri na teleiwsei an ksekinisei na stelnei kapoios kombos kanei pisw xrono backoff
                    // otan bgei apo ayth th sunarthsh tha eimai se xrono tpreamble kai den exei staleii akoma to paketo
                    sendWholePacket(endDevice);
                }else{
                    clock += endDevice.getTimeOnAir();
                    endDevice.addLatency(clock - occupiedClockTimestamp);
                    endDevice.settTransmit(endDevice.getTimeOnAir());
                    System.out.println(endDevice.getPacketToSend() + " transmission ends at " + clock);
//                    endDevice.addLatency(clock);
                    this.isOccupied = false;
                }
            }
        }

    }
    //kalytero onoma
    public void sendPreambleOfPacket(EndDevice endDevice){
        while(clock < priorityQueue.peek().getInternalClock() && clock < occupiedClockTimestamp + tPreample ){
            this.clock ++;
        }
        if (clock >= priorityQueue.peek().getInternalClock()){
            // edw shmainei oti thelei na steilei allos kombos kai briskei katilimeno to kanali
            EndDevice deviceToUpdate = priorityQueue.poll();

            System.out.println("The end device " + deviceToUpdate.getId() + " is using the CAD mode to check for channel occupancy.");

            int cadDuration = (int) Math.ceil(deviceToUpdate.getCADduration());
            int backoff = getRandomBackoff(deviceToUpdate);
            deviceToUpdate.setInternalClock(deviceToUpdate.getInternalClock() +backoff + cadDuration);
            deviceToUpdate.addLatency(backoff);
            deviceToUpdate.addLatency(cadDuration);
            deviceToUpdate.settReceive(cadDuration);
            priorityQueue.add(deviceToUpdate);
            System.out.println("The channel is busy from node " + endDevice.getId() + " at time " + clock);
            System.out.println("node : " + deviceToUpdate.getId() + " -> backoff " + backoff);
            if(priorityQueue.peek()!=null) {
                sendPreambleOfPacket(endDevice);
            }
        }
//        else if(clock== occupiedClockTimestamp + tPreample){
//            System.out.println("end of preamble");
//        }
    }

    public int getRandomBackoff(EndDevice endDevice){
        if(endDevice.getSpreadingFactor()==12){
            return 400 + this.backoff.nextInt(1601);
        }else {
            return this.backoff.nextInt(101);
        }
    }
    public void sendWholePacket(EndDevice endDevice){
        int transmissionEnds = occupiedClockTimestamp + endDevice.getTimeOnAir();
        while (clock < priorityQueue.peek().getInternalClock() && clock < occupiedClockTimestamp + endDevice.getTimeOnAir()){
            clock ++ ;
        }
        if( clock == occupiedClockTimestamp + endDevice.getTimeOnAir()){
            System.out.println(endDevice.getPacketToSend() + " transmission ends at " + clock);
            endDevice.addLatency(clock - occupiedClockTimestamp);
            endDevice.settTransmit(endDevice.getTimeOnAir());
//            endDevice.addLatency(clock);
            isOccupied = false;
        }else if(clock >= priorityQueue.peek().getInternalClock()){
            EndDevice colEndDevice = priorityQueue.poll();
            System.out.println("The end device " + colEndDevice.getId() + " is using CAD mode to check for channel occupancy.");

            int cadDuration = (int) Math.ceil(colEndDevice.getCADduration());
            colEndDevice.addLatency(cadDuration);
            colEndDevice.addLatency(colEndDevice.getTimeOnAir());
            colEndDevice.settReceive(cadDuration);
            colEndDevice.settTransmit(colEndDevice.getTimeOnAir());
            int transmissionTime = cadDuration + colEndDevice.getInternalClock();

            System.out.println("the end device " + colEndDevice.getId() + " starts transmitting at time " + transmissionTime +
                    " but end device " + endDevice.getId() + " occupied the channel at " + (clock + cadDuration) + " ms " +
                    "and ended at " + transmissionEnds + " ms.");
            System.out.println(" Collision detected! Packet from end device : " + colEndDevice.getId() + " lost." );
            lostPackets ++;
            if(priorityQueue.peek()!=null) {
                sendWholePacket(endDevice);
            }else{
                while (clock < occupiedClockTimestamp + endDevice.getTimeOnAir()){
                    clock ++ ;
                }
                endDevice.addLatency(clock - occupiedClockTimestamp);
                endDevice.settTransmit(endDevice.getTimeOnAir());
//                endDevice.addLatency(clock);
                System.out.println(endDevice.getPacketToSend() + " transmission ends at " + clock);
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
}
