
public class SimulatorApp {

    public static void main(String[] args){
        BeaconType beaconType = BeaconType.BROADCAST;
        Network network = new Network(12,"4/6" ,21.87, 9,
                10, 10000, beaconType);

//        BeaconType beaconType = BeaconType.UNICAST;
//        Network network = new Network(12,"4/6" ,0.9876, 9,
//                500, 10000, beaconType);

        //network.runSimulation();


        //Inter-Packet interval
        int ipi = 10000; //10000ms -> 10s
        //LBT (Listen before talk)
        network.runLBT(ipi);
    }
}
