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
        private int blockId; // Identity in the page table.
        private boolean reference; // If true, data was used recently.
        private boolean dirty; // If true, data was modified recently.

        public Entry() {
            blockId = -1;
            reference = false;
            dirty = false;
        }
    }

    private Entry[] pageTable = null;
    private Vector<byte[]> cache;

    private int findFreePage() {
        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i].blockId == -1) {
                return i;
            }
        }
        return -1;
    }

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
        if (pageTable[victimEntry].blockId >= -1) {
            SysLib.rawwrite(pageTable[victimEntry].blockId, cache.elementAt(victimEntry));
            pageTable[victimEntry].dirty = false;
        }
    }

    public synchronized boolean read(int blockId, byte buffer[]) {
        if (blockId > -1) {
            // Searches for loop to check if blockID is in the page table.
            for (int i = 0; i < pageTable.length; i++) {
                if (pageTable[i].blockId == blockId) {
                    System.arraycopy(cache.elementAt(i), 0, buffer, 0, buffer.length);
                    pageTable[i].reference = true;
                    return true;
                }
            }
            // BlockID is not in page table, so find a free page.
            int freePage = findFreePage();
            if (freePage > -1) {
                SysLib.rawread(blockId, cache.elementAt(freePage));
                System.arraycopy(cache.elementAt(freePage), 0, buffer, 0, buffer.length);
                pageTable[freePage].blockId = blockId;
                return true;
            }
            // No free page in page table, so find victim page.
            int victim = nextVictim();
            if (victim > -1) {
                if (pageTable[victim].dirty == true) {
                    writeBack(victim);
                }
                SysLib.rawread(blockId, cache.elementAt(victim));
                System.arraycopy(cache.elementAt(victim), 0, buffer, 0, buffer.length);
                pageTable[victim].blockId = blockId;
                return true;
            }
        }
        return false;
    }

    public synchronized boolean write(int blockId, byte buffer[]) {
        if (blockId > -1) {
            // Searches for loop to check if blockID is in the page table.
            for (int i = 0; i < pageTable.length; i++) {
                if (pageTable[i].blockId == blockId) {
                    if (pageTable[i].dirty == true) {
                        writeBack(i);
                    }
                    System.arraycopy(buffer, 0, cache.elementAt(i), 0, buffer.length);
                    pageTable[i].reference = true;
                    pageTable[i].dirty = true;
                    return true;
                }
            }
            // BlockID is not in page table, so find a free page.
            int freePage = findFreePage();
            if (freePage > -1) {
                System.arraycopy(buffer, 0, cache.elementAt(freePage), 0, buffer.length);
                pageTable[freePage].blockId = blockId;
                pageTable[freePage].dirty = true;
                return true;
            }
            // No free page in page table, so find victim page.
            int victim = nextVictim();
            if (victim > -1) {
                if (pageTable[victim].dirty == true) {
                    writeBack(victim);
                }
                System.arraycopy(buffer, 0, cache.elementAt(victim), 0, buffer.length);
                pageTable[victim].blockId = blockId;
                pageTable[victim].dirty = true;
                return true;
            }
        }
        return false;
    }

    public synchronized void sync() {
        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i].dirty == true) {
                writeBack(i);
            }
        }
        SysLib.sync();
    }

    public synchronized void flush() {
        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i].dirty == true) {
                writeBack(i);
                pageTable[i].blockId = -1;
                pageTable[i].reference = false;
            }
        }
        SysLib.sync();
    }
}
