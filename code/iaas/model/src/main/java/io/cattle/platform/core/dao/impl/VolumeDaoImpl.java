package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import static io.cattle.platform.core.model.tables.VolumeStoragePoolMapTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.VolumeDao;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeStoragePoolMap;
import io.cattle.platform.core.model.tables.records.VolumeRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.Record;

public class VolumeDaoImpl extends AbstractJooqDao implements VolumeDao {

    private static final Set<String> LOCAL_POOL_KINDS = new HashSet<String>(Arrays.asList(new String[]{"docker", "sim"}));

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Override
    public Volume findVolumeByExternalId(Long storagePoolId, String externalId) {
        Record record = create()
            .select(VOLUME.fields())
            .from(VOLUME)
            .join(VOLUME_STORAGE_POOL_MAP)
            .on(VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID.eq(storagePoolId))
            .where(VOLUME.EXTERNAL_ID.eq(externalId)
            .and((VOLUME.REMOVED.isNull().or(VOLUME.STATE.eq(CommonStatesConstants.REMOVING)))))
            .fetchAny();
        
        return record == null ? null : record.into(VolumeRecord.class);
    }

    @Override
    public void createVolumeInStoragePool(Map<String, Object> volumeData, String volumeName, StoragePool storagePool) {
        Record record = create()
                .select(VOLUME.fields())
                .from(VOLUME)
                .join(VOLUME_STORAGE_POOL_MAP)
                    .on(VOLUME_STORAGE_POOL_MAP.VOLUME_ID.eq(VOLUME.ID)
                    .and(VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID.eq(storagePool.getId())))
                .join(STORAGE_POOL)
                    .on(VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID))
                    .and(STORAGE_POOL.REMOVED.isNull())
                .where(VOLUME.NAME.eq(volumeName)
                    .and((VOLUME.REMOVED.isNull().or(VOLUME.STATE.eq(CommonStatesConstants.REMOVING)))))
                    .and(VOLUME.ACCOUNT_ID.eq(storagePool.getAccountId()))
                .fetchAny();
        if (record != null) {
            return;
        }

        Volume volume = resourceDao.createAndSchedule(Volume.class, volumeData);
        Map<String, Object> vspm = new HashMap<String, Object>();
        vspm.put("volumeId", volume.getId());
        vspm.put("storagePoolId", storagePool.getId());
        resourceDao.createAndSchedule(VolumeStoragePoolMap.class, vspm);
    }

    @Override
    public List<? extends Volume> findSharedOrUnmappedVolumes(long accountId, String volumeName) {
        List<VolumeRecord> volumes = create()
            .selectDistinct(VOLUME.fields())
            .from(VOLUME)
            .leftOuterJoin(VOLUME_STORAGE_POOL_MAP)
                .on(VOLUME_STORAGE_POOL_MAP.VOLUME_ID.eq(VOLUME.ID))
            .leftOuterJoin(STORAGE_POOL)
                .on(VOLUME_STORAGE_POOL_MAP.STORAGE_POOL_ID.eq(STORAGE_POOL.ID)
                .and(STORAGE_POOL.REMOVED.isNull()))
            .where(VOLUME.NAME.eq(volumeName)
                .and((VOLUME.REMOVED.isNull())))
                .and(VOLUME.ACCOUNT_ID.eq(accountId))
                .and(STORAGE_POOL.KIND.notIn(LOCAL_POOL_KINDS).or(STORAGE_POOL.KIND.isNull()))
            .fetchInto(VolumeRecord.class);

        return volumes;
    }
}
