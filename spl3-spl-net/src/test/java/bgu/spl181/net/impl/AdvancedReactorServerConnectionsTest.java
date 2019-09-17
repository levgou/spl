package bgu.spl181.net.impl;

import bgu.spl181.net.impl.BBreactor.ReactorMain;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class AdvancedReactorServerConnectionsTest extends ReactorServerClientTestTemplate{
    @Test(timeout = 50000)
    public void AdvancedReactorServerConnectionsTest() throws IOException, InterruptedException {
        String testPath = "./src/test/resources/AdvancedServerConnectionsTest/";

        AtomicInteger failLoginCounter = new AtomicInteger(0);

        CountDownLatch l = new CountDownLatch(39);

        Runnable testLambda = () -> {
            failLoginCounter.incrementAndGet();
            l.countDown();
        };

        server = A.initiateServer("DemoReactorConnections",A.serverPort);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Thread[] clients = A.initiateClients(40,A.serverIp,A.serverPort,
                testPath,"*",testLambda);

//        A.waitForClients(clients);
        l.await();
        if(failLoginCounter.get() > 20){
            Assert.fail();
        }
    }
}
