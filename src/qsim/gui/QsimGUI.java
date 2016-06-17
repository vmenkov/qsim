package qsim.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import java.io.*;
import java.util.*;
import java.text.*;

import qsim.*;

/** The main Queue Simulation GUI driver
 */
public class QsimGUI extends MyJFrame {

    static boolean debugMode = false; //true;
    QsimPanel abs = null;
    JLabel msgLabel = null;
 

    /** The queue simulator whose behavior is displayed in the GUI */
    Qsim qsim = null;

  // GUI components - menu items
    private JMenuItem openConfigItem, openArrivalsItem;          // file menu
    /*
    private JMenuItem readPiListItem;          
    private JMenuItem readFrontierItem;          
    */
    private JMenuItem saveLogItem;
    /*
    private JMenuItem writeFrontierImgItem;
    private JMenuItem saveSensorsItem;
    */
    private JMenuItem exitItem;

    private JMenuItem optionsItem;          // options menu


    void setLabel(String msg ) {
	setLabel(msg, msg);
    }

    void setLabel(String msg, String msgLong) {
	debugln(msgLong);
	msgLabel.setText(msg);
	msgLabel.repaint(2); // trying to to make sure the label shows right away
    }

    File filedir = new File(".");

    JMenu fileMenu, runMenu, optionsMenu;

    void setMenuEnabled(boolean yes) {
	fileMenu.setEnabled(yes);
	//runMenu.setEnabled(yes);
	optionsMenu.setEnabled(yes);
    }


    public QsimGUI() {
	super("Multilane screening simulation, Qsim ver. "+ Qsim.version);
	
	JMenuBar menuBar = new JMenuBar();
	setJMenuBar(menuBar);
	MenuHandler menuHandler = new MenuHandler();
	
	// file menu
	//JMenu 
	fileMenu = new JMenu("File");
	menuBar.add(fileMenu);
	
	openConfigItem = new JMenuItem("Read Config file", 'R' );
	openConfigItem.addActionListener(menuHandler);
	fileMenu.add(openConfigItem);

	openArrivalsItem = new JMenuItem("Read Arrivals schedule file", 'A' );
	openArrivalsItem.addActionListener(menuHandler);
	fileMenu.add(openArrivalsItem);
	openArrivalsItem.setEnabled(false);

	saveLogItem = new JMenuItem("Save log file", 'S' );
	saveLogItem.addActionListener(menuHandler);
	fileMenu.add(saveLogItem);
	
	/*
	readPiListItem= new JMenuItem("Read Pi list file");//, 'O' );
	readPiListItem.addActionListener(menuHandler);
	fileMenu.add(readPiListItem);

	readFrontierItem= new JMenuItem("Read saved frontier file");//, 'O' );
	readFrontierItem.addActionListener(menuHandler);
	fileMenu.add(readFrontierItem);
	*/
	fileMenu.addSeparator(); // separate line
	/*
	saveFrontierItem = new JMenuItem("Save Frontier (text file)");//, 'S' );
	saveFrontierItem.addActionListener(menuHandler);
	fileMenu.add(saveFrontierItem);

	writeFrontierImgItem = new JMenuItem("Write Frontier (image)");//, 'W' );
	writeFrontierImgItem.addActionListener(menuHandler);
	fileMenu.add(writeFrontierImgItem);

	saveSensorsItem = new JMenuItem("Save (approximated) sensors");//, '' );
	saveSensorsItem.addActionListener(menuHandler);
	fileMenu.add(saveSensorsItem);
	*/
	
	exitItem = new JMenuItem("Exit", 'x' );
	exitItem.addActionListener(menuHandler);
	fileMenu.add(exitItem);

	// run menu
	/*
	runMenu = new JMenu("Run");
	menuBar.add(runMenu);
	*/
	/*
	computeFrontierItem = new JMenuItem("Compute Frontier"); //, 'x' );
	computeFrontierItem.addActionListener(menuHandler);
	runMenu.add(computeFrontierItem);
	*/

	// options menu
	//JMenu 
	optionsMenu = new JMenu("Options");
	menuBar.add(optionsMenu);

	/*
	debugItem = new JCheckBoxMenuItem("Debug", Frontier.debug);
	debugItem.addActionListener(menuHandler);
	optionsMenu.add(debugItem);

	paranoidItem = new JCheckBoxMenuItem("Paranoid checking", Options.paranoid);
	paranoidItem.addActionListener(menuHandler);
	optionsMenu.add(paranoidItem);
	optionsMenu.addSeparator(); // separate line

	foldItem = new JCheckBoxMenuItem("Compact tree print & plot", Options.fold);
	foldItem.addActionListener(menuHandler);
	optionsMenu.add(foldItem);

	signaturesItem = new JCheckBoxMenuItem("Save (C,D) only", Options.signaturesOnly);
	signaturesItem.addActionListener(menuHandler);
	optionsMenu.add(signaturesItem);

	otherFrontItem = new JCheckBoxMenuItem("Show subset frontiers", showSubsetFrontiers);
	otherFrontItem.addActionListener(menuHandler);
	optionsMenu.add(otherFrontItem);

	applyEpsItem = new JCheckBoxMenuItem("Simplify sensors using eps", Options.epsAppliesToSensors);
	applyEpsItem.addActionListener(menuHandler);
	optionsMenu.add( applyEpsItem);

	useSimplifiedSensorsItem = new JCheckBoxMenuItem("Describe policies in terms of simplified sensors", Options.useSimplifiedSensors);
	useSimplifiedSensorsItem.addActionListener(menuHandler);
	optionsMenu.add( useSimplifiedSensorsItem);
	*/

	optionsItem = new JMenuItem("More algo options...", 'o' );
	optionsItem.addActionListener(menuHandler);
	optionsMenu.add(optionsItem);

	int width = 600;
	int height = 400;
	setSize(width, height);
	setLocation(100, 0);

	abs = new QsimPanel(width, height, this);
	msgLabel = new JLabel("Welcome to Multilane screening simulator! Use 'File|Read config' to initialize");

	Container c = getContentPane();
	c.setLayout(new BorderLayout());
	c.add(msgLabel, BorderLayout.SOUTH);
	c.add(abs);
	setVisible(true);
    }

