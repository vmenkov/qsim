package qsim;

import java.io.*;
import java.util.*;
import java.text.*;

/** Paul's policy P2 provides for dynamic assignment of lanes to
    screening profiles.  The way P2 is implemented is as follows:

<p>
(1) Each group of "identically equipped lanes" (i.e. lanes that have
the same set of profiles) is treated absolutely separately from other
lanes.

<p>
(2) Given the "target detection rate" dAcceptable, the two
"bracketing" profiles (the "lower" or "Faster" one with d <=
dAcceptable, and the "higher"  or "Slower" one with d>=dAcceptable)
are identified in each group of "identically equipped lanes". A real
value f is computer as the fraction of customers that would need to be
processed by the higher profile in order for the average notional
detection rate to be equal to dAcceptable,

<p>
(3) Every time a lane is about to start processing a customer, the
profile to use is chosen as follows:

<ul>
<li> (3.a) We compute the number N of currently operational (non-broken)
lanes in the appropriate lane group. Then, based on the [generally
flawed!] assumption that the number of lanes can serve as the proxy
for the number of customers, we compute the integer number  N_h =
round( N * f), to serve as the number of lanes that ought to be
assigned to the higher profile.

<li>
(3.b) We do NOT look at the profiles to which the lanes are currently
assigned. Rather, we identify the N_h lanes that currently have the
shortest queues. (In practices, ties are common here, as multiple
lines may have the same queue length; a randomized tie-breaking
algorithm would probably be appropriate,  but I don't bother with such
niceness), and if the lane in question is one of these N_h lanes, it
is told to use the "higher" (slower) profile for this customer.
</ul>

<p>
If no lanes ever break, then N_h, of course, stays constant throughout
the run, as per your original writeup. However, the number of lanes
that are actually assigned to the "lower" (Faster) profile at any
given time is often smaller than N-N_h, as it will be discussed below.

<p>The way queue length is measured is affected by the configuration
parameter "policy.countCurrentlyScreened".

 */
class PolicyP2 extends Policy {

    /** A Group instance describes all lanes with the same set of profiles. */
    static class Group {
	final double dAcceptable;
	/** Number of lanes that have this set of profiles */
	int n;
	/** All profiles supported by this lane's screening device. This is just a pointer to the array in one of the lanes. */
	final ScreenerProfile[] myprofiles;

	boolean useLowest=false, useHighest=false;
	/** Pointer to the low bracket */
	int lowBracketID;
	/** The two profiles, "lower" (0) and "higher" (1), bracketing the 
	    desired detection rate. */
	ScreenerProfile bracket(int k) {
	    return myprofiles[ lowBracketID + k];
	}

	Group(Queue lane, double d) {
	    myprofiles = lane.myprofiles;	    
	    n = 1;
	    dAcceptable = d;
	    setBrackets();
	}

	/** Does o[] contain the same set of profiles as myprofiles[]?
	    We carry shallow comparison here, because of the way 
	    profile lists are stored in Queue objects (shallow copy).
	 */
	private boolean sameProfiles(ScreenerProfile[] o) {
	    if (o.length != myprofiles.length) return false;
	    for(int i=0; i<o.length; i++) {
		if (o[i] != myprofiles[i])  return false;
	    }
	    return true;
	}

	/** Checks whether a given screening lane can be enrolled into this
	    group, and if so, does it. */
	boolean tryToInclude(Queue lane) {
	    if (!sameProfiles(lane.myprofiles)) return false;
	    n++;
	    return true;
	}

