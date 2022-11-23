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

class MacrosTest extends FunSuite {
  import Lang.*
  import Messages.KeyResolver

  test("inlined messages EN") {
    given KeyResolver = MyMessages.keyResolver(EN)
    assertEquals("this is my gnu", MyMessages.gnu())
  }

  test("inlined messages DE") {
    given KeyResolver = MyMessages.keyResolver(DE)
    assertEquals("Hier ist mein Gnu", MyMessages.gnu())
  }

  object MyMessages extends Messages {
    import Macros.*
    import Messages.*

    val gnu = msg0

    private def inlinedMessages = inlineMessages(
      "./test-files",
      Map(
        "messages_en.properties" -> "en",
        "messages_de.properties" -> "de"
      )
    )

    override protected def load(): Map[Lang, Map[String, MessageFormat]] =
      inlinedMessages

    override protected def newResolver(loaded: Map[Lang, Map[String, MessageFormat]])
      : Messages.Resolver =
      Resolver(loaded).withFallback(Lang.EN)
  }
}
