package bgu.spl.a2;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PromiseTest {

    private Promise<Double> p1;
    private Promise<Double> p2;
    private Promise<Object> p3;

    @Before
    public void setUp() throws Exception {
        p1 = new Promise<>();
        p2 = new Promise<>();
    }

    @After
    public void tearDown() throws Exception {
        p1 = null;
        p2 = null;
    }

    @Test
    public void isResolved() {
        assertFalse(p1.isResolved());
    }

    @Test
    public void resolve() {
        p1.resolve(5.2);
        try{
            p1.resolve(6.0);
        }
        catch (IllegalStateException ex) {}
        catch (Exception ex){
            Assert.fail();
        }

        assertTrue(p1.isResolved());
        assertEquals(new Double(5.2), p1.get());

        // base assumption - promise wont be accessed by more than 1 thread -
        // thus currently wont test thread safety


        // check 1000 times with various thread numbers
//        for (int j = 0; j < 1000; j++) {    // 1000 times
//            for (int k = 2; k < 11; k++) {  // various thread numbers
//
//                try {
//                    p3 = new Promise<>();
//                    Thread[] ts = new Thread[k];
//
//                    for (int i = 0; i < ts.length; i++) {                   // gen all threads
//                        ts[i] = new Thread(()-> p3.resolve(8.8));
//                    }
//
//                    for (Thread t: ts){                                     // run all threads
//                        t.start();
//                    }
//
//                    Thread.sleep(1000 * 20); // wait 20 seconds (more than needed)
//
//                    // if exception wasn't thrown - throw ex to fail test:
//                    throw new RuntimeException("didn't except on multiple resolves");
//                }
//
//                catch (IllegalStateException ex) {
//                    assertEquals(new Double(8.8), p1.get());
//                }
//                catch (Exception ex){
//                    Assert.fail();
//                }
//            }
//        }

    }

    @Test
    public void get() {
        try {
            int[] arr = {1,2,3};
            p3 = new Promise<>();

            try {
                p3.get();
            }
            catch (IllegalStateException ex) {}
            p3.resolve(arr);

            assertEquals(arr,p3.get());
        }

        catch (Exception ex){
            Assert.fail();
        }

    }

    @Test
    public void subscribe() {
        try{

            // basic:
            Integer[] I = {9};

            p3 = new Promise<>();
            p3.subscribe(new callback() {
                @Override
                public void call() {
                    I[0] = 10;
                }
            });

            p3.resolve(true);
            assertTrue(I[0] == 10);

            // assign callback when already resolved
            p3 = new Promise<>();
            p3.resolve(true);
            p3.subscribe(()-> I[0] =20);
            assertTrue(I[0] == 20);

            // callback is not called
            p3 = new Promise<>();
            p3.subscribe(()-> I[0] =30);
            assertTrue(I[0] == 20);

        }
        catch (Exception e){
            Assert.fail();
        }

    }
}