package qsim;

import java.io.*;
import java.util.*;

/** A Queue object simulates a single screening lane. */
public class Queue {

    static final long NEVER = Long.MIN_VALUE;

    final Parameters para;

    static public class ArrivalBase {
	final boolean threat;
	/** Simulation time of the customer's arrival */
	final long arrivalTime;
	ArrivalBase(boolean _threat, long _arrivalTime) {
	    threat = _threat;
	    arrivalTime = _arrivalTime;
	}
    }

    /** Encapsulates the information about a customer and his progress
	through the screening lane */
    static public class Arrival extends ArrivalBase {
	public String getLabel() {
	    return 
		transferred?
		(threat? "X" : "O"):
		(threat? "x" : "o");
	}
	
	/** The ScreenerProfile becomes associated with the Arrival
	    object once processing starts, and remains associated with
	    it as a permanent record */
	ScreenerProfile sp;
	/** Used as a reference to the screen profile in myprofiles[] */
	int spID; 
	boolean isBeingScreened() { return sp!=null; }
	/** How much time has elapsed since screening started on this customer,
	    as fraction of the maximum time that screening may take.
	    @returns a value in the range [0,1]
	*/
	double hasBeenScreenedForHowLong(long now) { 
	    return (double)(now - screenStartTime) / (double)sp.H;
	}
	/** Is set to true after screening has completed, if the arriving 
	    customer was a "threat" and was detected as such */
	boolean detected=false;
	long screenStartTime;
	/** Expected time of screening completion. (Is set when screening starts) */
	long screenEndTime = -1;
	Arrival(boolean _threat, long _arrivalTime) {
	    super( _threat, _arrivalTime);
	}
	Arrival(ArrivalBase b) {
	    super( b.threat, b.arrivalTime);
	}
	/** This will be set to true if the customer has been ever transferred
	    between lanes */
	boolean transferred = false;
	public String toString() {
	    return "["+getLabel()+"] Arrives at " + arrivalTime + 
		(sp==null ? ", never scanned" :
		 ", scanned at [" +  screenStartTime + " : " + screenEndTime + 
"]");
	}
    }

    /** All profiles supported by this lane's screening device */
    ScreenerProfile myprofiles[];

    /** Includes those waiting in line to be processed. The first
	element of the list may be currently being processed */
    LinkedList<Arrival> waiting = new LinkedList<Arrival>();
    int waitingCnt() { return waiting.size(); }

    /** Different ways of measuring the queue length: with or without the 
	customer currently being screeened */
    synchronized double queueLength() { 
	double w = waiting.size(); 
	Arrival a = waiting.peek();
	if (para.countCurrentlyScreened==Parameters.CountCurrentlyScreened.YES || w==0 || !a.isBeingScreened()) {
	} else if (para.countCurrentlyScreened==Parameters.CountCurrentlyScreened.NO) {
	    w = w-1;
	} else if (para.countCurrentlyScreened==Parameters.CountCurrentlyScreened.PARTIALLY) {
	    long now = parent.getNow();
	    double t = a.hasBeenScreenedForHowLong(now);
	    if (t<0 || t>1) throw new AssertionError("Invalid screening time detected: " + t + " for customer "+ a +"; now=" + now); 
	    w = w-t;
	}
	return w;
    }
 
    /** This parameter controls how frequently a working screening
	lane can accept customers from nearby broken lanes. (1
	transfer accepted for each ownPatronCnt0 customers
	screened). */
    final static int ownPatronCnt0 =2;
    /** How many customers we will screen before taking a customer from an
	adjacent broken lane */
    int ownPatronCnt=0;
    /** From which broken lane (this+1 or this-1?) did we take a
	customer last time?  (Sometimes the value can go beyond +1 or
	-1, if there are several broken lanes next to each other)  */
    int lastBrokenLaneDelta= 0;

    /** Those whose processing has been completed already */
    Vector<Arrival> completed = new Vector<Arrival>();
    /** Same as allCnt */
    int doneCnt() { return completed.size(); }

    /** Back link to the main simulator object */
    private Qsim parent;

    /** Link to other queues (used for getting customers from adjacent
	broken lanes) */
    private final Queue[] allQueues;
    /** This lane's position in the array of all lanes */
    private final int mypos;

    /**
       @param _allQueues Link to the already allocated array of
       all Queue objects in the simulator. This is needed to access
       adjacent broken lanes.
       @param _mypos Where this queue sits in allQueues
     */
    Queue(Parameters _para, long now, Qsim _parent, int _mypos,
	  StaticArrivalSchedule _sched) {	
	para = _para;
	parent = _parent;
	allQueues = parent.lanes;
	mypos = _mypos;
	sched = _sched;

	if (para.profiles==null || para.profiles.length==0) throw new IllegalArgumentException("No screening profile is defined in the parameter set!");

	if (para.profileIndexes[mypos]==null) {
	    myprofiles = para.profiles;
	} else {
	    myprofiles = new ScreenerProfile[para.profileIndexes[mypos].length];
	    for(int j=0; j<myprofiles.length; j++) {
		myprofiles[j] = para.getProfile(mypos,j);
	    }
	}

	pending = nextArrival(now);

	screenedCntByProfile = new int[myprofiles.length];
	currentProfileID = 0;
    }

