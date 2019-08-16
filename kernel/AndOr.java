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

import hlt.language.tools.Misc;

public abstract class AndOr extends Expression
{
  Type _type = Type.BOOLEAN();

  protected Expression _left;
  protected Expression _rite;

  public AndOr (Expression left, Expression rite)
    {
      _left = left;
      _rite = rite;      
    }

  public final Expression left ()
    {
      return _left;
    }

  public final Expression right ()
    {
      return _rite;
    }

  public final int numberOfSubexpressions ()
    {
      return 2;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0:
          return _left;
        case 1:
          return _rite;
        }

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0:
          _left = expression;
          break;
        case 1:
          _rite = expression;
          break;
        default:
          throw new NoSuchSubexpressionException(this,n);  
        }

      return this;
    }

  public abstract void compile (Compiler compiler);

  public final void setType (Type type)
    {
      throw new UnsupportedOperationException("setType(Type) in class "+Misc.simpleClassName(this));
    }

  public final Type type ()
    {
      return _type.value();
    }

  public final Type typeRef ()
    {
      return _type;
    }

  public final Type checkedType ()
    {
      return _type;
    }

  public final void setCheckedType (Type type)
    {
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      _left.setCheckedType();
      _rite.setCheckedType();
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      _left.typeCheck(Type.BOOLEAN(),typeChecker);
      _rite.typeCheck(Type.BOOLEAN(),typeChecker);
    }
}
