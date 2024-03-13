/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 *
 */

package org.eclipse.xpanse.modules.deployment.deployers.terraform.terraformlocal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.xpanse.modules.deployment.deployers.terraform.terraformlocal.TerraformLocalDeployment.STATE_FILE_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.xpanse.modules.async.TaskConfiguration;
import org.eclipse.xpanse.modules.deployment.DeployService;
import org.eclipse.xpanse.modules.deployment.DeployServiceEntityHandler;
import org.eclipse.xpanse.modules.deployment.ResourceHandlerManager;
import org.eclipse.xpanse.modules.deployment.deployers.terraform.callbacks.TerraformDeploymentResultCallbackManager;
import org.eclipse.xpanse.modules.deployment.deployers.terraform.exceptions.TerraformExecutorException;
import org.eclipse.xpanse.modules.deployment.deployers.terraform.terraformlocal.config.TerraformLocalConfig;
import org.eclipse.xpanse.modules.deployment.deployers.terraform.utils.TfResourceTransUtils;
import org.eclipse.xpanse.modules.deployment.utils.DeployEnvironments;
import org.eclipse.xpanse.modules.deployment.utils.ScriptsGitRepoManage;
import org.eclipse.xpanse.modules.models.service.deploy.DeployRequest;
import org.eclipse.xpanse.modules.models.service.deploy.enums.DeployerTaskStatus;
import org.eclipse.xpanse.modules.models.service.deploy.exceptions.ServiceNotDeployedException;
import org.eclipse.xpanse.modules.models.servicetemplate.Ocl;
import org.eclipse.xpanse.modules.models.servicetemplate.Region;
import org.eclipse.xpanse.modules.models.servicetemplate.enums.DeployerKind;
import org.eclipse.xpanse.modules.models.servicetemplate.utils.OclLoader;
import org.eclipse.xpanse.modules.orchestrator.PluginManager;
import org.eclipse.xpanse.modules.orchestrator.deployment.DeployResult;
import org.eclipse.xpanse.modules.orchestrator.deployment.DeployTask;
import org.eclipse.xpanse.modules.orchestrator.deployment.DeployValidateDiagnostics;
import org.eclipse.xpanse.modules.orchestrator.deployment.DeploymentScriptValidationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test for TerraformDeployment.
 */

@ExtendWith({SpringExtension.class})
@ContextConfiguration(classes = {TerraformLocalDeployment.class, DeployEnvironments.class,
        PluginManager.class, TerraformLocalConfig.class, DeployService.class,
        TaskConfiguration.class, ResourceHandlerManager.class, ScriptsGitRepoManage.class})
class TerraformLocalDeploymentTest {

    private final UUID id = UUID.randomUUID();
    private final String errorDeployer = "error_deployer";
    private final String invalidDeployer = """
            resource "random_id" "new" {
              byte_length = 4
            }
                                    
            output "random_id" {
              value = resource.random_id_2.new.id
            }
            """;
    @Autowired
    TerraformLocalDeployment terraformLocalDeployment;
    @MockBean
    DeployEnvironments deployEnvironments;
    @MockBean
    TerraformLocalConfig terraformLocalConfig;
    @MockBean
    PluginManager pluginManager;
    @MockBean
    DeployService deployService;
    @MockBean
    Executor taskExecutor;
    @MockBean
    TerraformDeploymentResultCallbackManager terraformDeploymentResultCallbackManager;
    @MockBean
    DeployServiceEntityHandler deployServiceEntityHandler;
    private DeployRequest deployRequest;
    private DeployResult deployResult;
    private Ocl ocl;
    private String tfState;

    @BeforeEach
    void setUp() throws Exception {
        OclLoader oclLoader = new OclLoader();
        ocl = oclLoader.getOcl(URI.create("file:src/test/resources/ocl_terraform_test.yml").toURL());

        deployRequest = new DeployRequest();
        deployRequest.setServiceName(ocl.getName());
        deployRequest.setVersion(ocl.getServiceVersion());
        deployRequest.setFlavor(ocl.getFlavors().getFirst().getName());
        Region region = new Region();
        region.setName(ocl.getCloudServiceProvider().getRegions().getFirst().getName());
        region.setArea(ocl.getCloudServiceProvider().getRegions().getFirst().getArea());
        deployRequest.setRegion(region);
        deployRequest.setCsp(ocl.getCloudServiceProvider().getName());
        deployRequest.setCategory(ocl.getCategory());
        deployRequest.setCustomerServiceName("test_deploy");
        deployRequest.setServiceRequestProperties(Map.ofEntries(Map.entry("key", "value")));

        doReturn(new HashMap<>()).when(this.deployEnvironments)
                .getCredentialVariablesByHostingType(any(), any(), any(), any());
    }

    @Test
    @Order(1)
    void testDeploy() {
        DeployTask deployTask = new DeployTask();
        deployTask.setId(id);
        deployTask.setOcl(ocl);
        deployTask.setDeployRequest(deployRequest);
        deployResult = terraformLocalDeployment.deploy(deployTask);
        Assertions.assertNotNull(deployResult);
        Assertions.assertNotNull(deployResult.getPrivateProperties());
        tfState = deployResult.getPrivateProperties().get(STATE_FILE_NAME);
        Assertions.assertNull(tfState);
        Assertions.assertNull(deployResult.getState());

    }

