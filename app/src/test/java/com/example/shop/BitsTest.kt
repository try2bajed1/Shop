package com.example.shop

import com.example.shop.utils.bitNumbers
import com.example.shop.utils.isPowerOfTwo
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class BitsTest {


    @Test
    fun isPowerOfTwo(){
        assertTrue(1.isPowerOfTwo())
        assertTrue(2.isPowerOfTwo())
        assertTrue(4.isPowerOfTwo())
        assertTrue(8.isPowerOfTwo())
        assertTrue(16.isPowerOfTwo())
        assertTrue(32.isPowerOfTwo())
        assertTrue(64.isPowerOfTwo())

        assertFalse(0.isPowerOfTwo())
        assertFalse(3.isPowerOfTwo())
        assertFalse(11.isPowerOfTwo())
        assertFalse(66.isPowerOfTwo())
        assertFalse(31.isPowerOfTwo())
        assertFalse(63.isPowerOfTwo())         
    }


    @Test
    fun testSum(){
        assertTrue(1.bitNumbers() == listOf(1))
        assertTrue(1023.bitNumbers() == listOf(1, 2, 4, 8, 16, 32, 64, 128, 256, 512))
        assertTrue(45.bitNumbers() == listOf(1, 4, 8, 32))
        assertTrue(3.bitNumbers() == listOf(1, 2))
        assertTrue(7.bitNumbers() == listOf(1, 2, 4))
        assertTrue(15.bitNumbers() == listOf(1, 2, 4, 8))
        assertTrue(31.bitNumbers() == listOf(1, 2, 4, 8, 16))

    }

}