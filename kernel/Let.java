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

public class Let extends Application
{
  protected Let ()
    {
    }

  public Let (Parameter parameter, Expression value, Expression body)
    {
      super(new Scope(parameter,body),value);
    }

  public Let (AbstractList parameters, AbstractList values, Expression body)
    {
      super(new Scope(parameters,body),values);
    }

  public Let (Parameter[] parameters, Expression[] values, Expression body)
    {
      super(new Scope(parameters,body),values);
    }

  public Let (AbstractList parameters, AbstractList types, AbstractList values, Expression body)
    {
      super(new Scope(parameters,body),values);

      for (int i=parameters.size(); i-->0;)
        ((Scope)function()).parameter(i).setType((Type)types.get(i));
    }

  public void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      Scope scope = (Scope)function();
      Type[] argumentTypes = new Type[arity()];

      for (int i=arity(); i-->0;)
        {
          typeChecker.unify(scope.parameter(i).typeRef(),_arguments[i].typeRef(),this);
          _arguments[i].typeCheck(typeChecker);
          argumentTypes[i] = _arguments[i].typeRef();
        }

      FunctionType functionType = new FunctionType(argumentTypes,_type).setNoCurrying();

      _function.typeCheck(functionType,typeChecker);
    }

  public /*final*/ String toString ()
    {
      StringBuilder buf = new StringBuilder("let ");

      Scope scope = (Scope)function();

      for (int i=0; i<arity(); i++)
        buf.append(scope.parameter(i))
           .append(" = ")
           .append(argument(i))
           .append("; ");

      buf.append("in ").append(scope.body());

      return buf.toString();
    }
}
