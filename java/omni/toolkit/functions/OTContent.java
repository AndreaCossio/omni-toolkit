package omni.toolkit.functions;

import com.appiancorp.suiteapi.applications.Application;
import com.appiancorp.suiteapi.applications.ApplicationService;
import com.appiancorp.suiteapi.applications.ApplicationsFolder;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.Content;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentFilter;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.ContentUploadOutputStream;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.knowledge.KnowledgeFolder;
import com.appiancorp.suiteapi.process.ApplicationDataType;
import com.appiancorp.suiteapi.process.ProcessDesignService;
import com.appiancorp.suiteapi.process.ProcessModelFolder;
import com.appiancorp.suiteapi.rules.Constant;
import com.appiancorp.suiteapi.rules.FreeformRule;
import com.appiancorp.suiteapi.rules.RulesFolder;
import com.appiancorp.suiteapi.type.AppianType;
import com.appiancorp.suiteapi.type.TypeService;
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

            Long constantId = cs.create(cons, ContentConstants.UNIQUE_FOR_ALL);
            /* Add to application and return constantId */
            OTHelper.addContentToApplication(cs, as, constantId, application);
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
            ContentUploadOutputStream outStr = cs.uploadDocument(doc, ContentConstants.UNIQUE_FOR_ALL);
            byte[] fileContentBytes = content.getBytes();
            try {
                outStr.write(fileContentBytes);
                outStr.flush();
            } finally {
                outStr.close();
            }

            /* Add to application and return documentId */
            Long documentId = outStr.getContentId();
            OTHelper.addContentToApplication(cs, as, documentId, application);
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

            /* Add to applicatoin and return folderId */
            Long newFolderId = cs.create(newFolder, ContentConstants.UNIQUE_FOR_ALL);
            OTHelper.addContentToApplication(cs, as, newFolderId, application);
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

            /* Add to application and return folderId */
            Long newApplicationId = cs.create(newApplication, ContentConstants.UNIQUE_FOR_ALL);
            OTHelper.addContentToApplication(cs, as, newApplicationId, application);
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

            Long newKcFolderId = cs.create(newKcFolder, ContentConstants.UNIQUE_FOR_ALL);
            /* Add to application and return folderId */
            OTHelper.addContentToApplication(cs, as, newKcFolderId, application);
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
            String pmfUuid = createdPmf.getUuid();
            if (application != null) {
                final Application appInstance = as.getApplication(application);
                appInstance.addObjectsByType(AppianTypeLong.PROCESS_MODEL_FOLDER, new String[] { pmfUuid });
                as.save(appInstance);
            }

            /* Return folderId */
            return pmfId;
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public Long otCreateRuleExpression(
            ContentService cs,
            ApplicationService as,
            TypeService ts,
            @Parameter @Name("name") String name,
            @Parameter @Name("description") String description,
            @Parameter @Name("ruleDefinition") String ruleDefinition,
            @Parameter @Name("folder") @FolderDataType Long folder,
            @Parameter(required = false) @Name("ruleInputs") TypedValue ruleInputs,
            @Parameter(required = false) @Name("application") @ApplicationDataType Long application) {

        try {
            /* Create Rule Expression */
            FreeformRule freeFormRule = OTHelper.createFreeFormRule(ts, name, description, ruleDefinition, folder,
                    ruleInputs);
            freeFormRule.setSubtype(ContentConstants.SUBTYPE_RULE_FREEFORM);

            Long ruleExressionId = cs.create(freeFormRule, ContentConstants.UNIQUE_FOR_ALL);
            /* Add to application and return contentId */
            OTHelper.addContentToApplication(cs, as, ruleExressionId, application);
            return ruleExressionId;
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public Long otCreateInterface(
            ContentService cs,
            ApplicationService as,
            TypeService ts,
            @Parameter @Name("name") String name,
            @Parameter @Name("description") String description,
            @Parameter @Name("interfaceDefinition") String interfaceDefinition,
            @Parameter @Name("folder") @FolderDataType Long folder,
            @Parameter(required = false) @Name("ruleInputs") TypedValue ruleInputs,
            @Parameter(required = false) @Name("application") @ApplicationDataType Long application) {

        try {
            /* Create Interface */
            FreeformRule freeFormRule = OTHelper.createFreeFormRule(ts, name, description, interfaceDefinition,
                    folder,
                    ruleInputs);
            freeFormRule.setSubtype(ContentConstants.SUBTYPE_RULE_INTERFACE);

            Long interfaceId = cs.create(freeFormRule, ContentConstants.UNIQUE_FOR_ALL);
            /* Add to application and return contentId */
            OTHelper.addContentToApplication(cs, as, interfaceId, application);
            return interfaceId;
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public String otGetContentUuidById(
            ContentService cs,
            @Parameter @Name("contentId") Long contentId) {

        try {
            /* Retrieve last verion of content */
            Content content = cs.getVersion(contentId, ContentConstants.VERSION_CURRENT);

            /* Return content uuid */
            return content.getUuid();
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }
}