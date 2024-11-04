import java.util.Random;

public class EndDevice extends Node implements Runnable{

    private static int internalClock;

    private Packet packetToSend ;
    private Sink sink;
    private Channel channel;
    private Random backoff;
    private int lostPackets;

    public EndDevice(long endDeviceID, LoraSettings loraSettings){
        super(endDeviceID,loraSettings);
    }
    public EndDevice(long endDeviceID, LoraSettings loraSettings,Channel channel, int clock){
        super(endDeviceID,loraSettings);
        this.channel=channel;
        this.backoff = new Random();
        setClock(clock);
        lostPackets = 0 ;
    }
    public void setSink(Sink sink){ this.sink = sink; }

    public void receivePacket(int numOfNodes){
        // calculation of the timeslot to start transmitting
        int timeslot = (int) (wubArrivalTime + (getTimeOnAir() + GUARDTIME) * super.getId());
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
    public Packet getPacketToSend() { return packetToSend; }
    public void setPacketToSend(Packet packetToSend) { this.packetToSend = packetToSend; }

    @Override
    public void run() {

        internalClock = clock ;

        try {
            Thread.sleep(1000*getId());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int currentClock = clock ;

        if (packetToSend == null) {
            System.out.println("Node " + super.getId() + " has no packet to send, stopping.");
            return; // Τερματισμός νήματος
        }

        // Αρχίστε να στέλνετε πακέτα
        while (packetToSend != null) {
            internalClock = internalClock + loraSettings.switchToCAD();
            channel.cca(internalClock);
            if (channel.isFree()) {
                channel.occupy(); // Καταλαμβάνει το κανάλι
                System.out.println("Node " + super.getId() + " sending packet " + packetToSend.getEndDeviceID());
                try {
                    System.out.println(super.getId() + " exw to kanali se xrono " + internalClock);
                    Thread.sleep(loraSettings.getTpreamble()); // Χρόνος preamble
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                internalClock = internalClock + loraSettings.getTpreamble();
                // Προσομοίωση μετάδοσης
                try {
                    Thread.sleep(getTimeOnAir() - loraSettings.getTpreamble()); // Χρόνος μετάδοσης
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                internalClock = internalClock + getTimeOnAir() - loraSettings.getTpreamble();
                System.out.println("Node " + super.getId() + " finished sending packet " + packetToSend.getEndDeviceID());
                System.out.println(packetToSend);

                channel.release(); // Απελευθερώνει το κανάλι
                this.packetToSend = null;
                break; // Τέλος αποστολής για αυτό το πακέτο
            } else {
                // Το κανάλι είναι κατειλημμένο
                System.out.println("Node " + super.getId() + " waiting to send packet " + packetToSend.getEndDeviceID());
                int backoff = this.backoff.nextInt(2000) ;
                //System.out.println("backoff " + backoff);
                try {
                    Thread.sleep(backoff); // Τυχαία αναμονή {0,2}s
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if(internalClock > currentClock + 10000){
                System.out.println("packet lost");
                packetToSend = null;
                lostPackets ++;
                return;
            }
        }
    }
//    @Override
//    public void run() {
//        // Αναμονή για την καθυστέρηση πριν την αποστολή
//        try {
//            Thread.sleep(1000 * getId());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // Έλεγχος αν το πακέτο είναι null πριν ξεκινήσει η αποστολή
//        if (packetToSend == null) {
//            System.out.println("Node " + getId() + " has no packet to send, stopping.");
//            return; // Τερματισμός νήματος
//        }
//
//        // Αρχίστε να στέλνετε πακέτα
//        while (packetToSend != null) {
//            if (channel.cca()) { // Έλεγχος καναλιού
//                channel.occupy(); // Προσπάθεια κατάληψης του καναλιού
//                // Έλεγχος για πιθανή σύγκρουση
//                if (!channel.isFree()) {
//                    System.out.println("Collision detected! Node " + getId() + " failed to send packet " + packetToSend.getEndDeviceID());
//                    channel.release(); // Απελευθερώνει το κανάλι λόγω αποτυχίας
//                    break; // Διακοπή αποστολής λόγω σύγκρουσης
//                }
//
//                System.out.println("Node " + getId() + " sending packet " + packetToSend.getEndDeviceID());
//                // Προσομοίωση μετάδοσης
//                try {
//                    Thread.sleep(getTimeOnAir()); // Χρόνος μετάδοσης
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                System.out.println("Node " + getId() + " finished sending packet " + packetToSend.getEndDeviceID());
//                channel.release(); // Απελευθερώνει το κανάλι
//                break; // Τέλος αποστολής για αυτό το πακέτο
//            } else {
//                // Το κανάλι είναι κατειλημμένο
//                System.out.println("Node " + getId() + " waiting to send packet " + packetToSend.getEndDeviceID());
//                try {
//                    Thread.sleep(backoff.nextInt(2000)); // Τυχαία αναμονή {0,2s}
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
    public int getLostPackets(){ return lostPackets; }

}
