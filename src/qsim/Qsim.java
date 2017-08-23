package qsim;

import java.io.*;
import java.util.*;
import java.text.*;

/** The main queue simulator class; simulates the behaviour of a battery of L lines */
public class Qsim {

    final int L;
    final Parameters para;
    final public Queue lanes[];
    /** The local simulated time (sec) */
    private long now;
    /** The local simulated time (sec) */
    public long getNow() { return now; }
    /** How many seconds of simulated time per 1 sec of real wall clock time.	
	The value of 0 means "infinity", i.e. run as fast as the hardware
	allow.
     */
    double scale=1.0;

    /** Controls the choice of screener profiles */
    Policy policy;

    /**@param _para Contains system parameters and simulation parameters
       @param _now Sets the times (usually, to 0)
     */
    public Qsim(Parameters _para, long _now, StaticArrivalSchedule[] ss)  {
	para = _para;
	L = para.L;
	now = _now;
	lanes = new Queue[L];
	for(int i=0; i<L; i++) {
	    StaticArrivalSchedule s = (ss==null)?null: ss[i];
	    lanes[i] = new Queue(para, now, this, i, s);
	}
	System.out.println(showProfiles());
	policy = Policy.createPolicy(para, this);
    }

    String showProfiles() {
	StringBuffer b= new 	StringBuffer();
	for(int i=0; i<L; i++) {
	    b.append("Lane["+(i+1)+"] profiles:");
	    for(ScreenerProfile p: lanes[i].myprofiles) {
		b.append(" " + p);
	    }
	    b.append("\n");
	}
	return b.toString();
    }

    
    private boolean stopRequested = false;
    private boolean attentionRequested = false;

    /** Called from GUI if the user clicks the STOP button */
    public synchronized void requestStop() {
	stopRequested = true;
    }
   
    /** Atomically resets the stop flog, and returns the old value */
    private synchronized boolean resetStopRequest() {
	boolean z = stopRequested;
	stopRequested = false;
	return z;
    }
     

    /** Called from GUI if the user has changed the system state somehow,
	e.g. by fixing a broken screening device; this means that 
	we need to recheck at least that device's status */
    public synchronized  void requestAttention() {
	attentionRequested = true;
    }

    /** History data which can be printed to a file at the end of run,
	or used to draw a chart showing the crowd size as a function
	of time.
	logData[i] = (t[i], crowd_size[i]))
     */
    private Vector<long[]> logData = new Vector<long[]>();

    synchronized private void logC(long t, int c) {
	int n=logData.size();
	if (n>0 && logData.elementAt(n-1)[0]==t) {
	    logData.elementAt(n-1)[1] = c;
	} else {
	    logData.add( new long[] {t, c});
	}
    }

    /** Saves the accummulated log information for this run.
     @param f File into which the data will be written */
    public void saveLog(File f) throws IOException {
	PrintWriter w = new PrintWriter(new FileWriter(f));
	for(long[] q: logData) {
	    w.println("" + q[0] + " " + q[1]);
	}
	w.print("caught={");
	for(int i=0; i<lanes.length; i++) {
	    w.print((i>0? ", " :"") + (i+1) + ": " + lanes[i].detectedCnt);
	}
	w.print("} missed={");
	for(int i=0; i<lanes.length; i++) {
	    w.print((i>0? ", " :"") + (i+1) + ": " + lanes[i].missedCnt);
	}
	w.println("}");
	w.close();
    }

    /** This may be subclassed to provide necessary functionality */
    static public class ProgressDisplay {
	/** Displays stats for one lane */
	public void display(int j, Queue q) {}
	/** Displays aggregate stats for all lanes (single line of text) */
	public void showSummary(String s) {}
	/** Displays additional stats (multi-line text) */
	public void showStats2(String s) {}
    }

    ProgressDisplay display = new ProgressDisplay();
    public void setProgressDisplay( ProgressDisplay d) { 
	display = d;
    }


