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

object Messages {
  type Lang        = Lang.Value
  private type Key = String

  private type ResolveFun    = Lang => ResolveKeyFun
  private type ResolveKeyFun = String => Option[MessageFormat]

  case class Resolver(resolve: ResolveFun) {
    def apply(lang: Lang): KeyResolver         = KeyResolver(resolve(lang))
    def withFallback(fallback: Lang): Resolver = Resolver(lang =>
      (key: String) => {
        resolve(lang)(key).orElse(resolve(fallback)(key).map(_.withPrefix(s"[${fallback}]")))
      }
    )
  }

  object Resolver {
    def apply(messages: Map[Lang, Map[String, MessageFormat]]): Resolver = Resolver((lang: Lang) =>
      (key: String) => {
        messages
          .get(lang)
          .flatMap(_.get(key))
      }
    )
  }

  case class KeyResolver(resolve: ResolveKeyFun) {
    def apply(key: Key): Option[MessageFormat] = resolve(key)
  }

  case class Resolution(key: String, arity: Option[Int] = None, fallback: Option[String] = None) {
    def withArity(arity: Int): Resolution          = this.copy(arity = Some(arity))
    def withFallback(fallback: String): Resolution = this.copy(fallback = Some(fallback))
  }

  sealed trait Inconsistency
  case class MissingMessage(lang: Lang, key: String) extends Inconsistency
  case class UnusedMessage(lang: Lang, key: String)  extends Inconsistency
  case class ArityMismatch(lang: Lang, key: String, expected: Int, actual: Int)
      extends Inconsistency

  def isIgnored(key: String): Boolean = {
    // ignore keys for explicit plurals
    val index = key.lastIndexOf('_')
    index > -1 && key.substring(index + 1).toIntOption.isDefined
  }

  def checkConsistency(
    resolutions: Seq[Resolution],
    resolver: Resolver,
    defined: Map[Lang, Set[String]]
  ): Seq[Inconsistency] = {
    val used = resolutions.flatMap(resolution => Seq(resolution.key) ++ resolution.fallback.toSeq)

    val unused        = defined
      .map {
        case (lang, keys) => (lang, keys -- used)
      }
      .flatMap {
        case (lang, keys) => keys.filterNot(isIgnored).map(key => UnusedMessage(lang, key))
      }
      .toSeq
      .sortBy(m => (m.lang, m.key))
    val missing       = defined.keySet
      .flatMap { lang =>
        used.flatMap(key =>
          if resolver(lang)(key).isDefined then
            None
          else
            Some(MissingMessage(lang, key))
        )
      }
      .toSeq
      .sortBy(m => (m.lang, m.key))
    val arityMismatch = defined.keySet
      .flatMap { lang =>
        resolutions
          .flatMap(resolution =>
            resolution.arity.flatMap(arity =>
              resolver(lang)(resolution.key).map((lang, resolution.key, arity, _))
            )
          )
          .flatMap { case (l, k, arity, mf) =>
            mf.withArity(arity).swap.map(actual => ArityMismatch(l, k, arity, actual)).toOption
          }
      }
      .toSeq
      .sortBy(m => (m.lang, m.key))

    missing ++ arityMismatch ++ unused
  }
}

abstract class Messages {
  import Messages.*

  protected def load(): Map[Lang, Map[String, MessageFormat]]

  protected def newResolver(loaded: Map[Lang, Map[String, MessageFormat]]): Resolver

  private val resolver = newResolver(load())

  def keyResolver(lang: Lang): KeyResolver = resolver(lang)

  def checkConsistency(): Seq[Inconsistency] = {
    val loaded  = load()
    val defined = loaded.map { case (lang, map) => (lang, map.keySet) }.toMap
    Messages.checkConsistency(this.resolutions(), Resolver(loaded), defined)
  }

  protected def msg0(using key: sourcecode.Name): Msg0 = msg0(Resolution(fieldKey(key)))
  protected def msg0(resolution: Resolution): Msg0     = Msg0(collect(resolution.withArity(0)))

  protected def msg1[T](using key: sourcecode.Name): Msg1[T] = msg1(Resolution(fieldKey(key)))
  protected def msg1[T](resolution: Resolution): Msg1[T]     = Msg1(collect(resolution.withArity(1)))

  protected def msg2[T1, T2](using key: sourcecode.Name): Msg2[T1, T2] =
    msg2(Resolution(fieldKey(key)))
  protected def msg2[T1, T2](resolution: Resolution): Msg2[T1, T2]     =
    Msg2(collect(resolution.withArity(2)))

  protected def msg3[T1, T2, T3](using key: sourcecode.Name): Msg3[T1, T2, T3] =
    msg3(Resolution(fieldKey(key)))
  protected def msg3[T1, T2, T3](resolution: Resolution): Msg3[T1, T2, T3]     =
    Msg3(collect(resolution.withArity(3)))

  protected def msg4[T1, T2, T3, T4](using key: sourcecode.Name): Msg4[T1, T3, T2, T4] =
    msg4(Resolution(fieldKey(key)))
  protected def msg4[T1, T2, T3, T4](resolution: Resolution): Msg4[T1, T2, T3, T4]     =
    Msg4(collect(resolution.withArity(4)))

