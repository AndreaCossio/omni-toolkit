package omni.toolkit.functions;

import java.util.Arrays;

import com.appiancorp.suiteapi.common.Constants;
import com.appiancorp.suiteapi.common.LocalObject;
import com.appiancorp.suiteapi.common.LocalObjectTypeMapping;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.expression.annotations.Function;
import com.appiancorp.suiteapi.expression.annotations.Parameter;
import com.appiancorp.suiteapi.personalization.UserOrGroupDataType;
import com.appiancorp.suiteapi.process.Assignment.Assignee;
import com.appiancorp.suiteapi.process.Assignment;
import com.appiancorp.suiteapi.process.ProcessDataType;
import com.appiancorp.suiteapi.process.ProcessExecutionService;
import com.appiancorp.suiteapi.process.TaskDataType;
import com.appiancorp.suiteapi.process.TaskDetails;
import com.appiancorp.suiteapi.process.TaskSummary;
import com.appiancorp.suiteapi.process.analytics2.ProcessAnalyticsService;
import com.appiancorp.suiteapi.type.AppianType;
import com.appiancorp.suiteapi.type.TypedValue;

import omni.toolkit.OTHelper;

@OTCategory
public class OTTask {

    @Function
    @TaskDataType
    public Long[] otGetCurrentTasksForProcess(
            ProcessExecutionService pes,
            @Parameter @ProcessDataType @Name("processId") Long processId) {

        try {
            /* Get task summary list */
            TaskSummary[] allTasks = (TaskSummary[]) pes.getCurrentTasksForProcess(
                processId,
                ProcessExecutionService.UNATTENDED_AND_ATTENDED_TASKS,
                0,
                Constants.COUNT_ALL,
                null,
                null
            ).getResults();

            /* Return result */
            return Arrays.stream(allTasks).map(r -> r.getId()).toArray(Long[]::new);
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    @TaskDataType
    public Long[] otGetAllTasksForCurrentUser(
            ProcessAnalyticsService pas) {

        try {
            /* Get task summary list */
            TaskSummary[] allTasks = (TaskSummary[]) pas.getAllTasks(
                0,
                Constants.COUNT_ALL,
                TaskSummary.SORT_BY_ID,
                Constants.SORT_ORDER_ASCENDING
            ).getResults();

            /* Return result */
            return Arrays.stream(allTasks).map(r -> r.getId()).toArray(Long[]::new);
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    @UserOrGroupDataType
    public LocalObject[] otGetTaskAssignees(
            ProcessExecutionService pes,
            @Parameter @Name("taskId") @TaskDataType Long taskId) {

        try {
            Assignee[] assignees = pes.getTaskAssignees(taskId);
            LocalObject[] userOrGroupList = new LocalObject[assignees.length];
            for (int i = 0; i < assignees.length; i++) {
                int type = assignees[i].getType().intValue();
                switch (type) {
                    case Assignment.ASSIGNEE_TYPE_USERS:
                        userOrGroupList[i] = new LocalObject(
                                LocalObjectTypeMapping.TYPE_USER,
                                assignees[i].getValue().toString());
                        break;
                    case Assignment.ASSIGNEE_TYPE_GROUPS:
                        userOrGroupList[i] = new LocalObject(
                                LocalObjectTypeMapping.TYPE_GROUP,
                                (Long) assignees[i].getValue());
                        break;
                    default:
                        break;
                }
            }
            return userOrGroupList;
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

    @Function
    public TypedValue otGetTaskDetails(
            ProcessExecutionService pes,
            @Parameter @Name("taskId") @TaskDataType Long taskId) {

        try {
            /* Get task details */
            TaskDetails td = pes.getTaskDetails(taskId);

            /* Return map with attributes */
            return new TypedValue((long) AppianType.MAP,
                    OTHelper.createTaskAttributesMap(td, otGetTaskAssignees(pes, taskId)));
        } catch (Exception e) {
            OTHelper.logError(e.getMessage());
            return null;
        }
    }

}