    /** Gradually advances the simulated time (variable "now") to be 
	equal to now1, at the appropriate speed (the simulated time to 
	wall clock time ratio). May return earlier, if the user has
	clicked the STOP button or effected some state-changing operations
	while we were sitting here.
	@param msec0 The wall clock time (msec) when "now" was equal to "now1"
	@return true if the user has requested simulation stop
     */
    private void goSlow(long now0, long now1, long msec0) {
	double speed = Options.getSpeed();
	if (speed<=0) { // a non-interactive run
	    now = now1;
	    return;
	}
	long msecTarget = msec0 + (long)((now1 - now0) * 1000.0 / speed);
	long lastMsecShow= -1;
	while(now < now1) {
	    long msecNow = (new Date()).getTime();
	    if (lastMsecShow<0) lastMsecShow=msecNow;

	    if (msecNow - lastMsecShow  >= 1000) {
		display.showSummary(" t=" + now + ", " + summaryText());
		display.showStats2(policy.report(lanes));
		lastMsecShow = msecNow;
	    }


	    now = Math.min(  Math.max(now, now0 + (long)((msecNow-msec0)*speed / 1000)), now1);
	    if (stopRequested || attentionRequested) return;
	    if (msecNow >= msecTarget) {
		now = now1;
		return;
	    }
	    long sleepMsec = Math.min(msecTarget - msecNow, 100);
	    try {		    
		Thread.sleep(sleepMsec);
	    } catch ( InterruptedException ex) {
	    }

	}
    }

    /** When is the time we need to attend to an event in some lane?
	@return Time of that event, or Queue.NEVER if none is expected
    */
    private long  findNextActivityTime() {       
	long minT = Queue.NEVER;
	for(int j=0; j< lanes.length; j++) {
	    Queue q=lanes[j];
	    long t = q.findNextActivityTime();
	    if (t==Queue.NEVER) continue;
	    if (minT==Queue.NEVER || t < minT) minT = t;
	}
	return minT;
    }

    /** The main simulation loop.

	@param T stop if simulation time reaches T. Use a negative
	number to mean, "don't stop until running out of users.
    */
    public void simulate(final long T) {
	stopRequested = false;

	final long msec0 = (new Date()).getTime();
	final long now0 = now;

	long lastPrint =0;
	while(T<0 || now<=T) {
	    if (resetStopRequest()) break;
	    if (attentionRequested) attentionRequested = false;

	    boolean mustRedisplay[] = new boolean[lanes.length];
	    // Handle all arrivals that may be happening right now
	    for(int j=0; j< lanes.length; j++) {
		mustRedisplay[j] = lanes[j].isBroken();
		if (lanes[j].handleImmediateArrival(now))
		    mustRedisplay[j] = true;
	    }

	    // Handle all completions that may be happening right now
	    for(int j=0; j< lanes.length; j++) {
		if (lanes[j].handleImmediateScreeningCompletion(now))
		    mustRedisplay[j] = true;
	    }

	    // Give work to all idle screeners for which work can be found
	    for(int j=0; j< lanes.length; j++) {
		if (lanes[j].handleIdle(now)) 
		    mustRedisplay[j] = true;
	    }
	
	    // Redisplay all affected or potentially affected lanes
	    for(int j=0; j< lanes.length; j++) {
		if (mustRedisplay[j]) display.display(j, lanes[j]);
	    }

	    // Display the current state
	    display.showSummary(" t=" + now + ", " + summaryText());
	    display.showStats2(policy.report(lanes));
	    logC(now, sumLen());

	    if (now / 100 > lastPrint / 100) {
		System.out.println("At t=" + now);
		describe();		
		lastPrint = now;
	    }

	    if (T>=0 && now >= T) break;

	    // When is the next time we need to check the system?
	    long nextNow =  findNextActivityTime();
	    if (nextNow==Queue.NEVER) {	
		// No internally-driven activity will happen anymore.
		// Any queued customers may only exist in broken
		// lanes, and are immobile.	
		System.out.println("No one is being processed or arriving anymore; Done!");
		break;
	    }

	    if (T>0 && nextNow>T) nextNow = T;

	    // slowly advance clock to the next event, or until the user
	    // clicks a button
	    goSlow(now0, nextNow, msec0);    
	}

	System.out.println("END: At t=" + now);
	describe();
	display.showSummary("t=" + now + ", " + summaryText());
    }

   
    void describe() {
	for(int i=0; i<L; i++) {
	    System.out.println("" + i + "\t" + lanes[i].describeQueue());
	}
	System.out.println(policy.report(lanes));
    }

