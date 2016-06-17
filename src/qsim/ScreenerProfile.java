package qsim;

import java.io.*;
import java.util.*;

/** Describes a screener profile (performance and detection
    rate). Each screening lane supports one or several profiles,
    between which it can switch, as controlled by the profile
    selection policy.  */
public class ScreenerProfile {
    /** Parameters of the triangular distribution of the processing time. */
    final long L,M,H;
    /** Detection rate, in the 0.0 to 1.0 range */
    final double d;
    
    ScreenerProfile(long _L, long _M, long _H, double _d) {
	L=_L;
	M=_M;
	H=_H;
	d=_d;
    }

    public String toString() {
	return "(Cost "+L+":"+ M+ ":"+H+", d="+d+")";
    }

    /** Looks for the parameters of a screener profile in a the option store 
	@param ht the option store
	@param prefix all option names should beging with this prefix (e.g. "p1.L", "p1.H" etc for prefix "p1."
	@return the screener profile composed of all the relevant options with the specified prefix.      
    */
    static ScreenerProfile findScreenerProfile(ParseConfig ht, String prefix)
	throws Parameters.ParseException  {
	long L = ht.getOptionLong(prefix + "L", Long.MIN_VALUE);
	long M = ht.getOptionLong(prefix + "M", Long.MIN_VALUE);
	long H = ht.getOptionLong(prefix + "H", Long.MIN_VALUE);
	double d =  ht.getOptionDouble(prefix + "d", -1.0);
	ScreenerProfile sp = new ScreenerProfile(L, M, H,  d);
	if (L==Long.MIN_VALUE) return null;
	if (!(0<=L && L<=M && M<=H)) throw new Parameters.ParseException("Screening profile with prefix " + prefix + " contains an illegal cost value\n" + sp);
	if (!(0.0<=d && d<=1.0)) throw new Parameters.ParseException("Screening profile with prefix " + prefix + " contains an illegal detection rate value\n" + sp);

	return sp;
    }
	
    /** Computes the average processing time for the triangular distribution.
     */
    double avgCost() {
	return (L+M+H)/3.0;
    }

    /** Randomly returns true, with the probability equal to this profile's 
	detection rate */
    boolean detects() {
	return (Qsim.gen.nextDouble() < d);
    }
    /** Returns a random value in the range from L to H, subject to
	the triangular distribution with the mode at M. */
    double serviceTime() {
	double a=(double)(M-L)/(double)(H-L);
	double x=Qsim.gen.nextDouble();
	double t = (x<a) ?
	    L+Math.sqrt(x/a)*(M-L) :
	    H-(H-M)* Math.sqrt((1-x)/(1-a));
	return t;
    }

}
   
