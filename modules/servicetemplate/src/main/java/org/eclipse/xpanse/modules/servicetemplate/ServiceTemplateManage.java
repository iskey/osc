/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 *
 */

package org.eclipse.xpanse.modules.servicetemplate;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.xpanse.modules.database.servicetemplate.ServiceTemplateEntity;
import org.eclipse.xpanse.modules.database.servicetemplate.ServiceTemplateQueryModel;
import org.eclipse.xpanse.modules.database.servicetemplate.ServiceTemplateStorage;
import org.eclipse.xpanse.modules.deployment.DeployerKindManager;
import org.eclipse.xpanse.modules.models.common.exceptions.OpenApiFileGenerationException;
import org.eclipse.xpanse.modules.models.service.utils.ServiceVariablesJsonSchemaGenerator;
import org.eclipse.xpanse.modules.models.servicetemplate.Deployment;
import org.eclipse.xpanse.modules.models.servicetemplate.Ocl;
import org.eclipse.xpanse.modules.models.servicetemplate.ReviewRegistrationRequest;
import org.eclipse.xpanse.modules.models.servicetemplate.enums.DeployerKind;
import org.eclipse.xpanse.modules.models.servicetemplate.enums.ServiceRegistrationState;
import org.eclipse.xpanse.modules.models.servicetemplate.enums.ServiceReviewResult;
import org.eclipse.xpanse.modules.models.servicetemplate.exceptions.InvalidServiceVersionException;
import org.eclipse.xpanse.modules.models.servicetemplate.exceptions.OpenTofuScriptFormatInvalidException;
import org.eclipse.xpanse.modules.models.servicetemplate.exceptions.ServiceTemplateAlreadyRegistered;
import org.eclipse.xpanse.modules.models.servicetemplate.exceptions.ServiceTemplateAlreadyReviewed;
import org.eclipse.xpanse.modules.models.servicetemplate.exceptions.ServiceTemplateNotRegistered;
import org.eclipse.xpanse.modules.models.servicetemplate.exceptions.ServiceTemplateUpdateNotAllowed;
import org.eclipse.xpanse.modules.models.servicetemplate.exceptions.TerraformScriptFormatInvalidException;
import org.eclipse.xpanse.modules.models.servicetemplate.utils.JsonObjectSchema;
import org.eclipse.xpanse.modules.models.servicetemplate.validators.BillingConfigValidator;
import org.eclipse.xpanse.modules.orchestrator.deployment.DeployValidateDiagnostics;
import org.eclipse.xpanse.modules.orchestrator.deployment.DeploymentScriptValidationResult;
import org.eclipse.xpanse.modules.security.UserServiceHelper;
import org.eclipse.xpanse.modules.servicetemplate.utils.AvailabilityZoneSchemaValidator;
import org.eclipse.xpanse.modules.servicetemplate.utils.DeployVariableSchemaValidator;
import org.eclipse.xpanse.modules.servicetemplate.utils.IconProcessorUtil;
import org.eclipse.xpanse.modules.servicetemplate.utils.ServiceTemplateOpenApiGenerator;
import org.semver4j.Semver;
import org.semver4j.SemverException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Implement Interface to manage service template newTemplate in database.
 */
@Slf4j
@Service
public class ServiceTemplateManage {

    @Resource
    private ServiceTemplateStorage storage;
    @Resource
    private ServiceTemplateOpenApiGenerator serviceTemplateOpenApiGenerator;
    @Resource
    private UserServiceHelper userServiceHelper;
    @Resource
    private ServiceVariablesJsonSchemaGenerator serviceVariablesJsonSchemaGenerator;
    @Resource
    private DeployerKindManager deployerKindManager;
    @Resource
    private BillingConfigValidator billingConfigValidator;

