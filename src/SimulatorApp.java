
public class SimulatorApp {

    public static void main(String[] args){
        //BeaconType beaconType = BeaconType.BROADCAST;
        BeaconType beaconType = BeaconType.UNICAST;

        Network set1 = new Network(12,"4/6" , 0.9876, 9,
                500, 10000, beaconType);

        Network set2 = new Network(9,"4/5" ,7.03, 9,
                500, 10000, beaconType);

        Network set3 = new Network(7,"4/5" ,21.87, 9,
                500, 10000, beaconType);

        set3.runSimulation();

//        //Inter-Packet interval
        int ipi = 10000; //10000ms -> 10s
        //LBT (Listen before talk)
        //network.runLBT(ipi);
    }
}
