public abstract class Node implements EnergyConsumption{
    private long id;
    protected int clock; //the time the system runs
    protected int mode; //current state
    protected double energyConsumption;
    protected LoraSettings loraSettings;
    protected final int GUARDTIME = 6;
    protected int wubArrivalTime = 17; // ms

    //modes:
    protected final int LISTENING_MODE = 1;
    protected final int TRANSMITTING_MODE = 2;
    protected final int SLEEP_MODE = 3;
    protected final int WAKE_UP_RECEIVER = 4;
    protected final int WAKE_UP_TRANSMITTER = 5;

    public Node(long id, LoraSettings loraSettings) {
        this.id = id;
        this.loraSettings = loraSettings;
        this.mode = SLEEP_MODE; // initial mode
        this.clock = 0;
        this.energyConsumption = 0;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public int getClock() {
        return clock;
    }

    public void setClock(int clock) {
        this.clock = clock;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public double getEnergyConsumption() {
        return energyConsumption;
    }

    public void setEnergyConsumption(double energyConsumption) {
        this.energyConsumption = energyConsumption;
    }

    public void addEnergyConsumption(double amount) {
        this.energyConsumption += amount;
    }

    //resetting energy after the end of the cycle
    public void resetEnergyConsumption() {
        this.energyConsumption = 0;
    }
    public void addClock(int amount){this.clock = clock + amount ; }

    public int getTimeOnAir(){
        return loraSettings.getTimeOnAir();
    }

}
