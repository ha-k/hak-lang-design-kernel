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
 * This is an abstract class for collection-denoting values. It is subclessed
 * by three concrete collection classes:
 * <ul>
 * <li><a href="NewSet.html"><tt>NewSet</tt></a>
 * <li><a href="NewBag.html"><tt>NewBag</tt></a>
 * <li><a href="NewList.html"><tt>NewList</tt></a>
 * </ul>
 */

abstract public class NewCollection extends ProtoExpression 
{
  /**
   * This is the collection's <i>base type</i> (<i>i.e.</i>, the type of its elements).
   */
  protected Type _baseType;

  /**
   * This array contains expressions denoting elements of this collection.
   */
  protected Expression[] _elements;

  public final int kind ()
    {
      return _kind;
    }

  protected int _kind;

  abstract protected NewCollection newCollection (Type baseType, Expression[] elements);

  public final Expression copy ()
    {
      if (_elements == null)
        return this;
      
      Expression[] elements = new Expression[_elements.length];
      for (int i=elements.length; i-->0;)
        elements[i] = _elements[i].copy();

      return newCollection(_baseType,elements);
    }

  public final Expression typedCopy ()
    {
      if (_elements == null)
        return this;
      
      Expression[] elements = new Expression[_elements.length];
      for (int i=elements.length; i-->0;)
        elements[i] = _elements[i].typedCopy();

      return newCollection(_baseType,elements).addTypes(this);
    }

  public final int numberOfSubexpressions ()
    {
      return _elements == null ? 0 : _elements.length;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      if (_elements == null || n < 0 || n >= _elements.length)
        throw new NoSuchSubexpressionException(this,n);

      return _elements[n];
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      if (_elements == null || n < 0 || n >= _elements.length)
        throw new NoSuchSubexpressionException(this,n);
      else
        _elements[n] = expression;

      return this;
    }

  public final Expression[] elements ()
    {
      return _elements;
    }

  public final Type baseType ()
    {
      return _baseType.value();
    }

  public final Type baseTypeRef ()
    {
      return _baseType;
    }

  public final void setCheckedType ()    
    {
      if (setCheckedTypeLocked()) return;
      for (int i = numberOfSubexpressions(); i--> 0;)
        _elements[i].setCheckedType();

      setCheckedType(type().copy());
    }
}
