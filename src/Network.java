
import java.util.ArrayList;
import java.util.List;

public class Network {

    private int spreadingFactor;
    private String codingRate;
    private double dataRate;
    private int numberOfEndDevices;
    private int totalPacketsRequired;
    private int cycleDuration;
    private CommunicationMode beaconType;
    private EventList eventList;
    private int currentCycle;
    private int startOfCycle;
    private int endOfCycle;
    private int clock;
    private Packet tempPacket;
    private List<EndDevice> endDeviceList;
    private Sink sink ;
    private ClusterHead clusterHead;
    private List<Integer> latenciesList;
    private int totalPackets;

    private List<Double> sinkEnergy ;
    private List<Double> clusterHeadEnergy ;
    private List<Double> endDeviceEnergy;
    private List<Double> radioDutyCycle ;
    private int interPacketInterval;

    public Network(int spreadingFactor, String codingRate, double dataRate, int numberOfEndDevices,
                   int totalPacketsRequired, int cycleDuration, CommunicationMode beaconType, int interPacketInterval) {
        this.spreadingFactor = spreadingFactor;
        this.codingRate = codingRate;
        this.dataRate = dataRate;
        this.numberOfEndDevices = numberOfEndDevices;
        this.totalPacketsRequired = totalPacketsRequired;
        this.cycleDuration = cycleDuration;
        this.beaconType = beaconType;
        this.eventList = new EventList(numberOfEndDevices,totalPacketsRequired);
        this.latenciesList = new ArrayList<>();

        this.clock = 0;
        this.currentCycle = 0;
        this.startOfCycle = 0;
        this.endOfCycle = cycleDuration;
        this.endDeviceList = new ArrayList<>();
        totalPackets = 0 ;

        this.sinkEnergy = new ArrayList<>();
        this.clusterHeadEnergy = new ArrayList<>();
        this.endDeviceEnergy = new ArrayList<>();
        this.radioDutyCycle = new ArrayList<>();

        this.interPacketInterval = interPacketInterval;
    }

    public void runSimulation(){

        //configure Network
        for(int i=0;i<numberOfEndDevices;i++){
            endDeviceList.add(new EndDevice((i + 1), initializeLoraSettings()));
        }
        //only with one ch
        this.clusterHead = new ClusterHead(1,initializeLoraSettings(),endDeviceList);
        this.sink = new Sink(1, initializeLoraSettings(), clusterHead);
        for(EndDevice endDevice:endDeviceList){
            endDevice.setSink(sink);
        }

        //creating an event list
        //this.eventList.generatePacketsWithPoissonDistribution(cycleDuration);
//        this.eventList.generatePacketsWithFixedInterval(totalPacketsRequired,numberOfEndDevices,cycleDuration,interPacketInterval);
        eventList.generatePacketsForAllNodesUntilLimit(cycleDuration);

        this.eventList.addSequenceNumber();

        //the sink lets the end devices collect data in cycle 0 and starts the data collection process from
        // cycle 1
        this.clock = cycleDuration;

        List<Packet> listOfPackets = new ArrayList<>();
        processPackets(eventList,listOfPackets);

        displayOverallResults();

    }

