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

import java.util.AbstractList;
import java.util.HashSet;
import java.util.HashMap;

/**
 * This class represents a scope for local variables. It may be seen as a lambda
 * abstraction that never needs to be lexically closed. It is used for creating a
 * <a href="Let.html"><tt>Let</tt></a> block as the application of such a
 * <tt>Scope</tt> to its local variable's initial values. As a result, it can never
 * be exported outside its enclosing scope, and therefore it can dispense with the
 * whole machinery of creating and maintaining lexical frames. Also, a scope does
 * not allow non-local exits.
 * <p>
 * <i><b>NB:</b> This class is not meant to be put in unknowing hands: using it
 * without being fully aware of its functionality may cause seriously wrong
 * behavior - if you do not know for sure that this is what you want, then use <a
 * href="Abstraction.html"> <tt>Abstraction</tt></a> instead!</i>
 */
public class Scope extends ProtoExpression
{
  protected Parameter[] _parameters;
  protected Expression _body;

  protected int _intArity;
  protected int _realArity;
  protected int _objectArity;

  public Scope (Parameter[] parameters, Expression body)
    {
      _parameters = parameters;         // NB: all strings in this array must be interned!
      _body = body;
      _flatten();
    }

  public Scope (Parameter parameter, Expression body)
    {
      _parameters = new Parameter[1];
      _parameters[0] = parameter;
      _body = body;
      _flatten();
    }

  public Scope (String name, Expression body)
    {
      this(new Parameter(name.intern()),body);
    }

  public Scope (Expression body)
    {
      this(new Parameter(),body);
    }

  public Scope (AbstractList parameters, Expression body)
    {
      if (parameters == null || parameters.isEmpty())
        {
          _parameters = new Parameter[1];
          _parameters[0] = Parameter.VOID;
        }
      else
        {
          _parameters = new Parameter[parameters.size()];

          for (int i=0; i<_parameters.length; i++)
            _parameters[i] = new Parameter(((String)parameters.get(i)).intern());
        }
      
      _body = body;
      _flatten();
    }

  public Expression copy ()
    {
      Parameter[] parameters = new Parameter[_parameters.length];
      for (int i=parameters.length; i-->0;)
        parameters[i] = (Parameter)_parameters[i].copy();

      return new Scope(parameters,_body.copy());
    }

  public Expression typedCopy ()
    {
      Parameter[] parameters = new Parameter[_parameters.length];
      for (int i=parameters.length; i-->0;)
        parameters[i] = (Parameter)_parameters[i].typedCopy();

      return new Scope(parameters,_body.typedCopy()).addTypes(this);
    }

  protected void _flatten ()
    {
      if (_body instanceof Scope && !(_body instanceof Abstraction))
        {
          Parameter[] nestedParameters = ((Scope)_body).parameters();
          Parameter[] newParameters = new Parameter[_parameters.length+nestedParameters.length];

          for (int i=_parameters.length; i-->0;)
            newParameters[i] = _parameters[i];
          for (int i=_parameters.length; i<newParameters.length;i++)
            newParameters[i] = nestedParameters[i-_parameters.length];

          _parameters = newParameters;
          _body = ((Scope)_body).body();
        }
    }

  public final int arity ()
    {
      return _parameters.length;
    }

  public final int intArity ()
    {
      return _intArity;
    }

  public final int realArity ()
    {
      return _realArity;
    }

  public final int objectArity ()
    {
      return _objectArity;
    }

  public final int voidArity ()
    {
      return _parameters.length - _intArity - _realArity - _objectArity;
    }

  public final void setSortedArities ()
    {
      for (int i=0; i<_parameters.length; i++)
        switch (_parameters[i].boxSort())
          {
          case Type.INT_SORT:
            _intArity++;
            break;
          case Type.REAL_SORT:
            _realArity++;
            break;
          case Type.OBJECT_SORT:
            _objectArity++;
            break;
          }
    }

