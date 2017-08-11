 package qsim;

import java.io.*;
import java.util.*;

/** Model parameters and algorithm parameters */
public class Parameters {

    /** Thrown when an error is found in the config file */
    public static class ParseException extends IOException {
	public ParseException(String msg) { super(msg); }
	public ParseException(File f, LineNumberReader r, String msg) {
	    super("File="+f+", line=" + r.getLineNumber() + ": " + msg);
	}

    }

    /** The available policies for the selection of screener profiles */
    enum Policy {
	P0, P1, P2, P3;
    };

    /** The legal values for the configuration parameter
	"policy.countCurrentlyScreened", which is used to control how
	the customer that's currently being screened is counted for
	the purposes of measuring the queue length */
     enum CountCurrentlyScreened {
	YES, NO, PARTIALLY;
    }
    
    /** Number of screening lanes */
    final public int L;

    
    /** Customer arrival rate (per lane); Poisson distribution */
    final public double lambda;
    /** The total number of arrivals to generate in each lane, after which the
	generator stops. If 0, never stop generating arrivals. */
    final public int nGenMax;
    /** The fraction of "bad" customers among the arrivals */
    final public double fracBad;
    final public double dAcceptable;
    /** All profiles that can be available to any screening
    device. Each individual lane may have all or some of these
    profiles at its disposal. */
    final ScreenerProfile[] profiles;

    /** For Lane j, profileIndexes[j] contains the list of profiles
	(zero-bases indexes into profiles[]) supported by that lane. If
	profileIndexes[j]==null, it means that Lane j supports all
	profiles in profiles[].
     */
    final int[][] profileIndexes;

    /** The screening profile selection policy */
    final Policy policy;

    /** How the customer that's currently being screened is counted
	for the purposes of measuring the queue length */
    final CountCurrentlyScreened countCurrentlyScreened;

    /** Looks up the j-th profile for Lane k */
    ScreenerProfile getProfile(int k, int j) {
	return profileIndexes[k]==null? profiles[j] :
	    profiles[ profileIndexes[k][j]];
    }
    
    /** Initializes the parameters from a file or from system properties.
	@param f file object; may be null
	@throws  IllegalArgumentException If the data are not like expected
    */
    public Parameters(File f) throws IOException {
	ParseConfig ht = (f==null) ? new ParseConfig() : new ParseConfig(f);
	L = ht.getOption("L",4);
	lambda = ht.getOptionDouble("lambda",0.1);
	nGenMax = ht.getOption("nGenMax",0);
	fracBad = ht.getOptionDouble("fracBad",0.1);
	dAcceptable = ht.getOptionDouble("dAcceptable",0);

	policy = ht.getOptionEnum(Policy.class, "policy", Policy.P0);

	countCurrentlyScreened = ht.getOptionEnum(CountCurrentlyScreened.class,
						  "policy.countCurrentlyScreened",
						  CountCurrentlyScreened.NO);

	System.out.println("Policy=" + policy + "; countCurrentlyScreened=" + countCurrentlyScreened);


	Vector<ScreenerProfile> v = new Vector<ScreenerProfile> ();
	for(int i=1; true; i++) {
	    String prefix = "p" + i + ".";
	    ScreenerProfile sp = ScreenerProfile.findScreenerProfile(ht,prefix);
	    if (sp==null) break;
	    v.add(sp);
	    }
	profiles = (ScreenerProfile[])v.toArray(new ScreenerProfile[0]);
	if (profiles.length==0) throw new IllegalArgumentException("No screening profile is defined in the parameter set!");

	for(int i=1; i<profiles.length; i++) {
	    if (profiles[i].avgCost()  <= profiles[i-1].avgCost()) throw new IllegalArgumentException("Profile " + (i+1) + " "+profiles[i]+" has the same or lower cost than the previous profile.\nProfiles are expected to be listed in increasing cost order");
	    if (profiles[i].d <= profiles[i-1].d)  throw new IllegalArgumentException("Profile " + (i+1) + " "+profiles[i]+" has the same or lower detection rate than the previous profile.\nProfiles are expected to be listed in increasing detection rate order");	    
	}

	profileIndexes = new int[L][];
	for(int j=0; j<L; j++) {
	    String prefix = "L" + (j+1) + ".";
	    profileIndexes[j] = findProfileList(ht, prefix);
	    System.out.println("For lane " + prefix +", found" + 
			       (profileIndexes[j]==null? " no" : " " + profileIndexes[j].length) + " profiles");
	}

    }

    /** Find the list of screening profiles specified as available for
	a particular lane. Expects the (1-based) numbers to be in the range
	1 &le; z &le; profiles.length; converts them to 0-based for the
	internal representation.
    */
    private int[] findProfileList(ParseConfig ht, String prefix) throws ParseException {
	Vector<Integer> v = new Vector<Integer>();
	for(int i=1; ; i++) {
	    String name = prefix + "profile" + i;
	    long z = ht.getOptionLong(name, Long.MIN_VALUE);
	    if (z < 0) {
		//System.out.println("No config var for " + name);
		break;
	    }
	    if (z==0 || z> profiles.length) throw new ParseException("Invalid value for " +name +"; expected range is [1:"+profiles.length+"]" );
	    v.add(new Integer((int)(z-1)));
	}	
	if (v.size()==0) return null;
	int[] z = new int[v.size()];
	for(int i=0; i<v.size(); i++) z[i] = v.elementAt(i).intValue();
	return z;
    }

    public String toString() {
	return "L="+L+", lambda=" + lambda+", fracBad=" + fracBad +"; has "+
	    profiles.length + " profiles";
    }
}

