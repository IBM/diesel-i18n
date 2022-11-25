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

import scala.quoted.Quotes
import scala.quoted.Expr
import scala.quoted.quotes

case class DeclaringSourceName(name: String)

object DeclaringSourceName {
  import scala.language.experimental.macros

  inline given DeclaringSourceName =
    ${ declaringSourceNameImpl }

  private def declaringSourceNameImpl(using Quotes): Expr[DeclaringSourceName] = {
    import quotes.reflect._
    var owner             = Symbol.spliceOwner.owner
    val isDeclaringSymbol = (s: Symbol) => s.isDefDef || s.isValDef
    while (!isDeclaringSymbol(owner)) do {
      owner = owner.owner
    }
    val name              = owner.name.trim()
    '{ DeclaringSourceName(${ Expr(name) }) }
  }

}
