package com.gint.app.bisis.replication.brokenrecords;

import java.util.logging.Logger;

import com.gint.app.bisis.replication.util.JTextAreaLogHandler;

public class Main {
  public Main(boolean noGUI) {
    if (!noGUI) {
      JTextAreaLogHandler handler = new JTextAreaLogHandler();
      Logger.getLogger("").addHandler(handler);
      ExportBrokenFrame frame = new ExportBrokenFrame();
      handler.setDestination(frame.getLogArea());
      frame.setIDs(ids.replace(',', '\n'));
      frame.setVisible(true);
    } else {
      new ExportBrokenTask(null);
    }
  }
  public static void main(String[] args) {
    new Main(false);
  }
  
  private String ids =
    "161932,161933,161934,161936,161942,161983,162002,162013,162070,162170," +
    "162388,162588,162657,162887,162888,163070,163144,163737,164483,164484," +
    "164485,164539,164642,164779,164780,164817,165107,165486,165958,165966," +
    "166250,166267,166418,166801,166802,166803,166804,166805,166806,166807," +
    "166808,166809,166817,166822,166828,166857,166935,166947,166948,167120," +
    "167184,167505,167560,167610,167792,167800,167978,168352,168484,168590," +
    "168898,168960,169212,169397,169887,170043,170541,170710,170962,171128," +
    "171181,171188,171254,171268,171315,171317,171394,171439,171611,171789";
}
