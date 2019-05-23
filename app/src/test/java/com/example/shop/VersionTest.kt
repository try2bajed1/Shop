package com.example.shop

import android.support.test.filters.SmallTest
import com.g00fy2.versioncompare.Version
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 14.03.19
 * Time: 13:18
 */

@RunWith(JUnit4::class)
@SmallTest
class VersionTest {

    @Test
    fun testEkamBoxVersion() {
        Assert.assertTrue(Version("1.4").isLowerThan("999.1.889-g31b5783"))
        Assert.assertTrue(Version("1.4").isLowerThan("1.4.1"))
        Assert.assertTrue(Version("1.5").isHigherThan("1.4.1"))
    }
}