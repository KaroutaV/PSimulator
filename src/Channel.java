import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class Channel {

    private final ReentrantLock lock = new ReentrantLock();


    private AtomicBoolean isOccupied = new AtomicBoolean(false);
    private int occupiedClockTimestamp; // Εικονικός χρόνος όταν καταλήφθηκε το κανάλι
    private int clock; // Global εικονικός χρόνος
    private int tPreamble;
    private PriorityBlockingQueue<EndDevice> waitingQueue; // Λίστα αναμονής για τους κόμβους

    public Channel(int tPreamble) {
        this.clock = 0; // Αρχική τιμή του εικονικού χρόνου
        this.tPreamble = tPreamble;
        this.waitingQueue = new PriorityBlockingQueue<>((a, b) -> Integer.compare(a.getInternalClock(), b.getInternalClock()));

    }

    // Αύξηση του global clock, την καλείς σε κάθε βήμα της προσομοίωσης
//    public void incrementClock(int timeStep) {
//        clock += timeStep;
//    }
    public void incrementClock(int timeStep) {
        lock.lock(); // Κλείδωμα
        try {
            clock += timeStep;
        } finally {
            lock.unlock(); // Ξεκλείδωμα
        }
    }

    // Καταλαμβάνει το κανάλι και καταγράφει την τρέχουσα χρονική σήμανση του clock
    public synchronized void occupy() {
        isOccupied.set(true);
        occupiedClockTimestamp = clock;
    }

    // Απελευθερώνει το κανάλι
    public synchronized void release() {
        isOccupied.set(false);
    }

    // Ελέγχει αν το κανάλι είναι ελεύθερο
    public boolean isFree() {
        return !isOccupied.get();
    }

    // Υπολογίζει τον χρόνο που πέρασε από τότε που το κανάλι καταλήφθηκε, σε σχέση με το clock
    public int timeSinceOccupied() {
        if (isOccupied.get()) {
            return clock - occupiedClockTimestamp;
        }
        return 0; // Αν το κανάλι είναι ελεύθερο, επιστρέφει 0
    }

    // CAD check: θεωρεί το κανάλι ελεύθερο αν έχει περάσει χρόνος μεγαλύτερος από tPreamble
    public boolean cadCheck(int timeOnAir) {
        int timeSinceOccupied = timeSinceOccupied(); // Χρόνος από την τελευταία κατάληψη καναλιού
        return ((timeSinceOccupied > tPreamble) &&
                (timeSinceOccupied <  timeOnAir)) || isFree();
    }

    // Πρόσβαση στον τρέχοντα εικονικό χρόνο (για χρήση από τα nodes)
    public int getClock() {
        return clock;
    }

}