    static private qsim.Parameters para=null;
 
    public static void main(String argv[]) {
	Options.setInteractive(true);
	QsimGUI app = new QsimGUI();
	app.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    });
	if (argv.length > 0) {	    
	    app.readConfig(new File(argv[0]));
	}
    }

    /** Class MenuHandler: handling all menu events. */
    private class MenuHandler implements ActionListener {
	public void actionPerformed(ActionEvent e) {
	    // file menu
	    if (e.getSource() == openConfigItem)
		openConfig();
	    else if (e.getSource() ==openArrivalsItem)
		openArrivalsFile();
	    else if (e.getSource() == saveLogItem)
		saveLog();
	    else if (e.getSource() == exitItem)
		System.exit(0);
/*
	    else if (e.getSource() == computeFrontierItem)
		computeFrontier();
	    else if (e.getSource() == debugItem) {
		Frontier.debug = debugItem.isSelected();
		System.out.println("Setting debug " + (Frontier.debug? "on": "off"));
	    } else if (e.getSource() == paranoidItem) {
		Options.paranoid = paranoidItem.isSelected();
		System.out.println("Setting Options.paranoid = "+Options.paranoid);
	    } else if (e.getSource() == foldItem) {
		Options.fold = foldItem.isSelected();
		System.out.println("Setting Options.fold=" + Options.fold);
	    } else if (e.getSource() == signaturesItem) {
		Options.signaturesOnly = signaturesItem.isSelected();
		System.out.println("Setting Options.signaturesOnly=" + Options.signaturesOnly);
	    } else if (e.getSource() ==  otherFrontItem) {
		showSubsetFrontiers = otherFrontItem.isSelected();
		System.out.println("Setting showSubsetFrontiers=" + showSubsetFrontiers);
	    } else if (e.getSource() ==  applyEpsItem) {		    
		Options.epsAppliesToSensors= applyEpsItem.isSelected();
		System.out.println("Setting Options.applyEpsItem=" + Options.epsAppliesToSensors);
	    } else if (e.getSource() ==  useSimplifiedSensorsItem) {
		Options.useSimplifiedSensors = useSimplifiedSensorsItem.isSelected();
		System.out.println("Setting Options.useSimplifiedSensors=" + 
				   Options.useSimplifiedSensors );
	    } 
*/
	    else if (e.getSource() == optionsItem) {
		optionDialog();
	    }
	}
    }

     
   public static void debug(String s) {
	if (debugMode)	    System.out.print(s);
    }

    public static void debugln(String s) {
	if (debugMode)	    System.out.println(s);
    }

    /** 
     * Reads config paramters
     */
    public void openConfig() {
	JFileChooser fileChooser = new JFileChooser(filedir);
	fileChooser.setDialogTitle("Open a config file");

	//-- this class is only available in JDK 1.6
	/*
	FileNameExtensionFilter filter = new FileNameExtensionFilter(
	    "Text files", "txt", "conf", "cnf");
	fileChooser.setFileFilter(filter);
	*/

	int returnVal = fileChooser.showOpenDialog(this);
	
	if (returnVal == JFileChooser.APPROVE_OPTION) {
	    File file = fileChooser.getSelectedFile();
	    readConfig(file);
	}
    }

    /** Reads the specified configuration file, initializes model
	parameters and the main queue simulator (Qsim) object.
     */
    private void readConfig(File file) {

	//	String filepath = file.getPath();
	//	filedir = file.getParentFile();

	setLabel("Reading config file "+ file);
	
	try {
	    para = new qsim.Parameters(file);
  	    setLabel("Successfully read data: " + para);

	    long now = 0;
	    qsim = new Qsim( para, now, null);
	    // re-layout the queue display area
	    abs.layout(para.L);
	    openArrivalsItem.setEnabled(true);
	} catch (Exception e) {
	    String msg0 = "Error when reading config file " + file;
	    String msg = msg0 + ":\n" + e.getMessage();
	    setLabel(msg0, msg);

	    if (!(e instanceof Parameters.ParseException)) {
		e.printStackTrace(System.err);
		msg += "\nPlease see the standard output for the stack trace";
	    }
	    JOptionPane.showMessageDialog(this,msg);
	}
    }

    public void openArrivalsFile() {
	JFileChooser fileChooser = new JFileChooser(filedir);
	fileChooser.setDialogTitle("Open an arrival schedule file");

	//-- this class is only available in JDK 1.6
	/*
	FileNameExtensionFilter filter = new FileNameExtensionFilter(
	    "Text files", "txt", "conf", "cnf");
	fileChooser.setFileFilter(filter);
	*/

	int returnVal = fileChooser.showOpenDialog(this);
	
	if (returnVal == JFileChooser.APPROVE_OPTION) {
	    File file = fileChooser.getSelectedFile();
	    readArrivalsFile(file);
	}
    }

    private void readArrivalsFile(File file) {
	if (para==null) {
	    setLabel("Please read a config file first");
	    return;
	}
	try {
	    StaticArrivalSchedule[] ss = StaticArrivalSchedule.readFile(file, para.L);
	    long now = 0;
	    qsim = new Qsim( para, now, ss);
	    // re-layout the queue display area (this is needed to
	    // associate the display tool with the new qsim)
	    abs.layout(para.L);
	    setLabel("Successfully read arrivals schedule from " + file);
	} catch (Exception e) {
	    String msg0 = "Error when reading arrivals file " + file;
	    String msg = msg0 + ":\n" + e.getMessage();
	    setLabel(msg0, msg);

	    if (!(e instanceof Parameters.ParseException)) {
		e.printStackTrace(System.err);
		msg += "\nPlease see the standard output for the stack trace";
	    }
	    JOptionPane.showMessageDialog(this,msg);
	}
    }


   /** Saves the log for this run
     */
    void saveLog() {

	if (qsim == null || qsim.getNow()==0) {

	    String msg=
		"No configuration has been loaded, or no simulation has been run with this configuration yet.\n Please read a config file and run a simulation first";
	    System.out.println(msg);
	    JOptionPane.showMessageDialog(this,msg);
	    return;
	}

	JFileChooser fileChooser = new JFileChooser(filedir);
	fileChooser.setDialogTitle("Specify an existing or new text file to (over)write");

	int returnVal = fileChooser.showOpenDialog(this);
	String filepath = "";
	
	if (returnVal == JFileChooser.APPROVE_OPTION) {
	    File file = fileChooser.getSelectedFile();
	    setLabel("Saving log to " + file);
	    try {
		qsim.saveLog(file);
	    } catch (Exception e) {
		String msg0 = "Error when writing log file " + file;
		String msg = msg0 + ":\n" + e.getMessage();
		setLabel(msg0, msg);
		e.printStackTrace(System.err);
		msg += "\nPlease see the standard output for the stack trace";
		JOptionPane.showMessageDialog(this,msg);
	    }
	}
    }


   public void optionDialog() {

	OptionDialog.showDialog(this,
				optionsItem,
				"Current option values:",
				"Frontier Finder options");

	return;
    }



}

 