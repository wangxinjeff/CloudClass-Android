package io.agora.uikit.impl.tool

import io.agora.uikit.R

class AgoraUIToolItem(
        val type: AgoraUIToolItemType,
        val iconRes: Int,
        val hasPopup: Boolean)

/**
 * Item types existing in the tool list
 */
enum class AgoraUIToolItemType {
    Select, Pen, Color, Text, Eraser, Roster;

    companion object {
        fun typeByOrdinal(ordinal: Int): AgoraUIToolItemType {
            return when (ordinal) {
                Select.ordinal -> Select
                Pen.ordinal -> Pen
                Color.ordinal -> Color
                Text.ordinal -> Text
                Eraser.ordinal -> Eraser
                Roster.ordinal -> Roster
                else -> Select
            }
        }
    }
}

/**
 * Whiteboard appliance types
 */
enum class AgoraUIApplianceType {
    Select, Pen, Rect, Circle, Line, Eraser, Text;
}

enum class AgoraUIToolType {
    All, Whiteboard
}

object AgoraUIToolItemList {
    val emptyList = mutableListOf<AgoraUIToolItem>()

    val whiteboardList = mutableListOf(
            AgoraUIToolItem(AgoraUIToolItemType.Select, R.drawable.agora_tool_icon_select, false),
            AgoraUIToolItem(AgoraUIToolItemType.Pen, R.drawable.agora_tool_icon_pen, true),
            AgoraUIToolItem(AgoraUIToolItemType.Color, R.drawable.agora_tool_icon_color, true),
            AgoraUIToolItem(AgoraUIToolItemType.Text, R.drawable.agora_tool_icon_text, true),
            AgoraUIToolItem(AgoraUIToolItemType.Eraser, R.drawable.agora_tool_icon_eraser, false))

    val AllItemList = mutableListOf(
            AgoraUIToolItem(AgoraUIToolItemType.Select, R.drawable.agora_tool_icon_select, false),
            AgoraUIToolItem(AgoraUIToolItemType.Pen, R.drawable.agora_tool_icon_pen, true),
            AgoraUIToolItem(AgoraUIToolItemType.Color, R.drawable.agora_tool_icon_color, true),
            AgoraUIToolItem(AgoraUIToolItemType.Text, R.drawable.agora_tool_icon_text, true),
            AgoraUIToolItem(AgoraUIToolItemType.Eraser, R.drawable.agora_tool_icon_eraser, false),
            AgoraUIToolItem(AgoraUIToolItemType.Roster, R.drawable.agora_tool_icon_userlist, true))

    val RosterOnlyList = mutableListOf(
            AgoraUIToolItem(AgoraUIToolItemType.Roster, R.drawable.agora_tool_icon_userlist, true))
}