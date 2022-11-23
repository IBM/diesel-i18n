/*
 * Copyright 2021 The Diesel Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package diesel.i18n

import munit.FunSuite

class MessagesTest extends FunSuite {
  import Messages._
  import Lang._

  test("first") {
    implicit def en = MyMessages.keyResolver(EN)
    assertEquals(MyMessages.toto(), "my toto")
    assertEquals(MyMessages.titi(13), "my titi 13")
  }

  test("with parameter") {
    implicit def en = MyMessages.keyResolver(EN)
    assertEquals(MyMessages.titi(13), "my titi 13")
  }

  test("with custom parameter") {
    implicit def en = MyMessages.keyResolver(EN)
    assertEquals(MyMessages.customList(Seq(13, 14)), "listing 13, 14")
  }

  test("fallback") {
    implicit def de = MyMessages.keyResolver(DE)
    assertEquals(MyMessages.titi(13), "[en]my titi 13")
  }

  test("missing fallback message de") {
    implicit def en = MyMessages.keyResolver(EN)
    assertEquals(MyMessages.missing(), "[missing]#0")
  }

  test("missing fallback message") {
    implicit def de = MyMessages.keyResolver(DE)
    assertEquals(MyMessages.missing(), "[missing]#0")
  }

  test("wrong arity") {
    implicit def en = MyMessages.keyResolver(EN)
    assertEquals(MyMessages.`wrong.arity`(13), "[wrong.arity]#1")
  }

  test("plurals") {
    implicit def en = MyMessages.keyResolver(EN)
    assertEquals(MyMessages.plural0(13)(), "many bars")
    assertEquals(MyMessages.plural0(1)(), "one bar")
    assertEquals(MyMessages.plural0(0)(), "no bars at all")
  }

  test("plurals de") {
    implicit def de = MyMessages.keyResolver(DE)
    assertEquals(MyMessages.plural0(13)(), "[en]many bars")
    assertEquals(MyMessages.plural0(1)(), "nur eine bar")
    assertEquals(MyMessages.plural0(0)(), "[en]no bars at all")
  }

  test("plurals with parameters") {
    implicit def en = MyMessages.keyResolver(EN)
    assertEquals(MyMessages.plural2(13)("foo", 13), "many or no things foo 13")
    assertEquals(MyMessages.plural2(1)("gnu", 1313), "one thing gnu")
    assertEquals(MyMessages.plural2(0)("bar", 131313), "many or no things bar 131313")
  }

  test("plurals with parameters de") {
    implicit def de = MyMessages.keyResolver(DE)
    assertEquals(MyMessages.plural2(13)("foo", 13), "[en]many or no things foo 13")
    assertEquals(MyMessages.plural2(1)("gnu", 1313), "[en]one thing gnu")
    assertEquals(MyMessages.plural2(0)("bar", 131313), "[en]many or no things bar 131313")
  }

  test("plurals with arity mismatch") {
    implicit def en = MyMessages.keyResolver(EN)
    assertEquals(MyMessages.pluralBroken(13)("foo"), "many foo")
    assertEquals(MyMessages.pluralBroken(1)("gnu"), "[pluralBroken]#1")
    assertEquals(MyMessages.pluralBroken(0)("bar"), "many bar")
  }

  test("self plurals") {
    implicit def en = MyMessages.keyResolver(EN)
    assertEquals(MyMessages.simpleCountPlural(13), "lots 13")
    assertEquals(MyMessages.simpleCountPlural(1), "one")
    assertEquals(MyMessages.simpleCountPlural(2), "two")
    assertEquals(MyMessages.simpleCountPlural(0), "empty")
  }

  test("consistency check") {
    val inconsistencies = MyMessages.checkConsistency()
    // println("DEBUGGING", munitPrint(inconsistencies))
    assertEquals(
      inconsistencies,
      Seq(
        MissingMessage(
          lang = EN,
          key = "missing"
        ),
        MissingMessage(
          lang = DE,
          key = "customList"
        ),
        MissingMessage(
          lang = DE,
          key = "missing"
        ),
        MissingMessage(
          lang = DE,
          key = "plural0_plural"
        ),
        MissingMessage(
          lang = DE,
          key = "plural2"
        ),
        MissingMessage(
          lang = DE,
          key = "plural2_plural"
        ),
        MissingMessage(
          lang = DE,
          key = "pluralBroken"
        ),
        MissingMessage(
          lang = DE,
          key = "pluralBroken_plural"
        ),
        MissingMessage(
          lang = DE,
          key = "simpleCountPlural"
        ),
        MissingMessage(
          lang = DE,
          key = "simpleCountPlural_plural"
        ),
        MissingMessage(
          lang = DE,
          key = "titi"
        ),
        MissingMessage(
          lang = DE,
          key = "wrong.arity"
        ),
        ArityMismatch(
          lang = EN,
          key = "pluralBroken",
          expected = 1,
          actual = 14
        ),
        ArityMismatch(
          lang = EN,
          key = "wrong.arity",
          expected = 1,
          actual = 2
        ),
        UnusedMessage(
          lang = EN,
          key = "not.used"
        )
      )
    )
  }

  test("messages encoding") {
    assertEquals("X", MyMessages.encoded()(MyMessages.keyResolver(EN)))
    assertEquals("ä", MyMessages.encoded()(MyMessages.keyResolver(DE)))
  }
}

object MyMessages extends Messages {

  import Messages._

  val toto                               = msg0
  val titi: Msg1[Int]                    = msg1[Int]
  val customList: Msg1[Seq[Int]]         = msg1[Seq[Int]].arg(commaSeparatedToString)
  val missing: Msg0                      = msg0
  val `wrong.arity`                      = msg1[Int]
  val plural0: Plural[Msg0]              = plural(msg0(_))
  val plural2: Plural[Msg2[String, Int]] = plural(msg2(_))
  val pluralBroken: Plural[Msg1[String]] = plural(msg1(_))
  val simpleCountPlural: SelfPlural      = selfPlural
  val encoded                            = msg0

  override protected def load(): Map[Lang, Map[String, MessageFormat]] =
    TestData.propertiesContent
      .flatMap { case (k, v) =>
        Lang(k).flatMap(lang => Loader.loadProperties(v).map((lang, _)))
      }
      .toMap

  override protected def newResolver(loaded: Map[Lang, Map[String, MessageFormat]])
    : Messages.Resolver =
    Resolver(loaded).withFallback(Lang.EN)
}

object TestData {
  import Lang._
  val propertiesContent: Map[String, String] = Map(
    EN.lang -> """|
                  |toto = my toto
                  |titi = my titi {0}
                  |customList = listing {0}
                  |wrong.arity = boom {1}
                  |plural0_plural = many bars
                  |plural0_0 = no bars at all
                  |plural0 = one bar
                  |plural2_plural = many or no things {0} {1}
                  |plural2 = one thing {0}
                  |not.used = never
                  |pluralBroken_plural = many {0}
                  |pluralBroken = only one {13}
                  |simpleCountPlural_plural = lots {0}
                  |simpleCountPlural_2 = two
                  |simpleCountPlural_1 = one
                  |simpleCountPlural_0 = empty
                  |simpleCountPlural = single
                  |encoded = X
                  |""".stripMargin,
    DE.lang -> """|
                  |toto = mein toto
                  |plural0 = nur eine bar
                  |encoded = ä
                  |""".stripMargin
    // issing.lang -> ""
  )
}
