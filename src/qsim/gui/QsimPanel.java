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

import qsim.*;

//--------------- for SVG output
/* import org.apache.batik.svggen.SVGGraphics2D; */

/**
 * The main panel in the Qsim interactive GUI window
 * ({@link QsimGUI}).
 */
public class QsimPanel extends JPanel implements ActionListener 
						 //, MouseInputListener//,  SVGAwareComponent 
{

    
    QsimGUI parent;
    //JPopupMenu popup;
    //    JMenuItem drawTreeMenuItem, saveBGItem;
    //JLabel popupLabel[] = new JLabel[3];

   /** 
      * Constructor for QsimPanel. 
      */
    public QsimPanel(int width, int height, QsimGUI _parent) {
	parent = _parent;
	setPreferredSize(new Dimension(width, height));
    }

    JComboBox[] cboxes = null;
    JLabel[] labels = null;
    JLabel statsLabel = null, statsLabel2=null;
    JButton buttonRun, buttonStop;
    CrowdPanel crowdPanel = null;
    
    static String WORKING = "Working", BROKEN="Broken";

    /** A hook for Qsim to display simulation progress using this
     panel.  Methods of this class are called from the simulator
     thread to update status labels shown in this panel. */
    public class GuiProgressDisplay extends Qsim.ProgressDisplay {
	/** Updates info text for the j-th lane */
	public void display(int j, qsim.Queue q) {
	    if (labels==null || j>=labels.length) return;
	    int maxlen = (int)(labels[j].getWidth()/oCharWidth) - 4;
	    labels[j].setText("<html>"+q.describeQueue2(true,maxlen)+"</html>");
	}
	public void showSummary(String s) {
	    statsLabel.setText(s);
	}
 	public void showStats2(String s) {
	    statsLabel2.setText("<html><pre>" +s+ "</pre></html>");
	}
	/** Plots the updated historical crowd size graph */
	public void plotCrowdData( Vector<long[]> logData) {
	    if (crowdPanel != null) {
		crowdPanel.repaint();
	    }
	}

	
    }

    private final GuiProgressDisplay display = new GuiProgressDisplay();

    static class CMD {
	static final String RUN = "Run", STOP = "Stop";
    }

    void layout(int L) {
	parent.qsim.setProgressDisplay(display);
	//setLayout(new BorderLayout());
	//setLayout(new GridLayout(L,2));
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	removeAll();

	cboxes = new JComboBox[L];
	labels = new JLabel[L];
	final int h = 20;

	JPanel p = new JPanel();
	p.setMaximumSize(new Dimension(Integer.MAX_VALUE, h+3));
	p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));	    

	buttonRun = new JButton("Run");
	buttonRun.setActionCommand(CMD.RUN);    
	buttonRun.setMaximumSize(new Dimension(100, h));
	buttonRun.addActionListener(this);
	buttonRun.setEnabled(true);
	p.add(buttonRun);

	buttonStop = new JButton("Stop");
	buttonStop.setActionCommand(CMD.STOP);    
	buttonStop.setMaximumSize(new Dimension(100, h));
	buttonStop.addActionListener(this);
	buttonStop.setEnabled(false);
	p.add(buttonStop);

	statsLabel = new JLabel("Stats for all lanes will go here");
	statsLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE,h));
	p.add(statsLabel);
	add(p);

	for(int j=0; j<L; j++) {

	    p = new JPanel();
	    p.setMaximumSize(new Dimension(Integer.MAX_VALUE, h+3));
	    p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));

	    String [] values = {WORKING, BROKEN};
	    cboxes[j] = new JComboBox(values);

	    cboxes[j].setMaximumSize(new Dimension(100, h));

	    cboxes[j].setSelectedIndex(0);
	    cboxes[j].addActionListener(this);
	    p.add(cboxes[j]);	    
	    labels[j] = new JLabel("Lane no. " + (j+1) + " ready");
	    labels[j].setMaximumSize(new Dimension(Integer.MAX_VALUE,h));
	    p.add(labels[j]);

	    add(p);
	}

	crowdPanel = new CrowdPanel(400,400, parent);
	add(crowdPanel);
	
	p = new JPanel();
	p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4*h+3));
	p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));	    
	statsLabel2 = new JLabel("Additional stats for all lanes will go here", SwingConstants.LEADING);
	statsLabel2.setMaximumSize(new Dimension(Integer.MAX_VALUE,4*h));
	p.add(statsLabel2);
	add(p);
	
	repaint();
	determineOCharWidth();

	
    }

    /** The width of char 'o' in labels; used to truncate text */
    private double oCharWidth = 0;
    /** What's the typical char width in the font used in the labels?
	Here we try to find an "average" size, with some padding, instead
	of trying to properly measure the length of actual strings.
	// FIXME: maybe we should just use a fixed-width font.
     */
    private void determineOCharWidth() {
	JComponent comp = (labels!=null && labels.length>0 && labels[0]!=null)? labels[0] : this;
	FontMetrics fmet = comp.getFontMetrics(comp.getFont());
	double w1 =  fmet.charWidth('o');
	String o5 = "ooooo";
	String s = o5 + o5 + o5 + o5 + o5 + o5;
	double w2 = (double)fmet.stringWidth(s)/(double)s.length();
	oCharWidth = (w2 > w1) ? w2 : w1;
	oCharWidth *= 1.01;
	System.out.println("Char width estimate: " + w1 +", " + w2 +", padded to " + oCharWidth);
    }



    /** implementing ActionListener... */
    public void actionPerformed(ActionEvent e) {

	if (cboxes!=null) {
	    for(int j=0; j<cboxes.length; j++) {
		if (e.getSource() == cboxes[j]) {
		    String selected = (String)cboxes[j].getSelectedItem();
		    boolean broken = (selected==BROKEN);
		    parent.qsim.lanes[j].setBroken(broken);
		    parent.setLabel("Lane " + j + " is now " + (broken? "broken": "fixed"));
		    display.display(j, parent.qsim.lanes[j]);
		    return;
		}
	    }
	}

	if (CMD.RUN.equals(e.getActionCommand())) {
	    buttonRun.setEnabled(false);
	    buttonStop.setEnabled(true);
	    parent.fileMenu.setEnabled(false);
	    parent.presented = new CrowdPresentedData();
	    (new QsimWrapper()).start();
	} else
	    if (CMD.STOP.equals(e.getActionCommand())) {
		//if (buttonStop!=null && e.getSource() == buttonStop) {
	    buttonStop.setEnabled(false);
	    buttonRun.setEnabled(true);
	    parent.fileMenu.setEnabled(true);
	    parent.setLabel("Suspended at t=" + parent.qsim.getNow());
	    parent.qsim.requestStop();
	}

    }
    /*
   public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
	//DDGUI.debugln("mouse entered at " + e.getX() + " " + e.getY());

    }

    public void mouseExited(MouseEvent e) {
    }

   
    public void mouseReleased(MouseEvent e) {
    }
    
    public void mouseDragged(MouseEvent e) {
    }
    
    public void mouseMoved(MouseEvent e) {
	//DDGUI.debugln("mouse moved at " + e.getX() + " " + e.getY());
    }

    public void mousePressed(MouseEvent e) {
    }
    */
    
    /** Is used to run queue simulator in a separate thread asynchronously
	from the GUI action handler */
    private class QsimWrapper extends Thread {
	public void run() {
    	    long T = Options.getT();
	    try {
		parent.setLabel("Starting simulation at t=" + parent.qsim.getNow() + ", will run until t=" +  T + ", speed=" + Options.getSpeed());

		parent.qsim.simulate(T);
		parent.setLabel("Simulation stopped at t=" + parent.qsim.getNow());
	    } catch(Exception ex) {
		parent.setLabel("ERROR: " + ex.getMessage());
		System.out.println("ERROR: " + ex.getMessage());
		ex.printStackTrace(System.err);
	    }
	    buttonStop.setEnabled(false);
	    buttonRun.setEnabled(true);	    
	    parent.fileMenu.setEnabled(true);
	}
    }
 
}
