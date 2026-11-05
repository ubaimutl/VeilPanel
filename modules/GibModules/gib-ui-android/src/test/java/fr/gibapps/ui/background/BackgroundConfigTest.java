// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.ui.background;
import org.junit.Test;
import static org.junit.Assert.*;

public class BackgroundConfigTest {
    @Test public void typeAndDirectionAreIndependentlyBounded() {
        int[] inputs = {Integer.MIN_VALUE, -1, 0, 1, 2, 3, 4, 5, 6, Integer.MAX_VALUE};
        for (int type : inputs) for (int direction : inputs) {
            BackgroundConfig c = new BackgroundConfig(type, 0, 0, direction);
            assertEquals(Math.max(0, Math.min(2, type)), c.type);
            assertEquals(Math.max(0, Math.min(5, direction)), c.direction);
        }
    }
    @Test public void bothColorsAreOpaqueAndValuesHaveEquality() {
        BackgroundConfig c = new BackgroundConfig(2, 0x123456, 0x80765432, 5);
        BackgroundConfig same = new BackgroundConfig(2, 0xff123456, 0xff765432, 5);
        assertEquals(0xff123456, c.color1);
        assertEquals(0xff765432, c.color2);
        assertEquals(c, same);
        assertEquals(c.hashCode(), same.hashCode());
        assertNotEquals(c, new BackgroundConfig(1, c.color1, c.color2, 5));
    }
}
