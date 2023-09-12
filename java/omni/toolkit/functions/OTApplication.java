package omni.toolkit.functions;

import com.appiancorp.suiteapi.applications.ApplicationService;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;
import com.appiancorp.suiteapi.process.ApplicationDataType;

import omni.toolkit.OTHelper;

@OTCategory
public class OTApplication {

    @Function
    @ApplicationDataType
    public Long otGetApplicationByUuid(
            ApplicationService as,
            @Parameter @Name("uuid") String uuid) {
        
        try {
            return as.getApplicationByUuid(uuid).getId();
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

}