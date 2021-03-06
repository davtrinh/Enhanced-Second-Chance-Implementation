//-----------------------------------------------------------------------------
//	Test4.java
//  Authors: Chad Dugie, David Trinh
//-----------------------------------------------------------------------------
//	Description:
//  Purpose of Test4 is to do a performance test on Cache.java to see
//  how it performs in four difference cases.
//
//-----------------------------------------------------------------------------

import java.util.*;

public class Test4 extends Thread {
    private final int iterations = 100; // Can increase or decrease
    private static final int BSIZE = 512;
    private byte[] writeBuff = new byte[BSIZE];
    private byte[] readBuff = new byte[BSIZE];

    //Used to hold the user arguments
    private int caseNum;
    private String diskCase;

    // Helps determine if disk cache will be used
    private final String useDisk = "enabled";
    private final String noDisk = "disabled";
    private Random randomNum = new Random();

    private long startTime; // Time
    private long endTime;
    private long randomTime;


    public Test4(String[] args)
    {
        if(args == null || args.length != 2)
            throw new RuntimeException("Please enter two valid arguments. " +
                    "Argument 1: [enabled | disabled]. Argument 2: [1-4] ");
        // Holds arguments
        diskCase = args[0].toLowerCase();
        caseNum = Integer.parseInt(args[1]);

        // Argument checking
        if(!(diskCase.equals(useDisk) || diskCase.equals(noDisk)))
            throw new RuntimeException("Please enter either \"enabled\" or \"disabled\" for the first argument");
        if(caseNum < 1 || caseNum > 4)
            throw new RuntimeException("Only an integer from 1-4 are allowed for the second argument");
    }

    //======================= run() ============================================
    //  Directs to a method for performance testing depending on the given
    //  argument.
    //
    public void run()
    {
        SysLib.flush(); // Clear
        switch(caseNum)
        {
            case 1:
                randomAccess();
                break;
            case 2:
                localAccess();
                break;
            case 3:
                mixedAccess();
                break;
            case 4:
                adversaryAccess();
                break;
            default: // not really necessary, could remove
                break;
        }
        SysLib.exit();
    }

    //======================= randomAccess() ===================================
    //  Reads and writes many blocks randomly across the disk.
    //
    public void randomAccess()
    {
        int[] arr = new int[iterations];

        // Fill array
        for(int i = 0; i < iterations; i++) {
            arr[i] = Math.abs(randomNum.nextInt() % BSIZE);
        }
        startTime = new Date().getTime();

        for(int i = 0; i < iterations; i++)
        {
            for(int a = 0; a < BSIZE; a++)
            {
                writeBuff[a] = (byte)a;
            }
            write(arr[i], writeBuff);
        }

        for(int i = 0; i < iterations; i++)
        {
            read(arr[i], readBuff);
        }
        endTime = new Date().getTime();
        showPerformance();
        check();
    }

    //======================= localAccess() ====================================
    //  Read and write a small selection of blocks many times to get a high
    //  ratio of cache and hits
    //
    public void localAccess()
    {
        startTime = new Date().getTime();
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < BSIZE; j++) {
                writeBuff[j] = (byte)(i + j);
            }
            for (int j = 0; j < 1000; j += 100) {
                write(j, writeBuff);
            }
            for (int j = 0; j < 1000; j += 100) {
                read(j, readBuff);
            }
        }
        endTime = new Date().getTime();
        showPerformance();
        check();
    }


    //======================= MixedAccess() ====================================
    //  90% of the total disk operations are localized accesses and 10%
    //  are random accesses.
    //
    public void mixedAccess()
    {
        int[] arr = new int[iterations];

        // Fill array
        for(int i = 0; i < iterations; i++) {
            if (Math.abs(randomNum.nextInt() % 10) > 8) {
                arr[i] = Math.abs(randomNum.nextInt() % BSIZE);
            } else {
                arr[i] = Math.abs(randomNum.nextInt() % 10);
            }
        }
        startTime = new Date().getTime();
        for(int i = 0; i < iterations; i++)
        {
            for(int a = 0; a < BSIZE; a++)
            {
                writeBuff[a] = (byte)a;
            }
            write(arr[i], writeBuff);
        }

        for(int i = 0; i < iterations; i++)
        {
            read(arr[i], readBuff);
        }
        endTime = new Date().getTime();
        showPerformance();
        check();
    }

    //======================= AdversaryAccess() ================================
    //  Generates disk accesses that do not make good use of the disk cache
    //
    public void adversaryAccess()
    {
        startTime = new Date().getTime();
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < BSIZE; j++) {
                writeBuff[j] = (byte)j;
            }
            for (int j = 0; j < 10; j++) {
                write(i * 10 + j, writeBuff);
            }
        }
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 10; j++) {
                read(i * 10 + j, readBuff);
            }
        }
        endTime = new Date().getTime();
        showPerformance();
        check();
    }

    //======================= read(int, byte[]) ================================
    //  Helper method that either uses cread or rawread.
    //
    private void read(int num, byte[] arr)
    {
        if(diskCase.equals(useDisk))
            SysLib.cread(num, arr);
        else
            SysLib.rawread(num, arr);

    }

    //======================= write(int, byte[]) ===============================
    //  Helper method that either uses cwrite or rawwrite.
    //
    private void write(int num, byte[] arr)
    {
        if(diskCase.equals(useDisk))
            SysLib.cwrite(num, arr);
        else
            SysLib.rawwrite(num, arr);
    }
    //======================= showPerformance() ================================
    //  Prints out performance for the particular case.
    //
    private void showPerformance()
    {
        switch(caseNum)
        {
            case 1:
                SysLib.cout("\nRandom Access Turnaround Time with cache " + diskCase + " : " + (endTime - startTime) + "msec");
                break;
            case 2:
                SysLib.cout("\nLocalized Access Turnaround Time with cache " + diskCase + " : " + (endTime - startTime) + "msec");
                break;
            case 3:
                SysLib.cout("\nMixed Access Turnaround Time with cache " + diskCase + " : " + (endTime - startTime) + "msec");
                break;
            case 4:
                SysLib.cout("\nAdversary Turnaround Time with cache " + diskCase + " : " + (endTime - startTime) + "msec");
                break;
        }

    }
    //======================= check() ==========================================
    //  Checks the read and write buffer to see if they are equal.
    //
    private void check()
    {
        if(!(Arrays.equals(readBuff, writeBuff)))
        {
            SysLib.cerr(" Data does not match!");
        }
    }
}