	/** Identifies the 2 "bracket" profiles bracketing the desired
	    detection rate dAcceptable. This is called from the 
	    Group constructor.
	 */
	private void setBrackets() {
	    lowBracketID = -1;
	    for(int i=0; i<myprofiles.length; i++) {
		if (i>0 && myprofiles[i].d <=  myprofiles[i-1].d) 
		    throw new IllegalArgumentException("Profiles not in order of increasing d: " + myprofiles[i-1].d + " is followed by " + myprofiles[i].d);
		if (myprofiles[i].d<= dAcceptable) lowBracketID=i;
	    }	    
	    if (lowBracketID < 0) { // all profiles are "higher" than the target
		useLowest=true;
	    } else  if (lowBracketID == myprofiles.length-1) {
		// all profiles are "lower" than the target
		useHighest=true;
	    }
	}

	/** What percentage of lanes should be allocated to the
	    "higher" profile in pure (static) P2 */
	double fracUseHigher;

	void p2assign() {
	    int p2higherCnt;
	    if (useLowest) {		
		p2higherCnt=n;
		fracUseHigher = 1.0;
	    }
	    else if (useHighest) {
		p2higherCnt=0;
		fracUseHigher = 0.0;
	    }  else {
		double d1 = myprofiles[lowBracketID].d;
		double d2 = myprofiles[lowBracketID+1].d;
		fracUseHigher = (dAcceptable-d1) / (d2-d1);
		p2higherCnt = (int)Math.round( n * fracUseHigher);
		System.out.println("P2: bracket d["+lowBracketID+"]=d1=" + d1 +", d2=" + d2 + "; (n=" + n+") * (f=" +fracUseHigher +") =  p2higherCnt=" +p2higherCnt);
	    }
	    double dExpected =
		((p2higherCnt<n? myprofiles[lowBracketID].d *(n-p2higherCnt):0) +
		 (p2higherCnt>0? myprofiles[lowBracketID+1].d * p2higherCnt : 0))/n;
		
	    System.out.println("P2: Out of " + n + " lanes, we'll set " + p2higherCnt + " lanes to the higher profile; f=" +fracUseHigher +"; dAcceptable=" +dAcceptable+", dExpected=" + dExpected );
	}
       
	static class QueueComparatorByLength implements Comparator<Queue> {
	    /** Normal -- ascending -- order */
	    public int compare(Queue o1, Queue o2) {
		double x = o1.queueLength() - o2.queueLength();
		return (x<0? -1 : x>0? 1 : 0);
	    }    
	}

	static private final QueueComparatorByLength cmp= new QueueComparatorByLength();
	
	/** Lists non-broken lanes */
	private static Queue [] selectWorkingLanes(Queue[] lanes) {
	    Vector<Queue> v = new Vector<Queue>(lanes.length);
	    for(Queue q: lanes) {
		if (!q.isBroken()) v.add(q);
	    }
	    return (Queue [])v.toArray(new Queue[v.size()]);
	}

	/** Does Lane no. mypos has a short queue? "Short" means one
	    within fraction f (0 &le; f &le; 1) of all queues. */
	static boolean hasOneOfTheShorterQueues(int mypos, Queue[] lanes, double f) {
	    Queue [] z = selectWorkingLanes(lanes);
	    Arrays.sort(z, cmp);
	    int p2higherCnt = (int)Math.round( z.length * f);
	    int rank = 0; // the number of queues shorter than this one
	    while(z[rank]!=lanes[mypos]) rank++;
	    return rank < p2higherCnt;
	}

	/** Policy is choosen independently within each group. The
	    shortest-queue lanes are given the "higher" (slow)
	    profile. */
	int chooseProfile(int mypos, Queue[] lanes) {
	    if (useLowest) return 0;
	    if (useHighest) return myprofiles.length-1;
	    boolean isShort=hasOneOfTheShorterQueues(mypos,lanes,fracUseHigher);
	    int d = isShort ? 1 : 0;
  	    return lowBracketID+d;
	}

	static final NumberFormat fmt = new DecimalFormat("0.000");

