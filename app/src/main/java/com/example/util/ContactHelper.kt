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
    val isAppOnly: Boolean = false,
    val phoneNumbers: List<ContactPhoneNumber> = if (phoneNumber.isNotBlank()) listOf(ContactPhoneNumber(phoneNumber, label)) else emptyList()
)

object ContactHelper {

    fun saveContactToDevice(context: Context, name: String, phoneNumber: String, label: String = "Mobile"): Boolean {
        return try {
            val ops = ArrayList<android.content.ContentProviderOperation>()
            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )
            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )
            val phoneType = when (label.lowercase()) {
                "home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
                "work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                else -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
            }
            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
                    .build()
            )
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            android.util.Log.e("ContactHelper", "Failed to save contact to device", e)
            false
        }
    }

    fun createContactPickerIntent(): Intent {
        return Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
    }

    /**
     * Initiates a direct WhatsApp voice call without opening the chat screen.
     * Uses Android Contacts Provider VoIP Data item if available, or direct WhatsApp call intent.
     */
    fun launchWhatsAppCall(context: Context, rawNumber: String) {
        val cleanNumber = rawNumber.replace(Regex("[^0-9+]"), "")
        val digitsOnly = cleanNumber.trimStart('+')

        if (digitsOnly.isEmpty()) {
            android.widget.Toast.makeText(context, "Invalid phone number for WhatsApp", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Query Android Contacts Provider for WhatsApp VoIP Call MIME type for this number
        try {
            val resolver = context.contentResolver
            val uri = ContactsContract.Data.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.Data._ID,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA3,
                ContactsContract.Data.MIMETYPE
            )
            val selection = "${ContactsContract.Data.MIMETYPE} IN (?, ?)"
            val selectionArgs = arrayOf(
                "vnd.android.cursor.item/vnd.com.whatsapp.voip.call",
                "vnd.android.cursor.item/vnd.com.whatsapp.w4b.voip.call"
            )

            var targetDataId: Long? = null
            var targetMimeType: String? = null

            resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(ContactsContract.Data._ID)
                val data1Col = cursor.getColumnIndex(ContactsContract.Data.DATA1)
                val data3Col = cursor.getColumnIndex(ContactsContract.Data.DATA3)
                val mimeCol = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)

                while (cursor.moveToNext()) {
                    val data1 = if (data1Col >= 0) cursor.getString(data1Col) ?: "" else ""
                    val data3 = if (data3Col >= 0) cursor.getString(data3Col) ?: "" else ""
                    val rowDigits1 = data1.replace(Regex("[^0-9]"), "")
                    val rowDigits3 = data3.replace(Regex("[^0-9]"), "")

                    if (rowDigits1.endsWith(digitsOnly) || digitsOnly.endsWith(rowDigits1) ||
                        rowDigits3.endsWith(digitsOnly) || digitsOnly.endsWith(rowDigits3) ||
                        (rowDigits1.length >= 7 && digitsOnly.contains(rowDigits1))) {
                        targetDataId = if (idCol >= 0) cursor.getLong(idCol) else null
                        targetMimeType = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                        break
                    }
                }
            }

            if (targetDataId != null && targetMimeType != null) {
                val directCallIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.parse("content://com.android.contacts/data/$targetDataId"),
                        targetMimeType
                    )
                    setPackage(if (targetMimeType!!.contains("w4b")) "com.whatsapp.w4b" else "com.whatsapp")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(directCallIntent)
                return
            }
        } catch (_: Exception) {
            // Proceed to direct scheme fallback
        }

        // 2. Direct VoIP Call Intent via WhatsApp URL scheme
        try {
            val callIntent = Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://call?phone=$digitsOnly")).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(callIntent)
            return
        } catch (_: Exception) {
            // Fallback to chat link
        }

        // 3. Fallback: Open WhatsApp directly
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$digitsOnly")).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
            try {
                // Try open browser wa.me if whatsapp app not installed
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digitsOnly")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "WhatsApp is not installed on this device", android.widget.Toast.LENGTH_SHORT).show()
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
     * Gets the carrier voicemail number from TelephonyManager, or defaults to *86
     * (the standard voicemail access code for Spectrum Mobile, Verizon, and partner MVNOs).
     */
    fun getVoicemailNumber(context: Context): String {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            val num = tm?.voiceMailNumber
            if (!num.isNullOrBlank()) return num
        } catch (e: Exception) {
            // Permission or carrier exception
        }
        return "*86"
    }

    /**
     * Checks if the given dialed or incoming number corresponds to voicemail.
     */
    fun isVoicemailNumber(context: Context, number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        val clean = number.trim()
        val configuredVm = getVoicemailNumber(context).trim()
        if (clean == configuredVm || clean == "*86" || clean == "1") return true
        if (clean == "901" || clean == "121" || clean == "123" || clean == "171" || clean == "800") return true
        try {
            if (android.telephony.PhoneNumberUtils.isVoiceMailNumber(clean)) return true
        } catch (_: Exception) {}
        return false
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
                val photo = if (photoIndex != -1) cursor.getString(photoIndex) else null
                val thumb = if (thumbIndex != -1) cursor.getString(thumbIndex) else null
                val type = if (typeIndex != -1) cursor.getInt(typeIndex) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE

                val label = when (type) {
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                    else -> "Mobile"
                }

                DeviceContact(
                    name = fullName.ifBlank { "Unknown" },
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
                    name = fullName,
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
        val nicknameMap = fetchNicknameMap(context)
        val contactMap = linkedMapOf<String, DeviceContactAccumulator>()
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.STARRED,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY
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
                val priIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)
                val supIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY)

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
                    val isPrimary = (priIdx != -1 && it.getInt(priIdx) > 0) || (supIdx != -1 && it.getInt(supIdx) > 0)

                    val key = contactId?.toString() ?: fullName.trim().lowercase()
                    val accumulator = contactMap.getOrPut(key) {
                        val nickname = if (contactId != null) nicknameMap[contactId] else null
                        DeviceContactAccumulator(
                            name = fullName,
                            photoUri = photo ?: thumb,
                            contactId = contactId,
                            nickname = nickname,
                            isStarred = true
                        )
                    }
                    accumulator.addNumber(number, label, isPrimary)
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return contactMap.values.map { it.toDeviceContact() }
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
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY
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
                val priIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)
                val supIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY)

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
                    val isPrimary = (priIdx != -1 && it.getInt(priIdx) > 0) || (supIdx != -1 && it.getInt(supIdx) > 0)

                    val key = contactId?.toString() ?: fullName.trim().lowercase()
                    val accumulator = contactsMap.getOrPut(key) {
                        val nickname = if (contactId != null) nicknameMap[contactId] else null
                        DeviceContactAccumulator(
                            name = fullName,
                            photoUri = photo ?: thumb,
                            contactId = contactId,
                            nickname = nickname
                        )
                    }
                    accumulator.addNumber(number, label, isPrimary)
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
    val nickname: String?,
    val isStarred: Boolean = false
) {
    private val numbers = mutableListOf<ContactPhoneNumber>()
    private val seen = mutableSetOf<String>()
    private var defaultNumberItem: ContactPhoneNumber? = null

    fun addNumber(number: String, label: String, isPrimary: Boolean = false) {
        val clean = number.replace(Regex("[^0-9+]"), "")
        if (clean.isNotEmpty() && seen.add(clean)) {
            val item = ContactPhoneNumber(number, label)
            numbers.add(item)
            if (isPrimary || defaultNumberItem == null) {
                defaultNumberItem = item
            }
        }
    }

    fun toDeviceContact(): DeviceContact {
        val primary = defaultNumberItem ?: numbers.firstOrNull()
        return DeviceContact(
            name = name,
            phoneNumber = primary?.number ?: "",
            label = primary?.label ?: "Mobile",
            photoUri = photoUri,
            contactId = contactId,
            nickname = nickname,
            phoneNumbers = numbers.toList(),
            isStarred = isStarred
        )
    }
}
