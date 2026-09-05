package com.example.util

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract

data class ContactPhoneNumber(
    val number: String,
    val label: String = "Mobile"
)

data class DeviceContact(
    val name: String,
    val phoneNumber: String,
    val label: String = "Mobile",
    val photoUri: String? = null,
    val contactId: Long? = null,
    val nickname: String? = null,
    val isStarred: Boolean = false,
    val phoneNumbers: List<ContactPhoneNumber> = if (phoneNumber.isNotBlank()) listOf(ContactPhoneNumber(phoneNumber, label)) else emptyList()
)

object ContactHelper {

    fun createContactPickerIntent(): Intent {
        return Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
    }

    /**
     * Launches WhatsApp voice call for a phone number (e.g. +91...)
     */
    fun launchWhatsAppCall(context: Context, rawNumber: String) {
        val cleanNumber = rawNumber.replace(Regex("[^0-9+]"), "")
        val digitsOnly = cleanNumber.trimStart('+')
        try {
            // Preferred WhatsApp Voice Call Intent
            val callIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$digitsOnly")
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(callIntent)
        } catch (e: Exception) {
            // Fallback to browser WhatsApp web / universal link
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digitsOnly")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (err: Exception) {
                err.printStackTrace()
            }
        }
    }

    /**
     * Checks if a phone number matches international WhatsApp routing rules (e.g. +91 prefix)
     */
    fun shouldSuggestWhatsApp(phoneNumber: String): Boolean {
        val clean = phoneNumber.replace(Regex("[^0-9+]"), "")
        return clean.startsWith("+91") || clean.startsWith("0091")
    }

    /**
     * Fetches a map of Contact ID -> Nickname from ContactsContract.Data
     */
    fun fetchNicknameMap(context: Context): Map<Long, String> {
        val nicknameMap = mutableMapOf<Long, String>()
        try {
            val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Nickname.NAME
            )
            val selection = "${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
            val cursor = context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME)
                while (it.moveToNext()) {
                    if (idIdx != -1 && nameIdx != -1) {
                        val cid = it.getLong(idIdx)
                        val nick = it.getString(nameIdx)
                        if (!nick.isNullOrBlank()) {
                            nicknameMap[cid] = nick.trim()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Read contacts permission might not be granted
        }
        return nicknameMap
    }

    fun extractContactFromUri(context: Context, uri: Uri): DeviceContact? {
        var cursor: Cursor? = null
        val nicknameMap = fetchNicknameMap(context)
        return try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.TYPE
            )
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val cidIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val thumbIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)

                val contactId = if (cidIndex != -1) cursor.getLong(cidIndex) else null
                val number = if (numberIndex != -1) cursor.getString(numberIndex) ?: "" else ""
                val fullName = if (nameIndex != -1) cursor.getString(nameIndex) ?: "" else ""
                val nickname = if (contactId != null) nicknameMap[contactId] else null
                val displayName = if (!nickname.isNullOrBlank()) nickname else fullName.ifBlank { "Unknown" }
                val photo = if (photoIndex != -1) cursor.getString(photoIndex) else null
                val thumb = if (thumbIndex != -1) cursor.getString(thumbIndex) else null
                val type = if (typeIndex != -1) cursor.getInt(typeIndex) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE

                val label = when (type) {
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                    else -> "Mobile"
                }

                DeviceContact(
                    name = displayName,
                    phoneNumber = number,
                    label = label,
                    photoUri = photo ?: thumb,
                    contactId = contactId,
                    nickname = nickname
                )
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            cursor?.close()
        }
    }

    fun lookupContactByNumber(context: Context, phoneNumber: String): DeviceContact? {
        if (phoneNumber.isBlank()) return null
        var cursor: Cursor? = null
        val nicknameMap = fetchNicknameMap(context)
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.NUMBER,
                ContactsContract.PhoneLookup.PHOTO_URI,
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                ContactsContract.PhoneLookup.TYPE
            )
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                val thumbIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                val typeIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE)

                val contactId = if (idIdx != -1) cursor.getLong(idIdx) else null
                val fullName = if (nameIdx != -1) cursor.getString(nameIdx) ?: phoneNumber else phoneNumber
                val nickname = if (contactId != null) nicknameMap[contactId] else null
                val displayName = if (!nickname.isNullOrBlank()) nickname else fullName
                val num = if (numIdx != -1) cursor.getString(numIdx) ?: phoneNumber else phoneNumber
                val photo = if (photoIdx != -1) cursor.getString(photoIdx) else null
                val thumb = if (thumbIdx != -1) cursor.getString(thumbIdx) else null
                val type = if (typeIdx != -1) cursor.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE

                val label = when (type) {
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                    else -> "Mobile"
                }

                DeviceContact(
                    name = displayName,
                    phoneNumber = num,
                    label = label,
                    photoUri = photo ?: thumb,
                    contactId = contactId,
                    nickname = nickname
                )
            } else null
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            cursor?.close()
        }
    }

    /**
     * Reads all starred/favorite contacts from device Contacts database
     * Using nickname if available, else full display name.
     */
    fun fetchStarredContacts(context: Context): List<DeviceContact> {
        val starredList = mutableListOf<DeviceContact>()
        val nicknameMap = fetchNicknameMap(context)
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.STARRED
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.STARRED} = 1"
            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.let {
                val cidIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val thumbIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val seenNumbers = mutableSetOf<String>()

                while (it.moveToNext()) {
                    val contactId = if (cidIdx != -1) it.getLong(cidIdx) else null
                    val fullName = if (nameIdx != -1) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numIdx != -1) it.getString(numIdx) ?: "" else ""
                    val cleanNum = number.replace(Regex("[^0-9+]"), "")
                    if (cleanNum.isNotEmpty() && seenNumbers.add(cleanNum)) {
                        val nickname = if (contactId != null) nicknameMap[contactId] else null
                        val displayName = if (!nickname.isNullOrBlank()) nickname else fullName
                        val type = if (typeIdx != -1) it.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                        val label = when (type) {
                            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                            else -> "Mobile"
                        }
                        val photo = if (photoIdx != -1) it.getString(photoIdx) else null
                        val thumb = if (thumbIdx != -1) it.getString(thumbIdx) else null

                        starredList.add(
                            DeviceContact(
                                name = displayName,
                                phoneNumber = number,
                                label = label,
                                photoUri = photo ?: thumb,
                                contactId = contactId,
                                nickname = nickname,
                                isStarred = true
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return starredList
    }

    /**
     * Updates the STARRED flag in Android's Contacts Provider for a contact by phone number
     */
    fun setContactStarred(context: Context, phoneNumber: String, starred: Boolean): Boolean {
        var contactId: Long? = null
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    if (idIdx != -1) contactId = cursor.getLong(idIdx)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (contactId != null) {
            return try {
                val values = android.content.ContentValues().apply {
                    put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
                }
                val contactUri = android.content.ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId!!)
                val count = context.contentResolver.update(contactUri, values, null, null)
                count > 0
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

    /**
     * Updates or creates a Nickname record for a contact in ContactsContract.Data
     */
    fun updateContactNickname(context: Context, phoneNumber: String, newNickname: String): Boolean {
        var contactId: Long? = null
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    if (idIdx != -1) contactId = cursor.getLong(idIdx)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (contactId != null) {
            return try {
                val dataUri = ContactsContract.Data.CONTENT_URI
                val cursor = context.contentResolver.query(
                    dataUri,
                    arrayOf(ContactsContract.Data._ID),
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE),
                    null
                )
                val dataId = cursor?.use {
                    if (it.moveToFirst()) it.getLong(it.getColumnIndexOrThrow(ContactsContract.Data._ID)) else null
                }

                if (dataId != null) {
                    val values = android.content.ContentValues().apply {
                        put(ContactsContract.CommonDataKinds.Nickname.NAME, newNickname)
                    }
                    context.contentResolver.update(
                        android.content.ContentUris.withAppendedId(dataUri, dataId),
                        values,
                        null,
                        null
                    )
                    true
                } else if (newNickname.isNotBlank()) {
                    val rawCursor = context.contentResolver.query(
                        ContactsContract.RawContacts.CONTENT_URI,
                        arrayOf(ContactsContract.RawContacts._ID),
                        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                        arrayOf(contactId.toString()),
                        null
                    )
                    val rawContactId = rawCursor?.use {
                        if (it.moveToFirst()) it.getLong(it.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)) else null
                    }
                    if (rawContactId != null) {
                        val values = android.content.ContentValues().apply {
                            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                            put(ContactsContract.CommonDataKinds.Nickname.NAME, newNickname)
                        }
                        context.contentResolver.insert(dataUri, values)
                        true
                    } else false
                } else false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

    fun fetchDeviceContacts(context: Context): List<DeviceContact> {
        val nicknameMap = fetchNicknameMap(context)
        val contactsMap = linkedMapOf<String, DeviceContactAccumulator>()
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
            )
            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.let {
                val cidIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val thumbIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (it.moveToNext()) {
                    val contactId = if (cidIdx != -1) it.getLong(cidIdx) else null
                    val fullName = if (nameIdx != -1) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numIdx != -1) it.getString(numIdx) ?: "" else ""
                    val cleanNum = number.replace(Regex("[^0-9+]"), "")
                    if (cleanNum.isEmpty()) continue

                    val type = if (typeIdx != -1) it.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    val label = when (type) {
                        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                        else -> "Mobile"
                    }
                    val photo = if (photoIdx != -1) it.getString(photoIdx) else null
                    val thumb = if (thumbIdx != -1) it.getString(thumbIdx) else null

                    val key = contactId?.toString() ?: fullName.trim().lowercase()
                    val accumulator = contactsMap.getOrPut(key) {
                        val nickname = if (contactId != null) nicknameMap[contactId] else null
                        val displayName = if (!nickname.isNullOrBlank()) nickname else fullName
                        DeviceContactAccumulator(
                            name = displayName,
                            photoUri = photo ?: thumb,
                            contactId = contactId,
                            nickname = nickname
                        )
                    }
                    accumulator.addNumber(number, label)
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted; return empty list
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return contactsMap.values.map { it.toDeviceContact() }
    }
}

private class DeviceContactAccumulator(
    val name: String,
    val photoUri: String?,
    val contactId: Long?,
    val nickname: String?
) {
    private val numbers = mutableListOf<ContactPhoneNumber>()
    private val seen = mutableSetOf<String>()

    fun addNumber(number: String, label: String) {
        val clean = number.replace(Regex("[^0-9+]"), "")
        if (clean.isNotEmpty() && seen.add(clean)) {
            numbers.add(ContactPhoneNumber(number, label))
        }
    }

    fun toDeviceContact(): DeviceContact {
        val primary = numbers.firstOrNull()
        return DeviceContact(
            name = name,
            phoneNumber = primary?.number ?: "",
            label = primary?.label ?: "Mobile",
            photoUri = photoUri,
            contactId = contactId,
            nickname = nickname,
            phoneNumbers = numbers.toList()
        )
    }
}
