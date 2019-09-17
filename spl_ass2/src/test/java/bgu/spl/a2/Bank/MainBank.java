package bgu.spl.a2.Bank;

import bgu.spl.a2.Action;
import bgu.spl.a2.ActorThreadPool;
import bgu.spl.a2.Promise;

import java.util.concurrent.CountDownLatch;

public class MainBank {

    public static void main(String[] args) throws InterruptedException {
        ActorThreadPool pool = new ActorThreadPool ( 8 ) ;
        Action<String > trans = new Transmission (100 , "A" , "B" , "bank2" , "bank1" ) ;
        pool.start ( ) ;
        pool.submit ( trans , "bank1" , new BankStates ( ) ) ;
        CountDownLatch l = new CountDownLatch ( 1 ) ;
        Promise<?> p = trans.getResult();
        p.subscribe(()->{

            l.countDown ( ) ;
        } ) ;
        l.await ( ) ;
        pool.shutdown();

    }
}
