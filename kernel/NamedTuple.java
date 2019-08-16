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
import hlt.language.tools.Misc;

/**
 * This is the class representing tuples whose components are identified by field names.
 * It derives from <a href="Tuple.html"><tt>Tuple</tt></a>, and in addition keeps an
 * array of <a href="TupleFieldName.html"><tt>TupleFieldName</tt></a>s recording the
 * names.  The reason for using <tt>TupleFieldName</tt>s rather than <tt>String</tt>s is
 * so that the array of field names may be sorted lexicographically for the purpose of
 * normalizing all named tuples and named tuple types, while recording their original
 * indices for displaying components in their "original" order. Note that this notion is
 * rather fuzzy as it corresponds to the ordering of components found by the constructor
 * (which is meant to be used by a parser) and can only be that of a specific written
 * named tuple (type) occurrence. Thus, this order may only be <i>one</i> among many if
 * distinct occurrences of this type, or tuples of this type, are written using
 * differring orders of components. Nevertheless, for consistently written tuples, the
 * components will be displayed in the expected order.
 *
 * <p>
 *
 * <b>N.B.</b>: the names in a named tuple may not be overloaded (<i>i.e.</i>, the same
 *              name cannot appear more than once in a named tuple with distinct types.
 */

public class NamedTuple extends Tuple
{
  private TupleFieldName[] _fields;
  // Should the field names be considered subexpressions? In this version, they ain't!

  private NamedTuple (Expression[] components, TupleFieldName[] fields)
    {
      _components = components;
      _fields = fields;
    }

  public NamedTuple (AbstractList components, AbstractList names)
    {
      // NB: this assumes that both lists are non-empty, and have the same size.

      _fields = new TupleFieldName[names.size()];
      for (int i=0; i<_fields.length; i++)
        _fields[i] = new TupleFieldName((String)names.get(i),i);
      if(_fields.length>0)
          Misc.sort(_fields);

     _components = new Expression[components.size()];
      for (int i=0; i<_components.length; i++)
        _components[i] = (Expression)components.get(_fields[i].index());
    }

  public final Expression copy ()
    {
      if (_components == null)
        return this;

      Expression[] components = new Expression[_components.length];
      for (int i=components.length; i-->0;)
        components[i] = _components[i].copy();

      return new NamedTuple(components,_fields);
    }

  public final Expression typedCopy ()
    {
      if (_components == null)
        return this;

      Expression[] components = new Expression[_components.length];
      for (int i=components.length; i-->0;)
        components[i] = _components[i].typedCopy();

      return new NamedTuple(components,_fields).addTypes(this);
    }

  public final TupleFieldName[] fields ()
    {
      return _fields;
    }

  /**
   * Returns the field name originally in the specified position; <tt>null</tt>
   * if position is not legal.
   */
  public final TupleFieldName field (int n)
    {
      for (int i=0; i<_fields.length; i++)
        if (_fields[i].index() == n)
          return _fields[i];

      return null;
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      _checkDuplicateFields(typeChecker);

      Type[] componentTypes = new Type[dimension()];

      for (int i=dimension(); i-->0;)
        {
          _components[i].typeCheck(componentTypes[i] = new TypeParameter(),typeChecker);
          typeChecker.disallowVoid(componentTypes[i].value(),_components[i],"tuple component");
        }

      typeChecker.typeCheck(this,new NamedTupleType(componentTypes,_fields));
    }

  private final void _checkDuplicateFields (TypeChecker typeChecker) throws TypingErrorException
    {
      TupleFieldName field = null;
      for (int i=_fields.length; i-->0;)
        {
          if (_fields[i].equals(field))
            typeChecker.error(new TypingErrorException("duplicate field: "+field).setExtent(this));
          field = _fields[i];
        }
    }

  public final String toString ()
    {
      int[] index = new int[dimension()];

      for (int i=0; i<index.length; i++)
        index[_fields[i].index()] = i;

      StringBuilder buf = new StringBuilder("<");

      for (int i=0; i<dimension(); i++)
        buf.append(_fields[index[i]])
           .append("=")
           .append(component(index[i]))
           .append(i==dimension()-1?"":",");

      return buf.append(">").toString();
    }
}
