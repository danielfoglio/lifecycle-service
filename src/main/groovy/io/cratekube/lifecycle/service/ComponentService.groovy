package io.cratekube.lifecycle.service

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import io.cratekube.lifecycle.AppConfig
import io.cratekube.lifecycle.api.ComponentApi
import io.cratekube.lifecycle.api.GitHubApi
import io.cratekube.lifecycle.api.KubectlApi
import io.cratekube.lifecycle.api.exception.FailedException
import io.cratekube.lifecycle.api.exception.NotFoundException
import io.cratekube.lifecycle.model.Component

import javax.inject.Inject

import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

@Slf4j
class ComponentService implements ComponentApi {
  KubectlApi kubectlApi
  GitHubApi gitHubApi
  ObjectMapper objectMapper

  @Inject
  ComponentService(KubectlApi kubectlApi, GitHubApi gitHubApi, ObjectMapper objectMapper) {
    this.kubectlApi = require kubectlApi, notNullValue()
    this.gitHubApi = require gitHubApi, notNullValue()
    this.objectMapper = require objectMapper, notNullValue()
  }

  Component getComponent(String name) {
    require name, notEmptyString()

    def currentVersion = null
    def config = null
    def stringResource = kubectlApi.getPodJsonByNameSelector(name)
    def jsonResource = objectMapper.readValue(stringResource, Map)
    if (jsonResource.items) {
      def image = jsonResource.items[0].spec.containers[0].image
      currentVersion = image.split(':')[1] as String
      config = stringResource
    }
    try {
      def latestVersion = gitHubApi.getLatestVersionFromAtomFeed(name)
      return new Component(name, config, currentVersion, latestVersion)
    } catch (Exception ex) {
      log.debug(ex.toString())
    }
    return null
  }

  @Override
  void applyComponent(String name, String version) throws FailedException, NotFoundException {
    require name, notEmptyString()
    require version, notEmptyString()

    def deployableComponent = gitHubApi.getDeployableComponent(name, version)
    kubectlApi.apply(deployableComponent)
  }
}
