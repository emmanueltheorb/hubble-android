package com.orb.hubble.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.launch

class FlowViewModel : ViewModel() {

    private val user = MutableStateFlow<User1?>(null)
    private val posts = MutableStateFlow(emptyList<Post1>())


    init {

    }

}

data class User1(
    val username: String? = null,
    val description: String? = null,
    val profilePicUrl: String? = null
)

data class Post1(
    val imageUrl: String? = null,
    val username: String? = null,
    val description: String? = null
)

data class ProfileState(
    val profilePicUrl: String? = null,
    val username: String? = null,
    val description: String? = null,
    val posts: List<Post1> = emptyList()
)

//class FlowViewModel : ViewModel() {
//
//    val countDownFlow = flow {
//        val startingValue = 5
//        var currentValue = startingValue
//        emit(startingValue)
//        while (currentValue > 0) {
//            delay(1000L)
//            currentValue--
//            emit(currentValue)
//        }
//    }
//
//    private val _stateFlow = MutableStateFlow(0)
//    val stateFlow = _stateFlow.asStateFlow()
//
//    private val _sharedFlow = MutableSharedFlow<Int>(replay = 5)
//    val sharedFlow = _sharedFlow.asSharedFlow()
//
//    init {
//        squareNumber(3)
//        viewModelScope.launch {
//            sharedFlow.collect {
//                delay(2000)
//                println("FIRST FLOW: The received number is $it")
//            }
//        }
//        viewModelScope.launch {
//            sharedFlow.collect {
//                delay(3000)
//                println("SECOND FLOW: The received number is $it")
//            }
//        }
//    }
//
//    fun squareNumber(number: Int) {
//        viewModelScope.launch {
//            _sharedFlow.emit(number * number)
//        }
//    }
//    fun incrementCounter() {
//        _stateFlow.value += 1
//    }
//
//    private fun collectFlow() {
//        val flow = flow {
//            delay(250)
//            emit("Appetizer")
//            delay(1000)
//            emit("Main dish")
//            delay(100)
//            emit("Dessert")
//        }
//        viewModelScope.launch {
//            flow.onEach {
//                println("FLOW: $it is delivered")
//            }
//                .collectLatest {
//                    println("FLOW: Now eating $it")
//                    delay(1500)
//                    println("FLOW: Finished eating $it")
//                }
//        }
//    }
//}