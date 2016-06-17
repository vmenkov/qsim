package qsim.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import qsim.Options;


/** Displaying option values and allowing the user to edit them
 */
class OptionDialog extends JDialog
                        implements ActionListener {
    private static OptionDialog dialog;
    private static String value = "";
    private JPanel list;

    /**
     * Set up and show the dialog.  The first Component argument
     * determines which frame the dialog depends on; it should be
     * a component in the dialog's controlling frame. The second
     * Component argument should be null if you want the dialog
     * to come up with its left corner in the center of the screen;
     * otherwise, it should be the component on top of which the
     * dialog should appear.
     */
    public static void showDialog(Component frameComp,
                                    Component locationComp,
                                    String labelText,
                                    String title) {
        Frame frame = JOptionPane.getFrameForComponent(frameComp);
        dialog = new OptionDialog(frame,
                                locationComp,
                                labelText,
				  title);
        dialog.setVisible(true);
        //return value;
    }

    private static class CMD {
	static final String SET="Set", CANCEL="Cancel";
    }
    
    private JTextField speedTF, tTF;

    ButtonGroup vsGroup;
    private OptionDialog(Frame frame,
			 Component locationComp,
			 String labelText,
			 String title) {
        super(frame, title, true);

        //Create and initialize the buttons.
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        cancelButton.setActionCommand(CMD.CANCEL);
        //
        final JButton setButton = new JButton("Set Values");
        setButton.setActionCommand(CMD.SET);
        setButton.addActionListener(this);
        getRootPane().setDefaultButton(setButton);


        Container contentPane = getContentPane();
        contentPane.setLayout( new BoxLayout(contentPane, BoxLayout.Y_AXIS));
	contentPane.add(new JLabel(labelText));

        //main part of the dialog
	list = new JPanel();

	int nrows = 5, ncols=2;
	list.setLayout(new GridLayout(nrows, ncols));

	/*
	list.add(new JLabel("E (extra cost for false positives)"));	
	list.add(eTF = new JTextField(""+ Options.getE(), 10));	

	list.add(new JLabel("maxDepth (-1 = all trees)"));	
	list.add(maxDepthTF = new JTextField(""+ Options.getMaxDepth(-1), 10));	

	list.add(new JLabel("Vertex-skipping eps"));	
	list.add(epsTF = new JTextField("" + Options.getEps(), 10));	
	*/

	list.add(new JLabel("Simulation speed"));
	list.add(speedTF = new JTextField("" + Options.getSpeed(), 10));	

	list.add(new JLabel("Simulation time range (T)"));
	list.add(tTF = new JTextField("" + Options.getT(), 10));	


	/*
	list.add(new JLabel("Pi values (e.g. '0', or '0 0.1 0.2 ... 1'"));
	list.add(piTF = new JTextField("" + Options.formatPiList(), 20));	
	*/

        contentPane.add(list);

	/*
	contentPane.add(new JLabel("--- Choose the vertex-skipping technique below ---"));

	//ButtonGroup 
	    vsGroup = new ButtonGroup();
	JPanel radioPanel = new JPanel(new GridLayout(0, 1));
	for (VSMethod vs : VSMethod.values()) {   
	    JRadioButton b = new JRadioButton(vs.toString());
	    //birdButton.setMnemonic(KeyEvent.VK_B);
	    b.setActionCommand(vs.toString());
	    b.setSelected(vs== Options.getVSMethod()); 
	    vsGroup.add(b);
	    radioPanel.add(b);
     	}

	//list.add(radioPanel);
	//list.add(new Label(""));
	contentPane.add(radioPanel);
	*/	

        //Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(setButton);

        contentPane.add(buttonPane);

        //Put everything together, using the content pane's BorderLayout.
	/*
        Container contentPane = getContentPane();
        contentPane.add(new JLabel(labelText), BorderLayout.NORTH);
        contentPane.add(list, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);
	*/

        pack();
        setLocationRelativeTo(locationComp);
    }

    //Handle clicks on the Set and Cancel buttons.
    public void actionPerformed(ActionEvent e) {
	String cmd = e.getActionCommand();
        if (CMD.CANCEL.equals(cmd)) {
	    OptionDialog.dialog.setVisible(false);
	} else if (CMD.SET.equals(cmd)) {
	    boolean failed = false;
	    System.out.println("Setting options...");

	    String s = speedTF.getText();
	    try {
		double val = Double.parseDouble(s);
		if (val <= 0) {
		    failed = true;
		    JOptionPane.showMessageDialog(this,"Invalid speed=" + s);
		    return;
		}
		Options.setSpeed(val);
		System.out.println("speed=" + val);
	    } catch(Exception ex) {
		failed = true;
		JOptionPane.showMessageDialog(this,"Cannot parse value " + s);
	    }

	    s = tTF.getText();
	    try {
		long val = Long.parseLong(s);
		if (val <= 0) {
		    failed = true;
		    JOptionPane.showMessageDialog(this,"Invalid T=" + s);
		    return;
		}
		Options.setT(val);
		System.out.println("T=" + val);
	    } catch(Exception ex) {
		failed = true;
		JOptionPane.showMessageDialog(this,"Cannot parse value " + s);
	    }


	    /*
	    s = piTF.getText();
	    try {
		s = s.trim();
		Options.parsePiList(s);
	    } catch(Exception ex) {
		failed = true;
		JOptionPane.showMessageDialog(this,"Cannot parse value " + s +"; msg=" + ex.getMessage());
	    }
	    
	    s = vsGroup.getSelection().getActionCommand();
	    System.out.println("vs sel string=" +s);
	    VSMethod vs = null;
	    try {
		vs  =VSMethod.valueOf(s);
		Options.setVSMethod(vs);
		System.out.println("Set vs=" +Options.getVSMethod());
	    } catch(Exception ex) {
		failed = true;
		JOptionPane.showMessageDialog(this,"Cannot parse value " + s);
	    }
    
	    */

	    if (!failed)  OptionDialog.dialog.setVisible(false);
        } 
    }



}
