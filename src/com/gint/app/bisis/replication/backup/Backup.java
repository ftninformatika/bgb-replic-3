package com.gint.app.bisis.replication.backup;

import java.util.logging.Logger;

import com.gint.app.bisis.replication.util.JTextAreaLogHandler;

public class Backup {
  
  public Backup(boolean noGUI) {
    if (!noGUI) {
      JTextAreaLogHandler handler = new JTextAreaLogHandler();
      Logger.getLogger("").addHandler(handler);
      BackupFrame frame = new BackupFrame();
      handler.setDestination(frame.getLogArea());
      frame.setVisible(true);
    } else {
      new BackupTask(null);
    }
  }

  public static void main(String[] args) {
    new Backup(args.length > 0);
  }
}
