package bgu.spl.a2;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * an abstract class that represents an action that may be executed using the
 * {@link ActorThreadPool}
 * <p>
 * Note for implementors: you may add methods and synchronize any of the
 * existing methods in this class *BUT* you must be able to explain why the
 * synchronization is needed. In addition, the methods you add to this class can
 * only be private!!!
 *
 * @param <R> the action result type
 */
public abstract class Action<R> {

    protected static ActorThreadPool pool = null;
    private String actorId;
    private PrivateState actorState;
    protected boolean dependenciesResolved = false;
    private callback callback = null;
    protected Promise<R> promise = new Promise<>();
    private String actionName;
    private AtomicInteger dependenciesNum = new AtomicInteger(0);

    /**
     * start handling the action - note that this method is protected, a thread
     * cannot call it directly.
     */
    // Important: any one implementing start should call then
    protected abstract void start();


    /**
     * start/continue handling the action
     * <p>
     * this method should be called in order to start this action
     * or continue its execution in the case where it has been already started.
     * <p>
     * IMPORTANT: this method is package protected, i.e., only classes inside
     * the same package can access it - you should *not* change it to
     * public/private/protected
     */
    /*package*/
    final void handle(ActorThreadPool pool, String actorId, PrivateState actorState) {
        this.pool = pool;
        this.actorId = actorId;
        this.actorState = actorState;

        if (!dependenciesResolved) {
            start();
        } else {
            callback.call();
        }

    }


    /**
     * add a callback to be executed once *all* the given actions results are
     * resolved
     * <p>
     * Implementors note: make sure that the callback is running only once when
     * all the given actions completed.
     *
     * @param actions
     * @param callback the callback to execute once all the results are resolved
     */
    protected final void then(Collection<? extends Action<?>> actions, callback callback) {

        this.callback = callback;
        dependenciesNum.set(actions.size());
        askNicelyActionsToNotifyMe(actions);

    }


    /**
     * resolve the internal result - should be called by the action derivative
     * once it is done.
     *
     * @param result - the action calculated result
     */
    protected final void complete(R result) {
        this.promise.resolve(result);
    }

    /**
     * @return action's promise (result)
     */
    public final Promise<R> getResult() {

        return this.promise;
    }

    /**
     * send an action to an other actor
     *
     * @param action     the action
     * @param actorId    actor's id
     * @param actorState actor's private state (actor's information)
     * @return promise that will hold the result of the sent action
     */
    public Promise<?> sendMessage(Action<?> action, String actorId, PrivateState actorState) {

        pool.submit(action, actorId, actorState);
        return action.promise;
    }

    /**
     * set action's name
     *
     * @param actionName - save action name at this.actionName
     */
    public void setActionName(String actionName) {

        this.actionName = actionName;
    }

    /**
     * @return action's name
     */
    public String getActionName() {

        return this.actionName;
    }


    // ----------------------------------------------- privates -----------------------------------------------

    /**
     * mark that all dependencies have been resolved
     */
    private void resolveDependencies() {
        dependenciesResolved = true;
    }

    /**
     * each time a dependency is resolved it tries to re-add me to my Q
     * to avoid multiple inQing - sync
     */
    synchronized private void tryToReinsertMe() {
        if (!dependenciesResolved && dependenciesNum.get() == 0) {
            resolveDependencies();
            pool.submit(this, actorId, actorState);
        }
    }

    /**
     * @param actions actions where I'll be subscribed to notify when they finish
     */
    private void askNicelyActionsToNotifyMe(Collection<? extends Action<?>> actions) {

        Action<?> me = this;
        Promise<?> curPromise;

        for (Action<?> ac : actions) {
            curPromise = ac.getResult();

            curPromise.subscribe(() -> {
                        dependenciesNum.decrementAndGet();
                        me.tryToReinsertMe();
                    }
            );
        }
    }
}
