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
   * Retrieves kubernetes pod resource json.
   *
   * @param name {@code non-empty} selector to filter on: {@code name=}
   * @return the json formatted output
   *
   * @throws FailedException
   */
  String getPodJsonByNameSelector(String name) throws FailedException
}
