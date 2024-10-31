
public class SimulatorApp {

    public static void main(String[] args){
//        BeaconType beaconType = BeaconType.BROADCAST;
//        Network network = new Network(9,"4/5" ,0.9876, 9,
//                500, 10000, beaconType);

        BeaconType beaconType = BeaconType.UNICAST;
        Network network = new Network(9,"4/5" ,0.9876, 9,
                500, 10000, beaconType);


        network.runSimulation();
    }
}