    /**
     * Update service template using id and the ocl model.
     *
     * @param id  id of the service template.
     * @param ocl the Ocl model describing the service template.
     * @return Returns service template DB newTemplate.
     */
    public ServiceTemplateEntity updateServiceTemplate(UUID id, Ocl ocl) {
        ServiceTemplateEntity existingTemplate = getServiceTemplateDetails(id, true, false);
        iconUpdate(existingTemplate, ocl);
        checkParams(existingTemplate, ocl);
        billingConfigValidator.validateServiceFlavors(ocl);
        validateServiceDeployment(ocl.getDeployment(), existingTemplate);
        existingTemplate.setOcl(ocl);
        existingTemplate.setServiceRegistrationState(ServiceRegistrationState.APPROVAL_PENDING);
        ServiceTemplateEntity updatedServiceTemplate = storage.storeAndFlush(existingTemplate);
        serviceTemplateOpenApiGenerator.updateServiceApi(updatedServiceTemplate);
        return updatedServiceTemplate;
    }

    private void checkParams(ServiceTemplateEntity existingTemplate, Ocl ocl) {

        String oldCategory = existingTemplate.getCategory().name();
        String newCategory = ocl.getCategory().name();
        compare(oldCategory, newCategory, "category");

        String oldName = existingTemplate.getName();
        String newName = ocl.getName();
        compare(oldName, newName, "service name");

        String oldVersion = existingTemplate.getVersion();
        String newVersion = getSemverVersion(ocl.getServiceVersion()).getVersion();
        compare(oldVersion, newVersion, "service version");

        String oldCsp = existingTemplate.getCsp().name();
        String newCsp = ocl.getCloudServiceProvider().getName().name();
        compare(oldCsp, newCsp, "csp");

        String oldServiceHostingType = existingTemplate.getServiceHostingType().toValue();
        String newServiceHostingType = ocl.getServiceHostingType().toValue();
        compare(oldServiceHostingType, newServiceHostingType, "service hosting type");
    }

    private void compare(String oldParams, String newParams, String type) {
        if (!newParams.toLowerCase(Locale.ROOT).equals(oldParams.toLowerCase(Locale.ROOT))) {
            log.error("Update service failed, Value of {} cannot be changed with an update request",
                    type);
            throw new ServiceTemplateUpdateNotAllowed(String.format(
                    "Update service failed, Value of %s be cannot changed with an update request",
                    type));
        }
    }

    private ServiceTemplateEntity getNewServiceTemplateEntity(Ocl ocl) {
        ServiceTemplateEntity newTemplate = new ServiceTemplateEntity();
        newTemplate.setName(StringUtils.lowerCase(ocl.getName()));
        newTemplate.setVersion(getSemverVersion(ocl.getServiceVersion()).getVersion());
        newTemplate.setCsp(ocl.getCloudServiceProvider().getName());
        newTemplate.setCategory(ocl.getCategory());
        newTemplate.setServiceHostingType(ocl.getServiceHostingType());
        newTemplate.setOcl(ocl);
        newTemplate.setServiceRegistrationState(ServiceRegistrationState.APPROVAL_PENDING);
        newTemplate.setServiceProviderContactDetails(ocl.getServiceProviderContactDetails());
        return newTemplate;
    }


    private Semver getSemverVersion(String serviceVersion) {
        try {
            return new Semver(serviceVersion);
        } catch (SemverException e) {
            String errorMsg = String.format("The service version %s is a invalid semver version.",
                    serviceVersion);
            throw new InvalidServiceVersionException(errorMsg);
        }
    }

    private void validateServiceVersion(Ocl ocl) {
        Semver newSemver = getSemverVersion(ocl.getServiceVersion());
        ServiceTemplateQueryModel query = new ServiceTemplateQueryModel(ocl.getCategory(),
                ocl.getCloudServiceProvider().getName(), ocl.getName(), null,
                ocl.getServiceHostingType(), null, false);
        List<ServiceTemplateEntity> templates = storage.listServiceTemplates(query);
        if (!CollectionUtils.isEmpty(templates)) {
            Semver highestVersion = templates.stream()
                    .map(serviceTemplate -> new Semver(serviceTemplate.getVersion())).sorted()
                    .toList().reversed().getFirst();
            if (!newSemver.isGreaterThan(highestVersion)) {
                String errorMsg = String.format("The version %s of service must be higher than the"
                                + " highest version %s of the registered services with same name",
                        newSemver, highestVersion);
                log.error(errorMsg);
                throw new InvalidServiceVersionException(errorMsg);
            }
        }
    }

