package com.example.galleryapp

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.galleryapp.network.ApiClient
import com.example.galleryapp.network.FileInfo
import com.example.galleryapp.network.GalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: GalleryViewModel by viewModels {
        GalleryViewModel.Factory(GalleryRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GalleryAppScreen(
                    viewModel = viewModel,
                    createImageUri = { createImageUri() }
                )
            }
        }
    }

    private fun createImageUri(): Uri? {
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GalleryComposeApp")
            }
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }
}

private enum class PendingAction {
    NONE, CAPTURE, PICK
}

private enum class MainTab(val title: String) {
    ALBUM("在线相册"),
    UPLOAD("上传图片"),
    SETTINGS("配置")
}

@Composable
private fun GalleryAppScreen(
    viewModel: GalleryViewModel,
    createImageUri: () -> Uri?
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(MainTab.ALBUM) }
    var pendingAction by remember { mutableStateOf(PendingAction.NONE) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            viewModel.selectImage(pendingCameraUri)
        } else {
            viewModel.showMessage("拍照已取消")
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.selectImage(uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        if (!granted) {
            viewModel.showMessage("权限未全部授予")
            pendingAction = PendingAction.NONE
            return@rememberLauncherForActivityResult
        }

        when (pendingAction) {
            PendingAction.CAPTURE -> {
                val uri = createImageUri()
                if (uri == null) {
                    viewModel.showMessage("创建拍照位置失败")
                } else {
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                }
            }

            PendingAction.PICK -> {
                pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            PendingAction.NONE -> Unit
        }
        pendingAction = PendingAction.NONE
    }

    fun launchWithPermission(action: PendingAction) {
        val permissions = requiredPermissions()
        if (hasPermissions(context, permissions)) {
            when (action) {
                PendingAction.CAPTURE -> {
                    val uri = createImageUri()
                    if (uri == null) {
                        viewModel.showMessage("创建拍照位置失败")
                    } else {
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    }
                }

                PendingAction.PICK -> {
                    pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }

                PendingAction.NONE -> Unit
            }
        } else {
            pendingAction = action
            permissionLauncher.launch(permissions)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == MainTab.ALBUM,
                    onClick = { selectedTab = MainTab.ALBUM },
                    icon = { androidx.compose.material3.Icon(Icons.Rounded.Cloud, contentDescription = "album") },
                    label = { Text(MainTab.ALBUM.title) }
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.UPLOAD,
                    onClick = { selectedTab = MainTab.UPLOAD },
                    icon = { androidx.compose.material3.Icon(Icons.Rounded.CloudUpload, contentDescription = "upload") },
                    label = { Text(MainTab.UPLOAD.title) }
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = { selectedTab = MainTab.SETTINGS },
                    icon = { androidx.compose.material3.Icon(Icons.Rounded.Settings, contentDescription = "settings") },
                    label = { Text(MainTab.SETTINGS.title) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Gallery Client", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(uiState.message, color = Color(0xFF455A64))
                }
            }

            when (selectedTab) {
                MainTab.ALBUM -> AlbumTab(
                    uiState = uiState,
                    onRefresh = { viewModel.loadImages() }
                )

                MainTab.UPLOAD -> UploadTab(
                    uiState = uiState,
                    onCapture = { launchWithPermission(PendingAction.CAPTURE) },
                    onPick = { launchWithPermission(PendingAction.PICK) },
                    onUpload = { info -> viewModel.uploadSelectedImage(context, info) }
                )

                MainTab.SETTINGS -> SettingsTab(
                    uiState = uiState,
                    onApply = { input -> viewModel.updateBackendBaseUrl(input) },
                    onRefresh = { viewModel.loadImages() }
                )
            }
        }
    }
}

@Composable
private fun AlbumTab(uiState: GalleryUiState, onRefresh: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(modifier = Modifier.weight(1f), onClick = onRefresh) {
            Text("刷新在线相册")
        }
        if (uiState.loading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp))
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(uiState.images, key = { it.fileName ?: it.hashCode() }) { item ->
            OnlineImageCard(item = item, baseUrl = uiState.backendBaseUrl)
        }
    }
}

