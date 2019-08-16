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
 * This kernel expression is used on an expression and defined opaque type, such that
 * the expression's type is the definition of the defined opaque type.  The semantics
 * consists simply in "hiding" the expression's type into the opaque type;
 * <i>i.e.</i>, its value is that of the expression seen as having the opaque
 * type. The inverse operation, which consists in opening an expression's opaque type
 * into its definition type is <a href="OpenType.html"><tt>OpenType</tt></a>.
 */
public class HideType extends ProtoExpression
{
  private Expression _expression;
  private Type _opaqueType;

  public HideType (Expression expression, Type type)
    {
      _expression = expression;
      _opaqueType = type;
    }

  public final Expression copy ()
    {
      return new HideType(_expression.copy(),_opaqueType);
    }

  public final Expression typedCopy ()
    {
      return new HideType(_expression.typedCopy(),_opaqueType).addTypes(this);
    }

  public final Expression expression ()
    {
      return _expression;
    }

  public final Type opaqueType ()
    {
      return _opaqueType;
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

  public final Expression setSubexpression (int n, Expression expression) throws NoSuchSubexpressionException
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

      if (_opaqueType.kind() != Type.DEFINED)
        typeChecker.error(locate(new TypingErrorException("can't hide into a non-opaque type: "
                                                          +_opaqueType)));

      _expression.typeCheck(((DefinedType)_opaqueType).definition(),typeChecker);
      typeChecker.unify(_type,_opaqueType,this);
    }

  public final void compile (Compiler compiler)
    {
      _expression.compile(compiler);
    }

  public final String toString ()
    {
      return _expression+" as "+_opaqueType;
    }

}