  protected def plural[M <: Msg](msg: Resolution => M)(using key: sourcecode.Name): Plural[M] =
    Plural(fieldKey(key), msg)
  protected def selfPlural(using key: sourcecode.Name): SelfPlural                            = SelfPlural(fieldKey(key))

  def defaultToString[T]: T => String             = { (arg: T) => arg.toString }
  def commaSeparatedToString[T]: Seq[T] => String = { (arg: Seq[T]) => arg.mkString(", ") }
  trait Msg {
    val resolution: Resolution
    protected def getIt(resolver: KeyResolver): MessageFormat = resolver(resolution.key)
      .orElse(resolution.fallback.flatMap(fallback => resolver(fallback)))
      .flatMap(mf => resolution.arity.flatMap(arity => mf.withArity(arity).toOption))
      .getOrElse(MessageFormat.text(
        s"[${resolution.fallback.getOrElse(resolution.key)}]${resolution.arity.map(a => s"#${a}").getOrElse("")}"
      ))
  }

  case class Msg0 private[i18n] (resolution: Resolution) extends Msg {
    def apply()(using resolver: KeyResolver): String = getIt(resolver)()
  }

  case class Msg1[T] private[i18n] (resolution: Resolution, f: (T => String) = defaultToString)
      extends Msg {
    def apply(arg: T)(using resolver: KeyResolver): String = getIt(resolver)(f(arg))
    def arg(f: T => String): Msg1[T]                       = this.copy(f = f)
  }

  case class Msg2[T1, T2] private[i18n] (
    resolution: Resolution,
    f1: (T1 => String) = defaultToString,
    f2: (T2 => String) = defaultToString
  ) extends Msg {
    def apply(arg1: T1, arg2: T2)(using resolver: KeyResolver): String =
      getIt(resolver)(f1(arg1), f2(arg2))
    def arg1(f: T1 => String): Msg2[T1, T2]                            = this.copy(f1 = f)
    def arg2(f: T2 => String): Msg2[T1, T2]                            = this.copy(f2 = f)
  }

  case class Msg3[T1, T2, T3] private[i18n] (
    resolution: Resolution,
    f1: (T1 => String) = defaultToString,
    f2: (T2 => String) = defaultToString,
    f3: (T3 => String) = defaultToString
  ) extends Msg {
    def apply(arg1: T1, arg2: T2, arg3: T3)(using resolver: KeyResolver): String =
      getIt(resolver)(f1(arg1), f2(arg2), f3(arg3))
    def arg1(f: T1 => String): Msg3[T1, T2, T3]                                  = this.copy(f1 = f)
    def arg2(f: T2 => String): Msg3[T1, T2, T3]                                  = this.copy(f2 = f)
    def arg3(f: T3 => String): Msg3[T1, T2, T3]                                  = this.copy(f3 = f)
  }

  case class Msg4[T1, T2, T3, T4] private[i18n] (
    resolution: Resolution,
    f1: (T1 => String) = defaultToString,
    f2: (T2 => String) = defaultToString,
    f3: (T3 => String) = defaultToString,
    f4: (T4 => String) = defaultToString
  ) extends Msg {
    def apply(arg1: T1, arg2: T2, arg3: T3, arg4: T4)(using resolver: KeyResolver): String =
      getIt(resolver)(f1(arg1), f2(arg2), f3(arg3), f4(arg4))
    def arg1(f: T1 => String): Msg4[T1, T2, T3, T4]                                        = this.copy(f1 = f)
    def arg2(f: T2 => String): Msg4[T1, T2, T3, T4]                                        = this.copy(f2 = f)
    def arg3(f: T3 => String): Msg4[T1, T2, T3, T4]                                        = this.copy(f3 = f)
    def arg4(f: T4 => String): Msg4[T1, T2, T3, T4]                                        = this.copy(f4 = f)
  }

  case class Plural[M <: Msg] private[i18n] (key: String, msg: Resolution => M) {
    msg(Resolution(key))
    msg(Resolution(key + "_plural"))

    def apply(count: Int): M = {
      if count == 1 then {
        msg(Resolution(key + "_1").withFallback(key))
      } else {
        msg(Resolution(key + "_" + count).withFallback(key + "_plural"))
      }
    }
  }

  case class SelfPlural private[i18n] (key: String) {
    private val p: Plural[Msg1[Int]]                           = Plural(key, msg1(_))
    def apply(count: Int)(using resolver: KeyResolver): String = {
      p(count)(count)
    }
  }

  private def fieldKey(enclosing: sourcecode.Name): String =
    enclosing.value

  private val allResolutions = collection.mutable.ArrayBuffer[Resolution]()

  private def collect(resolution: Resolution): Resolution = {
    if !isIgnored(resolution.key) then allResolutions.append(resolution)
    resolution
  }

  protected def resolutions(): Seq[Resolution] = allResolutions.toSeq
}
