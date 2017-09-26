/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.cmmn.engine.impl.behavior.impl;

import org.flowable.cmmn.engine.PlanItemInstanceCallbackType;
import org.flowable.cmmn.engine.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.process.ProcessInstanceService;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceState;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.Process;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.impl.interceptor.CommandContext;

import liquibase.util.StringUtils;

/**
 * @author Joram Barrez
 */
public class ProcessTaskActivityBehavior extends TaskActivityBehavior implements PlanItemActivityBehavior {
    
    protected Process process;
    protected Expression processRefExpression;
    
    public ProcessTaskActivityBehavior(Process process, Expression processRefExpression, ProcessTask processTask) {
        super(processTask);
        this.process = process;
        this.processRefExpression = processRefExpression;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        ProcessInstanceService processInstanceService = CommandContextUtil.getCmmnEngineConfiguration().getProcessInstanceService();
        if (processInstanceService == null) {
            throw new FlowableException("Could not start process instance: no " + ProcessInstanceService.class + " implementation found");
        }

        String externalRef = null;
        if (process != null) {
            externalRef = process.getExternalRef();
            if (StringUtils.isEmpty(externalRef)) {
                throw new FlowableException("Could not start process instance: no externalRef defined");
            }
            
            
        } else if (processRefExpression != null) {
            externalRef = processRefExpression.getValue(planItemInstanceEntity).toString();
            
        }
        
        String processInstanceId = null;
        boolean blocking = determineIsBlocking(planItemInstanceEntity);
        if (blocking) {
            processInstanceId = processInstanceService.startProcessInstanceByKey(externalRef, planItemInstanceEntity.getId());
        } else {
            processInstanceId = processInstanceService.startProcessInstanceByKey(externalRef);
        }
        
        planItemInstanceEntity.setReferenceType(PlanItemInstanceCallbackType.CHILD_PROCESS);
        planItemInstanceEntity.setReferenceId(processInstanceId);
        
        if (!blocking) {
            CommandContextUtil.getAgenda(commandContext).planCompletePlanItem(planItemInstanceEntity);
        }
    }
    
    @Override
    public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstance) {
        if (!PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {
            throw new FlowableException("Can only trigger a plan item that is in the ACTIVE state");
        }
        if (planItemInstance.getReferenceId() == null) {
            throw new FlowableException("Cannot trigger process task plan item instance : no reference id set");
        }
        if (!PlanItemInstanceCallbackType.CHILD_PROCESS.equals(planItemInstance.getReferenceType())) {
            throw new FlowableException("Cannot trigger process task plan item instance : reference type '" 
                    + planItemInstance.getReferenceType() + "' not supported");
        }
        
        // Triggering the plan item (as opposed to a regular complete) terminates the process instance
        CommandContextUtil.getAgenda(commandContext).planCompletePlanItem(planItemInstance);
        deleteProcessInstance(planItemInstance, commandContext);
    }

    @Override
    public void onStateTransition(DelegatePlanItemInstance planItemInstance, String transition) {
        // The process task plan item will be deleted by the regular TerminatePlanItemOperation
        if (PlanItemTransition.TERMINATE.equals(transition) || PlanItemTransition.EXIT.equals(transition)) {
            deleteProcessInstance(planItemInstance, CommandContextUtil.getCommandContext());
        }
    }
    
    protected void deleteProcessInstance(DelegatePlanItemInstance planItemInstance, CommandContext commandContext) {
        ProcessInstanceService processInstanceService = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getProcessInstanceService();
        processInstanceService.deleteProcessInstance(planItemInstance.getReferenceId());
    }
    
}
