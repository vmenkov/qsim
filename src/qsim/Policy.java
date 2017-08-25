package qsim;

import java.io.*;
import java.util.*;

/** A policy determines how we choose the screener profile for each
    lane that has multiple profiles available. 

    <p> This is an abstract class; each derived class implements one
    specific policy actually available in the simulator. Each such
    policy contains a set of rules whereby {@link ScreenerProfile
    screener profiles} are chosen to be used, statically or
    dynamically.
 */
abstract class Policy {

    Parameters.Policy name;

    /** The factory method creates a Policy object to be used in the 
	simulator.
	@param The policy will be chosen as specified in para.policy
     */
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
    
    /** Chooses a policy for Lane no. mypos to use at this moment. */
    abstract int chooseProfile(int mypos, Queue[] lanes);

    /** May be overridden by individual policies to produce a
	performance report. */
    String report(Queue[] lanes) {
	String s = "Profile assignment policy " + name;
	return s;
    }


}
