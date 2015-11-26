import java.util.*;

public class Cache {

    public Cache(int blockSize, int cacheBlocks) {
        pageTable = new Entry[cacheBlocks];
        cache = new Vector<byte[]>();
        for (int i = 0; i < cacheBlocks; i++) {
            pageTable[i] = new Entry();
            cache.add(new byte[blockSize]);
        }
    }


    private class Entry {
        private int blockId;        // data
        private boolean reference;  // If true, data was used recently
        private boolean dirty;      // If true, data was modified recently

        public Entry() {
            blockId = -1;
            reference = false;
            dirty = false;
        }

    }

    private Entry[] pageTable = null;
    private Vector<byte[]> cache;
    private int victim;
    // Done.
    private int findFreePage() {
        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i].blockId == -1) {
                return i;
            }
        }
        return -1;
    }
    // Done.
    private int nextVictim() {
        for (int i = 0; i < pageTable.length; i = (i + 1) % pageTable.length) {
            if (pageTable[i].reference == false) {
                return i;
            }
            pageTable[i].reference = false;
        }
        return -1;
    }

    private void writeBack(int victimEntry) {

    }

    public synchronized boolean read(int blockId, byte buffer[]) {
        if (blockId >= 0) {
            // Searches for loop to check if blockID is in the page table.
            for (int i = 0; i < pageTable.length; i++) {
                if (pageTable[i].blockId == blockId) {
                    // Read from cache.
                    pageTable[i].reference = true;
                    return true;
                }
            }
            int freePage = findFreePage();
            // BlockID is not in page table, so find a free page
            if (freePage > -1) {
                // Read from disk to cache.
                // Read from cache.
                pageTable[victim].reference = false;
                return true;
            }
            victim = nextVictim(); // Probably don't need to store this outside of method.
            if (pageTable[victim].dirty == true) {
                writeBack(victim);
            }
            // Read from disk to cache.
            // Read from cache.
            pageTable[victim].reference = false;

            // Uses disk.read called from Kernel
            SysLib.rawread(blockId, buffer);

            return true;
        }
        return false;
    }

    public synchronized boolean write(int blockId, byte buffer[]) {
        if (blockId >= 0) {
            //Kernel.disk.write(blockId, buffer);
            return true;
        }
        return false;
    }

    public synchronized void sync() {
    }

    public synchronized void flush() {
    }
}
