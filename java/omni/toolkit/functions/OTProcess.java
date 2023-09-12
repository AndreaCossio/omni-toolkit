package omni.toolkit.functions;

import java.util.Arrays;

import com.appiancorp.suiteapi.common.Constants;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.expression.PartialResult;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;
import com.appiancorp.suiteapi.process.ProcessDataType;
import com.appiancorp.suiteapi.process.ProcessDesignService;
import com.appiancorp.suiteapi.process.ProcessDetails;
import com.appiancorp.suiteapi.process.ProcessExecutionService;
import com.appiancorp.suiteapi.process.ProcessModelDataType;
import com.appiancorp.suiteapi.process.ProcessSummary;
import com.appiancorp.suiteapi.process.analytics2.ProcessAnalyticsService;
import com.appiancorp.suiteapi.type.AppianType;
import com.appiancorp.suiteapi.type.NamedTypedValue;
import com.appiancorp.suiteapi.type.TypedValue;

import omni.toolkit.OTHelper;

@OTCategory
public class OTProcess {

    private int getProcessStatusFromString(String status) {
        /* Match string to style */
        if (status != null && !status.equals("ALL")) {
            switch (status) {
                case "ACTIVE":
                    return ProcessSummary.STATE_ACTIVE;
                case "COMPLETED":
                    return ProcessSummary.STATE_COMPLETED;
                case "PAUSED":
                    return ProcessSummary.STATE_PAUSED;
                case "CANCELLED":
                    return ProcessSummary.STATE_CANCELLED;
                case "EXCEPTION":
                    return ProcessSummary.STATE_PAUSED_BY_EXCEPTION;
            }
        }

        /* Default style */
        return -1;
    }

    @Function
    public TypedValue otEvaluateExpressionResultForProcess(
            ProcessAnalyticsService pas,
            @Parameter @Name("processId") Long processId,
            @Parameter @Name("expression") String expression,
            @Parameter @Name("recursive") boolean recursive) {

        NamedTypedValue[] ntv = new NamedTypedValue[] {};
        PartialResult ps = pas.evaluateExpressionResultForProcess(processId, expression, recursive == true, ntv);
        
        /* Return result */
        return ps.getResult();
    }

    @Function
    public TypedValue otEvaluateExpression(
            ProcessDesignService pds,
            @Parameter @Name("expression") String expression) {

        try {
            return pds.evaluateExpression(expression);
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    @ProcessDataType
    public Long[] otGetProcessesForProcessModelByStatus(
            ProcessAnalyticsService pas,
            @Parameter @Name("processModelId") Long processModelId,
            @Parameter(required = false) @Name("status") String status) {

        try {
            /* List of processes */
            ProcessSummary[] ps = (ProcessSummary[]) pas.getProcessesForProcessModel(processModelId, 0,
                    Constants.COUNT_ALL, ProcessSummary.SORT_BY_PROCESS_ID,
                    Constants.SORT_ORDER_ASCENDING)
                    .getResults();

            /* Filter and return processes */
            int statusToFilter = getProcessStatusFromString(status);
            if (statusToFilter != -1) {
                return Arrays.stream(ps).filter(p -> (p.getStatus() == statusToFilter)).map(p -> p.getId()).toArray(Long[]::new);
            }
            return Arrays.stream(ps).map(p -> p.getId()).toArray(Long[]::new);
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    @ProcessModelDataType
    public Long otGetPmIdForProcess(
            ProcessExecutionService pes,
            @Parameter @Name("processId") Long processId) {

        try {
            return pes.getPmIdForProcess(processId);
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public TypedValue otGetProcessDetails(
            ProcessExecutionService pes,
            @Parameter @Name("processId") Long processId) {
        
        try {
            /* Get process details */
            ProcessDetails pd = pes.getProcessDetails(processId);

            /* Return map with attributes */
            return new TypedValue((long) AppianType.MAP, OTHelper.createProcessAttributesMap(pd));
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

}
