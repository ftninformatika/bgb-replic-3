package com.gint.app.bisis.replication.fixrn2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

import com.gint.util.gui.SwingWorker;
import com.gint.util.gui.WindowUtils;

public class FixFrame extends JFrame {

  public FixFrame() {
    super("Skidanje vodecih nula u RN i MR");
    pbProgress.setStringPainted(true);
    pbProgress.setMinimum(0);
    pbProgress.setValue(0);
    JScrollPane sp = new JScrollPane();
    sp.getViewport().setView(taLog);
    MigLayout mig = new MigLayout(
        "insets dialog, wrap",
        "[]rel[]",
        "[]rel[]rel[]para[]");
    getContentPane().setLayout(mig);
    getContentPane().add(new JLabel("Log"), "span 2, wrap");
    getContentPane().add(sp, "width 300:400:500, height 100:200:300, span 2, wrap");
    getContentPane().add(pbProgress, "span 2, growx, wrap");
    getContentPane().add(btnClose, "tag cancel");
    getContentPane().add(btnStart, "span 2, tag ok, wrap");
    pack();
    WindowUtils.centerOnScreen(this);
    
    getRootPane().setDefaultButton(btnStart);
    btnStart.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleStart();
      }
    });
    btnClose.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleClose();
      }
    });
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        handleClose();
      }
    });
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
  }
  
  public void handleStart() {
    SwingWorker worker = new SwingWorker() {
      public Object construct() {
        return new FixTask(FixFrame.this);
      }
    };
    worker.start();
    disableGUI();
  }

  public void handleClose() {
    if (!disabled)
      System.exit(0);
  }
  
  public JTextArea getLogArea() {
    return taLog;
  }
  
  public void setValue(int value) {
    pbProgress.setValue(value);
  }
  
  public void setMaximum(int maximum) {
    pbProgress.setMaximum(maximum);
  }
  
  public void disableGUI() {
    btnStart.setEnabled(false);
    btnClose.setEnabled(false);
    disabled = true;
  }
  
  public void enableGUI() {
    btnStart.setEnabled(true);
    btnClose.setEnabled(true);
    getRootPane().setDefaultButton(btnClose);
    disabled = false;
  }
  
  private JButton btnStart = new JButton("   Start   ");
  private JButton btnClose = new JButton("Zatvori");
  private JProgressBar pbProgress = new JProgressBar();
  private JTextArea taLog = new JTextArea();
  private boolean disabled = false;
}

