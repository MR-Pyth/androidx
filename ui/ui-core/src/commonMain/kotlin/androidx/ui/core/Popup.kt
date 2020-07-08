/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.core

import androidx.compose.Composable
import androidx.compose.Immutable
import androidx.compose.Providers
import androidx.compose.ambientOf
import androidx.compose.remember
import androidx.ui.unit.IntBounds
import androidx.ui.unit.IntOffset
import androidx.ui.unit.IntSize
import androidx.ui.unit.height
import androidx.ui.unit.width

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned relative to its parent, using the [alignment] and [offset].
 * The popup is visible as long as it is part of the composition hierarchy.
 *
 * @sample androidx.ui.core.samples.PopupSample
 *
 * @param alignment The alignment relative to the parent.
 * @param offset An offset from the original aligned position of the popup. Offset respects the
 * Ltr/Rtl context, thus in Ltr it will be added to the original aligned position and in Rtl it
 * will be subtracted from it.
 * @param isFocusable Indicates if the popup can grab the focus.
 * @param onDismissRequest Executes when the popup tries to dismiss itself. This happens when
 * the popup is focusable and the user clicks outside.
 * @param children The content to be displayed inside the popup.
 */
@Composable
fun Popup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    isFocusable: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    children: @Composable () -> Unit
) {
    val popupPositioner = remember(alignment, offset) {
        AlignmentOffsetPositionProvider(alignment, offset)
    }

    Popup(
        popupPositionProvider = popupPositioner,
        isFocusable = isFocusable,
        onDismissRequest = onDismissRequest,
        children = children
    )
}

/**
 * Opens a popup with the given content.
 *
 * The dropdown popup is positioned below its parent, using the [dropDownAlignment] and [offset].
 * The dropdown popup is visible as long as it is part of the composition hierarchy.
 *
 * @sample androidx.ui.core.samples.DropdownPopupSample
 *
 * @param dropDownAlignment The start or end alignment below the parent.
 * @param offset An offset from the original aligned position of the popup.
 * @param isFocusable Indicates if the popup can grab the focus.
 * @param onDismissRequest Executes when the popup tries to dismiss itself. This happens when
 * the popup is focusable and the user clicks outside.
 * @param children The content to be displayed inside the popup.
 */
@Composable
fun DropdownPopup(
    dropDownAlignment: DropDownAlignment = DropDownAlignment.Start,
    offset: IntOffset = IntOffset(0, 0),
    isFocusable: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    children: @Composable () -> Unit
) {
    val popupPositioner = remember(dropDownAlignment, offset) {
        DropdownPositionProvider(dropDownAlignment, offset)
    }

    Popup(
        popupPositionProvider = popupPositioner,
        isFocusable = isFocusable,
        onDismissRequest = onDismissRequest,
        children = children
    )
}

// TODO(b/142431825): This is a hack to work around Popups not using Semantics for test tags
//  We should either remove it, or come up with an abstracted general solution that isn't specific
//  to Popup
internal val PopupTestTagAmbient = ambientOf { "DEFAULT_TEST_TAG" }

@Composable
internal fun PopupTestTag(tag: String, children: @Composable () -> Unit) {
    Providers(PopupTestTagAmbient provides tag, children = children)
}

internal class PopupPositionProperties {
    var parentBounds = IntBounds(0, 0, 0, 0)
    var childrenSize = IntSize.Zero
    var parentLayoutDirection: LayoutDirection = LayoutDirection.Ltr
}

@Composable
fun Popup(
    popupPositionProvider: PopupPositionProvider,
    isFocusable: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    children: @Composable () -> Unit
) = ActualPopup(popupPositionProvider, isFocusable, onDismissRequest, children)

@Composable
internal expect fun ActualPopup(
    popupPositionProvider: PopupPositionProvider,
    isFocusable: Boolean,
    onDismissRequest: (() -> Unit)?,
    children: @Composable () -> Unit
)

