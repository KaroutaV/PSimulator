import java.util.List;

public class ClusterHead extends Node{

    private List<EndDevice> endDeviceList;
    public ClusterHead(long clusterHeadID, LoraSettings loraSettings, List<EndDevice> endDeviceList){
        super(clusterHeadID,loraSettings);
        this.endDeviceList = endDeviceList;
    }
    public void receiveBroadcastCommand(){
        resetEnergyConsumption();
        calculateEnergyConsumptions(endDeviceList.size());
        for(EndDevice endDevice:endDeviceList){
            endDevice.receivePacket(endDeviceList.size());
        }

    }
    public void calculateEnergyConsumptions(int numberOfEds){
        //in listenning mode until it receives the command
        setMode(1);
        //int timeInListMode = loraSettings.calculateTimeOnAir(2,1);
        int timeInListMode = 17;
        addEnergyConsumption(calculateEnergyConsumptions(timeInListMode,super.mode));

        //in transmitting mode
        setMode(2);
        int trasmittingTime = timeInListMode;
        addEnergyConsumption(calculateEnergyConsumptions(trasmittingTime,super.mode));

        // in listenning mode until the end
        super.setMode(1);
        int startListeningMode = timeInListMode + trasmittingTime + wubArrivalTime;
        int endOfListeningMode = startListeningMode + (super.loraSettings.getTimeOnAir() + super.GUARDTIME) * numberOfEds;
        addEnergyConsumption(calculateEnergyConsumptions(endOfListeningMode-startListeningMode,mode));
    }

    public void receiveUnicastCommand(long endDeviceID) {
        calculateEnergyConsumptions(1);
        for(EndDevice endDevice : endDeviceList){
            if(endDevice.getId()==endDeviceID){
                System.out.println("Cluster head sends a wake up beacon to end device with id " + endDeviceID);
                endDevice.receiveUnicastBeacon();
            }
        }
    }
}
