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
public class QsimPanel extends JPanel implements ActionListener, 
						 MouseInputListener//,  SVGAwareComponent 
{

    
    QsimGUI parent;
    JPopupMenu popup;
    //    JMenuItem drawTreeMenuItem, saveBGItem;
    JLabel popupLabel[] = new JLabel[3];

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

    static String WORKING = "Working", BROKEN="Broken";

    /** A hook for Qsim to display simulation progress using this
     panel.  Methods of this class are called from the simulator
     thread to update status labels shown in this panel. */
    public class GuiProgressDisplay extends Qsim.ProgressDisplay {
	/** Updates info text for the j-th lane */
	public void display(int j, qsim.Queue q) {
	    if (labels==null || j>=labels.length) return;
	    labels[j].setText("<html>"+q.describeQueue2(true)+"</html>");
	}
	public void showSummary(String s) {
	    statsLabel.setText(s);
	}
 	public void showStats2(String s) {
	    statsLabel2.setText("<html><pre>" +s+ "</pre></html>");
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

	p = new JPanel();
	p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4*h+3));
	p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));	    
	statsLabel2 = new JLabel("Additional stats for all lanes will go here", SwingConstants.LEADING);
	statsLabel2.setMaximumSize(new Dimension(Integer.MAX_VALUE,4*h));
	p.add(statsLabel2);
	add(p);

	//this.addMouseListener(this); // to monitor mouse clicks
	repaint();
    }

	/*
    {
	// //this.addMouseMotionListener(this); // ??

	//------------- popup menu for information about a policy
	//Create the popup menu.
	popup = new JPopupMenu("Policy details");

	String names[] = {"Policy info", "dD/dC Ratio", "Policy tree"};
	for(int i=0; i<names.length; i++) {
	    popupLabel[i] = new JLabel(names[i]);
	    popup.add(popupLabel[i]);
	}

	drawTreeMenuItem = new JMenuItem("Draw policy tree in a new window");
	drawTreeMenuItem.addActionListener(this);
	popup.add(drawTreeMenuItem);

	saveBGItem = new JMenuItem("Describe policy as a device");
	saveBGItem.addActionListener(this);
	popup.add(saveBGItem);

	//Add listener to components that can bring up popup menus.
	//MouseListener popupListener = new PopupListener();
	//output.addMouseListener(popupListener);
	//menuBar.addMouseListener(popupListener);
    }
	*/

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

	//	if (buttonRun!=null && e.getSource() == buttonRun) {
	
	if (CMD.RUN.equals(e.getActionCommand())) {
	    buttonRun.setEnabled(false);
	    buttonStop.setEnabled(true);
	    parent.fileMenu.setEnabled(false);
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


	/*
	if (e.getSource() == drawTreeMenuItem) {
	    DDGUI.debugln("Trying to draw the tree,,,");
	    new TreeFrame(this, selectedI, selectedPolicy);
	} else	if (e.getSource() == saveBGItem) {
	    DDGUI.debugln("Describing policy as a device");
	    saveAsADevice( this, selectedPolicy);
	}
	*/
    }

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