    private void iconUpdate(ServiceTemplateEntity serviceTemplateEntity, Ocl ocl) {
        try {
            ocl.setIcon(IconProcessorUtil.processImage(ocl));
        } catch (Exception e) {
            ocl.setIcon(serviceTemplateEntity.getOcl().getIcon());
        }
    }

    /**
     * Register service template using the ocl.
     *
     * @param ocl the Ocl model describing the service template.
     * @return Returns service template DB newTemplate.
     */
    public ServiceTemplateEntity registerServiceTemplate(Ocl ocl) {
        ServiceTemplateEntity newTemplate = getNewServiceTemplateEntity(ocl);
        ServiceTemplateEntity existingTemplate = storage.findServiceTemplate(newTemplate);
        if (Objects.nonNull(existingTemplate)) {
            String errorMsg = String.format("Service template already registered with id %s",
                    existingTemplate.getId());
            log.error(errorMsg);
            throw new ServiceTemplateAlreadyRegistered(errorMsg);
        }
        validateServiceVersion(ocl);
        ocl.setIcon(IconProcessorUtil.processImage(ocl));
        billingConfigValidator.validateServiceFlavors(ocl);
        validateServiceDeployment(ocl.getDeployment(), newTemplate);
        String userManageNamespace =
                userServiceHelper.getCurrentUserManageNamespace();
        newTemplate.setNamespace(userManageNamespace);
        ServiceTemplateEntity storedServiceTemplate = storage.storeAndFlush(newTemplate);
        serviceTemplateOpenApiGenerator.generateServiceApi(storedServiceTemplate);
        return storedServiceTemplate;
    }

    private void validateServiceDeployment(Deployment deployment,
                                           ServiceTemplateEntity serviceTemplate) {
        AvailabilityZoneSchemaValidator.validateServiceAvailability(
                deployment.getServiceAvailability());
        DeployVariableSchemaValidator.validateDeployVariable(deployment.getVariables());
        JsonObjectSchema jsonObjectSchema =
                serviceVariablesJsonSchemaGenerator.buildJsonObjectSchema(
                        deployment.getVariables());
        serviceTemplate.setJsonObjectSchema(jsonObjectSchema);
        validateTerraformScript(deployment);
    }

    /**
     * Get detail of service template using ID.
     *
     * @param id             the ID of
     * @param checkNamespace check the namespace of the service template belonging to.
     * @param checkCsp       check the cloud service provider of the service template.
     * @return Returns service template DB newTemplate.
     */
    public ServiceTemplateEntity getServiceTemplateDetails(UUID id, boolean checkNamespace,
                                                           boolean checkCsp) {
        ServiceTemplateEntity existingTemplate = getServiceTemplateById(id);
        if (checkNamespace) {
            boolean hasManagePermissions = userServiceHelper.currentUserCanManageNamespace(
                    existingTemplate.getNamespace());
            if (!hasManagePermissions) {
                throw new AccessDeniedException("No permissions to view or manage service template "
                        + "belonging to other namespaces.");
            }
        }
        if (checkCsp) {
            boolean hasManagePermissions = userServiceHelper.currentUserCanManageCsp(
                    existingTemplate.getCsp());
            if (!hasManagePermissions) {
                throw new AccessDeniedException("No permissions to review service template "
                        + "belonging to other cloud service providers.");
            }
        }
        return existingTemplate;
    }

    /**
     * Search service templates with query model.
     *
     * @param query service template query model.
     * @return Returns list of service template newTemplate.
     */
    public List<ServiceTemplateEntity> listServiceTemplates(ServiceTemplateQueryModel query) {
        fillParamFromUserMetadata(query);
        return storage.listServiceTemplates(query);
    }

