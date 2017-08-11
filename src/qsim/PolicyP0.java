package qsim; 

import java.io.*;
import java.util.*;

/** This is a dynamic profile selecting policy. It is very simple: every time
     a customer is to be processed by some lane, we compute the overall system's
     detection rate so far (based on how many customers have been processed
     by each profile anywhere in the system), and choose the profile for 
     screening this customer so that the overall detection rate will stay above 
     the acceptable threshold.
 */
class PolicyP0 extends Policy {

    int chooseProfile(int mypos, Queue[] lanes) {
	Queue lane = lanes[mypos];
	double sumD = 0;
	int sumN = 0;
	for(int i=0; i< lane.myprofiles.length; i++) {
	    sumD += lane.myprofiles[i].d * lane.screenedCntByProfile[i];
	    sumN += lane.screenedCntByProfile[i];
	}
	for(int i=0; i<lane.myprofiles.length; i++) {
	    if ((sumD + lane.myprofiles[i].d)/(sumN+1) >= lane.para.dAcceptable) {
		return i;
	    }
	}
	// not possible to keep the avg rate at the acceptable level, so
	// we just pick the last profile (which is supposed to have
	// the highest d)
	return lane.myprofiles.length-1;
    }

    PolicyP0(Queue[] lanes, double dAcceptable) {
    }


}
