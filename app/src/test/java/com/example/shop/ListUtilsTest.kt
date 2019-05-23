package com.example.shop

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class ListUtilsTest {

    companion object {
        private const val DOG = "Dog"
        private const val CAT = "Cat"
        private const val HUMAN = "Human"
        private const val DOGCAT = DOG + CAT
        private const val HUMANDOG = HUMAN + DOG
        private const val HUMANCAT = HUMAN + CAT
    }

    @Test
    fun getDifferenceTest() {
        val list = listOf(DOG, CAT, HUMANCAT)
        val list2 = listOf(CAT, DOG, HUMANDOG, DOGCAT)
        val requiredResult1 = listOf(HUMANCAT)
        val requiredResult2 = listOf(HUMANDOG, DOGCAT)
        list.getDifference(list2).let {
            assert(it.first == requiredResult1)
            assert(it.second == requiredResult2)
        }
    }

    @Test
    fun getDifferenceWithGetterTest() {
        val list = listOf(Dog(DOG), Dog(CAT), Dog(HUMANCAT))
        val list2 = listOf(Dog(CAT), Dog(DOG), Dog(HUMANDOG), Dog(DOGCAT))
        val requiredResult1 = listOf(Dog(HUMANCAT))
        val requiredResult2 = listOf(Dog(HUMANDOG), Dog(DOGCAT))
        list.getDifference(list2, { it.name }).let {
            assert(it.first == requiredResult1)
            assert(it.second == requiredResult2)
        }
    }

    @Test
    fun ungetterTest() {
        val list = listOf(DOG, CAT, HUMANCAT)
        val dataList = listOf(Dog(DOG), Dog(CAT), Dog(HUMANCAT), Dog(HUMANDOG))
        val resultList = listOf(Dog(DOG), Dog(CAT), Dog(HUMANCAT))
        assert(list.ungetter(dataList, { it.name }) == resultList)
    }

    data class Dog(val name: String)
}