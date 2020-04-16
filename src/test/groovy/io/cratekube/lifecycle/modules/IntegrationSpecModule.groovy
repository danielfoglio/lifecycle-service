package io.cratekube.lifecycle.modules

import io.cratekube.lifecycle.AppConfig
import io.cratekube.lifecycle.api.ComponentApi
import io.cratekube.lifecycle.api.GitHubApi
import io.cratekube.lifecycle.api.KubectlApi
import io.cratekube.lifecycle.api.ProcessExecutor
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule
import spock.mock.DetachedMockFactory

/**
 * Guice module used for integration specs.
 */
class IntegrationSpecModule extends DropwizardAwareModule<AppConfig> {
  DetachedMockFactory mockFactory = new DetachedMockFactory()

  @Override
  protected void configure() {
    bind ComponentApi toInstance mock(ComponentApi)
  }

  def <T> T mock(Class<T> type) {
    return mockFactory.Mock(type)
  }
}
