package omni.toolkit.smartservices;

import com.appiancorp.suiteapi.applications.Application;
import com.appiancorp.suiteapi.applications.ApplicationService;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.ContentUploadOutputStream;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.process.ApplicationDataType;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.palette.PaletteCategoryConstants;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;
import com.appiancorp.type.AppianTypeLong;

import omni.toolkit.OTHelper;

import com.appiancorp.suiteapi.process.framework.Order;

@PaletteInfo(paletteCategory = PaletteCategoryConstants.AUTOMATION_SMART_SERVICES, palette = "Omni Toolkit")
@Order({ "Name", "Content", "Extension", "Description", "Folder", "Application" })
public class OTCreateDocument extends AppianSmartService {

    /* Service */
    private final ContentService contentService;
    private final ApplicationService applicationService;

    /* In */
    private String name;
    private String content;
    private String extension;
    private String description;
    private Long folder;
    private Long application;

    /* Out */
    private Long documentId;
    private String documentUuid;

    public OTCreateDocument(ContentService cs, ApplicationService as) {
        this.contentService = cs;
        this.applicationService = as;
    }

    @Override
    public void run() throws SmartServiceException  {
        Document doc = new Document(this.folder, this.name, this.extension);
        doc.setDescription(this.description);
        doc.setSecurity(ContentConstants.SEC_INH_ALL);
        
        try {
            ContentUploadOutputStream outStr = this.contentService.uploadDocument(doc, ContentConstants.UNIQUE_FOR_ALL);
            byte[] fileContentBytes = this.content.getBytes();
            try {
                outStr.write(fileContentBytes);
                outStr.flush();
            } finally {
                outStr.close();
            }
            
            this.documentId = outStr.getContentId();
            this.documentUuid = this.contentService.getVersion(documentId, ContentConstants.VERSION_CURRENT).getUuid();
            if (this.application != null) {
                final Application appInstance = this.applicationService.getApplication(this.application);
                appInstance.addObjectsByType(AppianTypeLong.CONTENT_ITEM, new String[] { documentUuid });
                this.applicationService.save(appInstance);
            }
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            throw createException(e);
        }
    }

    @Name("documentId")
    public Long getRuleId() {
        return this.documentId;
    }

    @Name("documentUuid")
    public String getRuleUuid() {
        return this.documentUuid;
    }

    @Input(required = Required.ALWAYS)
    public void setName(String name) {
        this.name = name;
    }

    @Input(required = Required.ALWAYS)
    public void setContent(String content) {
        this.content = content;
    }

    @Input(required = Required.ALWAYS)
    public void setExtension(String extension) {
        this.extension = extension;
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

	private SmartServiceException createException(Throwable t) {
		SmartServiceException.Builder b = new SmartServiceException.Builder(getClass(), t);
		b.userMessage(t.getMessage());
		b.addCauseToUserMessageArgs();
		return b.build();
	}
}
