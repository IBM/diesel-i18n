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

case class DeclaringSourceName(name: String)

object DeclaringSourceName {
  import scala.language.experimental.macros

  implicit def declaringSourceName: DeclaringSourceName = macro declaringSourceNameImpl

  private type Context = scala.reflect.macros.blackbox.Context
  def declaringSourceNameImpl(c: Context): c.Expr[DeclaringSourceName] = {
    import c.universe._
    var owner = enclosingOwner(c)
    while (owner.isSynthetic) {
      owner = owner.owner
    }
    val name  = owner.name.decodedName.toString().trim()
    c.Expr[DeclaringSourceName](q"""${c.prefix}($name)""")
  }

  private def enclosingOwner(c: Context) = c.internal.enclosingOwner
}
