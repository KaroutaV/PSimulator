
public class SimulatorApp {

    public static void main(String[] args){
        CommunicationMode communicationMode1 = CommunicationMode.TDMA;
        CommunicationMode communicationMode2 = CommunicationMode.UNICAST;
        CommunicationMode communicationMode3 = CommunicationMode.LBT;

        //cycleDuration == interPacketInterval
        Network set1 = new Network(12,"4/6" , 0.9876, 9,
                500, 10000, communicationMode3, 10000);

        Network set2 = new Network(9,"4/5" ,7.03, 9,
                500, 10000, communicationMode1, 10000);

        Network set3 = new Network(7,"4/5" ,21.87, 9,
                500, 10000, communicationMode1, 10000);

        set1.runSimulation();
//        set1.changeCommunicationMode(communicationMode2);
//        set1.startProcess();
//        set1.changeCommunicationMode(communicationMode3);
//        set1.startProcess();

//        set2.runSimulation();
//        set2.changeCommunicationMode(communicationMode2);
//        set2.startProcess();
//        set2.changeCommunicationMode(communicationMode3);
//        set2.startProcess();

//        set3.runSimulation();
//        set3.changeCommunicationMode(communicationMode2);
//        set3.startProcess();
//        set3.changeCommunicationMode(communicationMode3);
//        set3.startProcess();
    }
}
