

public class Test4 {
    private static final int BSIZE = 512;
    private byte[] writeB = new byte[BSIZE];
    private byte[] readB = new byte[BSIZE];

    public Test4(String[] args) {
        if(args == null || args.length != 2)
            throw new RuntimeException("Please enter two valid arguments. " +
                    "Argument 1: [enabled | disabled]. Argument 2: [1-5] ");


    }
    public void run() {

    }

}

