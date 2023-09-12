package omni.toolkit.smartservices;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.naming.Context;

import com.appiancorp.services.ServiceContext;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.palette.PaletteCategoryConstants;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;
import com.appiancorp.suiteapi.type.Datatype;
import com.appiancorp.suiteapi.type.config.ImportResult;
import com.appiancorp.suiteapi.type.config.xsd.XsdTypeImporter;

import omni.toolkit.OTHelper;

import com.appiancorp.suiteapi.process.framework.Order;

@PaletteInfo(paletteCategory = PaletteCategoryConstants.AUTOMATION_SMART_SERVICES, palette = "Omni Toolkit")
@Order({ "Data Source", "Table Name", "Target Namespace", "Name", "Description", })
public class OTUpdateDataType extends AppianSmartService {

    /* Service */
    private final Context context;
    private final ServiceContext serviceContext;

    /* In */
    private String dataSource;
    private String tableName;
    private String targetNamespace;
    private String name;
    private String description;

    /* Out */
    private Long typeId;

    public OTUpdateDataType(Context ctx, ServiceContext sc) {
        this.context = ctx;
        this.serviceContext = sc;
    }

    @Override
    public void run() throws SmartServiceException  {
        
        /* XSD of the table */
        String xsd = OTHelper.getTableXsd(this.context, this.dataSource, this.tableName, this.targetNamespace, this.name, this.description);
        
        try {

            /* Exit if null */
            if (xsd != null) {
                /* Create input stream from xsd string */
                InputStream stream = new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8));

                /* Import xsd to Appian */
                ImportResult ir = XsdTypeImporter.importFromStream(stream, true, this.serviceContext);
                Datatype[] dts = ir.getNewDatatypes();
                Long[] r = new Long[dts.length];
                for (int i = 0; i < r.length; i++) {
                    r[i] = dts[i].getId();
                }

                this.typeId = r[0];
            }
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            throw createException(e);
        }
    }

    @Name("typeId")
    public Long getRuleId() {
        return this.typeId;
    }

    @Input(required = Required.ALWAYS)
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    @Input(required = Required.ALWAYS)
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Input(required = Required.ALWAYS)
    public void setTargetNamespace(String targetNamespace) {
        this.targetNamespace = targetNamespace;
    }

    @Input(required = Required.ALWAYS)
    public void setName(String name) {
        this.name = name;
    }

    @Input(required = Required.ALWAYS)
    public void setDescription(String description) {
        this.description = description;
    }

	private SmartServiceException createException(Throwable t) {
		SmartServiceException.Builder b = new SmartServiceException.Builder(getClass(), t);
		b.userMessage(t.getMessage());
		b.addCauseToUserMessageArgs();
		return b.build();
	}
}
