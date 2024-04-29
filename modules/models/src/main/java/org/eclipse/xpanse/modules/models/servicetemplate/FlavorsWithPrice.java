/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.modules.models.servicetemplate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * Defines for service flavors with price.
 */
@Data
public class FlavorsWithPrice {

    @NotNull
    @Schema(description = "The flavors of the managed service.")
    List<ServiceFlavorWithPrice> serviceFlavors;

    @NotNull
    @Schema(description = "Impact on service when flavor is changed.")
    private ModificationImpact modificationImpact;
}
