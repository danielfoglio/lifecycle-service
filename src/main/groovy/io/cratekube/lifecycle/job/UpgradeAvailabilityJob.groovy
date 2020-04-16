package io.cratekube.lifecycle.job

import de.spinscale.dropwizard.jobs.Job
import de.spinscale.dropwizard.jobs.annotations.Every
import groovy.util.logging.Slf4j
import io.cratekube.lifecycle.AppConfig
import io.cratekube.lifecycle.api.ComponentApi
import io.cratekube.lifecycle.model.Component
import io.cratekube.lifecycle.modules.annotation.ComponentCache
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import javax.inject.Inject

import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.Assertive.require

/**
 * Responsible for polling github and kubernetes to be able to expose component upgrade availability.
 */
@Slf4j
@Every // see https://github.com/dropwizard-jobs/dropwizard-jobs#configuring-jobs-in-the-dropwizard-config-file
class UpgradeAvailabilityJob extends Job {
  Map<String, Component> componentCache
  ComponentApi componentApi
  AppConfig config

  @Inject
  UpgradeAvailabilityJob(@ComponentCache Map<String, Component> componentCache, ComponentApi componentApi, AppConfig config) {
    this.componentCache = require componentCache, notNullValue()
    this.componentApi = require componentApi, notNullValue()
    this.config = require config, notNullValue()
  }

  /**
   * Updates a global cache with populated Component.
   * <p>
   * Component current version will come from a kubectl call.
   * If component is not deployed only latest version is set in cache.
   * <p>
   * Every time the service starts this job is executed and the cache is populated.
   * The job repeats on a configurable interval {@code JOB_EXECUTION_FREQUENCY} in app.yml.
   *
   * {@inheritDoc}
   */
  @Override
  void doJob(JobExecutionContext context) throws JobExecutionException {
    log.debug('Executing UpgradeAvailabilityJob')
    config.managedComponents.keySet().each {
      componentCache[it] = componentApi.getComponent(it)
    }
  }
}
