//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Wed Jun 20 14:29:51 2012 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.design.types.*;

/**
 * This kernel expression is used on an expression whose type is a defined opaque type.
 * Its semantics consists simply in "opening" the expression's opaque type; <i>i.e.</i>,
 * its value is that of the expression seen as having the hidden type. The inverse operation,
 * which consists in hiding an expression's type into a given defined opaque type having
 * that type as definition, is <a href="HideType.html"><tt>HideType</tt></a>.
 */
public class OpenType extends ProtoExpression
{
  private Expression _expression;

  public OpenType (Expression expression)
    {
      _expression = expression;
    }

  public final Expression expression ()
    {
      return _expression;
    }

  public final Expression copy ()
    {
      return new OpenType(_expression.copy());
    }

  public final Expression typedCopy ()
    {
      return new OpenType(_expression.typedCopy()).addTypes(this);
    }

  public final int numberOfSubexpressions ()
    {
      return 1;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      if (n == 0)
        return _expression;

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      if (n == 0)
        _expression = expression;
      else
        throw new NoSuchSubexpressionException(this,n);

      return this;
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      setCheckedType(type().copy());
      _expression.setCheckedType();
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      _expression.typeCheck(typeChecker);

      if (_expression.type().kind() != Type.DEFINED)
        typeChecker.error(locate(new TypingErrorException("can't open a non-opaque type: "
                                                          +_expression.type())));

      typeChecker.unify(_type,((DefinedType)_expression.type()).definition(),this);
    }

  public final void compile (Compiler compiler)
    {
      _expression.compile(compiler);
    }

  public final String toString ()
    {
      return "OpenType("+_expression+")";
    }

}
