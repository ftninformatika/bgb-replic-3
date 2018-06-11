package com.gint.app.bisis.replication.importt;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gint.app.bisis.common.records.Field;
import com.gint.app.bisis.common.records.Record;
import com.gint.app.bisis.common.records.RecordFactory;
import com.gint.app.bisis.common.records.Subfield;
import com.gint.app.bisis.replication.util.INIFile;
import com.gint.app.bisis.textsrv.TextServer;
import com.gint.util.file.FileUtils;

public class ImportTask {
  
  public ImportTask(ImportFrame frame) {
    File config = new File(FileUtils.getClassDir(getClass()) + "/import.ini");
    if (!config.isFile()) {
      log.fatal("Ne postoji konfiguracioni fajl: " + config.getAbsolutePath());
      return;
    }
    INIFile ini = new INIFile(FileUtils.getClassDir(getClass()) + "/import.ini");
    String secDriver = ini.getString("secondary", "driver");
    String secUrl = ini.getString("secondary", "url");
    String secUsername = ini.getString("secondary", "username");
    String secPassword = ini.getString("secondary", "password");
    String impDriver = ini.getString("import", "driver");
    String impUrl = ini.getString("import", "url");
    String impUsername = ini.getString("import", "username");
    String impPassword = ini.getString("import", "password");
    Date today = new Date();
    
    try {
      Class.forName(secDriver);
      Connection secConn = DriverManager.getConnection(secUrl, secUsername, secPassword);
      secConn.setAutoCommit(false);
      TextServer secSrv = new TextServer("sapdb", secConn, secUrl, secUsername, secPassword);
      log.info("Otvorena veza sa sekundarnom bazom.");
      Class.forName(impDriver);
      Connection impConn = DriverManager.getConnection(impUrl, impUsername, impPassword);
      impConn.setAutoCommit(false);
      TextServer impSrv = new TextServer("sapdb", impConn, impUrl, impUsername, impPassword);
      log.info("Otvorena veza sa osnovnom bazom.");
      
      Statement stmt = impConn.createStatement();
      ResultSet rset = stmt.executeQuery("SELECT count(*) FROM record_map");
      rset.next();
      int recordCount = rset.getInt(1);
      rset.close();
      stmt.close();
      if (frame != null)
        frame.setMaximum(recordCount);
      log.info("Ukupno " + recordCount + " preuzetih zapisa.");
      PreparedStatement pstmt = impConn.prepareStatement(
          "UPDATE record_map SET last_sync_date=? WHERE my_record_rn=?");
      stmt = impConn.createStatement();
      rset = stmt.executeQuery(
          "SELECT my_record_rn, imported_record_rn, last_sync_date FROM record_map");
      int count = 0;
      while (rset.next()) {
        count++;
        if (frame != null)
          frame.setValue(count);
        int myRN = rset.getInt(1);
        int origRN = rset.getInt(2);
        Date lastSync = rset.getDate(3);
        if (lastSync.after(today))
          continue;
        int hitCount = secSrv.select("RN="+origRN);
        if (hitCount == 0) {
          log.warn("U sekundarnoj bazi nema zapisa RN="+origRN);
          continue;
        }
        if (hitCount > 1) {
          log.warn("U sekundarnoj bazi ima vise od jednog zapisa sa RN="+origRN);
          continue;
        }
        int origID = secSrv.getDocID(1);
        String origRez = secSrv.getDoc(origID);
        Record origRec = RecordFactory.fromUNIMARC(0, origRez);
        String dates = origRec.getSubfieldContent("000b");
        if (dates == null) {
          log.warn("Zapis RN="+origRN+" u sekundarnoj bazi nema potpolje 000b.");
          continue;
        }
        if (dates.length() != 16) {
          log.warn("Zapis RN="+origRN+" u sekundarnoj bazi ima neispravan sadrzaj u potpolju 000b.");
          continue;
        }
        String sLastModified = dates.substring(8);
        Date lastModified = null;
        try {
          lastModified = sdf.parse(sLastModified);
        } catch (Exception ex) {
          log.warn("Zapis RN="+origRN+" u sekundarnoj bazi ima neispravan datum poslednje izmene (000b): " + sLastModified);
          continue;
        }
        if (lastModified.before(lastSync))
          continue;
        hitCount = impSrv.select("RN="+myRN);
        if (hitCount == 0) {
          log.warn("U lokalnoj bazi nema zapisa RN="+myRN);
          continue;
        }
        if (hitCount > 1) {
          log.warn("U lokalnoj bazi ima vise od jednog zapisa sa RN="+myRN);
          continue;
        }
        int myID = impSrv.getDocID(1);
        String myRez = impSrv.getDoc(myID);
        Record myRec = RecordFactory.fromUNIMARC(0, myRez);
        Record ustrikan = ustrikaj(myRec, origRec);
        impSrv.update(myID, RecordFactory.toUNIMARC(0, ustrikan));
        pstmt.setDate(1, new java.sql.Date(today.getTime()));
        pstmt.setInt(2, myRN);
        pstmt.executeUpdate();
        impConn.commit();
        log.info("Sinhronizovan zapis RN=" + myRN);
      }
      rset.close();
      stmt.close();
      pstmt.close();
      kopirajAnalitiku(secConn, impConn, secSrv, impSrv);
      secConn.close();
      impConn.close();
      log.info("Sinhronizacija zavrsena.");
      if (frame != null)
        frame.enableGUI();
    } catch (Exception ex) {
      log.fatal(ex);
    }
  }
  
