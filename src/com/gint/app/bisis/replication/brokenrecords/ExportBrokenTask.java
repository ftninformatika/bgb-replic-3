package com.gint.app.bisis.replication.brokenrecords;

import java.io.File;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gint.app.bisis.replication.util.INIFile;
import com.gint.util.file.FileUtils;

public class ExportBrokenTask {
  public ExportBrokenTask(ExportBrokenFrame frame) {
    File config = new File(FileUtils.getClassDir(getClass()) + "/broken.ini");
    if (!config.isFile()) {
      log.fatal("Ne postoji konfiguracioni fajl: " + config.getAbsolutePath());
      return;
    }
    INIFile ini = new INIFile(FileUtils.getClassDir(getClass()) + "/broken.ini");
    String driver = ini.getString("database", "driver");
    String url = ini.getString("database", "url");
    String username = ini.getString("database", "username");
    String password = ini.getString("database", "password");

    try {
      Class.forName(driver);
      Connection conn = DriverManager.getConnection(url, username, password);
      conn.setAutoCommit(false);
      log.info("Otvorena veza sa bazom.");
      int ids[] = getIDs(frame);
      log.info("Ukupno " + ids.length + " zapisa za eksport.");
      frame.setMaximum(ids.length);
      
      PreparedStatement selectRez = conn.prepareStatement(
          "SELECT document FROM documents WHERE doc_id=?");
      
      File dest = new File(FileUtils.getClassDir(getClass()) + "/broken.dat");
      RandomAccessFile out = new RandomAccessFile(dest, "rw");
      for (int i = 0; i < ids.length; i++) {
        if (ids[i] == 0)
          continue;
        selectRez.setInt(1, ids[i]);
        ResultSet rset = selectRez.executeQuery();
        if (rset.next()) {
          String rez = rset.getString(1);
          out.writeBytes(""+ids[i]+":");
          if (rez != null)
            out.writeBytes(rez);
          else
            out.writeBytes("<null>");
          out.writeBytes("\n");
        } else {
          log.warn("Ne postoji zapis sa ID=" + ids[i]);
        }
      }
      out.close();
      selectRez.close();
      conn.close();
      log.info("Zapisi eksportovani u fajl " + dest.getAbsolutePath());
    } catch (Exception ex) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace(pw);
      log.fatal(sw.toString());
    } finally {
      if (frame != null)
        frame.enableGUI();
    }
  }
  
  public int[] getIDs(ExportBrokenFrame frame) {
    String[] parts = frame.getIds().split("\\s");
    int retVal[] = new int[parts.length];
    for (int i = 0; i < retVal.length; i++)
      try {
        retVal[i] = Integer.parseInt(parts[i]);
      } catch (Exception ex) {
        //log.warn("Broj " + parts[i] + " nije ispravan.");
      }
    return retVal;
  }
  
  private static Log log = LogFactory.getLog(ExportBrokenTask.class.getName());
}