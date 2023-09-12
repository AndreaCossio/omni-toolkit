package omni.toolkit.functions;

import com.appiancorp.suiteapi.applications.Application;
import com.appiancorp.suiteapi.applications.ApplicationService;
import com.appiancorp.suiteapi.applications.ApplicationsFolder;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.Content;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentFilter;
import com.appiancorp.suiteapi.content.ContentOutputStream;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.knowledge.KnowledgeFolder;
import com.appiancorp.suiteapi.process.ApplicationDataType;
import com.appiancorp.suiteapi.process.ProcessDesignService;
import com.appiancorp.suiteapi.process.ProcessModelFolder;
import com.appiancorp.suiteapi.rules.Constant;
import com.appiancorp.suiteapi.rules.RulesFolder;
import com.appiancorp.suiteapi.type.AppianType;
import com.appiancorp.suiteapi.type.TypedValue;
import com.appiancorp.type.AppianTypeLong;

import omni.toolkit.OTHelper;

@OTCategory
public class OTContent {

    @Function
    public Long[] otGetContentIdByName(
            ContentService cs,
            @Parameter @Name("name") String name,
            @Parameter @Name("folderId") Long folderId) {

        try {
            /* Content filter of any type */
            ContentFilter contentFilter = new ContentFilter(ContentConstants.TYPE_ALL);
            contentFilter.setName(name);

            /* Return ids */
            return cs.getChildrenIds(folderId, contentFilter, ContentConstants.GC_MOD_NORMAL);
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public Long[] otGetContentIdByNameRecursive(
            ContentService cs,
            @Parameter @Name("name") String name,
            @Parameter(required = false) @Name("folderId") Long folderId) {

        try {
            /* Return if name is null */
            if (name.isEmpty()) {
                return null;
            }

            /* Init content filter */
            ContentFilter contentFilter = new ContentFilter(ContentConstants.TYPE_ALL);
            contentFilter.setName(name);

            /* Return recursion result */
            return OTHelper.walkSubfoldersToFindContent(cs, contentFilter,
                    folderId == null ? cs.getIdByUuid(ContentConstants.UUID_RULES_ROOT_FOLDER) : folderId);
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public TypedValue otGetContentDetailsById(
            ContentService cs,
            @Parameter @Name("contentId") Long contentId) {

        try {
            /* Retrieve last verion of content */
            Content content = cs.getVersion(contentId, ContentConstants.VERSION_CURRENT);

            /* Return list of dictionaries */
            return new TypedValue((long) AppianType.MAP, OTHelper.createContentAttributesMap(cs, content));
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public TypedValue otGetContentDetailsByUuid(
            ContentService cs,
            @Parameter @Name("uuid") String uuid) {

        try {
            /* Content id from uuid */
            Long contentId = cs.getIdByUuid(uuid);

            /* Retrieve last verion of content */
            Content content = cs.getVersion(contentId, ContentConstants.VERSION_CURRENT);

            /* Return list of dictionaries */
            return new TypedValue((long) AppianType.MAP, OTHelper.createContentAttributesMap(cs, content));
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public Long otCreateConstant(
            ContentService cs,
            ApplicationService as,
            @Parameter @Name("name") String name,
            @Parameter @Name("value") TypedValue value,
            @Parameter @Name("description") String description,
            @Parameter @Name("folder") @FolderDataType Long folder,
            @Parameter(required = false) @Name("application") @ApplicationDataType Long application) {

        /* Early exit */
        if (name == null || value == null || folder == null) {
            return null;
        }

        try {
            /* Create constant */
            Constant cons = new Constant();
            cons.setName(name);
            cons.setDescription(description);
            cons.setTypedValue(value);
            cons.setParent(folder);
            cons.setSecurity(ContentConstants.SEC_INH_ALL);

            /* Add constant on Application */
            Long constantId = cs.create(cons, ContentConstants.UNIQUE_FOR_ALL);
            String constantUuid = cs.getVersion(constantId, ContentConstants.VERSION_CURRENT).getUuid();
            if (application != null) {
                final Application appInstance = as.getApplication(application);
                appInstance.addObjectsByType(AppianTypeLong.CONTENT_ITEM, new String[] { constantUuid });
                as.save(appInstance);
            }

            /* Return constantId */
            return constantId;
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public Long otCreateDocument(
            ContentService cs,
            ApplicationService as,
            @Parameter @Name("name") String name,
            @Parameter @Name("content") String content,
            @Parameter @Name("extension") String extension,
            @Parameter @Name("description") String description,
            @Parameter @Name("folder") @FolderDataType Long folder,
            @Parameter(required = false) @Name("application") @ApplicationDataType Long application) {

        /* Early exit */
        if (name == null || extension == null || folder == null) {
            return null;
        }

        try {
            /* Create document */
            Document doc = new Document(folder, name, extension);
            doc.setDescription(description);
            doc.setSecurity(ContentConstants.SEC_INH_ALL);

            /* Upload to Appian and write content */
            ContentOutputStream outStr = cs.upload(doc, ContentConstants.UNIQUE_FOR_ALL);
            byte[] fileContentBytes = content.getBytes();
            try {
                outStr.write(fileContentBytes);
                outStr.flush();
            } finally {
                outStr.close();
            }

            /* Add document on Application */
            Long documentId = outStr.getContentId();
            String documentUuid = cs.getVersion(documentId, ContentConstants.VERSION_CURRENT).getUuid();
            if (application != null) {
                final Application appInstance = as.getApplication(application);
                appInstance.addObjectsByType(AppianTypeLong.CONTENT_ITEM, new String[] { documentUuid });
                as.save(appInstance);
            }

            /* Return constantId */
            return documentId;
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    @FolderDataType
    public Long otCreateRulesFolder(
            ContentService cs,
            ApplicationService as,
            @Parameter @Name("name") String name,
            @Parameter @Name("description") String description,
            @Parameter @Name("folder") @FolderDataType Long folder,
            @Parameter(required = false) @Name("application") @ApplicationDataType Long application) {

        try {
            /* Create folder */
            RulesFolder newFolder = new RulesFolder();
            newFolder.setName(name);
            newFolder.setDescription(description);
            newFolder.setParent(folder);
            newFolder.setSecurity(ContentConstants.SEC_INH_ALL);

            /* Add folder on Application */
            Long newFolderId = cs.create(newFolder, ContentConstants.UNIQUE_FOR_ALL);
            String newFolderUuid = cs.getVersion(newFolderId, ContentConstants.VERSION_CURRENT).getUuid();
            if (application != null) {
                final Application appInstance = as.getApplication(application);
                appInstance.addObjectsByType(AppianTypeLong.CONTENT_ITEM, new String[] { newFolderUuid });
                as.save(appInstance);
            }

            /* Return folderId */
            return newFolderId;
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    @FolderDataType
    public Long otCreateApplicationsFolder(
            ContentService cs,
            ApplicationService as,
            @Parameter @Name("name") String name,
            @Parameter @Name("description") String description,
            @Parameter @Name("folder") @FolderDataType Long folder,
            @Parameter(required = false) @Name("application") @ApplicationDataType Long application) {

        try {
            /* Create folder */
            ApplicationsFolder newApplication = new ApplicationsFolder();
            newApplication.setName(name);
            newApplication.setDescription(description);
            newApplication.setParent(folder);
            newApplication.setSecurity(ContentConstants.SEC_INH_ALL);

            /* Add folder on Application */
            Long newApplicationId = cs.create(newApplication, ContentConstants.UNIQUE_FOR_ALL);
            String newApplicationUuid = cs.getVersion(newApplicationId, ContentConstants.VERSION_CURRENT).getUuid();
            if (application != null) {
                final Application appInstance = as.getApplication(application);
                appInstance.addObjectsByType(AppianTypeLong.CONTENT_ITEM, new String[] { newApplicationUuid });
                as.save(appInstance);
            }

            /* Return folderId */
            return newApplicationId;
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    @FolderDataType
    public Long otCreateKnowledgeFolder(
            ContentService cs,
            ApplicationService as,
            @Parameter @Name("name") String name,
            @Parameter @Name("description") String description,
            @Parameter @Name("folder") @FolderDataType Long folder,
            @Parameter(required = false) @Name("application") @ApplicationDataType Long application) {

        try {
            /* Create folder */
            KnowledgeFolder newKcFolder = new KnowledgeFolder();
            newKcFolder.setName(name);
            newKcFolder.setDescription(description);
            newKcFolder.setParent(folder);
            newKcFolder.setSecurity(ContentConstants.SEC_INH_ALL);
            
            /* Add folder on Application */
            Long newKcFolderId = cs.create(newKcFolder, ContentConstants.UNIQUE_FOR_ALL);
            String newKcFolderUuid = cs.getVersion(newKcFolderId, ContentConstants.VERSION_CURRENT).getUuid();
            if (application != null) {
                final Application appInstance = as.getApplication(application);
                appInstance.addObjectsByType(AppianTypeLong.CONTENT_ITEM, new String[] { newKcFolderUuid });
                as.save(appInstance);
            }

            /* Return folderId */
            return newKcFolderId;
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public Long otCreateProcessModelFolder(
            ContentService cs,
            ApplicationService as,
            ProcessDesignService pds,
            @Parameter @Name("name") String name,
            @Parameter @Name("description") String description,
            @Parameter @Name("folder") @FolderDataType Long folder,
            @Parameter(required = false) @Name("application") @ApplicationDataType Long application) {

        try {
            /* Create folder */
            ProcessModelFolder pmf = new ProcessModelFolder(name);
            pmf.setDescription(description);
            pmf.setParentFolderId(folder);

            /* Add folder on Application */
            ProcessModelFolder createdPmf = pds.createFolder(pmf);
            Long pmfId = createdPmf.getId();
            String pmfUuid = cs.getVersion(pmfId, ContentConstants.VERSION_CURRENT).getUuid();
            if (application != null) {
                final Application appInstance = as.getApplication(application);
                appInstance.addObjectsByType(AppianTypeLong.CONTENT_ITEM, new String[] { pmfUuid });
                as.save(appInstance);
            }

            /* Return folderId */
            return pmfId;
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }
}