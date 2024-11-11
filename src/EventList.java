import java.util.*;

public class EventList {

    private Queue<Packet> eventQueue;
    private int totalPackets;
    private int numberOfNodes;
    private Random random;

    public EventList(int numberOfNodes, int totalPackets) {
        this.numberOfNodes = numberOfNodes;
        this.totalPackets = totalPackets;
        this.eventQueue = new PriorityQueue<>((p1, p2) -> Double.compare(p1.getGeneratedTime(), p2.getGeneratedTime()));
        this.random = new Random();
    }

    /**
     * Generates data packets in a network simulation and adds them to the event queue.
     * The packets are generated in cycles, where each cycle has a specified duration.
     * The number of packets in each cycle follows a Poisson distribution based on
     * a third of the total available nodes.
     *
     * For each cycle:
     * - A random subset of nodes is selected to send packets.
     * - Each selected node sends one packet at a random time within the cycle duration.
     * - The packets are stored in the event queue for processing.
     *
     * The process continues until the total number of packets reaches {@code totalPackets}.
     */
    public void generatePacketsWithPoissonDistribution(int cycleDuration) {
        int packetCount = 0;
        long cycleStartTime = 0; // Start time of the cycle

        // Continue generating packets until reaching the required total count
        while (packetCount < totalPackets) {
            // Determine the number of packets for this cycle using a Poisson distribution
            int packetsPerCycle = (numberOfNodes > 1) ? poissonDistribution(numberOfNodes / 3) : 1; // Use Poisson distribution or fixed to 1 if one node

            List<Integer> nodePerCycleList = new ArrayList<>();

            // Generate packets from random unique nodes
            for (int i = 0; i < packetsPerCycle && packetCount < totalPackets; i++) {
                int nodeId;
                boolean unique;
                do {
                    unique = true; // Assume node is unique
                    nodeId = random.nextInt(numberOfNodes) + 1; // Select a random node

                    // Check if the node has already been selected in this cycle
                    for (Integer node : nodePerCycleList) {
                        if (nodeId == node) {
                            unique = false; // node is not unique
                            break;
                        }
                    }
                } while (!unique);
                nodePerCycleList.add(nodeId);
                // Create a packet with a random time within the cycle
                Packet packet = new Packet(nodeId, cycleStartTime + random.nextInt(cycleDuration));
                eventQueue.add(packet);
                packetCount++;
            }

            // Move to the next cycle
            cycleStartTime += cycleDuration;
        }
    }

    private int poissonDistribution(double lambda) {
        if (lambda <= 0) {
            return 0; // Επιστροφή 0 όταν το lambda είναι 0 ή αρνητικό
        }
        double l = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;

        do {
            k++;
            p *= random.nextDouble();
        } while (p > l);

        return k - 1;
    }


    public void generatePacketsForAllNodesUntilLimit(int cycleDuration) {
        int packetCount = 0;
        long cycleStartTime = 0; // Start time of the cycle

        // Continue generating packets until the total count reaches totalPackets
        while (packetCount < totalPackets) {
            List<Integer> nodePerCycleList = new ArrayList<>();

            // Iterate over each node in the network
            for (int nodeId = 1; nodeId <= numberOfNodes && packetCount < totalPackets; nodeId++) {
                // Add the current node to the list
                nodePerCycleList.add(nodeId);

                // Create a packet with a random time within the cycle for the current node
                Packet packet = new Packet(nodeId, cycleStartTime + random.nextInt(cycleDuration));
                eventQueue.add(packet);
                packetCount++;
            }

            // Move to the next cycle start time
            cycleStartTime += cycleDuration;
        }
    }

    public void generatePacketsWithFixedInterval(int totalPackets, int numberOfNodes, int cycleDuration, int interPacketInterval) {
        int packetCount = 0;
        long currentTime = 0; // Τρέχων χρόνος της προσομοίωσης
        Map<Integer, Long> lastPacketTime = new HashMap<>(); // Χρόνος τελευταίου πακέτου για κάθε κόμβο

        // Αρχικοποίηση τελευταίου χρόνου δημιουργίας για κάθε κόμβο
        for (int i = 1; i <= numberOfNodes; i++) {
            lastPacketTime.put(i, 0L); // Αρχικά, δεν έχει δημιουργηθεί κανένα πακέτο
        }


        // Δημιουργία τυχαίων πακέτων από τους κόμβους
        for (int nodeId = 1; nodeId <= numberOfNodes && packetCount < totalPackets; nodeId++) {
            // Δημιουργία τυχαίου χρόνου εντός του τρέχοντος κύκλου
            long randomTime = currentTime + (long) (Math.random() * 500 /* cycleDuration * 2*/ );
            Packet packet = new Packet(nodeId, randomTime);
            lastPacketTime.put(nodeId,randomTime);
            eventQueue.add(packet);
            packetCount++;
        }

        // Προχωράμε στον χρόνο του επόμενου κύκλου
        currentTime += cycleDuration;


        // Συνεχίζουμε την παραγωγή πακέτων με βάση το IPI
        while (packetCount < totalPackets) {
            for (int nodeId = 1; nodeId <= numberOfNodes && packetCount < totalPackets; nodeId++) {
                long lastTime = lastPacketTime.get(nodeId);
                currentTime = lastTime + interPacketInterval;
                // Δημιουργία πακέτου
                Packet packet = new Packet(nodeId, currentTime);
                eventQueue.add(packet);
                packetCount++;
                // Ενημέρωση του χρόνου τελευταίου πακέτου
                lastPacketTime.put(nodeId, currentTime);
            }

            // Αυξάνουμε τον τρέχοντα χρόνο (μπορείς να προσαρμόσεις αυτή τη λογική)
            currentTime += cycleDuration; // ή μπορείς να χρησιμοποιήσεις μικρότερο βήμα, π.χ. 100 ms
        }
    }




    // print all the packets from the event list
    public void printEventList() {
        while (!eventQueue.isEmpty()) {
            Packet packet = eventQueue.poll();
            System.out.println(packet);
        }
    }
    public void addSequenceNumber() {
        Queue<Packet> tempQueue = new LinkedList<>();
        while (!eventQueue.isEmpty()) {
            Packet packet = eventQueue.poll();
            packet.setSequenceNumber();
            tempQueue.add(packet);
        }
        eventQueue = tempQueue;

    }
    public boolean isEmpty(){
        return eventQueue.isEmpty();
    }
    public Packet pullPacketFromEventList(){
        return eventQueue.poll();
    }
    public void addPacketToEventList(Packet packet){ eventQueue.add(packet); }

    public Packet findPacketByNodeId(long nodeId) {
        Iterator<Packet> iterator = eventQueue.iterator(); // Δημιουργία iterator για την eventQueue
        while (iterator.hasNext()) {
            Packet packet = iterator.next(); // Παίρνουμε το επόμενο πακέτο
            if (packet.getEndDeviceID() == nodeId) {
                return packet; // Επιστρέφει το πρώτο πακέτο που ταιριάζει
            }
        }
        return null; // Αν δεν βρεθεί, επιστρέφει null
    }
    public void clearEventList(){
        eventQueue.clear();
    }




}