/**
 * Calculates the position of a [Popup] on screen.
 */
@Immutable
interface PopupPositionProvider {
    /**
     * Calculates the position of a [Popup] on screen.
     *
     * @param parentLayoutBounds The bounds of the parent wrapper layout on screen.
     * @param layoutDirection The layout direction of the parent wrapper layout.
     * @param popupSize The size of the popup to be positioned.
     */
    fun calculatePosition(
        parentLayoutBounds: IntBounds,
        layoutDirection: LayoutDirection,
        popupSize: IntSize
    ): IntOffset
}

/**
 * The [DropdownPopup] is aligned below its parent relative to its left or right corner.
 * [DropDownAlignment] is used to specify how should [DropdownPopup] be aligned.
 */
enum class DropDownAlignment {
    Start,
    End
}

internal class AlignmentOffsetPositionProvider(
    val alignment: Alignment,
    val offset: IntOffset
) : PopupPositionProvider {
    override fun calculatePosition(
        parentLayoutBounds: IntBounds,
        layoutDirection: LayoutDirection,
        popupSize: IntSize
    ): IntOffset {
        // TODO: Decide which is the best way to round to result without reimplementing Alignment.align
        var popupGlobalPosition = IntOffset(0, 0)

        // Get the aligned point inside the parent
        val parentAlignmentPoint = alignment.align(
            IntSize(parentLayoutBounds.width, parentLayoutBounds.height),
            layoutDirection
        )
        // Get the aligned point inside the child
        val relativePopupPos = alignment.align(
            IntSize(popupSize.width, popupSize.height),
            layoutDirection
        )

        // Add the global position of the parent
        popupGlobalPosition += IntOffset(parentLayoutBounds.left, parentLayoutBounds.top)

        // Add the distance between the parent's top left corner and the alignment point
        popupGlobalPosition += parentAlignmentPoint

        // Subtract the distance between the children's top left corner and the alignment point
        popupGlobalPosition -= IntOffset(relativePopupPos.x, relativePopupPos.y)

        // Add the user offset
        val resolvedOffset = IntOffset(
            offset.x * (if (layoutDirection == LayoutDirection.Ltr) 1 else -1),
            offset.y
        )
        popupGlobalPosition += resolvedOffset

        return popupGlobalPosition
    }
}

internal class DropdownPositionProvider(
    val dropDownAlignment: DropDownAlignment,
    val offset: IntOffset
) : PopupPositionProvider {
    override fun calculatePosition(
        parentLayoutBounds: IntBounds,
        layoutDirection: LayoutDirection,
        popupSize: IntSize
    ): IntOffset {
        var popupGlobalPosition = IntOffset(0, 0)

        // Add the global position of the parent
        popupGlobalPosition += IntOffset(parentLayoutBounds.left, parentLayoutBounds.top)

        /*
        * In LTR context aligns popup's left edge with the parent's left edge for Start alignment
        * and parent's right edge for End alignment.
        * In RTL context aligns popup's right edge with the parent's right edge for Start alignment
        * and parent's left edge for End alignment.
        */
        val alignmentPositionX =
            if (dropDownAlignment == DropDownAlignment.Start) {
                if (layoutDirection == LayoutDirection.Ltr) {
                    0
                } else {
                    parentLayoutBounds.width - popupSize.width
                }
            } else {
                if (layoutDirection == LayoutDirection.Ltr) {
                    parentLayoutBounds.width
                } else {
                    -popupSize.width
                }
            }

        // The popup's position relative to the parent's top left corner
        val dropdownAlignmentPosition = IntOffset(alignmentPositionX, parentLayoutBounds.height)

        popupGlobalPosition += dropdownAlignmentPosition

        // Add the user offset
        val resolvedOffset = IntOffset(
            offset.x * (if (layoutDirection == LayoutDirection.Ltr) 1 else -1),
            offset.y
        )
        popupGlobalPosition += resolvedOffset

        return popupGlobalPosition
    }
}