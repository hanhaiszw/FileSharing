package utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class ToolUtilsTest {

    @Test
    public void randomString() {
        for (int i = 0; i < 10; i++) {
            String str = ToolUtils.randomString();
            System.out.println(str);
        }

    }
}