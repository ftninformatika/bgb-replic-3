package com.gint.app.bisis.replication.backup;

import java.util.logging.Logger;

import com.gint.app.bisis.replication.util.JTextAreaLogHandler;

public class Restore {

  public Restore(boolean noGUI) {
    if (!noGUI) {
      JTextAreaLogHandler handler = new JTextAreaLogHandler();
      Logger.getLogger("").addHandler(handler);
      RestoreFrame frame = new RestoreFrame();
      handler.setDestination(frame.getLogArea());
      frame.setVisible(true);
    } else {
      new RestoreTask(null);
    }
    
  }
  
  public static void main(String[] args) {
    new Restore(args.length > 0);
  }
}
