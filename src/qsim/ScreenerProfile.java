package qsim;

import java.io.*;
import java.util.*;

/** Describes a screener profile (performance and detection
    rate). Each screening lane supports one or several profiles,
    between which it can switch, as controlled by the profile
    selection policy.  */
public class ScreenerProfile {

    /** The shape of the screener profile (uniform or triangular) */
    enum Shape {
	UNIFORM, TRIANGULAR;
    };



    /** Parameters of the uniform or triangular distribution of the processing time. (There is no M in uniform distribution) */
    final long L,M,H;
    /** Detection rate, in the 0.0 to 1.0 range */
    final double d;

    final Shape shape;

    /** Creates a profile with a triangular distribution */
    ScreenerProfile(long _L, long _M, long _H, double _d) {
	shape = Shape.TRIANGULAR;
	L=_L;
	M=_M;
	H=_H;
	d=_d;
    }

    /** Creates a profile with a uniform distribution */
    ScreenerProfile(long _L, long _H, double _d) {
	shape = Shape.UNIFORM;
	L=_L;
	H=_H;
	M=0;
	d=_d;
    }
    
    public String toString() {
	return shape == Shape.UNIFORM?
	    "(Cost "+L+":"+ M+ ":"+H+", d="+d+")" :
	    "(Cost "+L+":"+H+", d="+d+")";
    }

    /** Looks for the parameters of a screener profile in the option store 
	@param ht the option store
	@param prefix all option names should beging with this prefix (e.g. "p1.L", "p1.H" etc for prefix "p1."
	@return the screener profile composed of all the relevant options with the specified prefix, or null if no params with the specified prefix have been found.
	@throws Parameters.ParseException if some essential parameters are missing or have nonsensical values
    */
    static ScreenerProfile findScreenerProfile(ParseConfig ht, String prefix)
	throws Parameters.ParseException  {
	long L = ht.getOptionLong(prefix + "L", Long.MIN_VALUE);
	long M = ht.getOptionLong(prefix + "M", Long.MIN_VALUE);
	long H = ht.getOptionLong(prefix + "H", Long.MIN_VALUE);
	double d =  ht.getOptionDouble(prefix + "d", -1.0);

	if (L==Long.MIN_VALUE && M==Long.MIN_VALUE && L==Long.MIN_VALUE && d<0) 
	    return null;

	ScreenerProfile sp = (M==Long.MIN_VALUE) ?      
	    new ScreenerProfile(L, H,  d):
	    new ScreenerProfile(L, M, H,  d);

	if (!(0<=L && L<=H)) throw new Parameters.ParseException("Screening profile with prefix " + prefix + " contains an illegal L or H cost value:\n" + sp);

	if (sp.shape == Shape.TRIANGULAR) {
	    if (!(L<=M && M<=H)) throw new Parameters.ParseException("Screening profile with prefix " + prefix + " contains an illegal M value (not L<=M<=H):\n" + sp);
	}

	
	if (!(0.0<=d && d<=1.0)) throw new Parameters.ParseException("Screening profile with prefix " + prefix + " contains an illegal detection rate value\n" + sp);

	return sp;
    }
	
    /** Computes the average processing time for the triangular or uniform distribution.
     */
    double avgCost() {
	return shape==Shape.UNIFORM? (L+H)/2.0 : (L+M+H)/3.0;
    }

    /** Randomly returns true, with the probability equal to this profile's 
	detection rate */
    boolean detects() {
	return (Qsim.gen.nextDouble() < d);
    }
    /** Returns a random value in the range from L to H, subject to
	the uniform or triangular distribution with the mode at M. */
    double serviceTime() {
	if (shape==Shape.UNIFORM) {
	    double x=Qsim.gen.nextDouble();
	    return L*(1-x)  + H * x;
	} else { // TRIANGULAR
	    double a=(double)(M-L)/(double)(H-L);
	    double x=Qsim.gen.nextDouble();
	    double t = (x<a) ?
		L+Math.sqrt(x/a)*(M-L) :
		H-(H-M)* Math.sqrt((1-x)/(1-a));
	    return t;
	}
    }

}
   
