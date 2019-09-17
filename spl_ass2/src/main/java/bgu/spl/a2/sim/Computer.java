package bgu.spl.a2.sim;

import java.util.List;
import java.util.Map;

public class Computer {

    private String computerType;
    final long failSig;
    final long successSig;


    public Computer(String computerType, long successSig, long failSig) {
        this.computerType = computerType;
        this.failSig = failSig;
        this.successSig = successSig;
    }

    /**
     * this method checks if the courses' grades fulfill the conditions
     *
     * @param courses       courses that should be pass
     * @param coursesGrades courses' grade
     * @return a signature if couersesGrades grades meet the conditions
     */
    public long checkAndSign(List<String> courses, Map<String, Integer> coursesGrades) {

        for (String s : courses) {
            if (coursesGrades.get(s) == null || coursesGrades.get(s) < 56) {
                return failSig;
            }
        }
        return successSig;
    }

    public long getFailSig() {
        return failSig;
    }

    public long getSuccessSig() {
        return successSig;
    }

    public String getType() {
        return computerType;
    }
}
