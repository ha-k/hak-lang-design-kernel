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
 * This is the class of tuple projection expressions.
 */
public class TupleProjection extends ProtoExpression
{
  private Expression _tuple;
  private Constant _field;
  private int _position;

  public TupleProjection (Expression tuple, Constant field)
    {
      _tuple = tuple;
      _field = field;
    }

  public TupleProjection (Expression tuple, int position)
    {
      this(tuple,new Int(position));
    }

  public TupleProjection (Expression tuple, String field)
    {
      this(tuple,new StringConstant(field));
    }

  public final Expression copy ()
    {
      return new TupleProjection(_tuple.copy(),_field);
    }

  public final Expression typedCopy ()
    {
      return new TupleProjection(_tuple.typedCopy(),_field).addTypes(this);
    }

  public final Expression tuple ()
    {
      return _tuple;
    }

  public final Constant field ()
    {
      return _field;
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
          return _tuple;
        case 1:
          return _field;
        }

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0:
          _tuple = expression;
          break;
        case 1:
          _field = (Constant)expression;
          break;
        default:
          throw new NoSuchSubexpressionException(this,n);
        }

      return this;
    }

  /**
   * Returns <tt>true</tt> iff this is of the form <tt>x.p1.p2.....pk</tt> where
   * <tt>x</tt> is a dummy occurrence of the specified parameter. If the occurrence
   * is a dummy bearing the parameter's name, it must also be sanitized into a
   * <a href="DummyLocal.html"><tt>dummy local</tt></a> occurrence of this parameter.
   * This is necessary in order to avoid that it be sanitized incorrectly later as
   * a slicing filter is taken outside its original binding scope by the processing
   * done by a <a href="Comprehension.html"><tt>Comprehension</tt></a>.   
   */
  public final boolean slicesParameter (Parameter parameter)
    {
      if (_tuple instanceof TupleProjection)
        return ((TupleProjection)_tuple).slicesParameter(parameter);

      if (_tuple instanceof Dummy
          && ((Dummy)_tuple).name() == parameter.name())
        {
          _tuple = new DummyLocal(parameter).addTypes(_tuple).setExtent(_tuple);
          return true;
        }

      return false;
    }

  public final int depth ()
    {
      return 1 + (_tuple instanceof TupleProjection ? ((TupleProjection)_tuple).depth() : 0);
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      _tuple.setCheckedType();
      setCheckedType(type().copy());
    }

  public final void setPosition (String field)
    {
      _position = ((NamedTupleType)_tuple.type()).position(field);
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      _tuple.typeCheck(typeChecker);

      if (!(_tuple.type().actualType() instanceof TupleType))
        typeChecker.error(new TypingErrorException("bad tuple type: "+_tuple.type()),this);

      // the following line is to allow projection on hidden tuple types
      TupleType tupleType = (TupleType)_tuple.type().actualType();

      if (tupleType.dimension() == 0)
        typeChecker.error(new TypingErrorException("empty tuple projection"),this);

      if (_field instanceof StringConstant)
        if (tupleType.kind() != Type.NAMED_TUPLE)
          typeChecker.error(new TypingErrorException("bad tuple field position: "+_field+
                                                     " should be an integer constant in [1,"+
                                                     tupleType.dimension()+"] range"),_field);
        else
          {
            _position = ((NamedTupleType)tupleType).position(((StringConstant)_field).stringValue());
            if (_position == 0)
              typeChecker.error(new TypingErrorException("bad tuple field name: "+_field+
                                                         " is not in "+
                                                         ((NamedTupleType)tupleType).fieldSet()),_field);
          }
      else
        if (_field instanceof Int)
          if (tupleType.kind() == Type.NAMED_TUPLE)
            if (TypeChecker.ALLOWS_POSITIONAL_NAMED_TUPLES)
              {
                _position = ((NamedTupleType)tupleType).fieldPosition(((Int)_field).value());
                if (_position <= 0 || _position > tupleType.dimension())
                  typeChecker.error(new TypingErrorException("bad tuple field position: "+_field+
                                                             " is not in [1,"+
                                                             tupleType.dimension()+"] range"),_field);
              }
            else
              typeChecker.error(new TypingErrorException("bad tuple field name: "+_field+
                                                         " should be a name in "+
                                                         ((NamedTupleType)tupleType).fieldSet()),_field);
          else
            {
              _position = ((Int)_field).value();
              if (_position <= 0 || _position > tupleType.dimension())
                typeChecker.error(new TypingErrorException("bad tuple field position: "+_field+
                                                           " is not in [1,"+
                                                           tupleType.dimension()+"] range"),_field);
            }
        else
          typeChecker.error(new TypingErrorException("bad tuple field: "+_field),_field);

      typeChecker.typeCheck(this,tupleType.component(_position-1));
    }

  public final void compile (Compiler compiler)
    {
      _tuple.compile(compiler);

      switch (boxSort())
        {
        case Type.INT_SORT:
          compiler.generate(new GetIntTupleComponent(offset()));
          return;
        case Type.REAL_SORT:
          compiler.generate(new GetRealTupleComponent(offset()));
          return;
        case Type.OBJECT_SORT:
          compiler.generate(new GetObjectTupleComponent(offset()));
        }
    }

  public final int offset ()
    {
      int offset = 0;
      byte sort = boxSort();
      TupleType tupleType = (TupleType)_tuple.checkedType().actualType();

      for (int i=0; i<_position; i++)
        if (tupleType.component(i).boxSort() == sort)
          offset++;

      return offset;
    }

  public final String toString ()
    {
      return _tuple+"@"+_field;
    }
}
