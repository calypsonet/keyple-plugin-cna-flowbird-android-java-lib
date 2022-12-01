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
package org.calypsonet.keyple.plugin.flowbird

import org.calypsonet.keyple.plugin.flowbird.reader.FlowbirdReader

class FlowbirdUiManager {

  companion object {
    fun displayHuntingNone() {
      FlowbirdReader.getInstance().displayHuntingNone()
    }

    fun displayWaiting() {
      FlowbirdReader.getInstance().displayWaiting()
    }

    fun displayResultSuccess() {
      FlowbirdReader.getInstance().displayResultSuccess()
    }

    fun displayResultFailed() {
      FlowbirdReader.getInstance().displayResultFailed()
    }
  }
}
