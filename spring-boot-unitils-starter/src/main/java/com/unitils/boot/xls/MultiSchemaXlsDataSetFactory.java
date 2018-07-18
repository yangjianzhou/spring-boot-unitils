package com.unitils.boot.xls;

import org.unitils.core.UnitilsException;
import org.unitils.dbunit.datasetfactory.DataSetFactory;
import org.unitils.dbunit.util.MultiSchemaDataSet;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

/**
 * @Author: yangjianzhou
 * @Description:
 * @Date:Created in 2018-07-08
 */
public class MultiSchemaXlsDataSetFactory implements DataSetFactory {

    protected String defaultSchemaName;

    public void init(Properties configuration, String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName;
    }

    public MultiSchemaDataSet createDataSet(File... dataSetFiles) {
        try {
            MultiSchemaXlsDataSetReader xlsDataSetReader = new MultiSchemaXlsDataSetReader(
                defaultSchemaName);
            return xlsDataSetReader.readDataSetXls(dataSetFiles);
        } catch (Exception e) {
            throw new UnitilsException("创建数据集失败: "
                + Arrays.toString(dataSetFiles), e);
        }
    }

    public String getDataSetFileExtension() {
        return "xls";
    }

}
