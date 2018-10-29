package utils;

import java.util.Random;

public class Test {
    public void test(){
        byte[] randomMatrix = new byte[3];
        Random random = new Random();
        random.nextBytes(randomMatrix);
    }
}
