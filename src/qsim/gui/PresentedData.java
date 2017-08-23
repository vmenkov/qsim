package qsim.gui;

import java.util.*;
import java.text.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.geom.AffineTransform;

import javax.swing.*;
//import javax.swing.event.*;


//import qsim.*;

/** Used to plot the crowd-againts-time curve
 */
abstract public class PresentedData {

       /** Produces a brief description of the graph, to be displayed above it */
    public String makeGraphTitle() {
	return "Some kind of a plot...";
    }

    /** Not supported in the parent class; children must implement */
    abstract public void paintPlot(Graphics2D g2d, Dimension bounds, boolean fromGUI);
    /*
    public void paintPlot(Graphics2D g2d, Dimension bounds, boolean fromGUI) {
	// FIXME
	throw new AssertionError("Not supported");
    }
    */

    /** Paints the frontier (and any auxiliary data) on a specified
      Graphics2D object.
      @param fromGUI This value should be true if feedback (user's
      ability to click on nodes etc.) should be enabled, as opposed to 
      simply producing a PNG/SVG image file.
     */
   public void paintPlot(Graphics2D g2d, boolean fromGUI) {
       paintPlot(g2d,  getRecommendedDim(), fromGUI);
   }


    private Dimension dim =  new java.awt.Dimension( 600, 400);  // (w h)
    public Dimension getRecommendedDim() {
	return dim;
    }

    public void setRecommendedDim(Dimension d) {
	dim = d;
    }

    /** A good step size (for grid drawing) to divide the range into */
    private static double goodStep(double range) {
	double step = 1;
	while (range > step * 10) {
	    step *= 2;
	    if (range > step * 10) step *= 5;
	}
	return step;
    }

    
    /**Draws a suitably spaced grid for the argument range [0:realWidth] x [0:realHeight] 
       @param g2d this is where we draw
       @param realWidth the range of time to display on the plot
       @param realHeight the range of the crowd size to display on the plot
       @return the AffineTransform object which can be later use to convert
       the positions of individual plot points to the screen coordinates
     */ 
    protected static AffineTransform drawGrid(Graphics2D g2d, Dimension bounds, double realWidth, double realHeight, String title, boolean web) {
	FontMetrics fm = g2d.getFontMetrics();
	int textHt  = fm.getMaxAscent();

	int margin = 2 * textHt + 8;

	AffineTransform at =  new 
	    AffineTransform((bounds.width-2*margin)/realWidth, 0, 
			    0, -(bounds.height-2*margin)/realHeight,
			    margin,  bounds.height-margin);

	g2d.setPaint(Color.blue);
	g2d.draw( at.createTransformedShape(new Rectangle2D.Double(0,0, realWidth,1)));

	// text base position
	Point2D p1;

	p1 = at.transform(new  Point2D.Double(0.32, 0), null);
	g2d.drawString("Time",
		       (int)p1.getX(), (int)p1.getY()+2*textHt+4);

	p1 = at.transform(new  Point2D.Double(0, 0), null);
	g2d.drawString("0", (int)p1.getX(), (int)p1.getY()+textHt+2);

	p1 = at.transform(new  Point2D.Double(0, 1), null);
	g2d.drawString("Crowd size    " + title,(int)p1.getX() + 5,
		       (int)p1.getY()-2);

	int modX = (bounds.width > 300) ? 1 : 2;

	double step = goodStep(realWidth);
       

	NumberFormat fmt = new DecimalFormat("0.0");
	//	int maxKx=  (int)(realWidth * 10);
	for(int k=1; k * step <= realWidth; k++) {
	    double x = k * step;
	    p1 = at.transform(new  Point2D.Double(x, 0), null);
	    Point2D p1q = at.transform(new  Point2D.Double(x, 0), null);
	    p1q.setLocation( p1q.getX(), p1q.getY()-10);
	    
	    g2d.setPaint(Color.blue);
	    g2d.draw( new Line2D.Double(p1, p1q));

	    if (k% modX ==0) {
		g2d.drawString(fmt.format(x), 
			       (int)p1.getX(), (int)p1.getY()+textHt+2);
	    }

	    if (x+0.01<realWidth) {
		g2d.setPaint(Color.yellow);
		Point2D p2 = at.transform(new  Point2D.Double(x, 1), null);
		g2d.draw( new Line2D.Double(p1, p2));
	    }
	    
	}

	step = goodStep(realHeight);
	
	for(int k=1; k *step <= realHeight; k++) {
	    double y = k * step;
	    p1 = at.transform(new  Point2D.Double(0, y), null);
	    Point2D p1q = at.transform(new  Point2D.Double(0, y), null);
	    p1q.setLocation( p1q.getX() + 10, p1q.getY());
	    
	    g2d.setPaint(Color.blue);
	    g2d.draw( new Line2D.Double(p1, p1q));
	    g2d.drawString(fmt.format(y),   3, (int)p1.getY());

	    if (k<10) {
		g2d.setPaint(Color.yellow);
		Point2D p2 = at.transform(new  Point2D.Double(realWidth,y), null);
		g2d.draw( new Line2D.Double(p1, p2));
	    }
	}
	return at;
    }

    /*
    protected static Point2D.Double policy2point(PolicySignature pol, double pi) {
	return new  Point2D.Double(pol.getPolicyCost(pi), 
				   pol.getDetectionRate());
    }

    protected static Point2D.Double mixedPolicy2point(DetectionRateForBudget db,
						      double pi) {
	return (db.w==1) ? policy2point(db.p1, pi) :
	    new Point2D.Double(db.p1.getPolicyCost(pi) * db.w + 
			       db.p2.getPolicyCost(pi) * (1-db.w), 
			       db.p1.getDetectionRate() * db.w +
			       db.p2.getDetectionRate() * (1-db.w));
    }

    protected static void square(Graphics2D g2d, Point2D p, int r) {
	int x = (int)p.getX()-r, y=(int)p.getY()-r;
	g2d.draw( new Rectangle2D.Double(x,y,2*r,2*r));
    }

    protected static void circle(Graphics2D g2d, Point2D p, int r) {
	int x = (int)p.getX()-r, y=(int)p.getY()-r;
	g2d.drawOval( x, y, 2*r, 2*r);
    }

    protected static void circle(Graphics2D g2d, Point2D p) {
	circle(g2d, p, radius);
    }
    */

    /** Find and mark the best non-mixed policy within the
	specified budget.

	For web demo */
    /*
    protected void plotBudget(Graphics2D g2d, AffineTransform at, double pi) {
	g2d.setPaint(Color.black);	    
	
	Point2D.Double thisPoint = mixedPolicy2point(db, pi);
	circle(g2d, at.transform(thisPoint, null), 10);
	
	double b = db.givenBudget;
	Point2D pt1 = at.transform(new  Point2D.Double(b, 0), null);
	Point2D pt2 = at.transform(new  Point2D.Double(b, 1), null);
	g2d.draw( new Line2D.Double(pt1, pt2));
    }
    */
    
    /*
    private Dimension dim =  new Dimension( 800, 800);  // (w h)
    public Dimension getRecommendedDim() {
	return dim;
    }

    public void setRecommendedDim(Dimension d) {
	dim = d;
    }
    */

}


