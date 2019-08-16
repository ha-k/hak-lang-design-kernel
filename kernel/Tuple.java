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
 * This is the class of tuple expressions whose components are identified by position.
 */
public class Tuple extends ProtoExpression
{
  protected Expression[] _components;

  protected Tuple ()
    {
    }

  public final static Tuple EMPTY = (Tuple)new Tuple().addType(TupleType.EMPTY);

  private Tuple (Expression[] components)
    {
      _components = components;
    }

  public final static Tuple newTuple (AbstractList components)
    {
      if (components == null || components.size() == 0)
        return EMPTY;

      Expression[] expressions = new Expression[components.size()];
      for (int i=expressions.length; i-->0;)
        expressions[i] = (Expression)components.get(i);

      return new Tuple(expressions);
    }

  public Expression copy ()
    {
      if (_components == null)
        return this;

      Expression[] components = new Expression[_components.length];
      for (int i=components.length; i-->0;)
        components[i] = _components[i].copy();

      return new Tuple(components);
    }

  public Expression typedCopy ()
    {
      if (_components == null)
        return this;

      Expression[] components = new Expression[_components.length];
      for (int i=components.length; i-->0;)
        components[i] = _components[i].typedCopy();

      return new Tuple(components).addTypes(this);
    }

  public final int numberOfSubexpressions ()
    {
      return _components == null ? 0 : _components.length;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      if (n >= 0 && n < _components.length)
        return _components[n];

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      if (n >= 0 && n < _components.length)
        _components[n] = expression;
      else
        throw new NoSuchSubexpressionException(this,n);

      return this;
    }

  public final int dimension ()
    {
      return _components == null ? 0 : _components.length;
    }

  public final Expression component (int i)
    {
      return _components[i];
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      for (int i=dimension(); i-->0;)
        _components[i].setCheckedType();
      setCheckedType(type().copy());
    }

  public void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      Type[] componentTypes = new Type[dimension()];

      for (int i=dimension(); i-->0;)
        {
          _components[i].typeCheck(componentTypes[i] = new TypeParameter(),typeChecker);
          typeChecker.disallowVoid(componentTypes[i].value(),_components[i],"tuple component");
        }

      typeChecker.typeCheck(this,TupleType.newTupleType(componentTypes));
    }

  public final void compile (Compiler compiler)
    {
      int intDim = 0, realDim = 0, objectDim = 0;
      TupleType tupleType = (TupleType)checkedType().actualType();

      for (int i=dimension(); i-->0;)
        {
          _components[i].compile(compiler);

          switch (tupleType.component(i).boxSort())
            {
            case Type.INT_SORT:
              intDim++;
              continue;
            case Type.REAL_SORT:
              realDim++;
              continue;
            case Type.OBJECT_SORT:
              objectDim++;
            }
        }

      compiler.generate(new PushTuple(intDim,realDim,objectDim));
    }

  public String toString ()
    {
      StringBuilder buf = new StringBuilder("<");

      for (int i=0; i<dimension(); i++)
        buf.append(component(i)+(i==dimension()-1?"":","));

      return buf.append(">").toString();
    }
}
