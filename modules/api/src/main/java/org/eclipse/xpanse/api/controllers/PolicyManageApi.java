/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 *
 */

package org.eclipse.xpanse.api.controllers;

import static org.eclipse.xpanse.modules.models.security.constant.RoleConstants.ROLE_ADMIN;
import static org.eclipse.xpanse.modules.models.security.constant.RoleConstants.ROLE_USER;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.xpanse.modules.models.policy.PolicyCreateRequest;
import org.eclipse.xpanse.modules.models.policy.PolicyQueryRequest;
import org.eclipse.xpanse.modules.models.policy.PolicyUpdateRequest;
import org.eclipse.xpanse.modules.models.policy.PolicyVo;
import org.eclipse.xpanse.modules.models.service.common.enums.Csp;
import org.eclipse.xpanse.modules.policy.policyman.PolicyManager;
import org.eclipse.xpanse.modules.security.IdentityProviderManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


/**
 * REST interface methods for user managing policies of the cloud service provider.
 */
@Slf4j
@RestController
@RequestMapping("/xpanse")
@CrossOrigin
@Secured({ROLE_ADMIN, ROLE_USER})
public class PolicyManageApi {

    @Resource
    private PolicyManager policyManager;

    @Resource
    private IdentityProviderManager identityProviderManager;

    /**
     * List the policies created by the user.
     *
     * @param csp     The cloud service provider.
     * @param enabled Is the policy enabled.
     * @return Returns list of the policies created by the user.
     */
    @Tag(name = "Policies Management",
            description = "APIs for managing user's infra policies.")
    @GetMapping(value = "/policies",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "List the policies defined by the user.")
    public List<PolicyVo> listPolicies(
            @Parameter(name = "cspName", description = "Name of csp which the policy belongs to.")
            @RequestParam(name = "cspName", required = false) Csp csp,
            @Parameter(name = "enabled", description = "Is the policy enabled.")
            @RequestParam(name = "enabled", required = false) Boolean enabled) {
        return policyManager.listPolicies(getPolicyQueryModel(csp, enabled));
    }


    /**
     * Get the details of the policy created by the user.
     *
     * @param id The id of the policy.
     * @return Returns list of the policies defined by the user.
     */
    @Tag(name = "Policies Management",
            description = "APIs for managing user's infra policies.")
    @GetMapping(value = "/policies/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "Get the details of the policy created by the user.")
    public PolicyVo getPolicyDetails(@PathVariable String id) {
        return policyManager.getPolicyDetails(UUID.fromString(id));
    }


    /**
     * Add policy created by the user.
     *
     * @param policyCreateRequest The policy to be created.
     */
    @Tag(name = "Policies Management",
            description = "APIs for managing user's infra policies.")
    @PostMapping(value = "/policies",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Add policy created by the user.")
    public PolicyVo addPolicy(
            @Valid @RequestBody PolicyCreateRequest policyCreateRequest) {
        return policyManager.addPolicy(policyCreateRequest);
    }

    /**
     * Update the policy created by the user.
     *
     * @param updateRequest The policy to be updated.
     */
    @Tag(name = "Policies Management",
            description = "APIs for managing user's infra policies.")
    @PutMapping(value = "/policies",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "Update the policy created by the user.")
    public PolicyVo updatePolicy(
            @Valid @RequestBody PolicyUpdateRequest updateRequest) {
        return policyManager.updatePolicy(updateRequest);
    }

    /**
     * Delete the policy created by the user.
     *
     * @param id The id of policy.
     */
    @Tag(name = "Policies Management",
            description = "APIs for managing user's infra policies.")
    @DeleteMapping(value = "/policies/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(description = "Delete the policy created by the user.")
    public void deletePolicy(@PathVariable("id") String id) {
        policyManager.deletePolicy(UUID.fromString(id));
    }


    private PolicyQueryRequest getPolicyQueryModel(Csp csp, Boolean enabled) {
        PolicyQueryRequest policyQueryRequest = new PolicyQueryRequest();
        if (Objects.nonNull(csp)) {
            policyQueryRequest.setCsp(csp);
        }
        if (Objects.nonNull(enabled)) {
            policyQueryRequest.setEnabled(enabled);
        }
        Optional<String> userIdOptional = identityProviderManager.getCurrentLoginUserId();
        policyQueryRequest.setUserId(userIdOptional.orElse(null));
        return policyQueryRequest;
    }


}
