//----------------------------------------------------------------------------
//	Cache.java
//	Author: Chad Dugie, David Trinh
//----------------------------------------------------------------------------
//	Description:
//  The cache class is designed to be used as a cache as part of ThreadOS.
//  It uses the enhanced second chance algorithm using the reference bit and
//  dirty bit that is stored with the private class Entry.
//  Entry also has a block ID stored inside.
//  The page table is created as an array of Entry classes.
//
//-----------------------------------------------------------------------------

import java.util.*;

public class Cache {

    //======================= Cache(int, int) ==================================
    //  Two argument constructor. Cache is called in Kernel.java
    //
    public Cache(int blockSize, int cacheBlocks) {
        pageTable = new Entry[cacheBlocks];
        cache = new Vector<byte[]>();
        for (int i = 0; i < cacheBlocks; i++) {
            pageTable[i] = new Entry();
            cache.add(new byte[blockSize]);
        }
    }

    //======================= Entry Class ======================================
    //  Stored variables blockId, reference, and dirty bit. Used for
    //  enhanced second chance algorithm for cache.
    //
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

    //======================= findFreePage() ===================================
    //  Searches pageTable for an unused page to use. Returns page number if
    //  found, else it will return -1.
    //
    private int findFreePage() {
        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i].blockId == -1) {
                return i;
            }
        }
        return -1;
    }

    //======================= nextVictim() =====================================
    //  Searches page table for a page with reference bit false and returns
    //  the page.
    //  Used to swap a page out when there is no more room.
    //
    private int nextVictim() {
        for (int i = 0; i < pageTable.length; i = (i + 1) % pageTable.length) {
            if (pageTable[i].reference == false) {
                return i;
            }
            pageTable[i].reference = false;
        }
        return -1;
    }

    //======================= writeBack(int) ===================================
    //  Writes to disk
    //
    private void writeBack(int victimEntry) {
        if (pageTable[victimEntry].blockId >= -1) {
            SysLib.rawwrite(pageTable[victimEntry].blockId, cache.elementAt(victimEntry));
            pageTable[victimEntry].dirty = false;
        }
    }

    //======================= read(int, byte) ==================================
    //  Reads a blockID from pageTable. If not found in page table, method will
    //  look for a unused page and write block id to the page. If free
    //  page is not found, method will look for a page to swap out.
    //
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

    //======================= write(int, byte) =================================
    //  Writes the buffer[ ]array contents to the cache block specified by
    //  blockId from the disk cache if it is in cache,
    //  otherwise finds a free cache block and writes the buffer [ ]
    //  contents on it. No write through.
    //  Upon an error, it should return false, otherwise return true.
    //
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

    //======================= sync() ===========================================
    //  Maintains clean block copies in Cache.java
    //
    public synchronized void sync() {
        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i].dirty == true) {
                writeBack(i);
            }
        }
        SysLib.sync();
    }

    //======================= flush) ===========================================
    //  Invalidates all cached blocks
    //
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
