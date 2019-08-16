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
 * This class is the class of field update expressions. This kind of update
 * concerns both named tuples and class object instances. The distinction is
 * made according to the type of the object.
 */
public class FieldUpdate extends ProtoExpression
{
  /**
   * The object expression whose field is updated.
   */
  private Expression _object;

  /**
   * The field global symbol.
   */
  private Global _field;

  /**
   * The expression whose value is assigned to the object's field.
   */
  private Expression _value;

  /**
   * The tuple update expression this corresponds to if not a class instance
   * object field update.
   */
  private Expression _tupleUpdate;

  /**
   * Returns the tuple update expression this corresponds to if not a class instance
   * object field update; <tt>null</tt> otherwise.
   */
  public final Expression tupleUpdate ()
    {
      return _tupleUpdate;
    }

  /**
   * Constructs a field update with the specified object expression, field
   * name and value expression.
   */
  public FieldUpdate (Expression object, Global field, Expression value)
    {
      _object = object;
      _field = field;
      _value = value; 
    }

  public final Expression copy ()
    {
      return new FieldUpdate(_object.copy(),(Global)_field.copy(),_value.copy());
    }

  public final Expression typedCopy ()
    {
      return new FieldUpdate(_object.typedCopy(),(Global)_field.typedCopy(),_value.typedCopy());
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
          return _object;
        case 1:
          return _field;
        case 2:
          return _value;
        }

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      switch (n)
        {
        case 0:
          _object = expression;
          break;
        case 1:
          _field = (Global)expression;
          break;
        case 2:
          _value = expression;
          break;
        default:
          throw new NoSuchSubexpressionException(this,n);
        }

      return this;
    }

  /**
   * Sets the checked type of the expression of this field update.
   */
  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      setCheckedType(type().copy());
      if (_tupleUpdate == null)
        {
          _object.setCheckedType();
          _field.setCheckedType();
          _value.setCheckedType();
        }
      else
        _tupleUpdate.setCheckedType();
    }

  private boolean _fieldIsBoxed = false;
  private boolean _valueIsBoxed = false;

  /**
   * Type-checks this field update in the context of the specified <a
   * href="../types/TypeChecker.html"> <tt>TypeChecker</tt></a>.  <b>NB:</b>
   * Like assignments, the type of a field update is that of the <b><i>value</i></b>.
   * <font color="red"><i>However</i></font>, ...
   */
  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      Expression field = new Application(_field,_object);
      field.typeCheck(typeChecker);

      if (_object.type().actualType() instanceof NamedTupleType)
        {
          _tupleUpdate = new TupleUpdate(new TupleProjection(_object,_field.name()),
                                         _value);

          if (VOID_ASSIGNMENTS)
            {
              typeChecker.unify(_type,Type.VOID,this);
              _tupleUpdate.typeCheck(typeChecker);
            }
          else
            _tupleUpdate.typeCheck(_type,typeChecker);

          return;
        }

      _fieldIsBoxed = field.type().isBoxedType();               // DANGER: this should be trailed!

      _value.typeCheck(typeChecker);
      _valueIsBoxed = _value.type().isBoxedType();              // DANGER: this should be trailed!

      if (!(_object.type() instanceof ClassType))
        throw locate(new AssignmentErrorException("bad field update: "+_object.type()+
                                                  " is not a class type"));
      if (!_field.codeEntry().isField())
        throw locate(new AssignmentErrorException("bad field update: "+_field+
                                                  " is not a field for class type "+
                                                  _object.type()));

      typeChecker.disallowVoid(_value.type(),_value,"assigned value");

      if (VOID_ASSIGNMENTS)
        {
          typeChecker.unify(field.typeRef(),_value.typeRef());
          typeChecker.typeCheck(this,Type.VOID);
        }
      else
        {
          typeChecker.typeCheck(this,field.typeRef());
          typeChecker.typeCheck(this,_value.typeRef());
          type().setBoxed(_fieldIsBoxed);
        }
    }

  /**
   * Compiles this field update of expressions in the context of the specified
   * <a href="Compiler.html"><tt>Compiler</tt></a>.
   */
  public final void compile (Compiler compiler)
    {
      if (_tupleUpdate != null)
        {
          _tupleUpdate.compile(compiler);
          return;
        }

      DefinedEntry fieldEntry = _field.definedEntry();
      byte sort = _fieldIsBoxed ? Type.OBJECT_SORT : _value.sort();

      _value.compile(compiler);

      if (_fieldIsBoxed)
        {
          if (!_valueIsBoxed)
            compiler.generateWrapper(_value.sort());
        }
      else
        if (_valueIsBoxed)
          compiler.generateUnwrapper(_value.sort());

      _object.compile(compiler);

      switch(sort)
        {
        case Type.INT_SORT:
          compiler.generate(new SetIntField(fieldEntry));
          break;
        case Type.REAL_SORT:
          compiler.generate(new SetRealField(fieldEntry));
          break;
        default:
          compiler.generate(new SetObjectField(fieldEntry));
        }

      if (VOID_ASSIGNMENTS)
        compiler.generateStackPop(sort);
    }
    
  public final String toString ()
    {
      return _object + "." + _field + " = " + _value;
    }

}
