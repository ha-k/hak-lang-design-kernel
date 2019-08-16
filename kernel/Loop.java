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

/**
 * This class is the class of conditional looping expressions.
 *
 */
public class Loop extends Expression
{
  /**
   * The loop's condition being assigned.
   */
  private Expression _condition;

  /**
   * The loop's body.
   */
  private Expression _body;

  /**
   * Constructs a loop with the specified condition and body.
   */
  public Loop (Expression condition, Expression body)
    {
      _condition = condition;
      _body = body; 
    }

  public final Expression copy ()
    {
      return new Loop(_condition.copy(),_body.copy());
    }

  public final Expression typedCopy ()
    {
      return new Loop(_condition.typedCopy(),_body.typedCopy()).addTypes(this);
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
          return _condition;
        case 1:
          return _body;
        }

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0:
          _condition = expression;
          break;
        case 1:
          _body = expression;
          break;
        default:
          throw new NoSuchSubexpressionException(this,n);
        }

      return this;
    }

  /**
   * The type of a loop is void.
   */
  public final Type type ()
    {
      return Type.VOID;
    }

  public final void setType (Type type)
    {
      throw new UnsupportedOperationException("setType(Type) in class "+Misc.simpleClassName(this));
    }

  /**
   * The type reference of a loop is its type.
   */
  public final Type typeRef ()
    {
      return type();
    }

  /**
   * The checked type of a loop is its type.
   */
  public final Type checkedType ()
    {
      return type();
    }

  /**
   * Sets the checked type of the condition and body.
   */
  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      _condition.setCheckedType();
      _body.setCheckedType();
    }

  /**
   * No-op...
   */
  public final void setCheckedType (Type type)
    {
    }

  /**
   * Type-checks this loop in the context of the specified
   * <a href="../types/TypeChecker.html"> <tt>TypeChecker</tt></a>.
   */
  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      _condition.typeCheck(Type.BOOLEAN(),typeChecker);   
      _body.typeCheck(typeChecker);
    }

  /**
   * Compiles this loop expression in the context of the specified
   * <a href="Compiler.html"><tt>Compiler</tt></a>.
   */
  public final void compile (Compiler compiler)
    {
      int loop = compiler.targetAddress();

      _condition.compile(compiler);
      if (_condition.checkedType().isBoxedType())
        compiler.generateUnwrapper(Type.INT_SORT);

      JumpOnFalse jof = new JumpOnFalse();
      compiler.generate(jof);

      _body.compile(compiler);
      compiler.generateStackPop(_body.boxSort());

      compiler.generate(new Jump(loop));

      jof.setAddress(compiler.targetAddress());
    }
    
  public final String toString ()
    {
      return "while " + _condition + " do " + _body;
    }

}
