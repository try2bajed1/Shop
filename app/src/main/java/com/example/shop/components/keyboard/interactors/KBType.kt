package com.example.shop.components.keyboard.interactors

/**
 * Created with IntelliJ IDEA.
 * User: nsenchurin
 * Date: 09.06.18
 * Time: 15:53
 */
abstract class KBType

class Num(val value: String) : KBType()
class Coma(val value:String=",") : KBType()
class BackSpace : KBType()