    /**
     * Review service template registration.
     *
     * @param id      ID of service template.
     * @param request the request of review registration.
     */
    public void reviewServiceTemplateRegistration(UUID id, ReviewRegistrationRequest request) {
        ServiceTemplateEntity existingTemplate = getServiceTemplateDetails(id, false, true);
        if (ServiceRegistrationState.APPROVED == existingTemplate.getServiceRegistrationState()
                || ServiceRegistrationState.REJECTED
                == existingTemplate.getServiceRegistrationState()) {
            String errMsg = String.format("Service template with id %s already reviewed.",
                    existingTemplate.getId());
            log.error(errMsg);
            throw new ServiceTemplateAlreadyReviewed(errMsg);
        }
        if (ServiceReviewResult.APPROVED == request.getReviewResult()) {
            existingTemplate.setServiceRegistrationState(ServiceRegistrationState.APPROVED);
        } else if (ServiceReviewResult.REJECTED == request.getReviewResult()) {
            existingTemplate.setServiceRegistrationState(ServiceRegistrationState.REJECTED);
        }
        String reviewComment = StringUtils.isNotBlank(request.getReviewComment())
                ? request.getReviewComment() : request.getReviewResult().toValue();
        existingTemplate.setReviewComment(reviewComment);
        storage.storeAndFlush(existingTemplate);
    }


    /**
     * Unregister service template using the ID of service template.
     *
     * @param id ID of service template.
     */
    public void unregisterServiceTemplate(UUID id) {
        ServiceTemplateEntity existingTemplate = getServiceTemplateDetails(id, true, false);
        storage.removeById(existingTemplate.getId());
        serviceTemplateOpenApiGenerator.deleteServiceApi(id.toString());
    }

    /**
     * generate OpenApi for service template using the ID.
     *
     * @param id ID of service template.
     * @return path of openapi.html
     */
    public String getOpenApiUrl(UUID id) {
        String openApiUrl = serviceTemplateOpenApiGenerator.getOpenApi(getServiceTemplateById(id));
        if (StringUtils.isBlank(openApiUrl)) {
            throw new OpenApiFileGenerationException("Get openApi Url is Empty.");
        }
        return openApiUrl;
    }

    private ServiceTemplateEntity getServiceTemplateById(UUID id) {
        ServiceTemplateEntity serviceTemplate = storage.getServiceTemplateById(id);
        if (Objects.isNull(serviceTemplate) || Objects.isNull(serviceTemplate.getOcl())) {
            String errMsg = String.format("Service template with id %s not found.", id);
            log.error(errMsg);
            throw new ServiceTemplateNotRegistered(errMsg);
        }
        return serviceTemplate;
    }

    private void validateTerraformScript(Deployment deployment) {
        if (deployment.getKind() == DeployerKind.TERRAFORM) {
            DeploymentScriptValidationResult tfValidationResult =
                    this.deployerKindManager.getDeployment(deployment.getKind())
                            .validate(deployment);
            if (!tfValidationResult.isValid()) {
                throw new TerraformScriptFormatInvalidException(
                        tfValidationResult.getDiagnostics().stream()
                                .map(DeployValidateDiagnostics::getDetail)
                                .collect(Collectors.toList()));
            }
        }

        if (deployment.getKind() == DeployerKind.OPEN_TOFU) {
            DeploymentScriptValidationResult tfValidationResult =
                    this.deployerKindManager.getDeployment(deployment.getKind())
                            .validate(deployment);
            if (!tfValidationResult.isValid()) {
                throw new OpenTofuScriptFormatInvalidException(
                        tfValidationResult.getDiagnostics().stream()
                                .map(DeployValidateDiagnostics::getDetail)
                                .collect(Collectors.toList()));
            }
        }
    }

    private void fillParamFromUserMetadata(ServiceTemplateQueryModel query) {
        if (query.isCheckNamespace()) {
            String namespace = userServiceHelper.getCurrentUserManageNamespace();
            query.setNamespace(namespace);
        }
    }


}