	/** Reports on this group's lanes' operations so far */
	String report(Queue[] lanes, Group[] lane2group) {
	    StringBuffer b = new StringBuffer();
	    int doneCnt[] = new int[myprofiles.length];

	    double sw=0, sw2=0;  // avg queue length and std deviation
	    int nWorking=0;
	    for(int i=0; i<lanes.length; i++) {
		if (lane2group[i] != this) continue;
		for(int j=0; j<myprofiles.length; j++) {
		    doneCnt[j] += lanes[i].screenedCntByProfile[j];
		}

		if (!lanes[i].isBroken()) {
		    double w = lanes[i].queueLength();
		    sw += w;
		    sw2 += w*w;
		    nWorking++;
		}
	    }

	    if (nWorking>0) {
		sw /= nWorking;
		sw2 /= nWorking;
		sw2 = Math.sqrt(sw2 - sw*sw);
	    }

	    double s = 0;
	    int sumDone = 0;
	    for(int j=0; j<doneCnt.length; j++) {
		s += myprofiles[j].d * doneCnt[j]; 
		sumDone += doneCnt[j];
	    }
	    double dAvg = s/sumDone;
	    b.append("Screened {");
	    
	    for(int j=0; j<doneCnt.length; j++) {
		if ((j< lowBracketID || j>lowBracketID+1) && doneCnt[j]==0) continue;
		String label = (j==lowBracketID ? "F": j==lowBracketID+1 ? "S": ""+j);
		b.append( " " + label + ":" + doneCnt[j]);
	    }
	    b.append("}.");
	    //"Target rate " + fmt.format(dAcceptable) + ", "
	    b.append("Avg actual rate " + fmt.format(dAvg));
	    b.append("\n");
	    b.append("Avg queue length = " + fmt.format(sw) + " +- " + fmt.format(sw2));

	    return b.toString();
	}
    }

    final Group[] groups;
    /** Pointers into groups[] */
    private final Group[] lane2group;

    int chooseProfile(int mypos, Queue[] lanes) {
	return lane2group[mypos].chooseProfile(mypos, lanes);
    }

    /** Invokes the constructor of an appropriate Group class or
	subclass. Overridden by derived classes as needed. */
    Group groupConstructor(Queue lane, double dAcceptable) {
	return new Group(lane, dAcceptable);
    }

    /** Divides list of lanes into groups of "equivalent" (identically
	equipped) ones. Used in the constructor. */
    private Group[] makeGroups(Queue[] lanes, double dAcceptable,
			       Group [] lane2group) {
	//System.out.println("PolicyP2.makeGroups: " + this.getClass());
	Vector<Group> v= new Vector<Group>();
	for(int i=0; i<lanes.length; i++) {
	    boolean found = false;
	    for(Group g: v) {
		if (g.tryToInclude(lanes[i])) {
		    lane2group[i] = g;
		    found=true;
		    break;
		}
	    }
	    if (found) continue;
	    Group g = groupConstructor(lanes[i], dAcceptable);
	    v.add(g);
	    lane2group[i] = g;
	}
	System.out.println("P2: Found " + v.size() + " groups of identically-equipped lanes");
	return (Group[])v.toArray(new Group[0]);
    }

    PolicyP2(Queue[] lanes, double dAcceptable) {
	//	System.out.println("Constructor: " + this.getClass());
	final int L = lanes.length;
	lane2group = new Group[L]; 
	groups = makeGroups(lanes, dAcceptable, lane2group);
	for(Group g: groups) {
	    g.p2assign();
	}
    }
    
    /** Reports this group's performance stats so far */
    String report(Queue[] lanes) {
	StringBuffer b = new StringBuffer(super.report(lanes));
	//b.append(". Count currently screened customer in qlen: " + countCurrentlyScreened);
	b.append(". Target rate " + Group.fmt.format(groups[0].dAcceptable));
	for(int i=0; i<groups.length; i++) {
	    b.append("\n");
	    b.append("Group "+(i+1)+" ("+groups[i].n+" lanes); " + groups[i].report(lanes, lane2group));
	}
	return b.toString();
    }  
}
