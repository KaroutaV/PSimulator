import java.util.ArrayList;
import java.util.List;

public class Network {

    private int spreadingFactor;
    private String codingRate;
    private double dataRate;
    private int numberOfEndDevices;
    private int totalPacketsRequired;
    private int cycleDuration;
    private BeaconType beaconType;
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

    public Network(int spreadingFactor, String codingRate, double dataRate, int numberOfEndDevices,
                   int totalPacketsRequired, int cycleDuration, BeaconType beaconType) {
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
        this.eventList.generatePacketsWithPoissonDistribution(cycleDuration);
        this.eventList.addSequenceNumber();

        //the sink lets the end devices collect data in cycle 0 and starts the data collection process from cycle 1
        this.clock = cycleDuration;

        List<Packet> packets = new ArrayList<>();

        /* checks the packets. Ιf the time the packet was created is
        less than the end of the cycle then puts the packets in another list to be sent
        from the eds to the sink otherwise it starts the process of collecting them from the sink
        and calculates the new cycle */
        //takes the first packet from the queue
        Packet packet = eventList.pullPacketFromEventList();
        List<Packet> listOfPackets = new ArrayList<>();
        while(!eventList.isEmpty()){
            if (packet.getGeneratedTime()<=endOfCycle){
                //eventList1.addPacketToEventList(packet);
                listOfPackets.add(packet);
                packet = eventList.pullPacketFromEventList();
                if(eventList.isEmpty()){
                    if(packet.getGeneratedTime()<=endOfCycle) {
                        //eventList1.addPacketToEventList(packet);
                        listOfPackets.add(packet);
                        dataCollection(listOfPackets);
                        listOfPackets.clear();
                    }else{
                        /* it enters this loop when the last packet it gets out of the queue
                         does not belong to the current cycle but to the next one.
                         Τhen activate the collection for the previous cycle and then
                         calculate the new cycle and perform the last data collection */

                        dataCollection(listOfPackets);
                        listOfPackets.clear();

                        //eventList1.addPacketToEventList(packet);
                        listOfPackets.add(packet);

                        dataCollection(listOfPackets);
                        listOfPackets.clear();
                    }
                }
            }else{
                dataCollection(listOfPackets);
                listOfPackets.clear();
            }
        }

        results();

    }
    public void dataCollection(List<Packet> packetList){
        currentCycle ++;
        startOfCycle = endOfCycle;
        endOfCycle = startOfCycle + cycleDuration;
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

        if (beaconType == BeaconType.BROADCAST) {
            //broadcast
            sink.sendBroadcastCommand(numberOfEndDevices);
            // After this command, the sink will have received all the packets in the cycle.
        } else if (beaconType == BeaconType.UNICAST) {
            //unicast
            sink.setLatency(0);
            for(EndDevice ed : endDeviceList){
                sink.sendUnicastCommand(ed.getId());
                this.clock = clock + sink.getClock();
                sink.addLatency(sink.clock);
            }
        }

        if(beaconType == BeaconType.BROADCAST) {
            this.clock = clock + sink.getClock();
        }

        printResults();

        if(beaconType==BeaconType.UNICAST){
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
        return loraSettings;
    }
    public void printResults(){
        System.out.println("---------------------Cycle" + currentCycle + "-------------------------------------");
        System.out.println("I received the packets at time " + clock);
        System.out.println("total packets received in cycle " + currentCycle + " are " + sink.getTotalPacketsReceived());
        totalPackets = totalPackets + sink.getTotalPacketsReceived();
        sink.setTotalPacketsReceived(0);
        System.out.println("Sink Energy consumptions = " + sink.getEnergyConsumption() + " mJ.");
        System.out.println("cluster head consumptions " + clusterHead.getEnergyConsumption() + " mJ.");
        double totalEnergyEds = 0;
        for(EndDevice endDevice: endDeviceList){
            totalEnergyEds = totalEnergyEds + endDevice.getEnergyConsumption();
        }
        System.out.println("End devices energy consumptions " + totalEnergyEds);
        System.out.println("cycle " + currentCycle + " latency : " + sink.getLatency() + " ms.");
        System.out.println("----------------------------------------------------------");
    }

    public void results(){
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
        System.out.println("----------------------------------------------------------");
    }

    public void runLBT(int ipi) {
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

        eventList.generatePacketsWithFixedInterval(totalPacketsRequired,numberOfEndDevices,cycleDuration,ipi);
        eventList.addSequenceNumber();


        List<Packet> packets = new ArrayList<>();

        /* checks the packets. Ιf the time the packet was created is
        less than the end of the cycle then puts the packets in another list to be sent
        from the eds to the sink otherwise it starts the process of collecting them from the sink
        and calculates the new cycle */
        //takes the first packet from the queue
        Packet packet = eventList.pullPacketFromEventList();
        List<Packet> listOfPackets = new ArrayList<>();
        while(!eventList.isEmpty()){
            if (packet.getGeneratedTime()<=endOfCycle){
                listOfPackets.add(packet);
                packet = eventList.pullPacketFromEventList();
                if(eventList.isEmpty()){
                    if(packet.getGeneratedTime()<=endOfCycle) {
                        listOfPackets.add(packet);
                        lbtSimulation(listOfPackets);

                        listOfPackets.clear();
                    }else{
                        /* it enters this loop when the last packet it gets out of the queue
                         does not belong to the current cycle but to the next one.
                         Τhen activate the collection for the previous cycle and then
                         calculate the new cycle and perform the last data collection */

                        lbtSimulation(listOfPackets);

                        listOfPackets.clear();

                        listOfPackets.add(packet);

                        lbtSimulation(listOfPackets);

                        listOfPackets.clear();
                    }
                }
            }else{
                lbtSimulation(listOfPackets);
                listOfPackets.clear();
            }
        }
        System.out.println(" total packets " + totalPacketsRequired);
        int totalPacketsLost= 0 ;
        for(EndDevice ed:endDeviceList){
            totalPacketsLost += ed.getLostPackets();
        }
        System.out.println("total packets lost " + totalPacketsLost);


    }
    public void lbtSimulation(List<Packet> packetList){
        currentCycle ++;
        startOfCycle = endOfCycle;
        endOfCycle = startOfCycle + cycleDuration;
        clock = startOfCycle;
        System.out.println("----------------------- Cycle "+ currentCycle + " -------------------------------");
        System.out.println("Cycle " + currentCycle + " starts at " + startOfCycle + " ends at " + endOfCycle);

        for(Packet packet : packetList){
            for(EndDevice endDevice : endDeviceList){
                if(packet.getEndDeviceID() == endDevice.getId()){
                    endDevice.setPacketToSend(packet);
                    endDevice.setClock(clock);
                    break;
                }
            }
        }

        Thread[] nodes = new Thread[numberOfEndDevices];

        for (int i = 0; i < numberOfEndDevices; i++) {
            nodes[i] = new Thread(endDeviceList.get(i)); // Δημιουργία νήματος για τον κόμβο
            nodes[i].start(); // Εκκίνηση νήματος κόμβου
        }

        //περιμένουμε όλα τα νήματα να τελειώσουν
        for (Thread node : nodes) {
            try {
                node.join(); // Περιμένουμε το τέλος του νήματος
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }



    }
}
