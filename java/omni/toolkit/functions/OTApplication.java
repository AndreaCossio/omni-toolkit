package omni.toolkit.functions;

import java.util.Arrays;
import java.util.Set;

import com.appiancorp.suiteapi.applications.Application;
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

    @Function
    public String[] otGetApplicationObjectsByType(
            ApplicationService as,
            @ApplicationDataType @Parameter @Name("application") Long application,
            @Parameter @Name("type") Long type) {
        try {

            Application app = as.getApplication(application);
            Set<Object> objectSet = app.getObjectsByType(type);

            /* Return the object list as list of strings */
            return Arrays.stream(objectSet.toArray()).map(
                    object -> object.toString())
                    .toArray(String[]::new);

        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

}