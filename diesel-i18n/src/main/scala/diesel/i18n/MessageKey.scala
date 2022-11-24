package diesel.i18n

case class MessageKey(key: String)

object MessageKey {
  import scala.language.experimental.macros

  implicit def messageKey: MessageKey = macro messageKeyImpl

  private type Context = scala.reflect.macros.blackbox.Context
  def messageKeyImpl(c: Context): c.Expr[MessageKey] = {
    import c.universe._
    var owner      = enclosingOwner(c)
    while (owner.isSynthetic) owner = owner.owner
    val simpleName = owner.name.decodedName.toString().trim()
    c.Expr[MessageKey](q"""${c.prefix}($simpleName)""")
  }

  private def enclosingOwner(c: Context) = c.internal.enclosingOwner
}
