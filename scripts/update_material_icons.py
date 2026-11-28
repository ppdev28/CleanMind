#!/usr/bin/env python3
"""Generate the app's Material Symbols Outlined (500 / 0 / 24 / unfilled).

Usage: python update_material_icons.py font.ttf font.codepoints
Requires fonttools. Original font: google/material-design-icons (Apache-2.0).
"""

import json
import re
import sys
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

ROOT = Path(__file__).resolve().parents[1]
CLASSIC = {
    "FountainPen": "stylus_fountain_pen",
    "Mic": "mic",
    "AttachFile": "attach_file",
    "Link": "link",
    "Add": "add",
    "AddBox": "add_box",
    "ArrowBack": "arrow_back",
    "Archive": "archive",
    "Bell": "notifications",
    "Check": "check",
    "CheckBox": "check_box",
    "CheckCircle": "check_circle",
    "Clock": "schedule",
    "Close": "close",
    "CenterFocusStrong": "center_focus_strong",
    "ContentCopy": "content_copy",
    "ContentPaste": "content_paste",
    "Delete": "delete",
    "FileDownload": "download",
    "FolderOpen": "folder_open",
    "CreateNewFolder": "create_new_folder",
    "GridView": "grid_view",
    "Home": "home",
    "Image": "image",
    "InsertDriveFile": "draft",
    "List": "format_list_bulleted",
    "MoreHoriz": "more_horiz",
    "MoreVert": "more_vert",
    "Notes": "notes",
    "Notifications": "notifications",
    "PhotoCamera": "photo_camera",
    "PictureAsPdf": "picture_as_pdf",
    "PushPin": "keep",
    "Search": "search",
    "Settings": "settings",
    "Share": "share",
    "StickyNote2": "sticky_note_2",
    "StrikethroughS": "strikethrough_s",
    "TextFormat": "text_format",
    "ViewList": "view_list",
    "Arrow": "chevron_right",
    "Undo": "undo",
    "Redo": "redo",
    "TaskAlt": "task_alt",
    "EditNote": "edit_note",
    "Alarm": "alarm",
    "Event": "event",
    "Tune": "tune",
}
for name in (
    "FormatAlignCenter",
    "FormatAlignLeft",
    "FormatAlignRight",
    "FormatBold",
    "FormatClear",
    "FormatIndentDecrease",
    "FormatIndentIncrease",
    "FormatItalic",
    "FormatLineSpacing",
    "FormatListBulleted",
    "FormatListNumbered",
    "FormatUnderlined",
):
    CLASSIC[name] = re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()
font = instantiateVariableFont(
    TTFont(sys.argv[1]), {"FILL": 0, "GRAD": 0, "opsz": 24, "wght": 500}
)
codepoints = {
    name: int(value, 16)
    for name, value in (
        line.split() for line in Path(sys.argv[2]).read_text().splitlines()
    )
}
glyphs = font.getGlyphSet()
scale = 24 / font["head"].unitsPerEm
paths = {}
for name, symbol in sorted(CLASSIC.items()):
    glyph = font.getBestCmap()[codepoints[symbol]]
    pen = SVGPathPen(glyphs)
    glyphs[glyph].draw(TransformPen(pen, (scale, 0, 0, -scale, 0, 24)))
    paths[name] = pen.getCommands()
output = """package com.example.cleannotes.presentation.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Material Symbols Outlined: weight 500, grade 0, optical size 24, fill 0. */
object MindIcons {
"""
for name, path in paths.items():
    mirror = name in (
        "ArrowBack",
        "Arrow",
        "Undo",
        "Redo",
        "FormatIndentIncrease",
        "FormatIndentDecrease",
        "FormatAlignLeft",
        "FormatAlignRight",
        "ViewList",
        "FormatListBulleted",
        "List",
        "Notes",
        "StickyNote2",
        "InsertDriveFile",
    )
    output += f'    val {name}: ImageVector by lazy {{ symbol("{name}", {json.dumps(path)}, {str(mirror).lower()}) }}\n'
output += """
    private fun symbol(name: String, path: String, mirror: Boolean): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = mirror)
            .addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
            .build()
}
"""
(
    ROOT
    / "app/src/main/java/com/example/cleannotes/presentation/ui/design/MindIcons.kt"
).write_text(output)
notification = f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFFFF" android:pathData="{paths["Notifications"]}"/></vector>\n'
(ROOT / "app/src/main/res/drawable/ic_notification.xml").write_text(notification)
print(f"Generated {len(paths)} symbols")
