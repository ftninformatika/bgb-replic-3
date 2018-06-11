package com.gint.app.bisis.replication.fixrn2;

import java.util.logging.Logger;

import com.gint.app.bisis.replication.util.JTextAreaLogHandler;

public class Main {
  public Main(boolean noGUI) {
    if (!noGUI) {
      JTextAreaLogHandler handler = new JTextAreaLogHandler();
      Logger.getLogger("").addHandler(handler);
      FixFrame frame = new FixFrame();
      handler.setDestination(frame.getLogArea());
      frame.setVisible(true);
    } else {
      new FixTask(null);
    }
  }
  public static void main(String[] args) {
    new Main(args.length > 0);
  }
}
