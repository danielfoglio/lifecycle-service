package io.cratekube.lifecycle.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.cratekube.lifecycle.AppConfig
import io.cratekube.lifecycle.api.ComponentApi
import io.cratekube.lifecycle.api.GitHubApi
import io.cratekube.lifecycle.api.KubectlApi
import io.cratekube.lifecycle.api.exception.FailedException
import io.cratekube.lifecycle.api.exception.NotFoundException
import io.cratekube.lifecycle.model.Component
import io.cratekube.lifecycle.modules.annotation.ComponentCache

import javax.inject.Inject

import static org.hamcrest.Matchers.notNullValue
import static org.valid4j.Assertive.require
import static org.valid4j.matchers.ArgumentMatchers.notEmptyString

class ComponentService implements ComponentApi {
  Map<String, Component> componentCache
  KubectlApi kubectlApi
  GitHubApi gitHubApi
  ObjectMapper objectMapper
  AppConfig config

  @Inject
  ComponentService(@ComponentCache Map<String, Component> componentCache, KubectlApi kubectlApi, GitHubApi gitHubApi, ObjectMapper objectMapper, AppConfig config) {
    this.componentCache = require componentCache, notNullValue()
    this.kubectlApi = require kubectlApi, notNullValue()
    this.gitHubApi = require gitHubApi, notNullValue()
    this.objectMapper = require objectMapper, notNullValue()
    this.config = require config, notNullValue()
  }

  Component getComponent(String name, boolean checkCache = true) {
    require name, notEmptyString()

    if (checkCache && componentCache[name]) {
      return componentCache[name]
    }

    def stringResource = kubectlApi.get(name)
    if (!stringResource) {
      return null
    }

    def jsonResource = objectMapper.readValue(stringResource, Map)
    def image = jsonResource.items[0].spec.containers[0].image
    def currentVersion = image.split(':')[1] as String
    def latestVersion = gitHubApi.getLatestVersionFromAtomFeed(config.managedComponents[name])
    def component = new Component(name, stringResource, currentVersion, latestVersion)
    return componentCache[name] = component
  }

  @Override
  void applyComponent(String name, String version) throws FailedException, NotFoundException {
    require name, notEmptyString()
    require version, notEmptyString()

    def deployableComponent = gitHubApi.getDeployableComponent(name, version)
    kubectlApi.apply(deployableComponent)
    // update cache with current component state
    getComponent(name, false)
  }
}
