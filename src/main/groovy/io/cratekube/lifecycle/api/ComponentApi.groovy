package io.cratekube.lifecycle.api

import io.cratekube.lifecycle.api.exception.FailedException
import io.cratekube.lifecycle.api.exception.NotFoundException
import io.cratekube.lifecycle.model.Component

/**
 * Manages CrateKube component actions.
 * <p>
 *   It is expected to not need background execution for kubectl commands.
 */
interface ComponentApi {
  /**
   * Retrieves the CrateKube component specified by name.
   * Returns null if component is not deployed and does not have released version in GitHub.
   *
   * @param name {@code non-empty} component name
   * @return the component
   */
  Component getComponent(String name)

  /**
   * Applies a specific CrateKube component version configuration yaml.
   *
   * @param name {@code non-empty} component name
   * @param version {@code non-empty} component version
   * @throws FailedException
   * @throws NotFoundException
   */
  void applyComponent(String name, String version) throws FailedException, NotFoundException
}
