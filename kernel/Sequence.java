//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Wed Jun 20 14:29:51 2012 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import java.util.AbstractList;

import hlt.language.design.types.*;
import hlt.language.design.instructions.*;

/**
 * This class is the class of sequences of expressions.
 *
 */
public class Sequence extends Expression
{
  /**
   * The expressions in this sequence.
   */
  protected Expression[] _expressions;

  /**
   * Constructs a sequence with the specified list of expressions.
   */
  public Sequence (AbstractList expressions)
    {
      _expressions = new Expression[expressions.size()];

      for (int i=0; i<_expressions.length; i++)
        _expressions[i] = (Expression)expressions.get(i);
    }

  public Sequence (Expression[] expressions)
    {
      _expressions = expressions;
    }

  public Sequence (Expression fst, Expression snd)
    {
      Expression[] seq = { fst, snd };
      _expressions = seq;
    }

  public Expression copy ()
    {
      Expression[] expressions = new Expression[_expressions.length];
      for (int i=expressions.length; i-->0;)
        expressions[i] = _expressions[i].copy();

      return new Sequence(expressions);
    }

  public Expression typedCopy ()
    {
      Expression[] expressions = new Expression[_expressions.length];
      for (int i=expressions.length; i-->0;)
        expressions[i] = _expressions[i].typedCopy();

      return new Sequence(expressions).addTypes(this);
    }

  public final int numberOfSubexpressions ()
    {
      return _expressions.length;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      if (n >= 0 && n < _expressions.length)
        return _expressions[n];

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression) throws NoSuchSubexpressionException
    {
      if (n >= 0 && n < _expressions.length)
        _expressions[n] = expression;
      else
        throw new NoSuchSubexpressionException(this,n);

      return this;
    }

  /**
   * The type of a sequence of expressions is that of the last one.
   */
  public final Type type ()
    {
      return _expressions[_expressions.length-1].type();
    }

  /**
   * No-op...
   */
  public final void setType (Type type)
    {
    }

  /**
   * The type reference of a sequence of expressions is that of the last one.
   */
  public final Type typeRef ()
    {
      return _expressions[_expressions.length-1].typeRef();
    }

  /**
   * The checked type of a sequence of expressions is that of the last one.
   */
  public final Type checkedType ()
    {
      return _expressions[_expressions.length-1].checkedType();
    }

  /**
   * Sets the checked Type of all expressions of this sequence.
   */
  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      for (int i=0; i<_expressions.length; i++)
        _expressions[i].setCheckedType();
    }

  /**
   * No-op...
   */
  public final void setCheckedType (Type type)
    {
    }

  /**
   * Type-checks all expressions in this sequence in the context
   * of the specified <a href="TypeChecker.html"> <tt>TypeChecker</tt></a>.
   */
  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      for (int i=0; i<_expressions.length; i++)
        _expressions[i].typeCheck(typeChecker);
    }
    
  /**
   * Compiles this sequence of expressions in the context of the specified
   * <a href="Compiler.html"><tt>Compiler</tt></a>.
   */
  public void compile (Compiler compiler)
    {
      for (int i=0; i<_expressions.length; i++)
        {
          _expressions[i].compile(compiler);
          if (i<_expressions.length-1)
            compiler.generateStackPop(_expressions[i].boxSort());
        }
    }
    
  public final String toString ()
    {
      StringBuilder buf = new StringBuilder("{ ");

      for (int i=0; i<_expressions.length; i++)
        buf.append(_expressions[i]+"; ");

      buf.append("}");

      return buf.toString();
    }
}


