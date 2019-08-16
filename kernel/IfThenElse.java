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

public class IfThenElse extends ProtoExpression
{
  private Expression _condition;
  private Expression _thenExpression;
  private Expression _elseExpression;

  public IfThenElse (Expression condition, Expression thenExpression, Expression elseExpression)
    {
      _condition = condition;
      _thenExpression = thenExpression;
      _elseExpression = elseExpression;
    }

  public final Expression copy ()
    {
      return new IfThenElse(_condition.copy(),_thenExpression.copy(),_elseExpression.copy());
    }

  public final Expression typedCopy ()
    {
      return new IfThenElse(_condition.typedCopy(),
                            _thenExpression.typedCopy(),
                            _elseExpression.typedCopy()).addTypes(this);
    }

  public final int numberOfSubexpressions ()
    {
      return 3;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0:
          return _condition;
        case 1:
          return _thenExpression;
        case 2:
          return _elseExpression;
        }

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression) throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0:
          _condition = expression;
          break;
        case 1:
          _thenExpression = expression;
          break;
        case 2:
          _elseExpression = expression;
          break;
        default:
          throw new NoSuchSubexpressionException(this,n);
        }

      return this;
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      setCheckedType(type().copy());
      _condition.setCheckedType();
      _thenExpression.setCheckedType();
      _elseExpression.setCheckedType();
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      _condition.typeCheck(Type.BOOLEAN(),typeChecker);
      _thenExpression.typeCheck(typeRef(),typeChecker);
      _elseExpression.typeCheck(typeRef(),typeChecker);
    }      

  public final void compile (Compiler compiler)
    {
      _condition.compile(compiler);
      if (_condition.checkedType().isBoxedType())
        compiler.generateUnwrapper(Type.INT_SORT);

      JumpOnFalse jof = new JumpOnFalse();
      compiler.generate(jof);

      _thenExpression.compile(compiler);

      Jump jmp = new Jump();
      compiler.generate(jmp);

      jof.setAddress(compiler.targetAddress());

      _elseExpression.compile(compiler);

      jmp.setAddress(compiler.targetAddress());
    }

  final public String toString ()
    {
      return "if " + _condition + " then " + _thenExpression + " else " + _elseExpression;
    }

}