    /** The one not in queue yet.  */
    Arrival pending = null;

    private boolean broken=false;

    public boolean isBroken() { return	broken;    }
    public void setBroken(boolean b) {	broken = b;    }

    /** How many customers have been generated by the arrival generator */
    int genCnt=0;
    int arvCnt=0;

    /** Counting screened customers (all; detected threat; missed threat) */
    int allCnt=0, detectedCnt=0, missedCnt=0;

    /** Points to a position in myprofiles */
    int currentProfileID = 0; //new ScreenerProfile(10,15,20,0.75);

    /** How many customers have been screened by each profile? These
	values sum to doneCnt */
    int[] screenedCntByProfile;

    synchronized boolean isIdle() {
	Arrival b = waiting.peek(); // the one being screened now 
	return (b == null || !b.isBeingScreened());
    }

    /** Used for arrival list generation */
    final long nextArrivalTime(long now) {
	double p = Qsim.gen.nextDouble();
	double t = -Math.log(1-p)/para.lambda;
	return now + (long)t;
    }

    /** This is used if we're using a pre-computed arrival 
	schedule, loaded from a file. */
    StaticArrivalSchedule sched = null;
    
    /** Switch this queue to using a pre-created (static) arrivals 
	schedule (probably, read from a file), instead of
	generating arrival times dynamically. */
    /*
    void setArrivals(StaticArrivalSchedule _sched) {
	sched = _sched;
    }
    */
    boolean finishedGeneration() {
	return (sched != null) ? !sched.hasNext() :
	    (para.nGenMax > 0 && genCnt >= para.nGenMax);
    }

    private boolean randomThreat() {
	return Qsim.gen.nextDouble() < para.fracBad;
    }

    /** Returns a newly generated Arrival object, or null (if all
	planned arrivals have already been generated) */
    Arrival nextArrival(long now) {
	if (finishedGeneration()) return null;
        Arrival a = (sched != null)? new Arrival( sched.next()) : 
	    new Arrival( randomThreat(), nextArrivalTime(now));
	genCnt ++;
	return a;
    }

    /** Prepares the list of para.nGenMax arrival times, to save in a file for
	use in a future run */
    ArrivalBase[] generateAllArrivalTimes() {
	if (para.nGenMax <= 0) throw new IllegalArgumentException("Needs to specify the number of customers (nGenMax) in the config file!");
	long t = 0;
	ArrivalBase[] w = new ArrivalBase[para.nGenMax]; 
	for(int i=0; i<para.nGenMax; i++) {
	    t =  nextArrivalTime(t);
	    w[i] = new ArrivalBase( randomThreat(), t);
	}
	return w;
    }


    /** @param b The currently processed customer, already popped out of the queue */
    private void completeScreening(Arrival b) {
	allCnt++;
	screenedCntByProfile[ b.spID] ++;

	if (b.threat) {
	    b.detected = !broken && b.sp.detects();
	    if (b.detected) detectedCnt++;
	    else missedCnt++;			      
	}
	completed.addElement(b);
	if (ownPatronCnt>0) ownPatronCnt--;
    }


    /** Chooses a suitable screening profile, pursuant to the simulator's
	profile selection policy. The assumption is that the profiles
	are listed in the order of increasing d, so we pick the first
	profile that's acceptable.
     */
    private int chooseProfile() {
	return parent.policy.chooseProfile(mypos, allQueues);
    }

    /** Labels the specified customer as one undergoing screening. 
	@param now Simulated time when screeening starts
     */
    private void startScreening(Arrival b, long now) {
	if (broken) return;
	
	if (myprofiles==null || myprofiles.length==0) throw new IllegalArgumentException("No screening profiles are available for this lane");
	currentProfileID = chooseProfile();
	b.sp = myprofiles[b.spID=currentProfileID];
	b.screenStartTime = now;
	b.screenEndTime = now + (long)b.sp.serviceTime();
    }

 
  /** Looks up the next event in this lane that needs to be attended to.
      @return Time of that event, or NEVER if none is expected
   */
    synchronized long  findNextActivityTime() {       
	Arrival b = waiting.peek(); // the one being screened now 
	if (b != null && b.isBeingScreened() && (pending==null || b.screenEndTime<=pending.arrivalTime)) {
	    // the next activity is the completion of the current screening
	    return b.screenEndTime;
	} else if (pending !=null) {
	    // the next activity is the arrival of the next customer
	    return pending.arrivalTime;
	} else {
	    return NEVER;
	}
    }

