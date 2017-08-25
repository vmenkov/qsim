package qsim.gui;

import java.io.*;
import java.text.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;

//import qsim.*;

//--------------- for SVG output
//import org.apache.batik.svggen.SVGGraphics2D;

/** Panel for drawing the crowd graph in the Qsim GUI 
  ({@link QsimGUI}).
 */
class CrowdPanel extends JPanel //implements ActionListener, 
				    //MouseInputListener,  SVGAwareComponent
{
 

    QsimGUI parent;
    //    JPopupMenu popup;
    //    JMenuItem drawTreeMenuItem, saveBGItem;
    //    JLabel popupLabel[] = new JLabel[3];

     /** 
      * Constructor for CrowdPanel. 
      */
    public CrowdPanel(int width, int height, QsimGUI _parent) {
	parent = _parent;
	setPreferredSize(new Dimension(width, height));
	setLayout(new BorderLayout());
    }

    /** 
     * Paints the component (to the screen).
     */
    synchronized public void paintComponent(Graphics g) {
	Graphics2D g2d = (Graphics2D) g;
	super.paintComponent(g2d);

	if (parent==null || parent.presented  == null) {
	    return;
	} 
	
	Rectangle bounds = this.getBounds();
	//	DDGUI.debugln("paintCompo: panel' bounds(x="+bounds.x+",y="+bounds.y+
	//	      "; w=" + bounds.width +", h=" + bounds.height+")");

	if (parent.presented != null) {
	    Dimension dim = new Dimension( bounds.width,bounds.height);
	    parent.presented.paintPlot(g2d, dim, true, parent.qsim);
	} else {
	    g2d.drawString("Crowd graph will go here",10,20);

	}

    }

    /** Paints the content of this element (i.e, the crowd graph) to a 
     Graphics2d that comes from the SVG rendering process. The actions are
     similar to paintComponent(), except that here we need to explicitly decide
     how big our SVG canvas ought to be.
     */
    /*
    public void paintSVG( SVGGraphics2D g2d) 	{
	Dimension dim =  new Dimension( 800, 800);  // (w h)
	g2d.setSVGCanvasSize(dim);
	parent.presented.paintPlot(g2d, dim, false, parent.qsim),
    }
    */

 
}
