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
 * This class is the type of expressions used to exit non-locally from -
 * <i>i.e.</i>, other than "falling off" its end of - an <i>exitable</i>
 * <a href="Abstraction.html"><tt>Abstraction</tt></a>.  The operational
 * semantics is simple: exit the current (<i>latest pushed</i>) exitable
 * closure; in other words exactly like a regular <tt>RETURN</tt> instruction
 * except that the state restored is that of the latest saved
 * <i>exitable</i> state. As for type-checking, the "exited-with"
 * value's type must be the same as with (<i>n.b.</i>, if subtyping,
 * compatible) the return type of the enclosing <i>exitable</i>
 * abstraction.
 */
public class ExitWithValue extends ProtoExpression
{
  private Expression _value;
  private boolean _typeAsValue = true;

  public ExitWithValue (Expression value)
    {
      _value = value;
    }

  public ExitWithValue (Expression value, boolean typeAsValue)
    {
      _value = value;
      _typeAsValue = typeAsValue;
    }

  public final Expression value ()
    {
      return _value;
    }

  public final Expression copy ()
    {
      return new ExitWithValue(_value.copy(),_typeAsValue);
    }

  public final Expression typedCopy ()
    {
      return new ExitWithValue(_value.typedCopy(),_typeAsValue).addTypes(this);
    }

  public final int numberOfSubexpressions ()
    {
      return 1;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      if (n == 0)
        return _value;

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      if (n == 0)
        _value = expression;
      else
        throw new NoSuchSubexpressionException(this,n);

      return this;
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      setCheckedType(type().copy());
      _value.setCheckedType();
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      _value.typeCheck(typeChecker);
      typeChecker.prove(new CheckExitableGoal(_value.type(),this));
      if (_typeAsValue)
        typeChecker.unify(_type,_value.type(),this);
    }

  public final void compile (Compiler compiler)
    {
      _value.compile(compiler);

      switch (_value.checkedType().boxSort())
        {
        case Type.VOID_SORT:
          compiler.generate(Instruction.NL_RETURN_VOID);
          return;

        case Type.INT_SORT:
          compiler.generate(Instruction.NL_RETURN_I);
          return;

        case Type.REAL_SORT:
          compiler.generate(Instruction.NL_RETURN_R);
          return;

        default:
          compiler.generate(Instruction.NL_RETURN_O);
          return;
        }
    }

  public final String toString ()
    {
      if (_value.checkedType().isVoid())
        return "return";
      
      return "return "+_value;
    }

}
