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

            clock = (int) Math.ceil(endDevice.getCADduration());
//            System.out.println("cad duration : " + clock);

            if (isFree()) { //an to kanali eleuthero
                occupy(); //katelabe to
                this.occupiedClockTimestamp = clock + endDevice.getInternalClock();
                clock = occupiedClockTimestamp;

                if(priorityQueue.peek() !=null) {
                    sendPreambleOfPacket(endDevice); // edw stelnei to preamble kai mexri na teleiwsei an ksekinisei na stelnei kapoios kombos kanei pisw xrono backoff
                    // otan bgei apo ayth th sunarthsh tha eimai se xrono tpreamble kai den exei staleii akoma to paketo
                    sendWholePacket(endDevice);
                }else{
                    clock = endDevice.getInternalClock() + endDevice.getTimeOnAir();
                    endDevice.addLatency(clock - occupiedClockTimestamp);
                    System.out.println(endDevice.getPacketToSend());
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

            int cadDuration = (int) Math.ceil(deviceToUpdate.getCADduration());
            int backoff = 500 ;//this.backoff.nextInt(2000);
            deviceToUpdate.setInternalClock(deviceToUpdate.getInternalClock()+backoff + cadDuration);
            deviceToUpdate.addLatency(backoff + cadDuration);
            priorityQueue.add(deviceToUpdate);
            System.out.println("node : " + deviceToUpdate.getId() + " -> backoff " + backoff);
            if(priorityQueue.peek()!=null) {
                sendPreambleOfPacket(endDevice);
            }
        }
//        else if(clock== occupiedClockTimestamp + tPreample){
//            System.out.println("to preamble kommati stalthike");
//        }
    }
    public void sendWholePacket(EndDevice endDevice){
        while (clock < priorityQueue.peek().getInternalClock() && clock < occupiedClockTimestamp + endDevice.getTimeOnAir()){
            clock ++ ;
        }
        if( clock == occupiedClockTimestamp + endDevice.getTimeOnAir()){
            System.out.println(endDevice.getPacketToSend());
            endDevice.addLatency(clock - occupiedClockTimestamp);
            isOccupied = false;
        }else if(clock >= priorityQueue.peek().getInternalClock()){
            EndDevice colEndDevice = priorityQueue.poll();
            int cadDuration = (int) Math.ceil(colEndDevice.getCADduration());
            colEndDevice.addLatency(cadDuration);
            System.out.println("the ed starts transmitting at time " + colEndDevice.getInternalClock() +
                    " but the previous node occupy the channel at time " + clock);
            System.out.println("collision! lost packet : " + colEndDevice.getPacketToSend() );
            lostPackets ++;
            if(priorityQueue.peek()!=null) {
                sendWholePacket(endDevice);
            }else{
                while (clock < occupiedClockTimestamp + endDevice.getTimeOnAir()){
                    clock ++ ;
                }
                endDevice.addLatency(clock - occupiedClockTimestamp);
                System.out.println(endDevice.getPacketToSend());
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
