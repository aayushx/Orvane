package com.orvane.app.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orvane.app.data.db.FilterEntity
import com.orvane.app.data.db.FolderEntity
import com.orvane.app.data.db.ListEntity
import com.orvane.app.data.db.SmartList
import com.orvane.app.data.db.TagEntity
import com.orvane.app.data.repo.HabitRepository
import com.orvane.app.data.repo.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DrawerUiState(
    val lists: List<ListEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val filters: List<FilterEntity> = emptyList(),
    val todayCount: Int = 0,
    val habitsCount: Int = 0,
    val trashCount: Int = 0,
)

@HiltViewModel
class DrawerViewModel @Inject constructor(
    repo: TaskRepository,
    habitRepo: HabitRepository,
) : ViewModel() {

    private val listsAndFolders = combine(repo.observeLists(), repo.observeFolders()) { l, f -> l to f }
    private val tagsAndFilters = combine(repo.observeTags(), repo.observeFilters()) { t, f -> t to f }
    private val counts = combine(
        repo.observeTodayCount(),
        habitRepo.observeActive().map { it.size },
        repo.observeTrash().map { trash -> trash.count { it.parentId == null } },
    ) { today, habits, trash -> Triple(today, habits, trash) }

    val state: StateFlow<DrawerUiState> = combine(listsAndFolders, tagsAndFilters, counts) { lf, tf, c ->
        DrawerUiState(
            lists = lf.first,
            folders = lf.second,
            tags = tf.first,
            filters = tf.second,
            todayCount = c.first,
            habitsCount = c.second,
            trashCount = c.third,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), DrawerUiState())
}

@Composable
fun DrawerContent(
    currentKey: String,
    onOpenList: (String) -> Unit,
    vm: DrawerViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    ModalDrawerSheet {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("T", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(12.dp))
                Text("Orvane", style = MaterialTheme.typography.titleLarge)
            }
            HorizontalDivider()
            SmartRow("Today", SmartList.TODAY, Icons.Default.Today, currentKey, state.todayCount, onOpenList)
            SmartRow("Tomorrow", SmartList.TOMORROW, Icons.Default.DateRange, currentKey, null, onOpenList)
            SmartRow("Next 7 days", SmartList.NEXT_7_DAYS, Icons.Default.CalendarViewDay, currentKey, null, onOpenList)
            SmartRow("Overdue", SmartList.OVERDUE, Icons.Default.EventAvailable, currentKey, null, onOpenList)
            SmartRow("All tasks", SmartList.ALL, Icons.Default.AllInbox, currentKey, null, onOpenList)
            SmartRow("Inbox", SmartList.INBOX, Icons.Default.Inbox, currentKey, null, onOpenList)
            SmartRow("Completed", SmartList.COMPLETED, Icons.Default.DoneAll, currentKey, null, onOpenList)
            HorizontalDivider(Modifier.padding(vertical = 6.dp))

            if (state.folders.isNotEmpty()) {
                SectionLabel("Folders")
                for (folder in state.folders) {
                    DrawerRow(
                        key = "folder:${folder.id}",
                        title = folder.name,
                        icon = Icons.Default.Folder,
                        isSelected = currentKey == "folder:${folder.id}",
                        count = null,
                        leadingTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onOpenList,
                    )
                }
            }

            SectionLabel("Lists")
            for (list in state.lists) {
                DrawerRow(
                    key = "list:${list.id}",
                    title = list.name,
                    icon = null,
                    isSelected = currentKey == "list:${list.id}",
                    count = null,
                    leadingTint = Color(list.color),
                    onOpenList,
                )
            }

            if (state.filters.isNotEmpty()) {
                SectionLabel("Filters")
                for (filter in state.filters) {
                    DrawerRow(
                        key = "filter:${filter.id}",
                        title = filter.name,
                        icon = Icons.Default.FilterList,
                        isSelected = currentKey == "filter:${filter.id}",
                        count = null,
                        leadingTint = MaterialTheme.colorScheme.primary,
                        onOpenList,
                    )
                }
            }

            if (state.tags.isNotEmpty()) {
                SectionLabel("Tags")
                Column(Modifier.padding(start = 16.dp)) {
                    for (tag in state.tags.take(12)) {
                        Row(
                            Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tag.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            DrawerRow(
                key = SmartList.TRASH,
                title = "Trash",
                icon = Icons.Default.Delete,
                isSelected = currentKey == SmartList.TRASH,
                count = state.trashCount.takeIf { it > 0 },
                leadingTint = MaterialTheme.colorScheme.onSurfaceVariant,
                onOpenList,
            )
        }
    }
}

@Composable
private fun SmartRow(
    title: String,
    key: String,
    icon: ImageVector,
    currentKey: String,
    count: Int?,
    onOpenList: (String) -> Unit,
) {
    DrawerRow(key, title, icon, currentKey == key, count, MaterialTheme.colorScheme.primary, onOpenList)
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp),
    )
}

/** A single selectable drawer item; [icon] null means a colored list dot placeholder. */
@Composable
private fun DrawerRow(
    key: String,
    title: String,
    icon: ImageVector?,
    isSelected: Boolean,
    count: Int?,
    leadingTint: Color,
    onOpenList: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onOpenList(key) }
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else leadingTint,
            )
        } else {
            Box(
                Modifier
                    .size(14.dp)
                    .padding(2.dp)
                    .background(leadingTint, CircleShape),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
