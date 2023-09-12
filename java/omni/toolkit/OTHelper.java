package omni.toolkit;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.appiancorp.suiteapi.common.LocalObject;
import com.appiancorp.suiteapi.content.Content;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentFilter;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.content.exceptions.InvalidTypeMaskException;
import com.appiancorp.suiteapi.process.ProcessDetails;
import com.appiancorp.suiteapi.process.TaskDetails;
import com.appiancorp.suiteapi.type.AppianType;
import com.appiancorp.suiteapi.type.TypedValue;

public class OTHelper {
    /* Logger */
    private static final Logger LOG = Logger.getLogger(OTHelper.class);

    /* Types */
    public static final ArrayList<String> booleanTypes = new ArrayList<>(Arrays.asList("BIT", "BOOL", "BOOLEAN"));
    public static final ArrayList<String> dateTypes = new ArrayList<>(Arrays.asList("DATE"));
    public static final ArrayList<String> timeTypes = new ArrayList<>(Arrays.asList("TIME", "TIMETZ"));
    public static final ArrayList<String> dateTimeTypes = new ArrayList<>(
            Arrays.asList("DATETIME", "DATETIME2", "SMALLDATETIME", "TIMESTAMP", "TIMESTAMPTZ"));
    public static final ArrayList<String> doubleTypes = new ArrayList<>(Arrays.asList("BIGINT", "BIGINT IDENTITY",
            "DECIMAL", "DECIMAL IDENTITY", "DOUBLE", "DOUBLE PRECISION", "FLOAT", "FLOAT4", "FLOAT8", "MONEY",
            "NUMERIC", "NUMERIC IDENTITY", "REAL", "SMALLINT UNSIGNED", "SMALLMONEY", "TINYINT", "TINYINT UNSIGNED"));
    public static final ArrayList<String> intTypes = new ArrayList<>(
            Arrays.asList("BIGINT UNSIGNED", "INT", "INT IDENTITY", "INT UNSIGNED", "INT2", "INT4", "INT8", "INTEGER",
                    "INTEGER UNSIGNED", "MEDIUMINT", "MEDIUMINT UNSIGNED", "NUMBER", "SERIAL2", "SERIAL4", "SERIAL8",
                    "SMALLINT", "SMALLINT IDENTITY", "TINYINT IDENTITY"));
    // public static final ArrayList<String> stringTypes = new
    // ArrayList<>(Arrays.asList("BINARY", "BYTEA", "CHAR", "ENUM", "GRAPHIC",
    // "NCHAR", "NTEXT", "NVARCHAR", "RAW", "SET", "SYSNAME", "TEXT",
    // "UNIQUEIDENTIFIER", "VARBINARY", "VARCHAR", "VARCHAR2", "VARGRAPHIC",
    // "XML"));
    public static final ArrayList<String> sizeTypes = new ArrayList<>(
            Arrays.asList("CHAR", "VARCHAR", "BINARY", "VARBINARY", "NVARCHAR"));

    /* Returns the proper string */
    public static final String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /* Returns the camel case string */
    public static final String toCamelCase(String text) {
        String[] parts = text.split("_");
        String camelCaseString = "";
        for (String part : parts) {
            camelCaseString = camelCaseString + toProperCase(part);
        }
        return camelCaseString;
    }

    /* Log func */
    public static final void logError(String message) {
        LOG.error(message);
    }

    /* Get connection to a db */
    public static final Connection getConnection(Context ctx, String dataSource) throws SQLException, NamingException {
        return ((DataSource) ctx.lookup(dataSource)).getConnection();
    }

    /* Get columns of a table */
    public static final ResultSet getColumnsOfTable(Connection conn, String tableName) throws SQLException {
        return conn.getMetaData().getColumns(null, null, tableName, null);
    }

    /* Get tables of a data source */
    public static final ResultSet getTablesOfDataSource(Connection conn) throws SQLException {
        return conn.getMetaData().getTables(null, null, "%", null);
    }

    /* Typed value string */
    public static final TypedValue stringTypedValue(String value) {
        return new TypedValue((long) AppianType.STRING, value);
    }

    /* Typed value integer */
    public static final TypedValue intTypedValue(Long value) {
        return new TypedValue((long) AppianType.INTEGER, value);
    }

    /* Typed value long */
    public static final TypedValue doubleTypedValue(Double value) {
        return new TypedValue((long) AppianType.DOUBLE, value);
    }

    /* Typed value integer */
    public static final TypedValue timestampTypedValue(Timestamp value) {
        return new TypedValue((long) AppianType.TIMESTAMP, value);
    }

    /* Returns the appian data type */
    public static final String getXmlDataType(String dataType) {
        /* Upper */
        String dataTypeUpper = dataType.toUpperCase();

        /* Match to Appian */
        if (booleanTypes.contains(dataTypeUpper))
            return "boolean";
        else if (dateTypes.contains(dataTypeUpper))
            return "date";
        else if (timeTypes.contains(dataTypeUpper))
            return "time";
        else if (dateTimeTypes.contains(dataTypeUpper))
            return "dateTime";
        else if (doubleTypes.contains(dataTypeUpper))
            return "double";
        else if (intTypes.contains(dataTypeUpper))
            return "int";

        /* By default return string */
        return "string";
    }

