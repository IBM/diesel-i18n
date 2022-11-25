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
