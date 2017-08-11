package qsim;

import java.io.*;
import java.util.*;
import java.text.*;

/** policy P3: giving out "tickets" for F[ast] and S[low], to ensure proper 
    detection rate.

    <p>It includes a "ticket box" system, whereby it is guaranteed
    that out of each 100 customers, exactly a certain pre-computed
    percentage will be processed by the "higher" screening profile.
    Thus on any run with a number of customers that's multiple of 100,
    the exact desired notional detection rate, dAcceptable, is
    achieved (within 1%).

    <p>The way the "ticket box" system works is as follows. For each
    batch of NT customers (NT=100, a hard-coded parameter) to process,
    100 "tickets" are created and put into a "ticket box". Some
    tickets are labeled "S" and others "F", to achieve the desired
    target value of the average (notional) detection rate. Every time
    a lane is about to start processing a customer, the ratio of the
    number of the remaining "F" tickets to that of the remaining "S"
    tickets is computed; the algorithm then checks if the lane's
    current queue length puts it within the appropriate number of the
    shortest queues. If it is, the lane is set to use the F profile to
    screen that customer, otherwise S; the appropriately labeled
    ticket is removed from the box and destroyed. While this approach
    still uses the same (faulty) assumption of using the lane count as
    a proxy to customer count, the algorithm is, at least, self
    correcting, adjusting the F/S ratio as tickets are consumed.

    <p>Below is a fairly typical log, illustrating the order in which
    5 batches of 100 tickets each (screening the total of 500 customers in
    one run) were used, on a run in which the F/S ratio was 1:1 :

<pre>
FFFFFSFFFFFFFFFFFSFFFFFSSFFSSSSFSSFSSFFSSSFFFFSSSFFSFSFFFSSFSFFSFSFSSFFSSFSFSSSSFSSSFFSSFFSSSSSSSSSS

SSSFFFFSFFFFFFFFFSSFSFFFFSSFSFFFFSFFSSSFFFFFFSSFFFFSSFFSSFSSFFSSFSFFSFSFFSSSSSFSSFSSSFSSFFSSSSSSSSSS

SFSFSFFFFFSSFFFFFFSSFFFSFFFSFFSFFFFSSSFFSFFSFFFSFFSSFSFFSFFSFSFFSSFSFSSFSFFSSSFSSSFFSFSSSSSSSSSSSSSS

SSFFFSSSFFFFFFSSSFFFSSFFFSFSFFFSSFSFSFFFSFFSSFFFFSSSSFFSFFFFSSSSSFFSFFSFSSFFSFFSSFSFSSFFSSSFSSFSSSSS

SSFFSFFSSFSFFFFSSSFFSFSFFSSFFFFSFSSFFSFFFFSSFFFFSFSSSFFFSFFSSFSFFFFFFFFFFFFFFSSSSSSSSFSSSSSSSSSSSSSS
</pre>

<p>
So while the F tickets are used more generously early in each bacth
(obviously, since F lanes have higher throughput; this is especially
so in the beginning of the 1st batch, when some lanes are still idle),
at least some F tickets stay available throughout the batch.
 */
class PolicyP3 extends PolicyP2 {

   static class GroupP3 extends Group {
       GroupP3(Queue lane, double d) {
	    super(lane, d);
       }

       static final int NT = 100;

       private int tickets[] = new int[2];
       private String ticketLabel[] = { "F", "S"};
       private StringBuffer ticketString = new StringBuffer(NT);


       private void allocTicketsIfNeeded() {
	   if (tickets[0]>0 || tickets[1]>0) return;
	   tickets[1] = (int)(fracUseHigher * NT);
	   tickets[0] = NT - tickets[1];
       }

       private void useTicket(int d) {
	   tickets[d] --;
	   ticketString.append(ticketLabel[d]);
	   if (ticketString.length()==NT) {
	       System.out.println(ticketString);
	       ticketString.delete(0,NT);
	   }
       }


       int chooseProfile(int mypos, Queue[] lanes) {
	    if (useLowest) return 0;
	    if (useHighest) return myprofiles.length-1;
	    allocTicketsIfNeeded();
	    double fHigh = (double)(tickets[1])/(double)(tickets[1]+tickets[0]);
	    boolean isShort=hasOneOfTheShorterQueues(mypos,lanes,fHigh);
	    int d = isShort ? 1 : 0;
	    useTicket(d);
	    return lowBracketID+d;
	}

   }


    Group groupConstructor(Queue lane, double dAcceptable) {
	return new GroupP3(lane, dAcceptable);
    }
   
    PolicyP3(Queue[] lanes, double dAcceptable) {
	super(lanes, dAcceptable); 
 	//init(lanes, dAcceptable);
   }


}
