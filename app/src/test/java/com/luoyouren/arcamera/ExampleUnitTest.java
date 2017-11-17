package com.luoyouren.arcamera;

import android.graphics.PointF;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    public PointF[] preProcess(PointF[] pointFs)
    {
        if (pointFs.length < 0)
        {
            return null;
        }

        pointFs[0].x = 90;
        pointFs[0].y = 8;

        return pointFs;
    }

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }
}