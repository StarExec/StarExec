package org.starexec.test.junit.data.to.tuples;

import org.junit.Test;
import org.starexec.data.to.tuples.PairIdJobId;
import org.testng.Assert;


public class PairIdJobIdTests {

    @Test
    public void equalityTest() {
        PairIdJobId first = new PairIdJobId(1, 1);
        PairIdJobId second = new PairIdJobId(1, 1);

        Assert.assertEquals(first, second);
    }

    @Test
    public void rightInequalityTest() {
        PairIdJobId first = new PairIdJobId(1, 2);
        PairIdJobId second = new PairIdJobId(1, 1);
        Assert.assertNotEquals(first, second);
    }

    @Test
    public void leftInequalityTest() {
        PairIdJobId first = new PairIdJobId(2, 1);
        PairIdJobId second = new PairIdJobId(1, 1);
        Assert.assertNotEquals(first, second);
    }

    @Test
    public void bothInequalityTest() {
        PairIdJobId first = new PairIdJobId(2, 2);
        PairIdJobId second = new PairIdJobId(1, 1);
        Assert.assertNotEquals(first, second);
    }

    @Test
    public void hashTest() {
        PairIdJobId first = new PairIdJobId(1, 1);
        PairIdJobId second = new PairIdJobId(1, 1);
        Assert.assertEquals(first.hashCode(), second.hashCode());
    }




}
