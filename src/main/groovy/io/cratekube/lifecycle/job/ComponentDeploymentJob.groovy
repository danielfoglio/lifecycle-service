package io.cratekube.lifecycle.job

import de.spinscale.dropwizard.jobs.Job
import de.spinscale.dropwizard.jobs.annotations.DelayStart
import de.spinscale.dropwizard.jobs.annotations.Every
import groovy.util.logging.Slf4j
import io.cratekube.lifecycle.AppConfig
import io.cratekube.lifecycle.api.ComponentApi
import io.cratekube.lifecycle.api.exception.ApiException
import io.cratekube.lifecycle.model.Component
import io.cratekube.lifecycle.modules.annotation.ComponentCache
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import javax.inject.Inject

import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.Assertive.require

/**
 * Responsible for polling github and kubernetes deploying the latest deployment.yml version from github.
 */
@Slf4j
@Every
@DelayStart('30s')
class ComponentDeploymentJob extends Job {
  Map<String, Component> componentCache
  ComponentApi componentApi
  AppConfig config

  @Inject
  ComponentDeploymentJob(@ComponentCache Map<String, Component> componentCache, ComponentApi componentApi, AppConfig config) {
    this.componentCache = require componentCache, notNullValue()
    this.componentApi = require componentApi, notNullValue()
    this.config = require config, notNullValue()
  }

  /**
   *  Retrieves and deploys the the managed component if it is not in k8s and the {@link AppConfig#managedComponents} value is {@code true}.
   *  <P>
   *  The job repeats on a configurable interval {@code DEPLOYMENT_JOB_EXECUTION_FREQUENCY} in app.yml.
   *  <p>
   * {@inheritDoc }
   */
  @Override
  void doJob(JobExecutionContext context) throws JobExecutionException {
    log.debug('Executing ComponentDeploymentJob')
    config.managedComponents.entrySet().each {
      if (it.value) {
        try {
          def component = componentApi.getComponent(it.key)
          if (component && !component.currentVersion && component.latestVersion) {
            log.debug("Deploying ${it.key}:${component.latestVersion}")
            componentApi.applyComponent(it.key, component.latestVersion)
          }
        } catch (ApiException ex) {
          log.debug(ex.toString())
        }
      }
    }
  }
}
