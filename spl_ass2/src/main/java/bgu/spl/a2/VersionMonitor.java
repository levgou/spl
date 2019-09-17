package bgu.spl.a2;

/**
 * Describes a monitor that supports the concept of versioning - its idea is
 * simple, the monitor has a version number which you can receive via the method
 * {@link #getVersion()} once you have a version number, you can call
 * {@link #await(int)} with this version number in order to wait until this
 * version number changes.
 *
 * you can also increment the version number by one using the {@link #inc()}
 * method.
 *
 * Note for implementors: you may add methods and synchronize any of the
 * existing methods in this class *BUT* you must be able to explain why the
 * synchronization is needed. In addition, the methods you add can only be
 * private, protected or package protected - in other words, no new public
 * methods
 *
 *
 * sync the class methods in order to avoid the following issues:
 *  > thread waiting - because it "was notified" a moment before going to wait
 *  > returning version which is not up to date
 *
 */
public class VersionMonitor {


    private int ver;

    protected void init() {
        this.ver = 0;

    }

    /**
     * @return version
     * @pre: none
     * @post: none
     */
    synchronized public int getVersion() {
        return ver;
    }

    /**
     * will increase version by 1
     *
     * @pre: none
     * @post: --this.getVersion() == @pre(this)
     */
    synchronized public void inc() {
        ver++;
        this.notify();
    }

    /**
     * @param version
     * @throws InterruptedException
     * @pre: ver <= {@code version}
     * @post: ver >= {@code version}
     */
    synchronized public void await(int version) throws InterruptedException {

        while(version==ver){
            this.wait();
        }

    }

}