    /* checks the packets. Ιf the time the packet was created is
        less than the end of the cycle then puts the packets in another list to be sent
        from the eds to the sink otherwise it starts the process of collecting them from the sink
        and calculates the new cycle
    */
    public void processPackets(EventList eventList, List<Packet> listOfPackets ) {
        while (!eventList.isEmpty()) {
            Packet packet = eventList.pullPacketFromEventList();

            if (packet.getGeneratedTime() <= endOfCycle) {
                listOfPackets.add(packet);
            } else {
                // Current packet is outside the current cycle
                if(beaconType == CommunicationMode.LBT){
                    lbtSimulation(listOfPackets);
                }else{
                    dataCollection(listOfPackets); // Collect data for current cycle
                }
                listOfPackets.clear(); // Reset for new cycle

                endOfCycle = calculateNewCycleEnd(); // Update end of cycle
                listOfPackets.add(packet); // Start new cycle with the current packet
            }

            // Final data collection if queue is empty
            if (eventList.isEmpty()) {
                if(beaconType == CommunicationMode.LBT){
                    lbtSimulation(listOfPackets);
                }else{
                    dataCollection(listOfPackets); // Collect data for current cycle
                }
            }
        }
    }
    public int calculateNewCycleEnd(){
        return this.endOfCycle = startOfCycle + cycleDuration;
    }
    public void dataCollection(List<Packet> packetList){
        currentCycle ++;
        startOfCycle = endOfCycle;
        endOfCycle = calculateNewCycleEnd();
        clock = endOfCycle;
        System.out.println("----------------------- Cycle "+ currentCycle + " -------------------------------");
        System.out.println("Cycle " + currentCycle + " starts at " + startOfCycle + " ends at " + endOfCycle);

        for(Packet packet : packetList){
            for(EndDevice endDevice : endDeviceList){
                if(packet.getEndDeviceID() == endDevice.getId()){
                    endDevice.setPacketToSend(packet);
                    break;
                }
            }
        }

        if (beaconType == CommunicationMode.TDMA) {
            //broadcast
//            sink.sendBroadcastCommand(numberOfEndDevices);
            sink.sendWakeUpBeacon(beaconType,0);
            // After this command, the sink will have received all the packets in the cycle.
        } else if (beaconType == CommunicationMode.UNICAST) {
            //unicast
            //sink.setLatency(0);
            for(EndDevice ed : endDeviceList){
                //sink.sendUnicastCommand(ed.getId());
                sink.sendWakeUpBeacon(beaconType,ed.getId());
                this.clock = clock + sink.getClock();
                sink.addLatency(sink.clock);
            }
        }

        if(beaconType == CommunicationMode.TDMA) {
            this.clock = clock + sink.getClock();
        }

        printCycleReport();

        if(beaconType== CommunicationMode.UNICAST){
            sink.resetEnergyConsumption();
            clusterHead.resetEnergyConsumption();
            for(EndDevice ed : endDeviceList){
                ed.resetEnergyConsumption();
            }
        }

        latenciesList.add(sink.getLatency());
        for(EndDevice ed : endDeviceList){
            ed.setPacketToSend(null);
        }
    }
    public LoraSettings initializeLoraSettings(){
        LoraSettings loraSettings = new LoraSettings(500,10, 8, 868, 8) ;
        loraSettings.setSpreadingFactor(spreadingFactor);
        loraSettings.setCodingRate(codingRate);
        loraSettings.setDataRate(dataRate);
        loraSettings.calculateTimeOnAir();
        return loraSettings;
    }
    public void printCycleReport(){
        System.out.println("---------------------Cycle" + currentCycle + "-------------------------------------");
        System.out.println("I received the packets at time " + clock);
        System.out.println("total packets received in cycle " + currentCycle + " are " + sink.getTotalPacketsReceived());
        totalPackets = totalPackets + sink.getTotalPacketsReceived();
        sink.setTotalPacketsReceived(0);
        System.out.println("Sink Energy consumptions = " + sink.getEnergyConsumption() + " mJ.");
        sinkEnergy.add(sink.getEnergyConsumption());
        System.out.println("Cluster Head consumptions " + clusterHead.getEnergyConsumption() + " mJ.");
        clusterHeadEnergy.add(clusterHead.getEnergyConsumption());
        double totalEnergyEds = 0;
        for(EndDevice endDevice: endDeviceList){
            totalEnergyEds = totalEnergyEds + endDevice.getEnergyConsumption();
        }
        endDeviceEnergy.add(totalEnergyEds);
        System.out.println("End devices energy consumptions " + totalEnergyEds);
        System.out.println("cycle " + currentCycle + " latency : " + sink.getLatency() + " ms.");
        double meanRDC = 0;
        for(EndDevice endDevice : endDeviceList){
            meanRDC += (double) (endDevice.gettReceive() + endDevice.gettTransmit()) / interPacketInterval ;
        }
        meanRDC = meanRDC/endDeviceList.size();
        System.out.println("End Device Radio Duty Cycle (RDC) %" + meanRDC*100);
        radioDutyCycle.add(meanRDC);
        System.out.println("----------------------------------------------------------");
    }

    public void displayOverallResults(){
        System.out.println();
        System.out.println();
        System.out.println("------------------------Results---------------------------");
        System.out.println("Total cycles performed " + currentCycle);
        System.out.println("Total packet received " + totalPackets);
        System.out.println("Reliability = " + totalPackets/totalPacketsRequired);

        Integer sum = 0;
        for(Integer latency : latenciesList){
            sum = sum + latency;
        }
        System.out.println("Mean data latency : " + (sum / currentCycle) + " ms.");

        double meanSinkEnergyCons = 0 ;
        double meanClusterHeadEnergyCons = 0 ;
        double meanEndDeviceEnergyCons = 0 ;
        for(int i=0; i< sinkEnergy.size() ; i++){
            meanSinkEnergyCons += sinkEnergy.get(i);
            meanClusterHeadEnergyCons += clusterHeadEnergy.get(i);
            meanEndDeviceEnergyCons += endDeviceEnergy.get(i);
        }
        meanSinkEnergyCons = meanSinkEnergyCons / sinkEnergy.size();
        meanClusterHeadEnergyCons = meanClusterHeadEnergyCons / clusterHeadEnergy.size();
        meanEndDeviceEnergyCons = meanEndDeviceEnergyCons / endDeviceEnergy.size();

        System.out.println("Sink Energy consumptions = " + meanSinkEnergyCons + " mJ.");
        System.out.println("Cluster Head Energy consumptions = " + meanClusterHeadEnergyCons + " mJ.");
        System.out.println("End Device Energy consumptions = " + meanEndDeviceEnergyCons + " mJ.");

        double totalRDC = 0;
        for(Double meanRDC : radioDutyCycle ){
            totalRDC += meanRDC;
        }
        totalRDC = totalRDC / radioDutyCycle.size();
        System.out.println("End Device Mean Radio Duty Cycle (%) " + totalRDC*100);

        System.out.println("----------------------------------------------------------");
    }

