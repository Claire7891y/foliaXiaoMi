package com.folia.xiaomi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.folia.xiaomi.data.model.Song
import com.folia.xiaomi.data.repository.LocalMusicRepository
import com.folia.xiaomi.data.repository.OnlineMusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs.asStateFlow()

    private val _onlineSongs = MutableStateFlow<List<Song>>(emptyList())
    val onlineSongs: StateFlow<List<Song>> = _onlineSongs.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _onlineReady = MutableStateFlow(false)
    /** 是否已配置在线音源 API */
    val onlineReady: StateFlow<Boolean> = _onlineReady.asStateFlow()

    fun scanLocal() {
        viewModelScope.launch {
            _localSongs.value = LocalMusicRepository.scan(getApplication())
        }
    }

    fun search(keyword: String) {
        viewModelScope.launch {
            _searching.value = true
            _onlineSongs.value = OnlineMusicRepository.search(keyword)
            _searching.value = false
        }
    }

    fun refreshOnlineStatus() {
        _onlineReady.value = OnlineMusicRepository.baseUrl.isNotBlank()
    }

    fun setOnlineBaseUrl(url: String) {
        OnlineMusicRepository.baseUrl = url.trim()
        refreshOnlineStatus()
    }
}
