package qsim;

import java.io.*;
import java.util.*;

/** 
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
