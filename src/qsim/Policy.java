package qsim;

import java.io.*;
import java.util.*;

/** A policy determines how we choose the screener profile for each
    lane that has multiple profiles available. 

    <p>
    This is an abstract class; the derived classes implement various
    policy actually available in the simulator.
 */
abstract class Policy {

    Parameters.Policy name;

    /** The factory method */
    static Policy createPolicy(Parameters para, Qsim qsim) {
	Policy p = null;
	if (para.policy == Parameters.Policy.P0) {
	    p = new PolicyP0(qsim.lanes, para.dAcceptable);
	} else if (para.policy == Parameters.Policy.P1) {
	    p = new PolicyP1(qsim.lanes, para.dAcceptable);
	} else if (para.policy == Parameters.Policy.P2) {
	    p = new PolicyP2(qsim.lanes, para.dAcceptable);
	} else if (para.policy == Parameters.Policy.P3) {
	    p = new PolicyP3(qsim.lanes, para.dAcceptable);
	} else {
	    return null;
	}
	p.name = para.policy;
	return p;
    }
    
    /** Chooses policy for Lane no. mypos to use at this moment. */
    abstract int chooseProfile(int mypos, Queue[] lanes);

    /** May be overridden by individual policies to produce a performance report */
    String report(Queue[] lanes) {
	String s = "Profile assignment policy " + name;
	return s;
    }


}