    /* Returns the xml element for the given column */
    public static final String generateAppInfoText(String columnName, String dataType, Boolean isId,
            Boolean isAutoGenerated, Boolean isNullable, Integer length) {
        /* Init */
        String id = isId ? "@Id " : "";
        String generatedValue = isAutoGenerated ? "@GeneratedValue " : "";
        String nullable = isNullable ? "" : "nullable=false, ";
        String unique = isId ? "unique=true, " : "";
        String len = sizeTypes.contains(dataType.toUpperCase()) ? "(" + length.toString() + ")" : "";

        /* Column */
        String column = "@Column(name=\"" + columnName + "\", " + nullable + unique + "columnDefinition=\""
                + dataType.toUpperCase() + len + "\")";

        /* Result */
        return id + generatedValue + column;
    }

    /* XSD document to string */
    public static final String xsdDocToString(Document xsdDoc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(xsdDoc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error converting to String", e);
        }
    }

    /* Walk subfolder until found */
    public static final Long[] walkSubfoldersToFindContent(ContentService cs, ContentFilter contentFilter,
            Long folderId) throws InvalidContentException, InvalidTypeMaskException {
        /* Find content inside current folder of iteration */
        Long[] ids = cs.getChildrenIds(folderId, contentFilter, ContentConstants.GC_MOD_NORMAL);

        /* If found return */
        if (ids.length != 0) {
            return ids;
        }

        /* Find all subfolders */
        Long[] recIds = cs.getChildrenIds(folderId, new ContentFilter(ContentConstants.TYPE_FOLDER),
                ContentConstants.GC_MOD_NORMAL);

        /* Search inside all subfolders */
        for (int i = 0; i < recIds.length; i++) {
            Long[] res = walkSubfoldersToFindContent(cs, contentFilter, recIds[i]);

            /* If found return */
            if (res != null) {
                return res;
            }
        }

        /* Not found */
        return null;
    }

    /* Convert table info to xsd */
    public static final String getTableXsd(Context ctx, String dataSource, String tableName, String targetNamespace,
            String name, String description) throws Exception {
        /* Connection to db */
        Connection conn = getConnection(ctx, dataSource);

        /* Get columns of the table */
        ResultSet rsColumns = getColumnsOfTable(conn, tableName);

        /* Return early if empty */
        if (rsColumns.next() == false) {
            return null;
        }

        /* Create xsd document */
        Document xsdDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        /* Schema */
        Element rootElement = xsdDoc.createElement("xsd:schema");
        rootElement.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
        rootElement.setAttribute("xmlns:tns", targetNamespace);
        rootElement.setAttribute("targetNamespace", targetNamespace);
        xsdDoc.appendChild(rootElement);

        /* Name */
        Element complexTypeElement = xsdDoc.createElement("xsd:complexType");
        complexTypeElement.setAttribute("name", name);
        rootElement.appendChild(complexTypeElement);

        /* Annotation */
        Element annotationElement = xsdDoc.createElement("xsd:annotation");
        complexTypeElement.appendChild(annotationElement);

        /* AppInfo */
        Element appInfoElement = xsdDoc.createElement("xsd:appinfo");
        appInfoElement.setAttribute("source", "appian.jpa");
        appInfoElement.setTextContent("@Table(name=\"" + tableName + "\")");
        annotationElement.appendChild(appInfoElement);

        /* Description */
        Element documentationElement = xsdDoc.createElement("xsd:documentation");
        Node cdata = xsdDoc.createCDATASection(description);
        documentationElement.appendChild(cdata);
        annotationElement.appendChild(documentationElement);

        /* Sequence to hold fields */
        Element sequenceElement = xsdDoc.createElement("xsd:sequence");
        complexTypeElement.appendChild(sequenceElement);

        /* Iterate over columns */
        do {
            String columnName = rsColumns.getString("COLUMN_NAME");
            String isNullable = rsColumns.getString("IS_NULLABLE");
            String dataType = rsColumns.getString("TYPE_NAME");
            String isAutoIncrement = rsColumns.getString("IS_AUTOINCREMENT");
            String isGeneratedColumn = rsColumns.getString("IS_GENERATEDCOLUMN");
            String columnSize = rsColumns.getString("COLUMN_SIZE");

            /* Column element */
            Element element = xsdDoc.createElement("xsd:element");
            element.setAttribute("name", toCamelCase(columnName));
            element.setAttribute("type", "xsd:" + getXmlDataType(dataType));
            element.setAttribute("nillable", "true");

            /* Column annotation */
            Element annotation = xsdDoc.createElement("xsd:annotation");
            element.appendChild(annotation);

            /* Column appInfo */
            Element app = xsdDoc.createElement("xsd:appinfo");
            app.setAttribute("source", "appian.jpa");
            String s = generateAppInfoText(
                    columnName,
                    dataType,
                    columnName.toLowerCase().equals("id"),
                    isGeneratedColumn.equalsIgnoreCase("yes") || isAutoIncrement.equalsIgnoreCase("yes"),
                    isNullable.equalsIgnoreCase("yes"),
                    columnSize == null ? 0 : Integer.parseInt(columnSize));
            app.setTextContent(s);
            annotation.appendChild(app);
            sequenceElement.appendChild(element);
        } while (rsColumns.next());

        /* Close connection */
        conn.close();

        /* Return as string */
        return xsdDocToString(xsdDoc);
    }

