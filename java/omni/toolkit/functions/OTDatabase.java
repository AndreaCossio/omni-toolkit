package omni.toolkit.functions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.naming.Context;

import com.appiancorp.services.ServiceContext;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.expression.annotations.*;
import com.appiancorp.suiteapi.type.AppianType;
import com.appiancorp.suiteapi.type.TypedValue;
import com.appiancorp.suiteapi.type.config.ImportResult;
import com.appiancorp.suiteapi.type.config.xsd.XsdTypeImporter;

import omni.toolkit.OTHelper;

@OTCategory
public class OTDatabase {

    @Function
    public TypedValue otGetTableMetadata(
            Context ctx,
            @Parameter @Name("dataSource") String dataSource,
            @Parameter @Name("tableName") String tableName) {
        
        /* Vars */
        Connection conn = null;
        ResultSet rsColumns = null;
        ArrayList<Map<Object, Object>> resultList = null;
                
        try {
            /* Connection to db */
            conn = OTHelper.getConnection(ctx, dataSource);

            /* Get columns of the table */
            rsColumns = OTHelper.getColumnsOfTable(conn, tableName);

            /* Return early if empty */
            if (rsColumns.next() == false) {
                return null;
            }

            /* Metadata */
            ResultSetMetaData rsmd = rsColumns.getMetaData();

            /* Result container */
            resultList = new ArrayList<Map<Object, Object>>();

            /* Iterate over columns */
            do {
                Map<Object, Object> row = new LinkedHashMap<>();

                /* Iterate over metadata fields */
                for (int i = 1; i < rsmd.getColumnCount() + 1; i++) {
                    row.put(OTHelper.stringTypedValue(rsmd.getColumnLabel(i)), OTHelper.stringTypedValue(rsColumns.getString(i)));
                }

                resultList.add(row);
            } while (rsColumns.next());
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        } finally {
            if (rsColumns != null) {
                try {
                    rsColumns.close();
                } catch (SQLException e) { /* Ignored */}
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) { /* Ignored */}
            }
        }

        /* Return list of dictionaries */
        return new TypedValue((long) AppianType.LIST_OF_MAP, resultList.toArray(new Map[resultList.size()]));
    }

    @Function
    public String otGetTableXsd(
            Context ctx,
            @Parameter @Name("dataSource") String dataSource,
            @Parameter @Name("tableName") String tableName,
            @Parameter @Name("targetNamespace") String targetNamespace,
            @Parameter @Name("name") String name,
            @Parameter @Name("description") String description) {

        /* XSD of the table */
        return OTHelper.getTableXsd(ctx, dataSource, tableName, targetNamespace, name, description);
    }

    @Function
    public Long[] otUpdateDataType(
            Context ctx,
            ServiceContext sc,
            @Parameter @Name("dataSource") String dataSource,
            @Parameter @Name("tableName") String tableName,
            @Parameter @Name("targetNamespace") String targetNamespace,
            @Parameter @Name("name") String name,
            @Parameter @Name("description") String description) {
        
        /* XSD of the table */
        String xsd = OTHelper.getTableXsd(ctx, dataSource, tableName, targetNamespace, name, description);

        /* Exit if null */
        if (xsd == null) {
            return null;
        }
        
        try {
            /* Create input stream from xsd string */
            InputStream stream = new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8));

            /* Import xsd to Appian */
            ImportResult ir = XsdTypeImporter.importFromStream(stream, true, sc);

            /* Return list of type ids */
            return Arrays.stream(ir.getNewDatatypes()).map(d -> d.getId()).toArray(Long[]::new);
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public String[] otGetDataSourceTables(
            Context ctx,
            @Parameter @Name("dataSource") String dataSource) {
        
        /* Vars */
        Connection conn = null;
        ResultSet rsTables = null;
        ArrayList<String> result = null;
                
        try {
            /* Connection to db */
            conn = OTHelper.getConnection(ctx, dataSource);

            /* Get tables of the data source */
            rsTables = OTHelper.getTablesOfDataSource(conn);

            /* Return early if empty */
            if (rsTables.next() == false) {
                return null;
            }
            
            /* Iterate over tables */
            result = new ArrayList<>();
            do {
                result.add(rsTables.getString("TABLE_NAME"));
            } while (rsTables.next());
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        } finally {
            if (rsTables != null) {
                try {
                    rsTables.close();
                } catch (SQLException e) { /* Ignored */}
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) { /* Ignored */}
            }
        }

        /* Return list */
        return result.toArray(new String[0]);
    }
}
