import java.util.ArrayList;
import java.util.List;

public class Sink extends Node{
    private ClusterHead clusterHead;
    private List<Packet> receivedPackets;
    private int numOfEds;
    private int latency;
    private int totalPacketsReceived ;
    private CommunicationMode communicationMode;
    private int requiredTimeslots;

    public Sink(long sinkID, LoraSettings loraSettings, ClusterHead clusterHead){
        super(sinkID, loraSettings);
        this.clusterHead = clusterHead;
        this.receivedPackets = new ArrayList<>();
    }

    public void sendWakeUpBeacon(CommunicationMode communicationMode, long endDeviceID){
        this.communicationMode = communicationMode;
        if(communicationMode == CommunicationMode.TDMA){
            resetEnergyConsumption();
            System.out.println("Sink sends a broadcast command beacon to Cluster Head");
            numOfEds = clusterHead.getNumberOfEds();
            latency = 0;
            calculateEnergyConsumptions(numOfEds);
        }else if(communicationMode == CommunicationMode.UNICAST){
            if(endDeviceID==1){
                resetEnergyConsumption();
                latency = 0 ;
            }
            calculateEnergyConsumptions(1);
            System.out.println();
            System.out.println("Sink sends a unicast command beacon to Cluster Head for the End Device with ID " + endDeviceID + " ( TimeOnAir = " + getTimeOnAir() + " ms)");
        }else if( communicationMode == CommunicationMode.LBT){
            latency = 0 ;
            System.out.println("Sink sends a broadcast command beacon to Cluster Head ");
            setClock(getTimeOnAir() + wubArrivalTime);
        }
        clusterHead.receiveWakeUpBeacon(communicationMode,endDeviceID);
    }

    public void calculateEnergyConsumptions(int numberOfEds){
        super.setMode(2); // transmitting mode
        this.requiredTimeslots = super.getRequiredTimeslots() ;

//        addEnergyConsumption(calculateEnergyConsumptions(wubDuration,mode));
        addEnergyConsumption(calculateEnergyConsumptions(requiredTimeslots,mode));
        setClock(requiredTimeslots);

        super.setMode(1);
        int timeInListeningMode=0;
        if(communicationMode == CommunicationMode.TDMA) {
            timeInListeningMode =  (calculateTimeslot()) * numberOfEds;
        }else if (communicationMode == CommunicationMode.UNICAST){
            timeInListeningMode = calculateTimeslot();
        }
        addEnergyConsumption(calculateEnergyConsumptions(timeInListeningMode, mode));
    }

    public void receivePacket(Packet packet, int slotStartTime,long endDeviceID) {
        if (packet != null) {
            System.out.println("The node with id " + packet.getEndDeviceID() + " starts transmitting at  " + slotStartTime + " ms." +
                    " End of transmission " + (slotStartTime + getTimeOnAir() + super.GUARDTIME) + " ms.");
//            System.out.println(packet);
            receivedPackets.add(packet);
            totalPacketsReceived ++;
        }else{
            System.out.println("The node with id " + endDeviceID + " has no packet to transmit. ");
        }
        if(endDeviceID==numOfEds && communicationMode == CommunicationMode.TDMA){
            totalPacketsReceived = receivedPackets.size();
            addClock(this.requiredTimeslots); //ch transmission
            int endOFTrans = clock + slotStartTime + calculateTimeslot();
            super.setClock(endOFTrans);
            latency = super.getClock();
            receivedPackets.clear();
        }else if (communicationMode == CommunicationMode.UNICAST){
            addClock(this.requiredTimeslots); //ch transmission
            int endOFTrans = this.clock + slotStartTime + calculateTimeslot();
            super.setClock(endOFTrans);
            addLatency(clock);
        }else if (communicationMode == CommunicationMode.LBT && packet != null){
            int endOFTrans =  slotStartTime + calculateTimeslot();
            super.setClock(endOFTrans);
            latency = super.getClock();
            receivedPackets.clear();
        }
    }

    public int getLatency(){ return latency;}
    public void setLatency(int latency){ this.latency = latency;}
    public void addLatency(int latency){ this.latency += latency;}
    public int getTotalPacketsReceived(){
        return totalPacketsReceived;
    }
    public void setTotalPacketsReceived(int amount){this.totalPacketsReceived = amount;}
}
