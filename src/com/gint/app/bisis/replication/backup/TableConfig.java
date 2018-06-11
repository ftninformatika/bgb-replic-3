package com.gint.app.bisis.replication.backup;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TableConfig {
  
  public static HashMap columns = new HashMap();
  public static HashMap types = new HashMap();
  
  public static List getColumns(String tableName) {
    return (List)columns.get(tableName);
  }
  
  public static List getTypes(String tableName) {
    return (List)types.get(tableName);
  }

  static {
    // EXPORT_HISTORY
    List exportHistoryCols = new ArrayList();
    List exportHistoryTypes = new ArrayList();
    exportHistoryCols.add("export_date");
    exportHistoryTypes.add(new Integer(Types.DATE));
    exportHistoryCols.add("export_type");
    exportHistoryTypes.add(new Integer(Types.CHAR));
    columns.put("EXPORT_HISTORY", exportHistoryCols);
    types.put("EXPORT_HISTORY", exportHistoryTypes);
    
    // REPLICATION_HISTORY
    List replicationHistoryCols = new ArrayList();
    List replicationHistoryTypes = new ArrayList();
    replicationHistoryCols.add("replication_date");
    replicationHistoryTypes.add(new Integer(Types.DATE));
    replicationHistoryCols.add("replication_type");
    replicationHistoryTypes.add(new Integer(Types.CHAR));
    columns.put("REPLICATION_HISTORY", replicationHistoryCols);
    types.put("REPLICATION_HISTORY", replicationHistoryTypes);
    
    // IMPORT_HISTORY
    List importHistoryCols = new ArrayList();
    List importHistoryTypes = new ArrayList();
    importHistoryCols.add("import_date");
    importHistoryTypes.add(new Integer(Types.DATE));
    columns.put("IMPORT_HISTORY", importHistoryCols);
    types.put("IMPORT_HISTORY", importHistoryTypes);
    
    // RECORD_MAP
    List recordMapCols = new ArrayList();
    List recordMapTypes = new ArrayList();
    recordMapCols.add("last_sync_date");
    recordMapTypes.add(new Integer(Types.DATE));
    recordMapCols.add("my_record_rn");
    recordMapTypes.add(new Integer(Types.INTEGER));
    recordMapCols.add("imported_record_rn");
    recordMapTypes.add(new Integer(Types.INTEGER));
    columns.put("RECORD_MAP", recordMapCols);
    types.put("RECORD_MAP", recordMapTypes);
  }

}
