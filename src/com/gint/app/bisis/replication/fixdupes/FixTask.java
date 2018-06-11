package com.gint.app.bisis.replication.fixdupes;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gint.app.bisis.common.records.Field;
import com.gint.app.bisis.common.records.Record;
import com.gint.app.bisis.common.records.RecordFactory;
import com.gint.app.bisis.common.records.Subfield;
import com.gint.app.bisis.common.records.Subsubfield;
import com.gint.app.bisis.replication.util.INIFile;
import com.gint.app.bisis.textsrv.ExpressionException;
import com.gint.app.bisis.textsrv.TextServer;
import com.gint.util.file.FileUtils;

public class FixTask {
  public FixTask(FixFrame frame) {
    File config = new File(FileUtils.getClassDir(getClass()) + "/fixdupes.ini");
    if (!config.isFile()) {
      log.fatal("Ne postoji konfiguracioni fajl: " + config.getAbsolutePath());
      return;
    }
    INIFile ini = new INIFile(FileUtils.getClassDir(getClass()) + "/fixdupes.ini");
    String driver = ini.getString("database", "driver");
    String url = ini.getString("database", "url");
    String username = ini.getString("database", "username");
    String password = ini.getString("database", "password");
    String mode = ini.getString("database", "mode");

    try {
      Class.forName(driver);
      Connection conn = DriverManager.getConnection(url, username, password);
      conn.setAutoCommit(false);
      TextServer txtsrv = new TextServer("sapdb", conn, url, username, password);
      log.info("Otvorena veza sa bazom.");
      int docIDs[] = getIDs("DT=a", txtsrv);
      int recordCount = docIDs.length;
      if (frame != null)
        frame.setMaximum(recordCount);
      log.info("Ukupno " + recordCount + " analitickih zapisa u bazi.");
      
      for (int i = 0; i < recordCount; i++) {
        Record rec = getRecord(docIDs[i], txtsrv);
        if (frame != null)
          frame.setValue(i+1);
        if (rec == null)
          continue;
        String RN = rec.getSubfieldContent("001e");
        if (RN == null || RN.length() == 0) {
          log.warn("Zapis ID="+docIDs[i]+" nema RN. Ignorise se.");
          continue;
        }
        String MR = rec.getSubfieldContent("4741");
        if (MR == null || MR.length() == 0) {
          log.warn("Zapis RN="+RN+" nema MR (4741). Ignorise se.");
          continue;
        }
        int clashes = txtsrv.select("MR="+MR);
        if (clashes < 2)
          continue;
        int clashIDs[] = new int[clashes];
        for (int j = 0; j < clashes; j++) {
          clashIDs[j] = txtsrv.getDocID(j+1);
          if (docIDs[i] == clashIDs[j])
            continue;
          Record clashRec = getRecord(clashIDs[j], txtsrv);
          if (clashRec == null)
            continue;
          if (recordsEqual(rec, clashRec)) {
            String clashRN = clashRec.getSubfieldContent("001e");
            if (mode.toLowerCase().equals("test")) {
              log.info("PRONADJENA DVA JEDNAKA ZAPISA: \n"
                  + RecordFactory.toFullFormat(0, rec) + "\n"
                  + RecordFactory.toFullFormat(0, clashRec));
            } else {
              log.info("Za zapis RN="+RN+" obrisan duplikat RN="+clashRN);
              txtsrv.delete(clashIDs[j]);
            }
          }
        }
      }
      conn.close();
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
  
  private int[] getIDs(String query, TextServer txtsrv) throws ExpressionException {
    int count = txtsrv.select(query);
    int docIDs[] = new int[count];
    for (int i = 0; i < count; i++)
      docIDs[i] = txtsrv.getDocID(i+1);
    return docIDs;
  }
  
  private Record getRecord(int id, TextServer txtsrv) {
    String rez = txtsrv.getDoc(id);
    if (rez == null || rez.length() == 0)
      return null;
    try {
      Record rec = RecordFactory.fromUNIMARC(0, rez);
      return rec;
    } catch (Exception ex) {
      log.warn("Parsiranje zapisa ID="+id+" je puklo. Ignorise se.");
      return null;
    }
  }
  
  private boolean recordsEqual(Record rec1, Record rec2) {
    if (rec1 == null && rec2 == null)
      return true;
    if (rec1 != null && rec2 != null) {
      if (rec1.getFields().size() != rec2.getFields().size())
        return false;
      for (int i = 0; i < rec1.getFields().size(); i++) {
        Field f1 = (Field)rec1.getFields().get(i);
        Field f2 = (Field)rec2.getFields().get(i);
        if (!fieldsEqual(f1, f2))
          return false;
      }
      return true;
    } else
      return false;
  }
  
  private boolean fieldsEqual(Field f1, Field f2) {
    if (f1 == null && f2 == null)
      return true;
    if (f1 != null && f2 != null) {
      if (!f1.getName().equals(f2.getName()))
        return false;
      if (f1.getSubfields().size() != f2.getSubfields().size())
        return false;
      for (int i = 0; i < f1.getSubfields().size(); i++) {
        Subfield sf1 = (Subfield)f1.getSubfields().get(i);
        Subfield sf2 = (Subfield)f2.getSubfields().get(i);
        if (f1.getName().equals("001") && sf1.getName()=='e' && sf2.getName()=='e')
          return true;
        if (!subfieldsEqual(sf1, sf2))
          return false;
      }
      return true;
    } else
      return false;
  }
  
  private boolean subfieldsEqual(Subfield sf1, Subfield sf2) {
    if (sf1 == null && sf2 == null)
      return true;
    if (sf1 != null && sf2 != null) {
      if (sf1.getName() != sf2.getName())
        return false;
      if (sf1.getSecField() != null && sf2.getSecField() != null)
        return fieldsEqual(sf1.getSecField(), sf2.getSecField());
      if (sf1.getSecField() != null && sf2.getSecField() == null)
        return false;
      if (sf1.getSecField() == null && sf2.getSecField() != null)
        return false;
      if (sf1.getSubsubfields().size() > 0 || sf2.getSubsubfields().size() > 0) {
        if (sf1.getSubsubfields().size() != sf2.getSubsubfields().size())
          return false;
        for (int i = 0; i < sf1.getSubsubfields().size(); i++) {
          Subsubfield ssf1 = (Subsubfield)sf1.getSubsubfields().get(i);
          Subsubfield ssf2 = (Subsubfield)sf1.getSubsubfields().get(i);
          if (!subsubfieldsEqual(ssf1, ssf2))
            return false;
        }
        return true;
      } else {
        String c1 = sf1.getContent();
        String c2 = sf2.getContent();
        if (c1 == c2)
          return true;
        if (c1 == null && c2 != null)
          return false;
        if (c1 != null && c2 == null)
          return false;
        return c1.equals(c2);
      }
    } else
      return false;
  }
  
  private boolean subsubfieldsEqual(Subsubfield ssf1, Subsubfield ssf2) {
    if (ssf1 == null && ssf2 == null)
      return true;
    if (ssf1 != null && ssf2 != null) {
      if (ssf1.getName() != ssf2.getName())
        return false;
      String c1 = ssf1.getContent();
      String c2 = ssf2.getContent();
      if (c1 == c2)
        return true;
      if (c1 == null && c2 != null)
        return false;
      if (c1 != null && c2 == null)
        return false;
      return c1.equals(c2);
    } else
      return false;
  }
  
  private static Log log = LogFactory.getLog(FixTask.class.getName());
}
