package qsim.gui;

import java.util.*;
import java.text.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.geom.AffineTransform;

import javax.swing.*;
//import javax.swing.event.*;


import qsim.Qsim;

/** Used to plot the crowd-againts-time curve
 */
public class CrowdPresentedData extends PresentedData {
    
    public void paintPlot(Graphics2D g2d, Dimension bounds, boolean fromGUI, Qsim qsim) {

	Vector<long[]> logData = qsim.getLogData();
	double realWidth=0, realHeight=0;

	synchronized(logData) {

	
	for(long[] q: logData) {
	    if (q[0] > realWidth) realWidth = q[0];
	    if (q[1] > realHeight) realHeight = q[1];
	}

	final boolean web=false;
	String title = "Crowd size for t=0..." + realWidth;
	AffineTransform at = drawGrid(g2d, bounds, realWidth, realHeight, title, web);
	g2d.setPaint(Color.black);

	boolean simplify = true; // simplify the  curve when multiple
	// points map to the same x coordinate

	if (!simplify) {	
	    Point2D.Double lastPoint =  new Point2D.Double(0,0);
	    for(long[] q: logData) {
		Point2D.Double thisPoint= new Point2D.Double((double)q[0], (double)q[1]);
		g2d.draw( at.createTransformedShape(new Line2D.Double(lastPoint, thisPoint)));
		lastPoint = thisPoint;
	    }
	} else { // simplified display, with a single vertical segment showing
	    // multiple points with nearly-same x
	    Point2D.Double lastPointReal =  new Point2D.Double(0,0);
	    // last already-drawn point
	    Point lastP = transformPoint(at, lastPointReal);
	    int lastX = lastP.x;
	    VerticalSegment vert = new VerticalSegment(lastP);
	    
	    for(long[] q: logData) {
		Point2D.Double thisPointReal = new Point2D.Double((double)q[0], (double)q[1]);
		
		Point p = transformPoint(at, thisPointReal);
		int x = p.x;
		
		if (x > lastX) {
		    vert.drawIfNeeded(g2d, p);
		    g2d.draw( new Line2D.Double(lastP, p));
		    lastX = x;
		    lastP = p;
		} else {
		    vert.include( p.y);
		}
	    }
	    vert.drawIfNeeded(g2d, lastP);
	}

	}
	    	
    }

    /** Auxiliary class used for drawing a single vertical segment
	representing multiple points with visibly the same x */
    static class VerticalSegment {
	int x;
	int ySeg[] = new int[2];
	VerticalSegment(Point p) { set(p); }
	void set(Point p) {
	    x = p.x;
	    ySeg[0] = ySeg[1] = p.y;
	}	    
	private void drawIfNeeded(Graphics2D g2d, Point newP) {
	    if (ySeg[0] != ySeg[1]) {
		g2d.draw( new Line2D.Double( new Point( x, ySeg[0]),
				      new Point( x, ySeg[1])));
	    }
	    set(newP);
	}
	void include(int y) {
	    if (y<ySeg[0]) ySeg[0] = y;
	    if (y>ySeg[1]) ySeg[1] = y;
	}	
    }
    
    
    /** Transforms q as per the specified affine transform, and then rounds
	the result's coordinates to int
     */
    static Point transformPoint(AffineTransform at, Point2D.Double q) {
	Point2D z = at.transform(q, null);
	return new Point((int)Math.round(z.getX()), (int)Math.round(z.getY()));
    }

}
