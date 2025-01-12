public class LoraSettings {

    private int spreadingFactor;
    private String codingRate;
    private int bandwidth; //kHz
    private double dataRate; //kb/s
    private int transmissionPower; //dBm
    private int preambleLength ;
    private int carrierFrequency;
    private int payload;    //packetHeader is the packet header, zero when the header is enabled
                            // and one when no header is present, packetHeader = 0
    private int packetHeader;   //  is the data rate optimizer, one when enabled, zero otherwise
    private int de;
    private double tSymbol ;  //duration that is the airtime of a single LoRa chirp
    private double cadDuration;
    private int tPreamble;
    private int timeOnAir;


    public LoraSettings(int bandwidth, int transmissionPower, int preambleLength, int carrierFrequency, int payload) {
        this.bandwidth = bandwidth;
        this.transmissionPower = transmissionPower;
        this.preambleLength = preambleLength;
        this.carrierFrequency = carrierFrequency;
        this.payload = payload;
        this.packetHeader = 0;
    }

    /* coding rate can be in range from 1 to 4 */
    public int getErrorCorrectionBits(){
        return switch (codingRate) {
            case "4/5" -> 1;
            case "4/6" -> 2;
            case "4/7" -> 3;
            case "4/8" -> 4;
            default -> throw new IllegalArgumentException("Invalid coding rate: " + codingRate);
        };
    }
    public void calculateTimeOnAir(){
        if(spreadingFactor>=11){ // or only if sf==12 ???
            this.de =1;
        }else{
            this.de=0;
        }
        double symbolRate; //Rsym
        symbolRate = (bandwidth * 1000)/ Math.pow(2,spreadingFactor);
        double tPreamble = (preambleLength + 4.25) * 1 / symbolRate ;

        this.tPreamble = (int) Math.round(tPreamble * 1000);


        double temp = (double) (8 * payload - 4 * spreadingFactor + 28 + 16 * getErrorCorrectionBits() - 20 * packetHeader) / (4*(spreadingFactor-2*de));
        temp = Math.ceil(temp)* (getErrorCorrectionBits()+4);
        temp = Math.max(temp,0);
        double tPayload = (8 + temp) * 1 / symbolRate ;
        double timeOnAir = ((tPreamble + tPayload) * 1000);

        this.timeOnAir = (int) Math.round(timeOnAir);
//        System.out.println("time on air " + timeOnAir);
//        System.out.println("Preamble time " + this.tPreamble);
    }

    public int getTimeOnAir(){ return this.timeOnAir; }

    public int calculateTimeOnAir(int payload, double dataRate){
        int prevPayload = this.payload;
        double prevDataRate = this.dataRate;
        this.payload = payload;
        this.dataRate = dataRate;
        calculateTimeOnAir();
        int toa = this.timeOnAir;
        this.payload = prevPayload;
        this.dataRate = prevDataRate ;
        calculateTimeOnAir();
        return toa;
    }

    public void setSpreadingFactor(int spreadingFactor) {
        this.spreadingFactor = spreadingFactor;
    }

    public void setCodingRate(String codingRate) {
        this.codingRate = codingRate;
    }

    public void setDataRate(double dataRate) {
        this.dataRate = dataRate;
    }

    public void setPayload(int payload) {
        this.payload = payload;
    }

    /*
    Introduction to Channel Activity Detection (https://www.semtech.com/uploads/technology/LoRa/cad-ensuring-lora-packets.pdf)

    The CAD (Channel Activity Detection) feature is only useful to detect LoRa preamble symbols. As CAD is not able
    to detect all transmissions especially when the preamble has been already sent, this causes packet collisions at
    the receiver, dropping some packets, resulting in lower reliability

    //The CAD mode available on all of LoRa radios is primarily designed for energy-efficient preamble detection

    First, a node requires TCADwakeup_seconds to wake up into the reception phase. The TCADwakeup is defined as :
    TCADwakeup = 32 / BW
    After that, the node starts to open a receive window. The time consumption for this stage can be calculated by
    tSymbol = Math.pow(2,spreadingFactor) / BW

    Then, the node processes incoming data to verify if there is any presence of preamble. The processing time or
    TCADprocessing in seconds is
    TCADprocessing = Math.pow(2,spreadingFactor) * spreadingFactor / 1750 * Math.pow(10,3)

    * */
    public double cadDuration(){
        // Tsymbol is the duration that is the airtime of a single LoRa chirp, depending on the SF value.
        this.tSymbol = Math.pow(2,spreadingFactor) / bandwidth;
        /*The radio then switches to the CAD mode and performs a CAD operation, which
        lasts [Tsymbol + (32/BW)] milliseconds, during which the radio performs a receive
        operation correlation on the received samples  */
        this.cadDuration = tSymbol + ((double) 32 / bandwidth);

        double tCADprocessing = (Math.pow(2,spreadingFactor) * spreadingFactor) / (1750 * Math.pow(10,3));
        this.cadDuration += tCADprocessing;
//        System.out.println("CAD TIME : " + cadDuration);

        return cadDuration;
    }

    public int getTpreamble() {
        return tPreamble;
    }
    public int getSpreadingFactor(){ return spreadingFactor;}
}
