package qsim.gui;

import javax.swing.*;

abstract class MyJFrame extends JFrame {

    MyJFrame(String title) { super(title); }

    /** Some method to produce a message to the user - maybe inside a label 
	at the right place... */
    abstract void setLabel(String msg);    

}