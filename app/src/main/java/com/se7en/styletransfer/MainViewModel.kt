package com.se7en.styletransfer

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(val styleTransferModel: StyleTransferModel): ViewModel() {

    val output = MutableLiveData<Bitmap>()
    fun execute(content: Bitmap, style: Bitmap) = viewModelScope.launch(Dispatchers.IO) {
        output.postValue(styleTransferModel.execute(content, style))
    }
}
