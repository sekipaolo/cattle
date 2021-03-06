package io.cattle.platform.iaas.api.auditing;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ContainerEventConstants;
import io.cattle.platform.core.model.AuditLog;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.iaas.api.auditing.dao.AuditLogDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.externalevent.ExternalEventConstants;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditServiceImpl implements AuditService{

    public static final Set<String> BLACK_LIST_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "publish",
                    "configContent".toLowerCase(),
                    "token",
                    ContainerEventConstants.CONTAINER_EVENT_KIND.toLowerCase(),
                    "serviceEvent".toLowerCase(),
                    "userPreference".toLowerCase(),
                    ExternalEventConstants.KIND_EXTERNAL_EVENT.toLowerCase()
            )));

    @Inject
    AuditLogDao auditLogDao;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    EventService eventService;

    @Inject
    ObjectManager objectManager;

    private static final Logger log = LoggerFactory.getLogger(AuditLogsRequestHandler.class);

    @Override
    public void logRequest(ApiRequest request, Policy policy) {
        if (Schema.Method.GET.isMethod(request.getMethod()) ||
                BLACK_LIST_TYPES.contains(request.getType().toLowerCase())) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        Map<?, ?> requestObject = CollectionUtils.toMap(request.getRequestObject());
        putInAsString(data, "requestObject", "Failed to convert request object to json.", request.getRequestObject());
        putInAsString(data, "responseObject", "Failed to convert response object to json.", request.getResponseObject());
        data.put("responseCode", request.getResponseCode());
        Identity user = null;
        for (Identity identity: policy.getIdentities()){
            if (identity.getExternalIdType().contains("user")){
                user = identity;
                break;
            }
        }
        if (user == null && policy.getIdentities().size() == 1){
            user = policy.getIdentities().iterator().next();
        }
        long runtime = ((long) request.getAttribute("requestEndTime")) - ((long)request.getAttribute("requestStartTime"));
        String authType = (String) request.getAttribute(AccountConstants.AUTH_TYPE);
        String resourceId =request.getResponseObject() instanceof Resource ? ((Resource) request.getResponseObject()).getId(): null;
        String resourceType = convertResourceType(request.getType());
        String eventType = "api." + resourceType + "." + (requestObject.get("action") != null ? (String) requestObject.get
                ("action") :convertToAction(request.getMethod()));
        publishEvent(auditLogDao.create(resourceType, parseId(resourceId), data, user,
                policy.getAccountId(), policy.getAuthenticatedAsAccountId(), eventType, authType, runtime, null, request.getClientIp()));
    }

    private String convertResourceType(String type) {
        switch (type.toLowerCase()) {
            case "environment":
                return "stack";
            case "project":
                return "environment";
            default:
                return type;
        }
    }

    private Long parseId(String resourceId) {
        Long parsedResourceId;
        if (resourceId == null){
            parsedResourceId = null;
        } else {
            try {
                parsedResourceId = Long.valueOf(ApiContext.getContext().getIdFormatter().parseId(resourceId));
            } catch (NumberFormatException e){
                parsedResourceId = null;
            }
        }
        return parsedResourceId;
    }

    private AuditEventType convertToAction(String method) {
        switch (Schema.Method.valueOf(method)){
            case DELETE:
                return AuditEventType.delete;
            case POST:
                return AuditEventType.create;
            case PUT:
                return AuditEventType.update;
            default:
                return AuditEventType.UNKNOWN;
        }
    }

    @Override
    public void logResourceModification(Object resource, Map<String, Object> data, AuditEventType eventType,
                                        String description, Long accountId, String clientIp) {
        putInAsString(data, "resource", "Failed to convert resource to json.", resource);
        publishEvent(auditLogDao.create(objectManager.getType(resource), parseId((String) ObjectUtils.getId(resource)),
                data, null, accountId, null, eventType.toString(), null, null, description, clientIp));
    }

    protected void publishEvent(AuditLog auditLog) {
        Map<String, Object> data = new HashMap<>();
        data.put(ObjectMetaDataManager.ACCOUNT_FIELD, auditLog.getAccountId());

        Event event = EventVO.newEvent(FrameworkEvents.STATE_CHANGE)
                .withData(data)
                .withResourceType(auditLog.getClass().getName())
                .withResourceId(auditLog.getId().toString());

        eventService.publish(event);
    }

    private void putInAsString(Map<String, Object> data, String fieldForObject, String errMsg, Object objectToPlace) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            jsonMapper.writeValue(os, objectToPlace);
            data.put(fieldForObject, os.toString());
        } catch (IOException e) {
            log.error(errMsg);
            data.put("requestObject", "{\"object\":\"" + errMsg + "\"");
        }
    }
}