    @Test
    @Order(2)
    void testDestroy() {
        DeployTask deployTask = new DeployTask();
        deployTask.setId(id);
        deployTask.setOcl(ocl);
        deployTask.setDeployRequest(deployRequest);
        if (StringUtils.isBlank(tfState)) {
            testDeploy();
        }
        when(this.deployEnvironments.getFlavorVariables(any(DeployTask.class))).thenReturn(
                new HashMap<>());
        Assertions.assertThrows(ServiceNotDeployedException.class,
                () -> terraformLocalDeployment.destroy(deployTask));
    }

    @Test
    @Order(3)
    void testDeleteTaskWorkspace() {
        String workspacePath = System.getProperty("java.io.tmpdir") + File.separator
                + terraformLocalConfig.getWorkspaceDirectory() + File.separator + id;
        this.terraformLocalDeployment.deleteTaskWorkspace(id);
        Assertions.assertFalse(new File(workspacePath).exists());
    }

    @Test
    void testDeploy_FailedCausedByTerraformExecutorException() {
        ocl.getDeployment().setDeployer(invalidDeployer);
        DeployTask deployTask = new DeployTask();
        deployTask.setId(UUID.randomUUID());
        deployTask.setOcl(ocl);
        deployTask.setDeployRequest(deployRequest);
        DeployResult deployResult = this.terraformLocalDeployment.deploy(deployTask);
        Assertions.assertNull(deployResult.getState());
        Assertions.assertNotEquals(DeployerTaskStatus.DEPLOY_SUCCESS.toValue(),
                deployResult.getMessage());

    }

    @Test
    void testDestroy_FailedCausedByTerraformExecutorException() {
        try (MockedStatic<TfResourceTransUtils> tfResourceTransUtils = Mockito.mockStatic(
                TfResourceTransUtils.class)) {
            tfResourceTransUtils.when(() -> TfResourceTransUtils.getStoredStateContent(any()))
                    .thenReturn("Test");
            ocl.getDeployment().setDeployer(errorDeployer);
            DeployTask deployTask = new DeployTask();
            deployTask.setId(id);
            deployTask.setOcl(ocl);
            deployTask.setDeployRequest(deployRequest);
            DeployResult deployResult = this.terraformLocalDeployment.destroy(deployTask);
            Assertions.assertTrue(deployResult.getProperties().isEmpty());
            Assertions.assertNull(deployResult.getState());
            Assertions.assertNotEquals(DeployerTaskStatus.DESTROY_FAILED.toValue(),
                    deployResult.getMessage());
        }
    }

    @Test
    void testGetDeployerKind() {
        DeployerKind deployerKind = terraformLocalDeployment.getDeployerKind();

        Assertions.assertEquals(DeployerKind.TERRAFORM, deployerKind);
    }

    @Test
    void testGetDeployPlanAsJson() {
        DeployRequest deployRequest = new DeployRequest();
        deployRequest.setServiceName(ocl.getName());
        deployRequest.setVersion(ocl.getServiceVersion());
        deployRequest.setFlavor(ocl.getFlavors().getFirst().getName());
        Region region = new Region();
        region.setName(ocl.getCloudServiceProvider().getRegions().getFirst().getName());
        region.setArea(ocl.getCloudServiceProvider().getRegions().getFirst().getArea());
        deployRequest.setRegion(region);
        deployRequest.setCsp(ocl.getCloudServiceProvider().getName());
        deployRequest.setCategory(ocl.getCategory());
        deployRequest.setCustomerServiceName("test_deploy");
        deployRequest.setServiceRequestProperties(Map.ofEntries(Map.entry("key", "value")));

        DeployTask deployTask = new DeployTask();
        deployTask.setId(id);
        deployTask.setOcl(ocl);
        deployTask.setDeployRequest(deployRequest);

        String deployPlanJson = terraformLocalDeployment.getDeploymentPlanAsJson(deployTask);
        Assertions.assertNotNull(deployPlanJson);

    }

    @Test
    void testGetDeployPlanAsJson_ThrowsException() {

        ocl.getDeployment().setDeployer(errorDeployer);
        DeployTask deployTask = new DeployTask();
        deployTask.setId(UUID.randomUUID());
        deployTask.setOcl(ocl);
        deployTask.setDeployRequest(deployRequest);
        deployResult = this.terraformLocalDeployment.deploy(deployTask);

        Assertions.assertThrows(TerraformExecutorException.class,
                () -> this.terraformLocalDeployment.getDeploymentPlanAsJson(deployTask));

    }

    @Test
    void testValidate() {
        DeploymentScriptValidationResult expectedResult = new DeploymentScriptValidationResult();
        expectedResult.setValid(true);
        expectedResult.setDiagnostics(new ArrayList<>());

        // Run the test
        final DeploymentScriptValidationResult result =
                terraformLocalDeployment.validate(ocl.getDeployment());

        // Verify the results
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void testValidateFailed() {
        ocl.getDeployment().setDeployer(invalidDeployer);

        DeploymentScriptValidationResult expectedResult = new DeploymentScriptValidationResult();
        expectedResult.setValid(false);
        DeployValidateDiagnostics diagnostics = new DeployValidateDiagnostics();
        diagnostics.setDetail(
                "A managed resource \"random_id_2\" \"new\" has not been declared in the root module.");
        expectedResult.setDiagnostics(List.of(diagnostics));

        // Run the test
        final DeploymentScriptValidationResult result =
                terraformLocalDeployment.validate(ocl.getDeployment());

        // Verify the results
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void testValidate_ThrowsTerraformExecutorException() {
        ocl.getDeployment().setDeployer(errorDeployer);
        Assertions.assertThrows(TerraformExecutorException.class,
                () -> this.terraformLocalDeployment.validate(ocl.getDeployment()));
    }

}
