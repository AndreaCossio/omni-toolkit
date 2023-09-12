package omni.toolkit.functions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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
                
        try {
            /* Connection to db */
            Connection conn = OTHelper.getConnection(ctx, dataSource);

            /* Get columns of the table */
            ResultSet rsColumns = OTHelper.getColumnsOfTable(conn, tableName);

            /* Return early if empty */
            if (rsColumns.next() == false) {
                return null;
            }

            /* Metadata */
            ResultSetMetaData rsmd = rsColumns.getMetaData();

            /* Result container */
            ArrayList<Map<Object, Object>> resultList = new ArrayList<Map<Object, Object>>();

            /* Iterate over columns */
            do {
                Map<Object, Object> row = new LinkedHashMap<>();

                /* Iterate over metadata fields */
                for (int i = 1; i < rsmd.getColumnCount() + 1; i++) {
                    row.put(OTHelper.stringTypedValue(rsmd.getColumnLabel(i)), OTHelper.stringTypedValue(rsColumns.getString(i)));
                }

                resultList.add(row);
            } while (rsColumns.next());

            /* Close connection */
            conn.close();

            /* Return list of dictionaries */
            return new TypedValue((long) AppianType.LIST_OF_MAP, resultList.toArray(new Map[resultList.size()]));
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public String otGetTableXsd(
            Context ctx,
            @Parameter @Name("dataSource") String dataSource,
            @Parameter @Name("tableName") String tableName,
            @Parameter @Name("targetNamespace") String targetNamespace,
            @Parameter @Name("name") String name,
            @Parameter @Name("description") String description) {

        try {
            /* XSD of the table */
            String xsd = OTHelper.getTableXsd(ctx, dataSource, tableName, targetNamespace, name, description);
            
            /* Return as string */
            return xsd;
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
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
        
        try {
            /* XSD of the table */
            String xsd = OTHelper.getTableXsd(ctx, dataSource, tableName, targetNamespace, name, description);

            /* Exit if null */
            if (xsd == null) {
                return null;
            }

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
                
        try {
            /* Connection to db */
            Connection conn = OTHelper.getConnection(ctx, dataSource);

            /* Get tables of the data source */
            ResultSet rsTables = OTHelper.getTablesOfDataSource(conn);

            /* Return early if empty */
            if (rsTables.next() == false) {
                return null;
            }
            
            /* Iterate over tables */
            ArrayList<String> result = new ArrayList<>();
            do {
                result.add(rsTables.getString("TABLE_NAME"));
            } while (rsTables.next());

            /* Close connection */
            conn.close();

            /* Return list */
            return result.toArray(new String[0]);
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }
}
