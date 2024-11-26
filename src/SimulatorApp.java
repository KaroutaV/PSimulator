
public class SimulatorApp {

    public static void main(String[] args){
//        CommunicationMode communicationMode = CommunicationMode.TDMA;
//        CommunicationMode communicationMode = CommunicationMode.UNICAST;
        CommunicationMode communicationMode = CommunicationMode.LBT;

        //cycleDuration == interPacketInterval
        Network set1 = new Network(12,"4/6" , 0.9876, 9,
                500, 10000, communicationMode, 10000);

        Network set2 = new Network(9,"4/5" ,7.03, 9,
                500, 10000, communicationMode, 10000);

        Network set3 = new Network(7,"4/5" ,21.87, 9,
                500, 10000, communicationMode, 10000);

        if(communicationMode == CommunicationMode.LBT){
            //LBT (Listen before talk)
            set1.runLBT();
        }else{
            // Broadcast or Unicast
            set1.runSimulation();
        }
    }
}
