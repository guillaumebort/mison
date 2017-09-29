package mison

import scala.language.experimental.macros

object Macros {

  /** A fast for loop with break support */
  def forloop(initialization: Int, termination: Int => Boolean, increment: Int => Int)(block: Int => Unit) = macro impl.forloop
  def loop(block: Unit) = macro impl.loop
  def break = ???

  object impl {
    import scala.reflect.macros.blackbox.Context

    def loop(c: Context)(block: c.Expr[Unit]): c.Expr[Unit] = {
      import c.universe._

      val continue = TermName(c.freshName("continue"))
      val block0 = optimize0(c)(block.tree, continue)

      val whileLoop =
        q"""
          var $continue: Boolean = true
          while($continue) {
            $block0
          }
        """

      c.Expr(whileLoop)
    }

    def forloop(c: Context)(initialization: c.Expr[Int], termination: c.Expr[Int => Boolean], increment: c.Expr[Int => Int])(block: c.Expr[Int => Unit]): c.Expr[Unit] = {
      import c.universe._

      val i = TermName(c.freshName("i"))
      val continue = TermName(c.freshName("continue"))
      val termination0 = optimize(c)(termination.tree, i, continue)
      val increment0 = optimize(c)(increment.tree, i, continue)
      val block0 = optimize(c)(block.tree, i, continue)

      val whileLoop =
        q"""
          var $i: Int = $initialization
          var $continue: Boolean = true
          while($termination0 && $continue) {
            $block0
            $i = $increment0
          }
        """

      c.Expr(whileLoop)
    }

    def optimize0(c: Context)(block: c.Tree, continue: c.TermName): c.Tree = {
      import c.universe._

      object Optimizer extends Transformer {
        override def transform(tree: Tree): Tree = tree match {
          case Select(_, TermName("break")) =>
            q"$continue = false"
          case _ =>
            super.transform(tree)
        }
      }

      c.resetLocalAttrs(Optimizer.transform(block))
    }

    def optimize(c: Context)(thunk: c.Tree, i: c.TermName, continue: c.TermName): c.Tree = {
      import c.universe._

      case class Optimizer(originalArg: TermName) extends Transformer {
        override def transform(tree: Tree): Tree = tree match {
          case Ident(`originalArg`) =>
            Ident(i)
          case Select(_, TermName("break")) =>
            q"$continue = false"
          case _ =>
            super.transform(tree)
        }
      }

      thunk match {
        case q"(..$params) => $expr" =>
          c.resetLocalAttrs(Optimizer(params.head.name).transform(expr))
        case _ =>
          sys.error("TODO")
      }
    }

  }

}