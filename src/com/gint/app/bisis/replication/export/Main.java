package com.gint.app.bisis.replication.export;

import java.util.logging.Logger;

import com.gint.app.bisis.replication.util.JTextAreaLogHandler;


public class Main {
  
  public Main(boolean noGUI) {
    if (!noGUI) {
      JTextAreaLogHandler handler = new JTextAreaLogHandler();
      Logger.getLogger("").addHandler(handler);
      ExportFrame frame = new ExportFrame();
      handler.setDestination(frame.getLogArea());
      frame.setVisible(true);
    } else {
      new ExportTask(null);
    }
  }
  
  public static void main(String[] args) {
    new Main(args.length > 0);
  }
}
