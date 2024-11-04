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
    private double tSymbol ;
    private double cadTime;
    private int tPreamble;


    public LoraSettings(int bandwidth, int transmissionPower, int preambleLength, int carrierFrequency, int payload) {
        this.bandwidth = bandwidth;
        this.transmissionPower = transmissionPower;
        this.preambleLength = preambleLength;
        this.carrierFrequency = carrierFrequency;
        this.payload = payload;
        this.packetHeader = 0;
    }
    public int codingRateToNumeric(){
        return switch (codingRate) {
            case "4/5" -> 1;
            case "4/6" -> 2;
            default -> throw new IllegalArgumentException("Invalid coding rate: " + codingRate);
        };
    }
    public int getTimeOnAir(){
        if(spreadingFactor>=11){ // or only if sf==12 ???
            this.de =1;
        }else{
            this.de=0;
        }
        double symbolRate; //Rsym
        symbolRate = bandwidth * 1000/ Math.pow(2,spreadingFactor);
        double tPreamble = (preambleLength + 4.25) * 1 / symbolRate ;

        this.tPreamble = (int) Math.round(tPreamble * 1000);

        double temp = (double) (8 * payload - 4 * spreadingFactor + 28 + 16 * codingRateToNumeric() - 20 * packetHeader) / (4*(spreadingFactor-2*de));
        temp = Math.ceil(temp)* (codingRateToNumeric()+4);
        temp = Math.max(temp,0);
        double tPayload = (8 + temp) * 1 / symbolRate ;
        double timeOnAir = ((tPreamble + tPayload) * 1000);
        return (int) Math.round(timeOnAir);
    }

    public int calculateTimeOnAir(int payload, double dataRate){
        int prevPayload = this.payload;
        double prevDataRate = this.dataRate;
        this.payload = payload;
        this.dataRate = dataRate;
        int toa = getTimeOnAir();
        this.payload = prevPayload;
        this.dataRate = prevDataRate ;
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

    /* The CAD (Channel Activity Detection) feature is only useful to detect LoRa preamble symbols. As CAD is not able
    to detect all transmissions especially when the preamble has been already sent, this causes packet collisions at
    the receiver, dropping some packets, resulting in lower reliability
    //The CAD mode available on all of LoRa radios is primarily designed for energy-efficient preamble detection
    * */
    public int switchToCAD(){
        // Tsymbol is the duration that is the airtime of a single LoRa chirp, depending on the SF value.
        this.tSymbol = Math.pow(2,spreadingFactor) / bandwidth;

        /*The radio then switches to the CAD mode and performs a CAD operation, which
        lasts [Tsymbol + (32/BW)] milliseconds, during which the radio performs a receive
        operation correlation on the received samples  */
        this.cadTime = tSymbol + ((double) 32 / bandwidth);

        return (int) cadTime;
    }

    public int getTpreamble() {
        return tPreamble;
    }
}
