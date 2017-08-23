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
	Point2D.Double lastPoint =  new Point2D.Double(0,0);
	for(long[] q: logData) {
	    Point2D.Double thisPoint= new Point2D.Double((double)q[0], (double)q[1]);
	    g2d.draw( at.createTransformedShape(new Line2D.Double(lastPoint, thisPoint)));
	    lastPoint = thisPoint;
	}
	}
	    	
    }

}
