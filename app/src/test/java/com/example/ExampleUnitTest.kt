package com.example

import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testCountryIsoForNumber() {
        assertEquals("US", ContactHelper.getCountryIsoForNumber("+14155550199"))
        assertEquals("IN", ContactHelper.getCountryIsoForNumber("+919876543210"))
        assertEquals("GB", ContactHelper.getCountryIsoForNumber("+442071838750"))
        assertNull(ContactHelper.getCountryIsoForNumber("5550199"))
    }

    @Test
    fun testGetDescriptiveNumberLabel() {
        val contact = DeviceContact(
            name = "John Doe",
            phoneNumber = "+14155550199",
            label = "Mobile",
            phoneNumbers = listOf(
                ContactPhoneNumber("+14155550199", "Mobile"),
                ContactPhoneNumber("+919876543210", "Work")
            )
        )

        val usLabel = ContactHelper.getDescriptiveNumberLabel(contact, "+14155550199")
        assertEquals("Mobile • US", usLabel)

        val inLabel = ContactHelper.getDescriptiveNumberLabel(contact, "+919876543210")
        assertEquals("Work • IN", inLabel)
    }

    @Test
    fun testSingleNumberContactLabel() {
        val contact = DeviceContact(
            name = "Jane Smith",
            phoneNumber = "5551234",
            label = "Home",
            phoneNumbers = listOf(
                ContactPhoneNumber("5551234", "Home")
            )
        )

        val label = ContactHelper.getDescriptiveNumberLabel(contact, "5551234")
        assertEquals("Home", label)
    }
}
