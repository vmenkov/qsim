package qsim;

import java.io.*;
import java.util.*;
import java.text.*;

/** An auxiliary application for generating arrival schedules, for use
    in Qsim runs.
*/
public class GenerateArrivals {

    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	System.out.println("Usage: java [options] qsim.GenerateArrivals config.dat [output-dir-name]");
	System.out.println("Options:");
	System.out.println(" [-Dseed=0] -- seed for the random number generator");
	//	System.out.println(" [-Dspeed=100] -- simulation speed (units of sim time per real-clock second). (0 means run fast)");
	System.out.println(" [-Druns=1] -- number of simulation runs for which arrival files are to be produced");

	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    static private String mkOutDirName() {
	return "arrivals-qsim-" + Qsim.odFmt.format( new Date());
    }
    
    private static NumberFormat nf = new DecimalFormat("000");

    static File arrivalFile(File dir, int k) {
	return new File(dir, "arrivals-"+ nf.format(k)+".dat");
    }

    static public void main(String argv[]) throws IOException {
	Options.setInteractive(false);
	System.out.println("Arrival Generator for Qsim ver. " + Qsim.version);

	int ja = 0;
	if (ja >= argv.length) usage();
	String configFileName = argv[ja++];

	String outDirName = mkOutDirName();
	if (ja < argv.length) outDirName =argv[ja++];

	System.out.println("Config file " + configFileName +"; output dir for arrvial schedule files "+ outDirName);
	File f= new File(configFileName);
	if (!f.exists()) usage("File " + f + " does not exist!");
	ParseConfig ht = new ParseConfig(f);
	qsim.Parameters para = new qsim.Parameters(f);

	File g = new File(outDirName);
	Qsim.checkOutDir(g);
	
	// The seed for the random number generator. 
	long seed = ht.getOptionLong("seed", 0);
	Qsim.gen = new Random(seed);

	int runs = ht.getOption("runs", 1);
	if (runs < 1) usage();

	System.out.println("Will produce  " + runs + " arrival schedule files (one per simulation run).");

	long now = 0;
	Qsim qsim = new Qsim(para, now, null);

	for(int k=0; k<runs; k++) {
	    f = arrivalFile(g,k);
	    System.out.println("Saving arrival schedule file " + f);
	    qsim.generateArrivalSchedule(f);
	}

    }


}
