package omni.toolkit.smartservices;

import com.appiancorp.suiteapi.applications.Application;
import com.appiancorp.suiteapi.applications.ApplicationService;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.process.ApplicationDataType;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.palette.PaletteCategoryConstants;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;
import com.appiancorp.suiteapi.rules.Constant;
import com.appiancorp.suiteapi.type.TypedValue;
import com.appiancorp.type.AppianTypeLong;
import com.appiancorp.suiteapi.process.framework.Order;

@PaletteInfo(paletteCategory = PaletteCategoryConstants.AUTOMATION_SMART_SERVICES, palette = "Omni Toolkit")
@Order({ "Name", "Value", "Description", "Folder", "Application" })
public class OTCreateConstant extends AppianSmartService {

    /* Service */
    private final ContentService contentService;
    private final ApplicationService applicationService;

    /* In */
    private String name;
    private TypedValue value;
    private String description;
    private Long folder;
    private Long application;

    /* Out */
    private Long constantId;
    private String constantUuid;

    public OTCreateConstant(ContentService cs, ApplicationService as) {
        this.contentService = cs;
        this.applicationService = as;
    }

    @Override
    public void run() {
        Constant cons = new Constant();
        cons.setName(this.name);
        cons.setDescription(this.description);
        cons.setTypedValue(this.value);
        cons.setParent(this.folder);
        cons.setSecurity(ContentConstants.SEC_INH_ALL);

        try {
            this.constantId = this.contentService.create(cons, ContentConstants.UNIQUE_FOR_ALL);
            this.constantUuid = this.contentService.getVersion(constantId, ContentConstants.VERSION_CURRENT).getUuid();
            if (this.application != null) {
                final Application appInstance = this.applicationService.getApplication(this.application);
                appInstance.addObjectsByType(AppianTypeLong.CONTENT_ITEM, new String[] { constantUuid });
                this.applicationService.save(appInstance);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Name("constantId")
    public Long getRuleId() {
        return this.constantId;
    }

    @Name("constantUuid")
    public String getRuleUuid() {
        return this.constantUuid;
    }

    @Input(required = Required.ALWAYS)
    public void setName(String name) {
        this.name = name;
    }

    @Input(required = Required.ALWAYS)
    public void setValue(TypedValue value) {
        this.value = value;
    }

    @Input(required = Required.ALWAYS)
    public void setDescription(String description) {
        this.description = description;
    }

    @Input(required = Required.ALWAYS)
    @FolderDataType
    public void setFolder(Long folder) {
        this.folder = folder;
    }

    @Input(required = Required.OPTIONAL)
    @ApplicationDataType
    public void setApplication(Long application) {
        this.application = application;
    }

}
