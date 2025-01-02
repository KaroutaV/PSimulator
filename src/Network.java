
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Network {

    private int spreadingFactor;
    private String codingRate;
    private double dataRate;
    private int numberOfEndDevices;
    private int totalPacketsRequired;
    private int cycleDuration;
    private CommunicationMode communicationMode;
    private EventList eventList;
    private int currentCycle;
    private int startOfCycle;
    private int endOfCycle;
    private int clock;
    private List<EndDevice> endDeviceList;
    private Sink sink ;
    private ClusterHead clusterHead;
    private int totalPackets;

    private int interPacketInterval;
    private Statistics statistics;

    public Network(int spreadingFactor, String codingRate, double dataRate, int numberOfEndDevices,
                   int totalPacketsRequired, int cycleDuration, CommunicationMode communicationMode, int interPacketInterval) {
        this.spreadingFactor = spreadingFactor;
        this.codingRate = codingRate;
        this.dataRate = dataRate;
        this.numberOfEndDevices = numberOfEndDevices;
        this.totalPacketsRequired = totalPacketsRequired;
        this.cycleDuration = cycleDuration;
        this.communicationMode = communicationMode;
        this.eventList = new EventList(numberOfEndDevices,totalPacketsRequired);

        this.clock = 0;
        this.currentCycle = 0;
        this.startOfCycle = 0;
        this.endOfCycle = cycleDuration;
        this.endDeviceList = new ArrayList<>();
        totalPackets = 0 ;

        this.statistics = new Statistics();

        this.interPacketInterval = interPacketInterval;
    }

    public void runSimulation(){

        Channel channel = new Channel(initializeLoraSettings().getTpreamble());
        //configure Network
        for(int i=0;i<numberOfEndDevices;i++){
            endDeviceList.add(new EndDevice((i + 1), initializeLoraSettings(),channel,clock));
        }

        //only with one ch
        this.clusterHead = new ClusterHead(1,initializeLoraSettings(),endDeviceList);
        this.sink = new Sink(1, initializeLoraSettings(), clusterHead);
        for(EndDevice endDevice:endDeviceList){
            endDevice.setSink(sink);
        }
        channel.setSink(sink);

        //creating an event list

        this.eventList.generatePacketsWithPoissonDistribution(cycleDuration, interPacketInterval);
//        this.eventList.generatePacketsWithPoissonDistribution(cycleDuration);
//        this.eventList.generatePacketsWithProbability(cycleDuration,70);
        this.eventList.addSequenceNumber();

        startProcess();
    }

    public void startProcess(){
        //the sink lets the end devices collect data in cycle 0 and starts the data collection process from
        // cycle 1
        this.clock = cycleDuration;

        List<Packet> listOfPackets = new ArrayList<>();
        distributePacketsIntoCycles(eventList,listOfPackets);

        displayOverallResults();
        System.out.println();
        System.out.println();
        reset();
    }

    /**
     * Distributes packets into appropriate cycles for processing and manages
     * the transition between cycles based on packet generation times.
     *
     * @param eventList The list of packets to be processed.
     * @param listOfPackets A temporary list to hold packets for the current cycle.
     */
    public void distributePacketsIntoCycles(EventList eventList, List<Packet> listOfPackets ) {
        EventList tempEventList = new EventList(numberOfEndDevices,totalPacketsRequired);
        while (!eventList.isEmpty()) {
            Packet packet = eventList.pullPacketFromEventList();
            tempEventList.addPacketToEventList(packet);

            // Check if the packet belongs to the current cycle
            if (packet.getGeneratedTime() <= endOfCycle) {
                listOfPackets.add(packet);
            } else {
                // If the packet is outside the current cycle, process the current cycle
                processCycle(listOfPackets); // Process the completed cycle
                listOfPackets.clear(); // Reset the list for the new cycle

                endOfCycle = startOfCycle + cycleDuration; // Update end of cycle
                listOfPackets.add(packet); // Start new cycle with the current packet
            }

            // Final data collection if queue is empty
            if (eventList.isEmpty()) {
                processCycle(listOfPackets); // Collect data for current cycle
            }
        }
        eventList.copyEventList(tempEventList);
    }
    public void processCycle(List<Packet> packetList){
        // 1. Update the cycle parameters (cycle count, start and end times, clock)
        prepareCycle();
        System.out.println("----------------------- Cycle "+ currentCycle + " -------------------------------");
        System.out.println("Cycle " + currentCycle + " starts at " + startOfCycle + " ends at " + endOfCycle);

        // 2. Assign incoming packets to the appropriate end devices
        assignPacketsToEndDevices(packetList);

        // 3. Send wake-up beacons and handle communication based on beacon type
        sendBeacon();

        //update clock
        if(communicationMode == CommunicationMode.TDMA || communicationMode ==CommunicationMode.LBT) {
            this.clock = clock + sink.getClock();
        }

        // 4. Print the cycle's performance and energy statistics
        printCycleReport();
        statistics.addLatency(sink.getLatency());

        // 5. Reset state and energy metrics for the next cycle
        resetNodes();

    }
    public void  prepareCycle(){
        this.currentCycle ++;
        this.startOfCycle = endOfCycle;
        this.endOfCycle = startOfCycle + cycleDuration;
        this.clock = startOfCycle;
    }
    public void assignPacketsToEndDevices(List<Packet> packetList){
        for(Packet packet : packetList){
            for(EndDevice endDevice : endDeviceList){
                if(packet.getEndDeviceID() == endDevice.getId()){
                    endDevice.setPacketToSend(packet);
                    break;
                }
            }
        }
    }
    public void sendBeacon(){
        if (communicationMode == CommunicationMode.TDMA) {
            sink.sendWakeUpBeacon(communicationMode,0);
        } else if (communicationMode == CommunicationMode.UNICAST) {
            for(EndDevice ed : endDeviceList){
                sink.sendWakeUpBeacon(communicationMode,ed.getId());
                clock += sink.getClock();
                System.out.println("Simulation clock : " + clock);
            }
        }else if(communicationMode == CommunicationMode.LBT){
            for(EndDevice endDevice : endDeviceList){
                endDevice.randomBackoff(numberOfEndDevices);
            }
            sink.sendWakeUpBeacon(communicationMode,0);
        }
    }
    public void resetNodes(){
        // Reset energy consumption for all nodes in unicast mode
        if(communicationMode == CommunicationMode.UNICAST){
            sink.resetEnergyConsumption();
            clusterHead.resetEnergyConsumption();
            for(EndDevice ed : endDeviceList){
                ed.resetEnergyConsumption();
            }
        }
        for(EndDevice ed : endDeviceList){
            ed.setPacketToSend(null);
        }
        for(EndDevice endDevice : endDeviceList){
            endDevice.resetEndDevice();
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
    public void printCycleReport() {
        System.out.println("--------------------- CYCLE " + currentCycle + " RESULTS-------------------------------------");

        // Print the current clock time
        System.out.println("I received the packets at time " + clock);

        // Print the number of packets received in the current cycle
        int packetsReceivedInCycle = sink.getTotalPacketsReceived();
        System.out.println("Total packets received in cycle " + currentCycle + " are " + packetsReceivedInCycle);

        // Update the total packets count
        totalPackets += packetsReceivedInCycle;

        // Reset the sink's packet counter for the next cycle
        sink.setTotalPacketsReceived(0);

        DecimalFormat df = new DecimalFormat("#.##");

        // Record and print the energy consumption of the sink
        double sinkEnergyConsumption = sink.getEnergyConsumption();
        System.out.println("Sink Energy consumption = " + df.format(sinkEnergyConsumption) + " mJ.");
        statistics.addSinkEnergy(sinkEnergyConsumption);

        // Record and print the energy consumption of the cluster head
        double clusterHeadEnergyConsumption = clusterHead.getEnergyConsumption();
        System.out.println("Cluster Head consumption = " + df.format(clusterHeadEnergyConsumption) + " mJ.");
        statistics.addClusterHeadEnergy(clusterHeadEnergyConsumption);

        // Calculate, record, and print the total energy consumption of all end devices
        double totalEndDevicesEnergy = 0;
        double endDeviceEnergy = 0 ;
        for (EndDevice endDevice : endDeviceList) {
            if(communicationMode == CommunicationMode.LBT) {
                if (endDevice.getId() != 1) {
                    endDeviceEnergy = endDevice.getEnergyConsumption() + endDevice.calculateEnergy(endDevice.gettTransmit(), 2);
                    endDeviceEnergy += endDevice.calculateEnergy(endDevice.gettReceive(), 1);
                    totalEndDevicesEnergy += endDeviceEnergy;
                } else {
                    totalEndDevicesEnergy += endDevice.getEnergyConsumption();
                }
            }else{
                totalEndDevicesEnergy += endDevice.getEnergyConsumption();
            }
        }
        System.out.println("End devices energy consumption = " + df.format(totalEndDevicesEnergy) + " mJ.");
        statistics.addEndDeviceEnergy(totalEndDevicesEnergy);

        // Record and print the latency of the current cycle
        int cycleLatency = sink.getLatency();
        System.out.println("Cycle " + currentCycle + " latency: " + cycleLatency + " ms.");
        statistics.addLatency(cycleLatency);

        // Calculate, record, and print the average Radio Duty Cycle (RDC) for the current cycle
        double meanRDC = calculateRDC();
        System.out.println("End Device Radio Duty Cycle (RDC) %: " + df.format(meanRDC * 100));
        statistics.addRadioDutyCycle(meanRDC);

        // End of the cycle report
        System.out.println("----------------------------------------------------------");
    }


    public void displayOverallResults() {
        System.out.println();
        System.out.println();
        System.out.println("------------------------Results---------------------------");
        System.out.println("Protocol : " + communicationMode );
        System.out.println("Total cycles performed " + currentCycle);
        System.out.println("Total packets received " + totalPackets);
        System.out.println("Reliability = " + (double) totalPackets / totalPacketsRequired);
        this.totalPackets=0;

        DecimalFormat df = new DecimalFormat("#.##");
        System.out.println("Mean data latency: " + statistics.calculateMeanLatency() + " ms.");
        System.out.println("Mean Sink Energy consumption: " + df.format(statistics.calculateMeanSinkEnergy()) + " mJ.");
        System.out.println("Mean Cluster Head Energy consumption: " + df.format(statistics.calculateMeanClusterHeadEnergy()) + " mJ.");
        System.out.println("Mean End Device Energy consumption: " + df.format(statistics.calculateMeanEndDeviceEnergy())+ " mJ.");
        System.out.println("Mean End Device Radio Duty Cycle (%): " + df.format(statistics.calculateMeanRadioDutyCycle() * 100));
        System.out.println("----------------------------------------------------------");
    }

    private double calculateRDC() {
        double rdc = 0;
        for (EndDevice endDevice : endDeviceList) {
            rdc += (communicationMode == CommunicationMode.TDMA || communicationMode == CommunicationMode.UNICAST) ?
                    (double) (endDevice.gettReceive() + endDevice.gettTransmit()) / interPacketInterval :
                    (17.0 + endDevice.gettReceive() + endDevice.gettTransmit()) / interPacketInterval;
        }
        return (rdc / endDeviceList.size()) ;
    }

    public void changeCommunicationMode(CommunicationMode communicationMode){
        this.communicationMode = communicationMode;
    }

    public void reset(){
        this.currentCycle = 0;
        this.startOfCycle=0;
        this.endOfCycle = startOfCycle + cycleDuration;
        for(EndDevice endDevice : endDeviceList){
            endDevice.setClock(0);
            endDevice.setLatency(0);
            endDevice.setLostPackets(0);
            endDevice.resetEnergyConsumption();
        }
        clusterHead.resetEnergyConsumption();
        clusterHead.setClock(0);
        sink.resetEnergyConsumption();
        sink.setLatency(0);
        sink.setClock(0);
        this.totalPackets = 0;
        statistics.resetStatistis();
        this.sink.setTotalPacketsReceived(0);
    }
}
