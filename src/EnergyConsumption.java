public interface EnergyConsumption {

    double sx1276ListeningMode = 50/1000.0; //mW to W
    double sx1276TransmittingMode = 250/1000.0; // mW to W
    double deepSleepMode = 1.83 * 1e-6 ; // μw to W
    double wakeUpReceiver = 284 * 1e-6 ; //μW to W
    double wakeUpTransmitter = 260/1000.0 ; //mW to W
    default double calculateEnergyConsumptions(long time, int mode){
        if (mode < 1 || mode > 5) {
            throw new IllegalArgumentException("Invalid mode. Must be between 1 and 5.");
        }

        double t = (double) time / 1000 ; // ms to s
        double energy = switch (mode) {
            case 1 -> t * sx1276ListeningMode;
            case 2 -> t * sx1276TransmittingMode;
            case 3 -> t * deepSleepMode;
            case 4 -> t * wakeUpReceiver;
            case 5 -> t * wakeUpTransmitter;
            default -> 0.0;
        };

        return energy * 1000 ;
    }
}
