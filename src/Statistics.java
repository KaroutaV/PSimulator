import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;

public class Statistics {
    private List<Double> sinkEnergy;
    private List<Double> clusterHeadEnergy;
    private List<Double> endDeviceEnergy;
    private List<Double> radioDutyCycle;
    private List<Integer> latencies;

    public Statistics() {
        this.sinkEnergy = new ArrayList<>();
        this.clusterHeadEnergy = new ArrayList<>();
        this.endDeviceEnergy = new ArrayList<>();
        this.radioDutyCycle = new ArrayList<>();
        this.latencies = new ArrayList<>();
    }

    public void resetStatistis(){
        sinkEnergy.clear();
        clusterHeadEnergy.clear();
        endDeviceEnergy.clear();
        radioDutyCycle.clear();
        latencies.clear();
    }

    // Methods to add data
    public void addSinkEnergy(double energy) {
        sinkEnergy.add(energy);
    }

    public void addClusterHeadEnergy(double energy) {
        clusterHeadEnergy.add(energy);
    }

    public void addEndDeviceEnergy(double energy) {
        endDeviceEnergy.add(energy);
    }

    public void addRadioDutyCycle(double rdc) {
        radioDutyCycle.add(rdc);
    }

    public void addLatency(int latency) {
        latencies.add(latency);
    }

    // Methods to calculate averages
    public double calculateMeanSinkEnergy() {
        return sinkEnergy.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public double calculateMeanClusterHeadEnergy() {
        return clusterHeadEnergy.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public double calculateMeanEndDeviceEnergy() {
        return endDeviceEnergy.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public double calculateMeanRadioDutyCycle() {
        return radioDutyCycle.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public double calculateMeanLatency() {
        return Math.round(latencies.stream().mapToInt(Integer::intValue).average().orElse(0.0));
    }

}

