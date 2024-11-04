public class Channel {
    private boolean isOccupied = false;
    private long occupiedSince = 0;
    private final long tpreamble; // Ο χρόνος μετά τον οποίο το κανάλι μπορεί να θεωρηθεί λανθασμένα ελεύθερο

    public Channel(long tpreamble) {
        this.tpreamble = tpreamble;
    }

    public synchronized boolean cca(int internalClock) {
        if (!isOccupied) {
            return true; // Το κανάλι είναι ελεύθερο
        }

        // Έλεγχος αν έχει περάσει χρόνος μεγαλύτερος του tpreamble
        if (internalClock > tpreamble) {
            System.out.println("Warning: Channel idle detection failed! Potential collision.");
            return true; // Λανθασμένα ελεύθερο, πιθανή σύγκρουση
        }

        return false; // Το κανάλι είναι ακόμα κατειλημμένο
    }

    public synchronized void occupy() {
        isOccupied = true;
        occupiedSince = System.currentTimeMillis();
    }

    public synchronized void release() {
        isOccupied = false;
        occupiedSince = 0;
    }

    public boolean isFree() {
        return !isOccupied;
    }
}