@Composable
private fun UploadTab(
    uiState: GalleryUiState,
    onCapture: () -> Unit,
    onPick: () -> Unit,
    onUpload: (String) -> Unit
) {
    var uploadInfo by remember { mutableStateOf("Uploaded by Compose client") }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(modifier = Modifier.weight(1f), onClick = onCapture) {
            Text("拍照")
        }
        Button(modifier = Modifier.weight(1f), onClick = onPick) {
            Text("相册选图")
        }
    }

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = uploadInfo,
        onValueChange = { uploadInfo = it },
        label = { Text("上传 info 字段") }
    )

    Button(modifier = Modifier.fillMaxWidth(), onClick = { onUpload(uploadInfo) }) {
        Text("上传到服务器")
    }

    Text("当前选中图片预览")
    if (uiState.selectedUri != null) {
        AsyncImage(
            model = uiState.selectedUri,
            contentDescription = "selected image",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFFE8E8E8)),
            contentScale = ContentScale.Crop
        )
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            Text("还没有选中图片", modifier = Modifier.padding(20.dp), color = Color.Gray)
        }
    }
}

@Composable
private fun SettingsTab(
    uiState: GalleryUiState,
    onApply: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var backendInput by remember(uiState.backendBaseUrl) { mutableStateOf(uiState.backendBaseUrl) }

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = backendInput,
        onValueChange = { backendInput = it },
        label = { Text("后端地址") },
        placeholder = { Text("http://43a7939f.r40.cpolar.top/") }
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(modifier = Modifier.weight(1f), onClick = { onApply(backendInput) }) {
            Text("应用地址")
        }
        Button(modifier = Modifier.weight(1f), onClick = onRefresh) {
            Text("测试刷新")
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("当前生效地址", fontWeight = FontWeight.Bold)
            Text(uiState.backendBaseUrl)
            Text("提示：填写 host:port 也可以，应用时会自动补全 http:// 和 /")
        }
    }
}

@Composable
private fun OnlineImageCard(item: FileInfo, baseUrl: String) {
    val fileName = item.fileName ?: ""
    val url = "${baseUrl}file/${Uri.encode(fileName)}"
    Card {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AsyncImage(
                model = url,
                contentDescription = fileName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFFE0E0E0)),
                contentScale = ContentScale.Crop
            )
            Text(fileName, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            if (!item.info.isNullOrBlank()) {
                Text(item.info, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 2)
            }
        }
    }
}

private fun requiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
    return permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
}

data class GalleryUiState(
    val selectedUri: Uri? = null,
    val images: List<FileInfo> = emptyList(),
    val loading: Boolean = false,
    val message: String = "准备就绪",
    val backendBaseUrl: String = ApiClient.DEFAULT_BASE_URL
)

class GalleryViewModel(private val repository: GalleryRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(
        GalleryUiState(backendBaseUrl = repository.getBaseUrl())
    )
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadImages()
    }

    fun updateBackendBaseUrl(newValue: String) {
        repository.setBaseUrl(newValue)
        val normalized = repository.getBaseUrl()
        _uiState.value = _uiState.value.copy(
            backendBaseUrl = normalized,
            message = "后端地址已更新: $normalized"
        )
        loadImages()
    }

    fun selectImage(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedUri = uri, message = "已选择图片")
    }

    fun showMessage(msg: String) {
        _uiState.value = _uiState.value.copy(message = msg)
    }

    fun loadImages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val result = repository.getAllImages()
            _uiState.value = if (result.isSuccess) {
                val data = result.getOrNull().orEmpty()
                _uiState.value.copy(loading = false, images = data, message = "已加载 ${data.size} 张图片")
            } else {
                _uiState.value.copy(loading = false, message = "加载失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun uploadSelectedImage(context: Context, info: String) {
        val selected = _uiState.value.selectedUri
        if (selected == null) {
            showMessage("请先拍照或选图")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val result = repository.uploadImage(context, selected, info)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    message = "上传成功: ${result.getOrNull()?.savedFileName}"
                )
                loadImages()
            } else {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    message = "上传失败: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    class Factory(private val repository: GalleryRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(repository) as T
        }
    }
}
