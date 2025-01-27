package com.machiav3lli.backup.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.imageLoader
import com.machiav3lli.backup.R
import com.machiav3lli.backup.data.dbs.entity.Schedule
import com.machiav3lli.backup.data.entity.ChipItem
import com.machiav3lli.backup.data.entity.InfoChipItem
import com.machiav3lli.backup.data.entity.Log
import com.machiav3lli.backup.data.entity.Package
import com.machiav3lli.backup.data.entity.StorageFile
import com.machiav3lli.backup.ui.pages.pref_multilineInfoChips
import com.machiav3lli.backup.ui.pages.pref_singularBackupRestore

@Composable
fun HomePackageRecycler(
    modifier: Modifier = Modifier,
    productsList: List<Package>,
    selection: Set<String>,
    onLongClick: (Package) -> Unit = {},
    onClick: (Package) -> Unit = {},
) {
    val imageLoader = LocalContext.current.imageLoader

    InnerBackground(modifier) {
        VerticalItemList(
            list = productsList,
            itemKey = { it.packageName }
        ) {
            MainPackageItem(
                it,
                selection.contains(it.packageName),
                imageLoader,
                onLongClick,
                onClick
            )
        }
    }
}

@Composable
fun UpdatedPackageRecycler(
    modifier: Modifier = Modifier,
    productsList: List<Package>?,
    onClick: (Package) -> Unit = {},
) {
    HorizontalItemList(
        modifier = modifier,
        list = productsList,
        itemKey = { it.packageName }
    ) {
        UpdatedPackageItem(it, onClick)
    }
}

@Composable
fun BatchPackageRecycler(
    modifier: Modifier = Modifier,
    productsList: List<Package>?,
    restore: Boolean = false,
    apkBackupCheckedList: SnapshotStateMap<String, Int>,
    dataBackupCheckedList: SnapshotStateMap<String, Int>,
    onBackupApkClick: (String, Boolean, Int) -> Unit = { _: String, _: Boolean, _: Int -> },
    onBackupDataClick: (String, Boolean, Int) -> Unit = { _: String, _: Boolean, _: Int -> },
    onClick: (Package, Boolean, Boolean) -> Unit = { _: Package, _: Boolean, _: Boolean -> },
) {
    InnerBackground(modifier) {
        VerticalItemList(
            list = productsList,
            itemKey = { it.packageName }
        ) {
            val apkBackupChecked = remember(apkBackupCheckedList[it.packageName]) {
                mutableStateOf(apkBackupCheckedList[it.packageName])
            }
            val dataBackupChecked = remember(dataBackupCheckedList[it.packageName]) {
                mutableStateOf(dataBackupCheckedList[it.packageName])
            }

            if (restore && pref_singularBackupRestore.value) RestorePackageItem(
                it,
                apkBackupChecked,
                dataBackupChecked,
                onClick,
                onBackupApkClick,
                onBackupDataClick,
            )
            else BatchPackageItem(
                it,
                restore,
                apkBackupChecked.value == 0,
                dataBackupChecked.value == 0,
                onClick,
                onApkClick = { p, b ->
                    onBackupApkClick(p.packageName, b, 0)
                },
                onDataClick = { p, b ->
                    onBackupDataClick(p.packageName, b, 0)
                }
            )
        }
    }
}

@Composable
fun ScheduleRecycler(
    modifier: Modifier = Modifier,
    productsList: List<Schedule>?,
    onClick: (Schedule) -> Unit = {},
    onRun: (Schedule) -> Unit = {},
    onCheckChanged: (Schedule, Boolean) -> Unit = { _: Schedule, _: Boolean -> },
) {
    InnerBackground(modifier) {
        VerticalItemList(
            list = productsList
        ) {
            ScheduleItem(it, onClick, onRun, onCheckChanged)
        }
    }
}

