package io.cattle.platform.agent.instance.service.impl;

import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

/**
 * This class is invoked on instance healthcheck changes
 * 
 * @author alena
 *
 */
public class ContainerNicLookup extends NicPerVnetNicLookup implements InstanceNicLookup {
    @Inject
    ObjectManager objectManager;

    @Inject
    GenericMapDao mapDao;

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof Instance)) {
            return null;
        }
        Instance container = (Instance) obj;
        return super.getNicPerVnetForAccount(container.getAccountId());
    }
}
