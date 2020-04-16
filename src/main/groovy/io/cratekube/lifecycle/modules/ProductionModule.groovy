package io.cratekube.lifecycle.modules

import com.google.inject.Provides
import io.cratekube.lifecycle.AppConfig
import io.cratekube.lifecycle.GitHubConfig
import io.cratekube.lifecycle.api.ComponentApi
import io.cratekube.lifecycle.api.GitHubApi
import io.cratekube.lifecycle.api.KubectlApi
import io.cratekube.lifecycle.api.ProcessExecutor
import io.cratekube.lifecycle.model.Component
import io.cratekube.lifecycle.modules.annotation.ComponentCache
import io.cratekube.lifecycle.service.ComponentService
import io.cratekube.lifecycle.service.GitHubService
import io.cratekube.lifecycle.service.KubectlCommand
import io.cratekube.lifecycle.service.KubectlService
import io.dropwizard.client.JerseyClientBuilder
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule

import javax.inject.Singleton
import javax.ws.rs.client.Client

/**
 * Default module to be used when running this application.
 */
class ProductionModule extends DropwizardAwareModule<AppConfig> {
  @Override
  protected void configure() {
    bind ComponentApi to ComponentService
    bind ProcessExecutor to KubectlCommand
    bind KubectlApi to KubectlService
    bind GitHubApi to GitHubService
    bind GitHubConfig toInstance configuration().github
  }

  /**
   * @return the cache map for component version and latest version data structure - key will be dropwizard config name
   */
  @Provides
  @ComponentCache
  @Singleton
  Map<String, Component> componentVersionCache() {
    return [:]
  }

  @Provides
  @Singleton
  Client clientProvider() {
    return new JerseyClientBuilder(environment()).using(configuration().jerseyClient).build('external-client')
  }
}
