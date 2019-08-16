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
import hlt.language.design.instructions.*;

/**
 * This class is the mother class of assignment expressions.
 */
public abstract class Assignment extends ProtoExpression
{
  /**
   * The location being set.
   */
  protected Expression _lhs;

  /**
   * The expression whose value is assigned to the location being set.
   */
  protected Expression _rhs;

  public final int numberOfSubexpressions ()
    {
      return 2;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0:
          return _lhs;
        case 1:
          return _rhs;
        }

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0:
          _lhs = expression;
          break;
        case 1:
          _rhs = expression;
          break;
        default:
          throw new NoSuchSubexpressionException(this,n);
        }

      return this;
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      _checkedType = type().copy();
      _lhs.setCheckedType();
      _rhs.setCheckedType();
    }

  /**
   * Type-checks this assignment in the context of the specified
   * <a href="../types/TypeChecker.html"> <tt>TypeChecker</tt></a>.
   */
  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      Type type = VOID_ASSIGNMENTS ? _rhs.typeRef() : _type;

      _lhs.typeCheck(type,typeChecker);
      _rhs.typeCheck(type,typeChecker);

      typeChecker.disallowVoid(type.value(),this,"assigned value");

      if (VOID_ASSIGNMENTS)
        typeChecker.unify(_type,Type.VOID,this);
    }

  public final String toString ()
    {
      return _lhs + " = " + _rhs;
    }

}