    /** Who's in line? */
    public String describeQueue() {
	StringBuffer b=new StringBuffer();
	b.append("Gen="+genCnt+ "; Arv="+arvCnt+"; Done " + allCnt +", detected=" + detectedCnt+ " missed=" +missedCnt);
	b.append("\t");
	b.append(describeQueue2(false));
	return b.toString();
    }
    
    private String statusLabel() {
	//	String status = broken? "X" : currentProfileID==0? "F" : 
	//	    currentProfileID==1? "S" : ""+(currentProfileID+1);		
	String status =  broken? "X" : "P" + (currentProfileID+1);
	return status;
    }

    public String describeQueue2(boolean html) {
	StringBuffer b=new StringBuffer();
	//	String status = broken? "X" : currentProfileID==0? "F" : 
	//	    currentProfileID==1? "S" : ""+(currentProfileID+1);
	b.append(statusLabel() + ":" + ownPatronCnt + " ");
	for(Arrival a: waiting) {
	    String z=a.getLabel();
	    if (html && a.isBeingScreened()) z= "<b><u>" + z + "</u></b>";
	    b.append(z);
	}
	return b.toString();
    }

    /** Checks if this lane wants to give its first customer to 
	another lane. For this to happen, this lane should be broken,
	should have customers in its queue, and the first customer
	should not be being processed at this lane (as the last customer
	who is still processed by the already-broken lane).
	If it does, removes the first waiting customer from the
	queue and returns it.
    */
    synchronized Arrival giveOnePatron() {
	if (!broken || waitingCnt()==0) return null;
	Arrival a = waiting.peek();
	if (a.isBeingScreened()) return null;
	return waiting.pop();
    }

    /** 
	Transfers a customer from a neighbor's broken lane to one's
	own queue, if appropriate. The conditions checked for this
	lane are as follows:
	<ul>
	<li>The lane should not be broken
	<li>The lane should not be processing a customer right now
	<li>It must obey the rules for interleaving "imported" and "own" customers.
	</ul>
	
	@return true if a customer has indeed been transferred from a neighboring lane
    */
    private synchronized boolean helpNeighbors() {
	if (broken) return false;
	if (waitingCnt()>0 && waiting.peek().isBeingScreened()) return false;
	if (waitingCnt()>0 && ownPatronCnt>0) return false;

	int delta[] =  deltaList(lastBrokenLaneDelta);
	for(int j=0; j<delta.length; j++) {
	    int bPos = mypos + delta[j];
	    Arrival a = allQueues[bPos].giveOnePatron();
	    if (a != null) {
		waiting.push(a);
		a.transferred = true;
		lastBrokenLaneDelta = delta[j];
		ownPatronCnt = ownPatronCnt0;
		return true;
	    }
	}
	return false;
    }
  
    /** Returns an array of deltas, i.e. pointers to nearby lanes from
	which we can try getting stuck customers. */
    private int[] deltaList(int delta0) {
	int direction = (delta0>=0? 1: -1);
	int n1 = parent.numberOfLanesToHelp(mypos, direction);
	int n2 = parent.numberOfLanesToHelp(mypos, -direction);
	int z[] = new int[n1+n2];
	int k=0;
	// continue in the original direction
	for(int j=Math.abs(delta0)+1; j<=n1; j++) {
	    z[k++] = j * direction; 
	}
	// change direction
	for(int j=1; j<=n2; j++) {
	    z[k++] =  -j * direction; 
	}
	// change direction again
	for(int j=1; j<=Math.abs(delta0) && j<=n1; j++) {
	    z[k++] =  j * direction; 
	}
	return z;
    }
    

    /** If a customer arrives right now, handle that 
	@return true if an arrival indeed took place
     */
    synchronized boolean handleImmediateArrival(long now) {
	if (pending == null || now < pending.arrivalTime) return false;
	if (now > pending.arrivalTime)  throw new IllegalArgumentException("Oy vey, it's t=" + now + " already, and we have missed the arrival time for A= " + pending);
	waiting.add(pending);
	arvCnt++;
	pending = nextArrival(now);
	return true;
   }

    /** Completes a screening if it's happening right now */
    synchronized boolean handleImmediateScreeningCompletion(long now) {
       Arrival a = waiting.peek();
       if (a == null || !a.isBeingScreened() || a.screenEndTime > now) return false;
       if (a.screenEndTime < now)  throw new IllegalArgumentException("Oy vey, it's t=" + now + " already, and we have missed the screening completion time for A=" + pending);
       waiting.pop();
       completeScreening(a);
       return true;
    }

    /** If this lane is idle but there is work to do (here or at a
	broken neighbor), engage it into this work
	@return true if a new screening indeed starts
     */
    synchronized boolean handleIdle(long now) {
	if (broken) return false;
	helpNeighbors(); // insert a customer from a neighbor, if appropriate
	Arrival a = waiting.peek();
	if (a==null || a.isBeingScreened()) return false;
	startScreening(a, now);	
	return true;
    }

}
