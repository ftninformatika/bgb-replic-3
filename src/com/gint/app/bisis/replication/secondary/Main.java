package com.gint.app.bisis.replication.secondary;

import java.util.logging.Logger;

import com.gint.app.bisis.replication.util.JTextAreaLogHandler;

public class Main {
  
  public Main(boolean noGUI) {
    if (!noGUI) {
      JTextAreaLogHandler handler = new JTextAreaLogHandler();
      Logger.getLogger("").addHandler(handler);
      SecondaryFrame frame = new SecondaryFrame();
      handler.setDestination(frame.getLogArea());
      frame.setVisible(true);
    } else {
      new SecondaryTask(null);
    }
  }
  
  public static void main(String[] args) {
    new Main(args.length > 0);
  }
}
