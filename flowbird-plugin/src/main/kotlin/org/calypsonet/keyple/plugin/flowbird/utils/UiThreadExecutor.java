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
package org.calypsonet.keyple.plugin.flowbird.utils;

import android.os.Handler;
import android.os.Looper;

/**
 * Helper class to execute runnable in UI Thread Useful to call AsyncTask from background (or
 * unknown) thread.
 *
 * <p>
 */
public class UiThreadExecutor {
  private static final Handler sUiHandler;
  private static final Thread sUiThread;

  static {
    Looper uiLooper = Looper.getMainLooper();
    sUiThread = uiLooper.getThread();
    sUiHandler = new Handler(uiLooper);
  }

  public UiThreadExecutor(Runnable runnable) {
    if (Thread.currentThread() == sUiThread) {
      runnable.run();
    } else {
      sUiHandler.post(runnable);
    }
  }

  public UiThreadExecutor(Runnable runnable, int latency) {
    sUiHandler.postDelayed(runnable, latency);
  }
}
