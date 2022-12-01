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
package org.calypsonet.keyple.plugin.flowbird.example.util;

import android.os.Looper;
import androidx.annotation.NonNull;
import java.util.Timer;
import java.util.TimerTask;

public class BackgroundThreadExecutor {

  private static boolean isUiThread() {
    return Thread.currentThread() == Looper.getMainLooper().getThread();
  }

  public BackgroundThreadExecutor(@NonNull Runnable command) {
    if (!isUiThread()) {
      command.run();
    } else {
      new Thread(command).start();
    }
  }

  public BackgroundThreadExecutor(@NonNull final Runnable command, final int latency) {
    new Timer()
        .schedule(
            new TimerTask() {
              @Override
              public void run() {
                if (!isUiThread()) {
                  command.run();
                } else {
                  new Thread(command).start();
                }
              }
            },
            latency);
  }
}