@Composable
fun ExportedScheduleRecycler(
    modifier: Modifier = Modifier,
    productsList: List<Pair<Schedule, StorageFile>>?,
    onImport: (Schedule) -> Unit = {},
    onDelete: (StorageFile) -> Unit = {},
) {
    InnerBackground(modifier) {
        VerticalItemList(
            list = productsList
        ) {
            ExportedScheduleItem(it.first, onImport) { onDelete(it.second) }
        }
    }
}

@Composable
fun LogRecycler(
    modifier: Modifier = Modifier,
    productsList: List<Log>?,
    onShare: (Log) -> Unit = {},
    onDelete: (Log) -> Unit = {},
) {
    InnerBackground(modifier) {
        VerticalItemList(
            list = productsList
        ) {
            LogItem(it, onShare, onDelete)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InfoChipsBlock(
    modifier: Modifier = Modifier,
    list: List<InfoChipItem>,
) {
    if (pref_multilineInfoChips.value)
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            list.forEach { chip ->
                InfoChip(item = chip)
            }
        }
    else LazyRow(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        items(list) { chip ->
            InfoChip(item = chip)
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectableChipGroup(
    //TODO hg42 move to item/Icons.kt ?
    modifier: Modifier = Modifier,
    list: List<ChipItem>,
    selectedFlag: Int,
    onClick: (Int) -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        list.forEach { item ->
            SelectionChip(
                item = item,
                isSelected = item.flag == selectedFlag,
            ) {
                onClick(item.flag)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiSelectableChipGroup(
    //TODO hg42 move to item/Icons.kt ?
    modifier: Modifier = Modifier,
    list: List<ChipItem>,
    selectedFlags: Int,
    onClick: (Int, Int) -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        list.forEach { item ->
            SelectionChip(
                item = item,
                isSelected = item.flag and selectedFlags != 0,
            ) {
                onClick(selectedFlags xor item.flag, item.flag)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiSelectableChipGroup(
    modifier: Modifier = Modifier,
    list: Set<String>,
    selected: Set<String>,
    onClick: (Set<String>) -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        list.forEach { item ->
            SelectionChip(
                label = item,
                isSelected = item in selected,
            ) {
                onClick(if (item in selected) selected - item else selected + item)
            }
        }
    }
}

@Composable
fun <T : Any> VerticalItemList(
    modifier: Modifier = Modifier,
    list: List<T>?,
    itemKey: ((T) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("VerticalItemList"),
        contentAlignment = if (list.isNullOrEmpty()) Alignment.Center else Alignment.TopStart
    ) {
        when {
            list == null   -> Text(
                text = stringResource(id = R.string.loading_list),
                color = MaterialTheme.colorScheme.onSurface
            )

            list.isEmpty() -> Text(
                text = stringResource(id = R.string.empty_filtered_list),
                color = MaterialTheme.colorScheme.onSurface
            )

            else           -> {
                // TODO add scrollbars
                val state = rememberLazyListState()

                LazyColumn(
                    state = state,
                    modifier = modifier
                        .testTag("VerticalItemList.Column"),
                    verticalArrangement = Arrangement.Absolute.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    itemsIndexed(
                        items = list,
                        itemContent = { _: Int, it: T ->
                            itemContent(it)
                        },
                        key = { index: Int, it: T ->
                            itemKey?.invoke(it) ?: index
                        }
                    )
                    item {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)  // workaround for floating buttons hiding the elements
                            // unfortunately the sizes are private
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> HorizontalItemList(
    modifier: Modifier = Modifier,
    list: List<T>?,
    itemKey: ((T) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = if (list.isNullOrEmpty()) Alignment.Center else Alignment.CenterStart
    ) {
        when {
            list == null   -> Text(
                text = stringResource(id = R.string.loading_list),
                color = MaterialTheme.colorScheme.onSurface
            )

            list.isEmpty() -> Text(
                text = stringResource(id = R.string.empty_filtered_list),
                color = MaterialTheme.colorScheme.onSurface
            )

            else           -> {
                LazyRow(
                    horizontalArrangement = Arrangement.Absolute.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                ) {
                    items(items = list, key = itemKey, itemContent = itemContent)
                }
            }
        }
    }
}