  private Record ustrikaj(Record myRec, Record origRec) {
    Record retVal = new Record();
    // iz lokalnog se cuvaju polja 000, 001, 992, 996 i 997
    Field f000 = myRec.getField("000");
    if (f000 != null)
      retVal.add(f000);
    Field f001 = myRec.getField("001");
    if (f001 != null)
      retVal.add(f001);
    Iterator it = origRec.getFields().iterator();
    while (it.hasNext()) {
      Field f = (Field)it.next();
      if (f.getName().equals("000") || f.getName().equals("001") ||
          f.getName().equals("992") || f.getName().equals("996") ||
          f.getName().equals("997"))
        continue;
      retVal.add(f);
    }
    Field f992 = myRec.getField("992");
    if (f992 != null)
      retVal.add(f992);
    List l996 = myRec.getFields("996");
    retVal.getFields().addAll(l996);
    List l997 = myRec.getFields("997");
    retVal.getFields().addAll(l997);
    return retVal;
  }
  
  // za sve zapise u sekundarnoj bazi koji imaju svoju analitiku
  // proveri da li su preuzeti u primarnu bazu. ako su preuzeti, a nemaju
  // preuzete i analiticke zapise, prekopiraj analiticke zapise iz sekundarne
  // u primarnu bazu i pri tome azuriraj njihov MR tako da pokazuju na lokalni
  // RN glavnog zapisa u primarnoj bazi
  private void kopirajAnalitiku(Connection secConn, Connection impConn, 
      TextServer secSrv, TextServer impSrv) throws Exception {
    
    // proveri da li je master zapis preuzet
    PreparedStatement findMasterRecord = impConn.prepareStatement(
        "SELECT my_record_rn FROM record_map WHERE imported_record_rn=?");
    
    PreparedStatement countAnalyticRecords = impConn.prepareStatement(
        "SELECT count(*) FROM pref_MR WHERE content=?");
    
    PreparedStatement insertAnalyticalInMap = impConn.prepareStatement(
        "INSERT INTO record_map (my_record_rn, imported_record_rn, last_sync_date) " +
        "VALUES (?, ?, ?)");
    
    // dohvati RN svih glavnih zapisa u sekundarnoj bazi koji imaju analitiku
    Statement stmt = secConn.createStatement();
    ResultSet rset = stmt.executeQuery(
        "SELECT DISTINCT content FROM pref_MR WHERE doc_id IN " +
        "(SELECT doc_id FROM pref_DT WHERE content='A')");

    while (rset.next()) {
      int masterRN = Integer.parseInt(rset.getString(1));
      findMasterRecord.setInt(1, masterRN);
      ResultSet rset1 = findMasterRecord.executeQuery();
      if (rset1.next()) {
        int localMasterRN = rset1.getInt(1);
        countAnalyticRecords.setString(1, Integer.toString(localMasterRN));
        ResultSet rset2 = countAnalyticRecords.executeQuery();
        rset2.next();
        int analyticCount = rset2.getInt(1);
        rset2.close();
        if (analyticCount == 0) {
          Record[] records = getRecords(secSrv, masterRN);
          for (int i = 0; i < records.length; i++) {
            if (records[i] == null)
              continue;
            Subfield RN = records[i].getSubfield("001e");
            Subfield MR = records[i].getSubfield("4741");
            if (RN == null || MR == null) {
              log.warn("Zapis u sekundarnoj bazi nema RN ili MR: " + 
                  RecordFactory.toFullFormat(0, records[i]));
              continue;
            }
            int myRN = getRN(impConn);
            int origRN = 0;
            try {
              origRN = Integer.parseInt(RN.getContent());
            } catch (Exception ex) {
              log.warn("Zapis u sekundarnoj bazi ima neispravan RN: " + RN.getContent());
              continue;
            }
            RN.setContent(Integer.toString(myRN));
            MR.setContent(Integer.toString(localMasterRN));
            impSrv.insert(RecordFactory.toUNIMARC(0, records[i]));
            insertAnalyticalInMap.clearParameters();
            insertAnalyticalInMap.setInt(1, myRN);
            insertAnalyticalInMap.setInt(2, origRN);
            insertAnalyticalInMap.setDate(3, new java.sql.Date(
                new java.util.Date().getTime()));
            insertAnalyticalInMap.executeUpdate();
            impConn.commit();
          }
        }
      }
      rset1.close();
    }
    findMasterRecord.close();
    countAnalyticRecords.close();
    rset.close();
    stmt.close();
  }
  
  private Record[] getRecords(TextServer srv, int MR) throws Exception {
    Record[] retVal;
    int hitCount = srv.select("MR="+MR);
    retVal = new Record[hitCount];
    for (int i = 0; i < hitCount; i++) {
      int id = srv.getDocID(i + 1);
      String rez = srv.getDoc(id);
      if (rez == null || rez.length() == 0)
        retVal[i] = null;
      else
        retVal[i] = RecordFactory.fromUNIMARC(0, rez);
    }
    return retVal;
  }
  
  public int getRN(Connection conn) {
    int RN = 0;
    try {
      Statement stmt = conn.createStatement();
      ResultSet rset = stmt.executeQuery(
          "SELECT counter_value FROM misc_counters " +
          "WHERE counter_name='RN' FOR UPDATE");
      if (rset.next()) {
        RN = rset.getInt(1) + 1;
        stmt.executeUpdate(
            "UPDATE misc_counters SET counter_value=counter_value+1 " +
            "WHERE counter_name='RN'");
      } else {
        RN = 0;
      }
      rset.close();
      stmt.close();
      conn.commit();
    } catch (Exception ex) {
      log.fatal("Greska kod dobijanja novog RN broja.");
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace(pw);
      log.fatal(sw.toString());
    }
    return RN;
  }

  private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
  private static Log log = LogFactory.getLog(ImportTask.class.getName());
  
}
