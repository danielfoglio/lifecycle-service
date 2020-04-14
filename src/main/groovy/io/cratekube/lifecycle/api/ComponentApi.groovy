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
   * Retrieves the CrateKube component specified by name. Returns null if it does not exist.
   *
   * @param name {@code non-empty} component name
   * @return the component
   */
  Component getComponent(String name)

  /**
   * Applies a specific CrateKube component version configuration yaml.
   * <p>
   * Notes: We need to first verify the version exists in github,
   * then retrieve from github, fail if not present, and use the k8s yaml 'template' to apply version.
   *
   * @param name {@code non-empty} component name
   * @param version {@code non-empty} component version
   * @throws FailedException
   * @throws NotFoundException
   */
  void applyComponent(String name, String version) throws FailedException, NotFoundException
}