    /* Content Attributes */
    public static final Map<Object, Object> createContentAttributesMap(ContentService cs, Content content) throws Exception {
        Map<Object, Object> map = new HashMap<>();
        Long contentId = content.getId();
        map.put(stringTypedValue("name"), stringTypedValue(content.getDisplayName()));
        map.put(stringTypedValue("description"), stringTypedValue(content.getDescription()));
        map.put(stringTypedValue("type"), intTypedValue(content.getType().longValue()));
        map.put(stringTypedValue("nVersions"), intTypedValue(content.getNumberOfVersions()));
        map.put(stringTypedValue("parentId"), intTypedValue(content.getParent()));
        map.put(stringTypedValue("parentName"), stringTypedValue(content.getParentName()));
        map.put(stringTypedValue("parentType"), intTypedValue(content.getParentType().longValue()));
        map.put(stringTypedValue("lockedAt"), timestampTypedValue(content.getLockedAt()));
        map.put(stringTypedValue("lockedBy"), stringTypedValue(content.getLockedByUsername()));
        map.put(stringTypedValue("id"), intTypedValue(contentId));
        map.put(stringTypedValue("uuid"), stringTypedValue(content.getUuid()));
        map.put(stringTypedValue("url"), stringTypedValue(cs.getContentUrl(contentId)));
        map.put(stringTypedValue("opaqueUri"), stringTypedValue(cs.getOpaqueContentUri(contentId)));
        map.put(stringTypedValue("internalFilename"), stringTypedValue(cs.getInternalFilename(contentId)));
        map.put(stringTypedValue("externalFilename"), stringTypedValue(cs.getExternalFilename(contentId)));
        return map;
    }

    /* Process Attributes */
    public static final Map<Object, Object> createProcessAttributesMap(ProcessDetails pd) throws Exception {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put(stringTypedValue("id"), intTypedValue(pd.getId()));
        map.put(stringTypedValue("initiator"), stringTypedValue(pd.getInitiator()));
        map.put(stringTypedValue("name"), stringTypedValue(pd.getName()));
        map.put(stringTypedValue("parentId"), intTypedValue(pd.getParentId()));
        map.put(stringTypedValue("processModelId"), intTypedValue(pd.getProcessModelId()));
        map.put(stringTypedValue("status"), intTypedValue((long) pd.getStatus()));
        map.put(stringTypedValue("completedTaskCount"), intTypedValue(pd.getCompletedTaskCount().longValue()));
        map.put(stringTypedValue("currentTaskCount"), intTypedValue(pd.getCurrentTaskCount().longValue()));
        map.put(stringTypedValue("startTime"), timestampTypedValue(pd.getStartTime()));
        map.put(stringTypedValue("timezone"), stringTypedValue(pd.getTimezone()));
        map.put(stringTypedValue("deadline"), timestampTypedValue(pd.getDeadline()));
        map.put(stringTypedValue("elapsed"), doubleTypedValue(pd.getElapsedMilliseconds().doubleValue()));
        map.put(stringTypedValue("endTime"), timestampTypedValue(pd.getEndTime()));
        return map;
    }

    /* Task Attributes */
    public static final Map<Object, Object> createTaskAttributesMap(TaskDetails td, LocalObject[] assignees) {
        Map<Object, Object> map = new HashMap<>();
        map.put(stringTypedValue("id"), intTypedValue(td.getId()));
        map.put(stringTypedValue("acceptedTime"), timestampTypedValue(td.getAcceptedTime()));
        map.put(stringTypedValue("assignedTime"), timestampTypedValue(td.getAssignedTime()));
        map.put(stringTypedValue("assignees"), 
            new TypedValue(new Long(AppianType.LIST_OF_USER_OR_GROUP), assignees)
        );
        map.put(stringTypedValue("completedTime"), timestampTypedValue(td.getCompletedTime()));
        map.put(stringTypedValue("deadline"), timestampTypedValue(td.getTaskDeadline()));
        map.put(stringTypedValue("elapsed"), doubleTypedValue(td.getElapsed()));
        map.put(stringTypedValue("owners"), 
            new TypedValue(new Long(AppianType.LIST_OF_USERNAME), td.getOwners())
        );
        map.put(stringTypedValue("processId"), intTypedValue(td.getProcessId()));
        map.put(stringTypedValue("processInitiator"), stringTypedValue(td.getProcessInitiator()));
        map.put(stringTypedValue("status"), intTypedValue(td.getStatus().longValue()));
        map.put(stringTypedValue("description"), stringTypedValue(td.getDescription()));
        map.put(stringTypedValue("displayName"), stringTypedValue(td.getDisplayName()));
        map.put(stringTypedValue("priority"), stringTypedValue(td.getPriority().getDescription()));
        return map;
    }
}