  public final int numberOfSubexpressions ()
    {
      return _parameters.length + 1;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      if (n >= 0 && n < _parameters.length)
        return _parameters[n];

      if (n == _parameters.length)
        return _body;

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression)
    throws NoSuchSubexpressionException
    {
      if (n >= 0 && n < _parameters.length)
        _parameters[n] = (Parameter)expression;
      else
        if (n == _parameters.length)
          _body = expression;
        else
          throw new NoSuchSubexpressionException(this,n);

      return this;
    }

  public final Parameter[] parameters ()
    {
      return _parameters;
    }

  public final Parameter parameter (int i)
    {
      return _parameters[i];
    }

  public final Expression body ()
    {
      return _body;
    }

  public final void setBody (Expression body)
    {
      _body = body;
    }

  public final void setCheckedType (Type type)
    {
      _checkedType = type;
      setSortedArities();
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;

      _checkedType = type().copy();
      for (int i=0; i<_parameters.length; i++)
        _parameters[i].setCheckedType();
      _body.setCheckedType();
      setSortedArities();
    }

  public void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      Type[] parameterTypes = new Type[arity()];

      for (int i=0; i<arity(); i++)
        {
          _parameters[i].typeCheck(typeChecker);
          parameterTypes[i] = _parameters[i].typeRef();
        }
      
      typeChecker.unify(_type,new FunctionType(parameterTypes,_body.typeRef()),this);
      _body.typeCheck(typeChecker);
    }

  public final boolean containsFreeName (String name)
    {
      for (int i = _parameters.length; i-->0;)
        if (name == _parameters[i].name()) return false;

      return _body.containsFreeName(name);
    }  

  private Expression _enclosingScope;

  final public Expression enclosingScope ()
    {
      return _enclosingScope;
    }

  /**
   * Sets this scope's enclosing scope link to the specified expression,
   * then visits the body expression to proceed linking up the scope
   * trees of its nested comprehensions to this, and returns the number
   * of such nested comprehensions.
   */
  final int linkScopeTree (Expression ancestor)
    {
      if (_scopeTreeIsLinked)
        return _nestedComprehensionCount;
        
      _enclosingScope = ancestor;
      _nestedComprehensionCount = _body.linkScopeTree(this);

      _scopeTreeIsLinked = true;

      return _nestedComprehensionCount;
    }

  public Expression substitute (HashMap substitution)
    {
      if (substitution.isEmpty())
        return this;

      Object[] values = new Object[_parameters.length];
      for (int i=_parameters.length; i-->0;)
        values[i] = substitution.remove(_parameters[i].name());

      if (!substitution.isEmpty())
        _body = _body.substitute(substitution);

      for (int i=_parameters.length; i-->0;)
        if (values[i] != null)
          substitution.put(_parameters[i].name(),values[i]);
      
      return this;
    }

  public final Expression sanitizeNames (ParameterStack parameters)
    {
      for (int i=0; i<_parameters.length; i++)
        parameters.push(_parameters[i]);

      _body = _body.sanitizeNames(parameters);

      for (int i=0; i<_parameters.length; i++)
        parameters.pop();

      return this;
    }

  protected boolean _isSortSanitized = false;

  public void sanitizeSorts (Enclosure enclosure)
    {
      if (_isSortSanitized) return;

      enclosure.push(this);
      _body.sanitizeSorts(enclosure);
      enclosure.pop();

      _isSortSanitized = true;
    }

  public Expression shiftOffsets (int intShift, int realShift, int objectShift,
                                  int intDepth, int realDepth, int objectDepth)
    {
      _body = _body.shiftOffsets(intShift,realShift,objectShift,
                                 intDepth+_intArity,realDepth+_realArity,objectDepth+_objectArity);

      return this;
    }

  protected PushScope _pushInstruction ()
    {
      return new PushScope(voidArity(),_intArity,_realArity,_objectArity);
    }

  public final void compile (Compiler compiler)
    {
      compiler.generate(_pushInstruction(),_body);
    }

  public String toString ()
    {
      String s = "scope";

      s += "(";

      for (int i=0; i<arity(); i++)
        {
          s += _parameters[i];
          if (i < arity()-1) s += ",";
        }

      return s + ") " + _body;
    }
}
