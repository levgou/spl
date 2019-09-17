package bgu.spl.a2;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class VersionMonitorTest {

    private VersionMonitor vm0;
    private VersionMonitor vm1;
    private VersionMonitor vm2;
    private CountDownLatch l;

    @Before
    public void setUp() throws Exception {
        vm0 = new VersionMonitor();
        vm1 = new VersionMonitor();
        vm2 = new VersionMonitor();
    }

    @After
    public void tearDown() throws Exception {
        vm0 = null;
        vm1 = null;
        vm2 = null;
        l = null;
    }

    @Test
    public void getVersion() {
        assertEquals(0, vm0.getVersion());
    }

    @Test
    public void inc() {
        try {


            // basic inc:
            vm0.inc();
            assertEquals(1, vm0.getVersion());

            // ---------------------------------------------------------------------------
            // check 100 times with 1,000 threads
            for (int i = 0; i < 100; i++) {
                System.out.println("iteration: " + i);
                l = new CountDownLatch(1000);
                for (int j = 0; j < 1000; j++) {
                    Thread t1 = new Thread(() -> {
                        vm1.inc();
                        l.countDown();
                    });

                    t1.start();
                }
                l.await();

                assertEquals(1000 * (i + 1), vm1.getVersion());

            }

            // ---------------------------------------------------------------------------

            //check 100 times with 1000 threads, now with parallel inc / getVersion
            for (int i = 0; i < 100; i++) {
                System.out.println("iteration: " + i);

                l = new CountDownLatch(1000);
                for (int j = 0; j < 1000; j++) {
                    Thread t2;
                    if (j % 2 == 0) {
                        t2 = new Thread(() ->{
                            vm2.inc();
                            l.countDown();
                        });
                    } else {
                        t2 = new Thread(() -> {
                            vm2.getVersion();
                            l.countDown();
                        });
                    }
                    t2.start();
                }
                l.await();

                assertEquals(500*(i+1), vm2.getVersion());
            }


        } catch (Exception e) {
            Assert.fail();
        }

    }

    @Test
    public void await() {
        try {
            int[] iArr = {0};

            Thread waiter = new Thread(() -> {
                try {
                    vm0.await(vm0.getVersion());
                    ++iArr[0];
                } catch (InterruptedException e) {
                    Assert.fail();
                }

            });
            waiter.start();
            Thread.sleep(1000); // in order for the thread "go to sleep" before waking him up
            vm0.inc();
            waiter.join();                      // wait for waiter to finish its task
            assertEquals(iArr[0], 1);

        } catch (Exception e) {
            Assert.fail();
        }

    }
}