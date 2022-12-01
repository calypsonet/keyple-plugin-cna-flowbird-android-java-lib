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
package org.calypsonet.keyple.plugin.flowbird.reader

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Tag(
    val customerType: String?,
    val cardId: Long?,
    val peripheral: String?,
    val atr: String?,
    val readableData: String?
) : Parcelable

sealed class HuntEventResult<T>

class TagResultSuccess<T>(val data: T) : HuntEventResult<T>()

class TagResultError<T>(val error: Throwable) : HuntEventResult<T>()

class CardRemoved<T> : HuntEventResult<T>()
