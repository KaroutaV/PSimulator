import java.util.List;

public class ClusterHead extends Node{

    private List<EndDevice> endDeviceList;
    public ClusterHead(long clusterHeadID, LoraSettings loraSettings, List<EndDevice> endDeviceList){
        super(clusterHeadID,loraSettings);
        this.endDeviceList = endDeviceList;
    }

    public void receiveWakeUpBeacon(CommunicationMode communicationMode, long endDeviceID){
        if(communicationMode == CommunicationMode.TDMA || communicationMode == CommunicationMode.LBT){
            sendBroadcastBeacon(communicationMode);
        }else if(communicationMode == CommunicationMode.UNICAST){
            sendUnicastBeacon(endDeviceID,communicationMode);
        }
    }
    public void sendBroadcastBeacon(CommunicationMode communicationMode){
        System.out.println("Cluster Head send a broadcast wake up beacon to end devices. ");
        System.out.println();
        resetEnergyConsumption();
        calculateEnergyConsumptions(endDeviceList.size());
        for(EndDevice endDevice:endDeviceList){
            endDevice.receiveBeacon(communicationMode);
        }
    }
    public void sendUnicastBeacon(long endDeviceID, CommunicationMode communicationMode) {
        calculateEnergyConsumptions(1);
        for(EndDevice endDevice : endDeviceList){
            if(endDevice.getId()==endDeviceID){
                System.out.println("Cluster head sends a wake up beacon to end device with id " + endDeviceID);
                endDevice.receiveBeacon(communicationMode);
            }
        }
    }
    public void calculateEnergyConsumptions(int numberOfEds){
        //in listenning mode until it receives the command
        setMode(1);
        int timeInListMode = loraSettings.calculateTimeOnAir(2,1);
        addEnergyConsumption(calculateEnergyConsumptions(timeInListMode,super.mode));

        //in transmitting mode
        setMode(2);
        int trasmittingTime = timeInListMode;
        addEnergyConsumption(calculateEnergyConsumptions(trasmittingTime,super.mode));

        // in listenning mode until the end
        super.setMode(1);
        int startListeningMode = timeInListMode + trasmittingTime + wubArrivalTime;
        int endOfListeningMode;
        if(numberOfEds!=1) {
            endOfListeningMode = startListeningMode + (super.getTimeOnAir() + super.GUARDTIME) * numberOfEds;
        }else {
            endOfListeningMode = startListeningMode + (super.getTimeOnAir() + super.GUARDTIME);
        }
        addEnergyConsumption(calculateEnergyConsumptions(endOfListeningMode-startListeningMode,mode));
    }


    public int getNumberOfEds(){ return endDeviceList.size();}
}