    String summaryText() {
	int sumLen = 0, sumArv=0, sumDone=0, sumDet=0, sumMissed=0;
 	for(int i=0; i<L; i++) {
	    sumLen += lanes[i].waitingCnt();
	    sumArv += lanes[i].arvCnt;
	    sumDone += lanes[i].allCnt;
	    sumDet  += lanes[i].detectedCnt;
	    sumMissed  += lanes[i].missedCnt;
	}

	return "Arrivals " + sumArv +"; Queued " + sumLen + ", done "+ sumDone + ", caught " + sumDet + ", missed " + sumMissed;

    }


    private int sumLen() {
	int sumLen = 0;
 	for(int i=0; i<L; i++) {
	    sumLen += lanes[i].waitingCnt();
	}
	return sumLen;	
    }

    static final public String version = "0.6.1";

    static Random gen = new Random(0); 

    /*
    void setArrivals(StaticArrivalSchedule[] ss) {
	for(int i=0; i<L; i++) {
	    lanes[i].setArrivals(ss[i]);
	}
    }
    */

    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	System.out.println("Usage: java [options] qsim.Qsim config.dat [output-dir-name]");
	System.out.println("Options:");
	System.out.println(" [-DT=-1] -- will stop after simulation time reaches that point. (-1 means don't stop until running out of customers)");
	System.out.println(" [-Dspeed=100] -- simulation speed (units of sim time per real-clock second). (0 means run fast)");
	System.out.println(" [-Druns=1] -- number of simulation runs");
	System.out.println(" [-Dseed=0] -- seed for the random number generator");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    /** Also, each run should have its configuration file, and any
     * other information unique to that run persisted in a specially
     * created directory whose name consists of the date and time
     * string, with any informative name string appended thereto. For
     * example:
     
     27-Mar-2016-21_33-paul001

     [Note that 21:13, the time, is reformatted because some ftp services cannot cope with the ':'.]

     <p>I am using the format,
     out-qsim-yyyy-MM-dd_HH-mm-ss

    */
    static private String mkOutDirName() {
	return "out-qsim-" + odFmt.format( new Date());
    }
    
    static final DateFormat odFmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    /** Tries to create out dir, if needed */
    static void checkOutDir(File g) {
	if (g.exists()) {
	    System.out.println("Will write output files to existing dir " + g);
	} else {
	    boolean code = g.mkdirs();
	    if (!code) {
		System.out.println("Failed to create output directory " + g);
		System.exit(1);
	    }
	    System.out.println("Creating dir " + g + "; success=" + code);
	}
    }

    static public void main(String argv[]) throws IOException {
	Options.setInteractive(false);
	System.out.println("Qsim ver. " + version);

	int ja = 0;
	if (ja >= argv.length) usage();
	String configFileName = argv[ja++];

	String outDirName = mkOutDirName();
	if (ja < argv.length) outDirName =argv[ja++];

	System.out.println("Config file " + configFileName +"; output dir "+ outDirName);
	File f= new File(configFileName);
	if (!f.exists()) usage("File " + f + " does not exist!");
	ParseConfig ht = new ParseConfig(f);
	qsim.Parameters para = new qsim.Parameters(f);

	// The seed for the random number generator. 
	long seed = ht.getOptionLong("seed", 0);
	gen = new Random(seed);

	int runs = ht.getOption("runs", 1);
	if (runs < 1) usage();
	long T = ht.getOptionLong("T", -1);

	String arrivals = ht.getOption("arrivals", null);
	StaticArrivalSchedule[][] ss = new StaticArrivalSchedule[runs][];

	// Read all schedules in advance, to avoid later surprises
	if (arrivals != null) {
	    System.out.println("Will use pre-computed arrival schedules from directory " + arrivals + ", instead of generating arrival times dynamically");
	    File dir = new File(arrivals);
	    if (!dir.exists() || !dir.isDirectory()) usage(arrivals + " is not a directory");
	    
	    for(int k=0; k<runs; k++) {	 
		f = GenerateArrivals.arrivalFile(dir,k);
		if (!f.exists()) usage("File " + f +", which is supposed to contain arrival schedule for run no. " + k +", does not exist!");
		ss[k] = StaticArrivalSchedule.readFile(f, para.L);
	    }
	}


	File g = new File(outDirName);
	checkOutDir(g);
	

	System.out.println("Will carry out " + runs + " simulation run(s).");
	if (T>0) {
	    System.out.println("Each run will terminate when all customers have been processed, or at t="+T+", whichever happens sooner.");
	} else {
	    System.out.println("Each run will terminate when all customers have been processed.");
	}


	NumberFormat nf = new DecimalFormat("000");
	for(int k=0; k<runs; k++) {
	    System.out.println("---- Run " +k + " of " + runs);
	    long now = 0;       
	    Qsim qsim = new Qsim(para, now, ss[k]);

	    qsim.simulate(T);
	    f = new File(g, "queue-"+ nf.format(k)+".dat");
	    System.out.println("Saving log file " + f);
	    qsim.saveLog(f);
	}

    }

