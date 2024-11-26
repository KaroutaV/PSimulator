import java.util.ArrayList;
import java.util.List;

public class Sink extends Node{
    private ClusterHead clusterHead;
    private List<Packet> receivedPackets;
    private int numOfEds;
    private int latency;
    private int totalPacketsReceived ;
    private CommunicationMode communicationMode;

    public Sink(long sinkID, LoraSettings loraSettings, ClusterHead clusterHead){
        super(sinkID, loraSettings);
        this.clusterHead = clusterHead;
        this.receivedPackets = new ArrayList<>();
    }

    public void sendWakeUpBeacon(CommunicationMode communicationMode, long endDeviceID){
        this.communicationMode = communicationMode;
        if(communicationMode == CommunicationMode.TDMA || communicationMode == CommunicationMode.LBT){
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
            System.out.println("Sink sends a unicast command beacon to Cluster Head for the End Device with ID " + endDeviceID);
        }
        clusterHead.receiveWakeUpBeacon(communicationMode,endDeviceID);
    }

    public void calculateEnergyConsumptions(int numberOfEds){
        super.setMode(2); // transmitting mode
        int timeOnAir = loraSettings.calculateTimeOnAir(2,1);
//        System.out.println("beacon air time " + timeOnAir);

        addEnergyConsumption(calculateEnergyConsumptions(timeOnAir,mode));
        setClock(timeOnAir);

        //after timeOnAir * 2 + wubArrivalTime it changes to listening mode (mode 1)
        super.setMode(1);

        int startListeningMode = clock + timeOnAir + wubArrivalTime;
        if(communicationMode == CommunicationMode.TDMA) {
            int endOfListeningMode = startListeningMode + (super.getTimeOnAir() + super.GUARDTIME) * numberOfEds;
            addEnergyConsumption(calculateEnergyConsumptions(endOfListeningMode - startListeningMode, mode));
        }else if (communicationMode == CommunicationMode.UNICAST){
            int endOfListeningMode = startListeningMode + super.getTimeOnAir();
            addEnergyConsumption(calculateEnergyConsumptions(endOfListeningMode - startListeningMode, mode));
        }
        setClock(startListeningMode - wubArrivalTime);
    }

    public void receivePacket(Packet packet, int timeslot,long endDeviceID) {
        if (packet != null) {
            System.out.println("The node with id " + packet.getEndDeviceID() + " starts transmitting at " + timeslot + " ms.");
            System.out.println(packet);
            receivedPackets.add(packet);
            totalPacketsReceived ++;
        }
        if(endDeviceID==numOfEds && communicationMode == CommunicationMode.TDMA){
            totalPacketsReceived = receivedPackets.size();
            int endOFTrans = timeslot + getTimeOnAir() + GUARDTIME ;
            super.addClock(endOFTrans);
            latency = super.getClock();
            receivedPackets.clear();
        }else if (communicationMode == CommunicationMode.UNICAST){
            int endOFTrans = this.clock + timeslot + getTimeOnAir();
            super.setClock(endOFTrans);
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
