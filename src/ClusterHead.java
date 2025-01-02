import java.util.List;

public class ClusterHead extends Node{

    private List<EndDevice> endDeviceList;
    CommunicationMode communicationMode;
    public ClusterHead(long clusterHeadID, LoraSettings loraSettings, List<EndDevice> endDeviceList){
        super(clusterHeadID,loraSettings);
        this.endDeviceList = endDeviceList;
    }

    public void receiveWakeUpBeacon(CommunicationMode communicationMode, long endDeviceID){
        this.communicationMode = communicationMode;
        if(communicationMode == CommunicationMode.TDMA || communicationMode == CommunicationMode.LBT){
            sendBroadcastBeacon(communicationMode);
        }else if(communicationMode == CommunicationMode.UNICAST){
            sendUnicastBeacon(endDeviceID,communicationMode);
        }
    }
    public void sendBroadcastBeacon(CommunicationMode communicationMode){
        System.out.println("Cluster Head send a broadcast wake up beacon to end devices. (17 ms) ");
        System.out.println();
        if(communicationMode == CommunicationMode.TDMA) {
            resetEnergyConsumption();
            calculateEnergyConsumptions(endDeviceList.size());
        }
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
        int timeInListMode = getRequiredTimeslots();
        addEnergyConsumption(calculateEnergyConsumptions(timeInListMode,super.mode));

        //in transmitting mode
        setMode(2);
        int transmittingTime = wubDuration ;
        if(getTimeOnAir()<transmittingTime) {
            transmittingTime= getRequiredTimeslots();
        }
        addEnergyConsumption(calculateEnergyConsumptions(transmittingTime,mode));

        // in listenning mode until the end
        super.setMode(1);
        int timeInListeningMode = 0;
        if(communicationMode == CommunicationMode.TDMA){
            timeInListeningMode = (super.calculateTimeslot()) * numberOfEds;
        }else if(communicationMode == CommunicationMode.UNICAST){
            timeInListeningMode = super.calculateTimeslot();
        }
        addEnergyConsumption(calculateEnergyConsumptions(timeInListeningMode,mode));
    }
    public int getNumberOfEds(){ return endDeviceList.size();}
}
