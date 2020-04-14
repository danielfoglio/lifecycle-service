package io.cratekube.lifecycle.api

import io.cratekube.lifecycle.api.exception.FailedException

/**
 * Base interface for kubectl operations.
 */
interface KubectlApi {
  /**
   * Applies the specified yaml.
   *
   * @param yaml {@code non-empty} configuration to apply
   *
   * @throws FailedException
   */
  void apply(String yaml) throws FailedException

  /**
   * Retrieves kubernetes resources.
   *
   * @param arguments {@code non-empty} string argument list
   * @return the json formatted output or null if no resource exists
   *
   * @throws FailedException
   */
  String get(String arguments) throws FailedException
}