    /*
    Parameters readPara(File f) {
	Parameters para = new Parameters();
	para.read(f);
	return para;
    }
    */

    /*
    private boolean laneExistsAndIsBroken(int k) {
	return k>=0 && k<lanes.length && lanes[k].isBroken();
    }

    private boolean laneExistsAndIsWorking(int k) {
	return k>=0 && k<lanes.length && !lanes[k].isBroken();
    }
    */

    /** Checks whether Lane k is "helpless", i.e. broken and has no
	adjacent non-broken neighbor. 
	@return true if Lane k exists (not out of range) and is "helpless"
    */
    /*
    boolean laneIsHelpless(int k) {
	return laneExistsAndIsBroken(k) &&
	    !laneExistsAndIsWorking(k-1) &&
	    !laneExistsAndIsWorking(k+1);
    }
    */

    /** How many broken lanes does Lane mypos (which is assumed to be
	a working lane) need to help? (This depends on whether there is
	a capable "helper" on the other side).  If there is an odd
	number of broken lanes between this lane and the next working lane,
	both working lanes will be helping the middle broken lane.
	@param direction +1 or -1
    */
    int numberOfLanesToHelp(int mypos, int direction) {
	for(int n=0; true; n++) {
	    int k = mypos + (n + 1) * direction;
	    if (k<0 || k>=lanes.length) return n;
	    if (!lanes[k].isBroken()) return (n+1)/2;
	}
    }

    /** Generates and saves customer arrival schedule, for future use.
	This method first produces an arrival schedule for each list,
	and then merges them, arranging them in chronological order
    */
    void generateArrivalSchedule(File f) throws IOException {
	PrintWriter pw = new PrintWriter(new FileWriter(f));
	Queue.ArrivalBase[][] a = new Queue.ArrivalBase[L][];
	for(int j=0; j<L; j++) {
	    a[j] = lanes[j].generateAllArrivalTimes();
	}
	pw.println("# This file contains customer arrival times. Each line is inb the format");
	pw.println("# t j threat");
	pw.println("# Where t is the time, j is the (1-based) lane number, threat=0 or 1");
	pw.println("# The data are for L="+L+" lanes, with " + para.nGenMax + " customers in each one");

	int ptr[] = new int[L];
	while(true) {
	    int jmin=-1;
	    long mint=-1;
	    for(int j=0; j<L; j++) {
		if (ptr[j] >= a[j].length) continue;
		if (jmin<0 || a[j][ptr[j]].arrivalTime < mint) {
		    mint = a[j][ptr[jmin = j]].arrivalTime;
		}
	    }
	    if (jmin < 0) break; // done
	    Queue.ArrivalBase x =  a[jmin][ptr[jmin]++];
	    pw.println(""+ x.arrivalTime+"\t"+ (jmin+1)+"\t"+ (x.threat? 1:0));
	}
	pw.close();
    }
   
}
