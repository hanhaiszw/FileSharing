package utils;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class TestTest {

    @Test
    public void test1() {
        byte[] randomMatrix = new byte[3];
        Random random = new Random();
        random.nextBytes(randomMatrix);
        System.out.println(randomMatrix[0]);
        System.out.println(randomMatrix[1]);
        System.out.println(randomMatrix[2]);
    }
}