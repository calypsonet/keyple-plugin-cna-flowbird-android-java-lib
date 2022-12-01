/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.plugin.flowbird.contactless

enum class FlowbirdSupportContactlessProtocols constructor(val key: String) {
  ALL("ALL"),
  A("A"),
  B("B");

  companion object {
    fun findEnumByKey(key: String): FlowbirdSupportContactlessProtocols {
      for (value in values()) {
        if (value.key == key) {
          return value
        }
      }
      throw IllegalStateException("FlowbirdSupportContactlessProtocols '$key' is not defined")
    }
  }
}
