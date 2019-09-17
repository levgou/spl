package bgu.spl181.net.json;

import bgu.spl181.net.types.DB;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AccessLimiter {

    static private final ReadWriteLock movie_rwl = new ReentrantReadWriteLock();
    static private final ReadWriteLock user_rwl = new ReentrantReadWriteLock();

    public static void getWriteAccess(DB type){
        if (type == DB.MOVIE){
            movie_rwl.writeLock().lock();
        }
        else if (type == DB.USER) {
            user_rwl.writeLock().lock();
        }
    }

    public static void getReadAccess(DB type){
        if (type == DB.MOVIE){
            movie_rwl.readLock().lock();
        }
        else if (type == DB.USER) {
            user_rwl.readLock().lock();
        }
    }

    public static void finishWrite(DB type){
        if (type == DB.MOVIE){
            movie_rwl.writeLock().unlock();
        }
        else if (type == DB.USER) {
            user_rwl.writeLock().unlock();
        }
    }

    public static void finishRead(DB type){
        if (type == DB.MOVIE){
            movie_rwl.readLock().unlock();
        }
        else if (type == DB.USER) {
            user_rwl.readLock().unlock();
        }
    }

}
