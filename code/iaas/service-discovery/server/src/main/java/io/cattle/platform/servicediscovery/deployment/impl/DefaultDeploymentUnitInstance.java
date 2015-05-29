package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.InstanceUnit;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public class DefaultDeploymentUnitInstance extends DeploymentUnitInstance implements InstanceUnit {
    protected String instanceName;
    protected Instance instance;

    public DefaultDeploymentUnitInstance() {
        super(null, null, null);
    }

    public DefaultDeploymentUnitInstance(DeploymentServiceContext context, String uuid,
            Service service,
            String instanceName, Instance instance, Map<String, String> labels) {
        super(context, uuid, service);
        this.instanceName = instanceName;
        this.instance = instance;
        if (this.instance != null) {
            exposeMap = context.exposeMapDao.findInstanceExposeMap(this.instance);
        }
    }

    @Override
    public boolean isError() {
        return this.instance.getRemoved() != null;
    }

    @Override
    public void remove() {
        // 1) schedule remove for the instance
        // removal instance is scheduled ahead of map removal, to avoid situation when map removal is scheduled, but
        // removal scheduling fails by some reason
        if (!(instance.getState().equals(CommonStatesConstants.REMOVED) || instance.getState().equals(
                CommonStatesConstants.REMOVING))) {
            try {
                context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, instance,
                        null);
            } catch (ProcessCancelException e) {
                context.objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                        instance, ProcessUtils.chainInData(new HashMap<String, Object>(),
                                InstanceConstants.PROCESS_STOP, InstanceConstants.PROCESS_REMOVE));
            }
        }

        // 2) remove the mapping
        List<? extends ServiceExposeMap> maps = context.objectManager.mappedChildren(
                context.objectManager.loadResource(Instance.class, instance.getId()),
                ServiceExposeMap.class);
        for (ServiceExposeMap map : maps) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, map, null);
        }
    }

    @Override
    public DeploymentUnitInstance start(Map<String, Object> deployParams) {
        if (createNew()) {
            deployParams.put("name", this.instanceName);
            Map<String, Object> launchConfigData = context.sdService.buildServiceInstanceLaunchData(service,
                    deployParams);
            Pair<Instance, ServiceExposeMap> instanceMapPair = context.exposeMapDao.createServiceInstance(launchConfigData,
                    service, this.instanceName);
            this.instance = instanceMapPair.getLeft();
            this.exposeMap = instanceMapPair.getRight();
        }
        context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, instance,
                null);
        context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, exposeMap,
                null);

        if (InstanceConstants.STATE_STOPPED.equals(instance.getState())) {
            context.objectProcessManager.scheduleProcessInstanceAsync(
                            InstanceConstants.PROCESS_START, instance, null);
        }
        this.instance = context.objectManager.reload(this.instance);
        return this;
    }

    @Override
    public boolean createNew() {
        return this.instance == null;
    }

    @Override
    public DeploymentUnitInstance waitForStart() {
        this.instance = context.resourceMonitor.waitFor(context.objectManager.reload(this.instance),
                new ResourcePredicate<Instance>() {
            @Override
            public boolean evaluate(Instance obj) {
                return InstanceConstants.STATE_RUNNING.equals(obj.getState());
            }
        });
        return this;
    }

    @Override
    public void stop() {
        if (this.instance != null && this.instance.getState().equals(InstanceConstants.STATE_RUNNING)) {
            context.objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP, instance,
                    null);
        }
    }

    @Override
    public boolean isStarted() {
        return context.objectManager.reload(this.instance).getState().equalsIgnoreCase(InstanceConstants.STATE_RUNNING);
    }

    @Override
    public Instance getInstance() {
        return instance;
    }
}
