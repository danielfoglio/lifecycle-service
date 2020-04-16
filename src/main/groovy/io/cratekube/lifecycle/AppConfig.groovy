package io.cratekube.lifecycle

import de.spinscale.dropwizard.jobs.JobConfiguration
import groovy.transform.Immutable
import io.cratekube.lifecycle.auth.ApiKeyAuthConfig
import io.dropwizard.Configuration
import io.dropwizard.client.JerseyClientConfiguration
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration

import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

/**
 * Configuration class for this Dropwizard application.
 */
class AppConfig extends Configuration implements JobConfiguration {
  @Valid
  @NotNull
  JerseyClientConfiguration jerseyClient

  @Valid
  @NotNull
  SwaggerBundleConfiguration swagger

  @Valid
  @NotNull
  ApiKeyAuthConfig auth

  @NotEmpty
  String kubeconfigLocation

  /**
   * Used to specify how often the dropwizard-jobs library
   * should execute the {@link io.cratekube.lifecycle.job.UpgradeAvailabilityJob}
   */
  @Valid
  @NotEmpty
  Map<String , String> jobs

  @Valid
  @NotEmpty
  Map<String , Boolean> managedComponents

  @Valid
  @NotNull
  GitHubConfig github
}

@Immutable
class GitHubConfig {
  /** location of the organization home */
  @NotEmpty
  String orgHome

  /** base location of the organizations raw content */
  @NotEmpty
  String orgBaseRawHome
}
