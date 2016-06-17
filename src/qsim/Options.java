package qsim;

import java.io.*;

/** This class stores a few static members, which are values of
 * certain options for the queue similator algorithm. They can be set
 * and accessed via the set and get access methods, which are also
 * used by the {@link qsim.gui.QsimGUI Qsim GUI}. Default
 * values for most variables can be from the command line via the
 * -Dname=value Java system properties. Read the source code of this
 * class for the names of these options.
 */
public class Options {

    /** Just the option names, to avoid using quoted strings throughout the
     * program */
    static final String T = "T", SPEED = "speed";

    /** The option table. Presently, no config file is used - just get options 
     from the command line using Java system properties (-Dname=value) */
    static ParseConfig options = new ParseConfig(); 

    /** Affects certain features of the program's behavior */
    private static boolean interactive=true;
    public static void setInteractive(boolean val) {
	interactive=val;
    }

    /** Simulation speed, in terms of seconds of simulated time per
	a second of wall clock time. A zero or negative value means,
	don't care about the wall clock; that's appropriate for batch
	(non-interactive) runs.
    */
    final static double defaultSpeedInteractive = 10,
	defaultSpeedBatch = 0; 
    public static double getSpeed() {
	return options.getOptionDouble( SPEED, 
					interactive? defaultSpeedInteractive :
					defaultSpeedBatch);
    }

    public static void setSpeed(double val) {
	options.setOption(SPEED, val);
    }
    
    final static long defaultT = 10000; 
    public static long getT() {
	return options.getOptionLong( T, defaultT);
    }
    public static void setT(long val) {
	options.setOption(T, val);
    }



}

