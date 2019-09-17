package bgu.spl.a2.sim.actions;

import bgu.spl.a2.Promise;
import bgu.spl.a2.callback;
import bgu.spl.a2.sim.Computer;
import bgu.spl.a2.sim.Warehouse;
import bgu.spl.a2.sim.privateStates.DepartmentPrivateState;
import bgu.spl.a2.sim.privateStates.StudentPrivateState;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;

public class CheckAdministrativeObligations extends ActionServices<ImmutablePair<Enum, String>> {

    private String compName;
    private Collection<ImmutablePair<String, StudentPrivateState>> students;
    private List<String> courses;
    private DepartmentPrivateState dps;
    private String departmentName;
    private boolean triedToGetPc;
    private Promise<Computer> pcPromise;

    public CheckAdministrativeObligations(String compName,
                                          List<String> courses,
                                          Collection<ImmutablePair<String, StudentPrivateState>> students,
                                          DepartmentPrivateState dps,
                                          String departmentName) {

        this.compName = compName;
        this.students = students;
        this.courses = courses;
        this.dps = dps;
        this.departmentName = departmentName;
        this.setActionName("Administrative Check");
        this.triedToGetPc = false;
    }


    /**
     *      get pc from warehouse, the give it to each student to sign themself
     */
    @Override
    protected void start() {

        if (!triedToGetPc) {
            triedToGetPc = true;
            pcPromise = Warehouse.getInstance().acquire(compName);
            pcPromise.subscribe(()-> pool.submit(this, departmentName, dps));
        }

        // got pc in hand
        else {
            List<ActionServices<?>> signingActions = new ArrayList<>();

            // all students are provided with the pc to get signed
            for (ImmutablePair<String, StudentPrivateState> s : students) {
                SignStudent signS = new SignStudent(s.left, s.right, courses, pcPromise.get());
                sendMessage(signS, s.left, s.right);
                signingActions.add(signS);
            }

            then(signingActions, new callback() {

                @Override
                public void call() {
                    Warehouse.getInstance().release(compName);
                    complete(new ImmutablePair<>(statuses.SUCCESS, "Signed all students"));
                }

            });
        }

    }


    // =================================================================================================================
    private class SignStudent extends ActionServices<ImmutablePair<Enum, String>> {

        private String studentName;
        private StudentPrivateState sps;
        private List<String> courses;
        private Computer comp;


        private SignStudent(String studentName, StudentPrivateState sps, List<String> courses, Computer comp) {
            this.studentName = studentName;
            this.sps = sps;
            this.courses = courses;
            this.comp = comp;

        }


        @Override
        protected void start() {

            sps.setSignature(comp.checkAndSign(courses, sps.getGrades()));
            complete(new ImmutablePair<>(statuses.SUCCESS,
                    String.format("Student signed with %d", sps.getSignature())));
            }
        }

    } // SignStudent




