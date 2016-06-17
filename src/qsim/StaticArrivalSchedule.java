package qsim;

import java.io.*;
import java.util.*;

/** A precomputed customer arrival schedule */
public class StaticArrivalSchedule {
    private Queue.ArrivalBase[] list;
    StaticArrivalSchedule(Queue.ArrivalBase[] _list) {
	list = _list;
    }
    int size() { return list.length; }
    private int nextPtr=0;    
    boolean hasNext() {
	return nextPtr < list.length;
    }
    Queue.ArrivalBase next() {
	if (hasNext()) return list[ nextPtr++];
	throw new IllegalArgumentException("Trying to read beyond the end of list");
    }

    /** Reads pre-created arrival schedules for multiple queues */
    @SuppressWarnings("unchecked")
    public static StaticArrivalSchedule[] readFile(File f, int L) throws IOException, 
							   Parameters.ParseException {
	//Vector<Queue.ArrivalBase> v[]= new Vector<Queue.ArrivalBase>[L];
	Vector v[]= new Vector[L];
	for(int j=0; j<L; j++) {
	    v[j] = new Vector();
	}
	int [] ptr =  new int[L];
	LineNumberReader r = new LineNumberReader( new FileReader(f));
	String s=null;
	while((s = r.readLine())!=null) {
	    s = s.trim();
	    if (s.startsWith("#")) continue;
	    String q[] = s.split("\\s+");
	    if (q.length!=3) throw new Parameters.ParseException(f,r," Cannot parse line: " + s);
	    long t = Long.parseLong(q[0]);
	    int j = Integer.parseInt(q[1]);
	    int it = Integer.parseInt(q[2]);
	    if (t < 0)  throw new Parameters.ParseException(f,r," Negative time t=" + t);
	    if (j < 1 || j>L)  throw new Parameters.ParseException(f,r," Lane number out of range ("+j+")");
	    if (it < 0 || it>1)  throw new Parameters.ParseException(f,r," Threat must be 0 or 1 (found "+it+")");
	    boolean threat = (it!=0);
	    Vector<Queue.ArrivalBase> w = (Vector<Queue.ArrivalBase>)v[j-1];
	    if (w.size()>0 && w.lastElement().arrivalTime > t) throw new  Parameters.ParseException(f,r," Arrival times for lane "+j+" are not in chronological order: " + s);
	    w.add(new Queue.ArrivalBase(threat, t));
	}	
	r.close();
	StaticArrivalSchedule[] ss = new StaticArrivalSchedule[L];
	for(int j=0; j<L; j++) {
	    Vector<Queue.ArrivalBase> w = (Vector<Queue.ArrivalBase>)v[j];
	    System.out.println("Read schedule["+j+"], len=" + w.size());
	    ss[j] = new StaticArrivalSchedule(w.toArray(new Queue.ArrivalBase[0]));	   
	}
	return ss;
	
    }

}