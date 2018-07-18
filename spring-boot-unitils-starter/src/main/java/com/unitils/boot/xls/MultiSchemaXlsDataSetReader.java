package com.unitils.boot.xls;

import org.apache.commons.lang.StringUtils;
import org.dbunit.database.AmbiguousTableNameException;
import org.dbunit.dataset.*;
import org.dbunit.dataset.excel.XlsDataSet;
import org.unitils.core.UnitilsException;
import org.unitils.dbunit.util.MultiSchemaDataSet;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * @Author: yangjianzhou
 * @Description:
 * @Date:Created in 2018-07-08
 */
public class MultiSchemaXlsDataSetReader {
    private String pattern = ".";
    private String defaultSchemaName;

    public MultiSchemaXlsDataSetReader(String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName;
    }

    public MultiSchemaDataSet readDataSetXls(File... dataSetFiles) {
        try {
            Map<String, List<ITable>> tbMap = getTables(dataSetFiles);
            MultiSchemaDataSet dataSets = new MultiSchemaDataSet();

            for (Map.Entry<String, List<ITable>> entry : tbMap.entrySet()) {
                List<ITable> tables = entry.getValue();
                try {
                    DefaultDataSet ds = new DefaultDataSet(tables.toArray(new ITable[]{}));
                    dataSets.setDataSetForSchema(entry.getKey(), ds);
                } catch (AmbiguousTableNameException e) {
                    throw new UnitilsException("构造DataSet失败！", e);
                }
            }
            return dataSets;
        } catch (Exception e) {
            throw new UnitilsException("解析Excel文件出错：", e);
        }
    }

    private Map<String, List<ITable>> getTables(File... dataSetFiles) {
        Map<String, List<ITable>> tableMap = new HashMap<String, List<ITable>>();
        // 需要根据schema把Table重新组合一下
        try {
            String schema, tableName;
            for (File file : dataSetFiles) {
                IDataSet dataSet = new XlsDataSet(new FileInputStream(file));
                String[] tableNames = dataSet.getTableNames();
                for (String tn : tableNames) {
                    String[] temp = tn.split(pattern);
                    if (temp.length == 2) {
                        schema = temp[0];
                        tableName = temp[1];
                    } else {
                        schema = this.defaultSchemaName;
                        tableName = tn;
                    }

                    ITable table = dataSet.getTable(tn);
                    if (!tableMap.containsKey(schema)) {
                        tableMap.put(schema, new ArrayList<ITable>());
                    }
                    tableMap.get(schema).add(new XlsTable(tableName, table));
                }
            }
        } catch (Exception e) {
            throw new UnitilsException("Unable to create DbUnit dataset for data set files: " + Arrays.toString(dataSetFiles), e);
        }
        return tableMap;
    }

    //构造XslTable表
    class XlsTable extends AbstractTable {
        private ITable delegate;
        private String tableName;

        public XlsTable(String tableName, ITable table) {
            this.delegate = table;
            this.tableName = tableName;
        }

        public int getRowCount() {
            return delegate.getRowCount();
        }

        public ITableMetaData getTableMetaData() {
            ITableMetaData meta = delegate.getTableMetaData();
            try {
                return new DefaultTableMetaData(tableName, meta.getColumns(),
                    meta.getPrimaryKeys());
            } catch (DataSetException e) {
                throw new UnitilsException("Don't get the meta info from  "
                    + meta, e);
            }
        }

        public Object getValue(int row, String column) throws DataSetException {
            Object delta = delegate.getValue(row, column);
            if (delta instanceof String) {
                if (StringUtils.isEmpty((String) delta)) {
                    return null;
                }
            }
            return delta;
        }

    }
}
