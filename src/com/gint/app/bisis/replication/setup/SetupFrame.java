package com.gint.app.bisis.replication.setup;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DriverManager;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

import com.gint.app.bisis.replication.util.ServerType;
import com.gint.util.gui.WindowUtils;

public class SetupFrame extends JFrame {
  
  public SetupFrame() {
    super("Instalacija servisa za replikaciju zapisa");
    
    cbServerType.addItem(ServerType.EXPORT);
    cbServerType.addItem(ServerType.SECONDARY);
    cbServerType.addItem(ServerType.IMPORT);
    
    MigLayout mig = new MigLayout(
        "insets dialog, wrap",
        "[]rel[]",
        "[]rel[]rel[]rel[]para[]para[]");
    getContentPane().setLayout(mig);
    getContentPane().add(new JLabel("Parametri servera"), "span 2, wrap");
    getContentPane().add(new JLabel("Adresa"), "");
    getContentPane().add(tfAddress, "width 120:120:150, wrap");
    getContentPane().add(new JLabel("Username"), "");
    getContentPane().add(tfUsername, "width 120:120:150, wrap");
    getContentPane().add(new JLabel("Password"), "");
    getContentPane().add(pfPassword, "width 120:120:150, wrap");
    getContentPane().add(new JLabel("Tip servera"), "");
    getContentPane().add(cbServerType, "wrap");
    getContentPane().add(btnUninstall, "tag cancel");
    getContentPane().add(btnSetup, "tag ok");
    pack();
    WindowUtils.centerOnScreen(this);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent ev) {
        handleClose();
      }
    });
    btnSetup.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        handleSetup();
      }
    });
    btnUninstall.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        handleUninstall();
      }
    });
    btnClose.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        handleClose();
      }
    });
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    getRootPane().setDefaultButton(btnSetup);
  }
  
  public void handleSetup() {
    Connection conn = getConnection();
    if (conn == null)
      return;
    ServerType serverType = (ServerType)cbServerType.getSelectedItem();
    if (serverType.exists(conn)) {
      JOptionPane.showMessageDialog(this, "\u0160ema je ve\u0107 instalirana!", 
          "Obavestenje", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    serverType.setup(conn);
    close(conn);
    JOptionPane.showMessageDialog(this, "\u0160ema je instalirana.", 
        "Obavestenje", JOptionPane.INFORMATION_MESSAGE);
  }
  
  public void handleUninstall() {
    Connection conn = getConnection();
    if (conn == null)
      return;
    ServerType serverType = (ServerType)cbServerType.getSelectedItem();
    if (!serverType.exists(conn)) {
      JOptionPane.showMessageDialog(this, "\u0160ema nije instalirana!", 
          "Obavestenje", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    serverType.remove(conn);
    close(conn);
    JOptionPane.showMessageDialog(this, "\u0160ema je uklonjena.", 
        "Obavestenje", JOptionPane.INFORMATION_MESSAGE);
  }
  
  public void handleClose() {
    System.exit(0);
  }
  
  public static void main(String[] args) {
    SetupFrame frame = new SetupFrame();
    frame.setVisible(true);
  }
  
  private String makeUrl() {
    String address = tfAddress.getText().trim();
    return "jdbc:sapdb://" + address + 
        "/BISIS?unicode=yes&sqlmode=ORACLE&autocommit=off&timeout=0&isolation=TRANSACTION_READ_UNCOMMITTED";
  }
  
  private Connection getConnection() {
    String username = tfUsername.getText().trim();
    String password = new String(pfPassword.getPassword()).trim();
    Connection conn = null;
    try {
      Class.forName("com.sap.dbtech.jdbc.DriverSapDB");
      conn = DriverManager.getConnection(makeUrl(), username, password);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage(), "Greska", 
          JOptionPane.ERROR_MESSAGE);
    }
    return conn;
  }
  
  private void close(Connection conn) {
    try {
      conn.close();
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage(), "Greska", 
          JOptionPane.ERROR_MESSAGE);
    }
  }
  
  private JTextField tfAddress = new JTextField();
  private JTextField tfUsername = new JTextField();
  private JPasswordField pfPassword = new JPasswordField();
  private JComboBox cbServerType = new JComboBox();
  private JButton btnSetup = new JButton("  Instaliraj  ");
  private JButton btnUninstall = new JButton("  Deinstaliraj  ");
  private JButton btnClose = new JButton("Zatvori");
}
