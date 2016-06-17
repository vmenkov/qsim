package qsim;

import java.io.*;
import java.util.*;

/** Paul's policy P1: fixed assignment of lanes to policies
 */
class PolicyP1 extends Policy {
    final int chosenProfileID[];

    int chooseProfile(int mypos, Queue[] lanes) {
	return chosenProfileID[mypos];
    }

    PolicyP1(Queue[] lanes, double dAcceptable) {
	final int L = lanes.length;
	boolean done[] = new boolean[L];
	chosenProfileID = new int[L];
	double s = L* dAcceptable;
	for(int n = L; n> 0; n--) {
	    double dTarget = s / n;
	    int iBest=-1, pidBest=-1;
	    double costBest=0;
	    for(int i=0; i<L; i++) {
		if (done[i]) continue;
		int k = cheapestProfileAboveD(lanes[i].myprofiles, dTarget);
		if (k < 0) continue;
		double cost = lanes[i].myprofiles[k].avgCost();
		if (iBest<0 ||  cost < costBest) {
		    iBest = i;
		    pidBest = k;
		    costBest = cost;
		}
	    }
	
	    if (iBest >= 0) {
		System.out.println("Out of " + n + " lanes (dT="+dTarget+"), choose profile[" + iBest + "][" + pidBest + "], d=" + lanes[iBest].myprofiles[pidBest] + ", cost="+costBest);	
	    } else {
		// noone has a profile with sufficiently high d; so just pick the highest d of them all
		double dBest=0;
		for(int i=0; i<L; i++) {
		    if (done[i]) continue;
		    int k = highestDProfile(lanes[i].myprofiles);
		    double d = lanes[i].myprofiles[k].d;
		    if (iBest<0 || d > dBest) {
			iBest = i;
			pidBest = k;
			dBest = d;
		    }
		}		
		System.out.println("Out of " + n + " lanes (dT="+dTarget+"), choose profile[" + iBest + "][" + pidBest + "], d=" + lanes[iBest].myprofiles[pidBest] + ", d=" + dBest);
		if (dBest>=dTarget) throw new AssertionError();
	    }


	    chosenProfileID[iBest] = pidBest;
	    done[iBest] = true;
	    s -= lanes[iBest].myprofiles[pidBest].d;
	}
    }

    /** @return the index of the cheapest profile whose detection rate is
	at least dMin. If no such profile exists, -1 is returned.
     */
    private static int cheapestProfileAboveD(ScreenerProfile p[], double dMin) {
	if (p==null || p.length==0) throw new IllegalArgumentException("Empty profile list");
	int iBest = -1;
	double costBest = 0;
	for(int i=0; i<p.length; i++) {
	    if (p[i].d < dMin) continue;
	    double cost = p[i].avgCost();
	    if (iBest<0 ||  cost < costBest) {
		iBest = i;
		costBest = cost;
	    }
	}
	return iBest;
    }

    private static int highestDProfile(ScreenerProfile p[]) {
	if (p==null || p.length==0) throw new IllegalArgumentException("Empty profile list");
	int iBest = -1;
	double dBest = 0;
	for(int i=0; i<p.length; i++) {
	    if (iBest<0 || p[i].d > dBest) {
		iBest = i;
		dBest = p[i].d;
	    }
	}
	return iBest;
    }


}