    public void runLBT() {
        //configure Network
        Channel channel = new Channel(initializeLoraSettings().getTpreamble());

        for(int i=0;i<numberOfEndDevices;i++){
            endDeviceList.add(new EndDevice((i + 1), initializeLoraSettings(),channel,clock));
        }
        this.clusterHead = new ClusterHead(1,initializeLoraSettings(),endDeviceList);
        this.sink = new Sink(1, initializeLoraSettings(), clusterHead);
        for(EndDevice endDevice:endDeviceList){
            endDevice.setSink(sink);
        }

        //this.eventList.generatePacketsWithPoissonDistribution(cycleDuration);
//        eventList.generatePacketsWithFixedInterval(totalPacketsRequired,numberOfEndDevices,cycleDuration,interPacketInterval);
        eventList.generatePacketsForAllNodesUntilLimit(cycleDuration);
        eventList.addSequenceNumber();

        List<Packet> listOfPackets = new ArrayList<>();
        processPackets(eventList,listOfPackets);
        resultsLBT();
    }
    public void resultsLBT(){
        double reliability = (double) (totalPacketsRequired - Channel.lostPackets) / totalPacketsRequired;
        System.out.println(" Reliability = " + reliability);
        double meanDataLatency = 0.0;
        for(Integer latency : latenciesList){
            meanDataLatency += latency;
        }
        meanDataLatency = meanDataLatency / latenciesList.size();
        System.out.println("Mean Data Latency (ms) " + meanDataLatency);
        double totalRDC = 0;
        for(Double meanRDC : radioDutyCycle ){
            totalRDC += meanRDC;
        }
        totalRDC = totalRDC / radioDutyCycle.size();
        System.out.println("End Device Mean Radio Duty Cycle (%) " + totalRDC*100);

    }
    public void lbtSimulation(List<Packet> packetList){

        System.out.println("----------------------- Cycle "+ currentCycle + " -------------------------------");

        endOfCycle = startOfCycle + cycleDuration;
        clock = startOfCycle;

        System.out.println("Cycle " + currentCycle + " starts at " + startOfCycle + " ends at " + endOfCycle);

        for(EndDevice endDevice : endDeviceList){
            endDevice.setClock(clock);
        }

        for(Packet packet : packetList){
            for(EndDevice endDevice : endDeviceList){
                if(packet.getEndDeviceID() == endDevice.getId()){
                    endDevice.setPacketToSend(packet);
                    break;
                }
            }
        }

        sink.sendWakeUpBeacon(beaconType,0);

        Channel channel = endDeviceList.get(0).getChannel();
        channel.startSimulation();

        int latency = 0 ;
        for(EndDevice endDevice: endDeviceList){
            latency += endDevice.getLatency();
        }

        //estw 500 ms mexri na steilei o sink mnm na ksekinhsei h syllogi
        latency += 430;
//        latency += 16 ;
        latenciesList.add(latency);

        List<Double> rdcList = new ArrayList<>();
        double rdc = 0 ;
        for(EndDevice endDevice: endDeviceList){
            rdc += (17.0 + endDevice.gettReceive()+ endDevice.gettTransmit())/interPacketInterval;
        }
        rdc = rdc / endDeviceList.size();
        rdcList.add(rdc);

        System.out.println();
        System.out.println("---------------------------------------------------");
        System.out.println("cycle " + currentCycle + " latency " + latency);
        double meanRDC = 0 ;
        for(Double rdc1 : rdcList){
            meanRDC += rdc1 ;
        }
        meanRDC = meanRDC / rdcList.size();
        radioDutyCycle.add(meanRDC);
        System.out.println("mean radio duty cycle " + meanRDC);
        System.out.println("---------------------------------------------------");

        for(EndDevice endDevice : endDeviceList){
            endDevice.setPacketToSend(null);
            endDevice.setLatency(0);
            endDevice.settReceive(0);
            endDevice.settTransmit(0);
        }
        currentCycle ++;
        startOfCycle = endOfCycle;
    }
}
