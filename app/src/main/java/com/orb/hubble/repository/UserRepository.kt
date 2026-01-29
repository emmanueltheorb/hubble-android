package com.orb.hubble.repository

import android.content.Context
import android.provider.ContactsContract
import com.orb.hubble.data.ClientEvent
import com.orb.hubble.data.Contact
import com.orb.hubble.data.Post
import com.orb.hubble.data.User
import com.orb.hubble.data.WebsocketEvent
import com.orb.hubble.service.WebsocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    val followedUsers: StateFlow<List<String>>
    val contacts: StateFlow<List<String>>

    suspend fun getFollowedUsers(userId: String)
    suspend fun getContacts(userId: String)
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val websocketService: WebsocketService // Your API service
) : UserRepository {

    private val _followedUsers = MutableStateFlow<List<String>>(emptyList())
    override val followedUsers: StateFlow<List<String>> = _followedUsers.asStateFlow()

    private val _contacts = MutableStateFlow<List<String>>(emptyList())
    override val contacts: StateFlow<List<String>> = _contacts.asStateFlow()

    init {
        observeWebSocketEvents()
    }

    private fun observeWebSocketEvents() {
        // Collect followed users
        CoroutineScope(Dispatchers.IO).launch {
            websocketService.events
                .map { event ->
                    when (event) {
                        is WebsocketEvent.FollowedUsers -> event.followedUsers
                        else -> null
                    }
                }
                .filterNotNull()
                .collect { followedUsers ->
                    _followedUsers.value = followedUsers
                }
        }

        // Collect contacts
        CoroutineScope(Dispatchers.IO).launch {
            websocketService.events
                .map { event ->
                    when (event) {
                        is WebsocketEvent.Contacts -> event.contacts
                        else -> null
                    }
                }
                .filterNotNull()
                .collect { contacts ->
                    _contacts.value = contacts
                }
        }
    }

    override suspend fun getFollowedUsers(userId: String) {
        websocketService.sendEvent(ClientEvent.GetFollowedUsers(userId))
    }

    override suspend fun getContacts(userId: String) {
        websocketService.sendEvent(ClientEvent.GetContacts(userId))
    }

    private suspend fun getContactsFromPhonebook(): List<String> {
        // Implement phonebook contact matching
        // This would:
        // 1. Read contacts from phonebook
        // 2. Extract phone numbers
        // 3. Match with users in your app via API
        return emptyList()
    }
}

class ContactHelper @Inject constructor(
    private val context: Context
) {

    suspend fun getPhonebookContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        cursor?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                var number = cursor.getString(numberIndex)

                // Clean up phone number
                number = number.replace("[^0-9+]".toRegex(), "")

                contacts.add(Contact(number, name))
            }
        }

        return@withContext contacts.distinctBy { it.phoneNumber }
    }
}