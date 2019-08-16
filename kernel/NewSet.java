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
 * This is the class denoting sets.
 */

public class NewSet extends NewCollection
{
  public NewSet ()
    {
      _kind = Type.SET;
      _baseType = new TypeParameter();
    }  

  public NewSet (Type baseType)
    {
      _kind = Type.SET;
      _baseType = baseType;
    }  

  public NewSet (AbstractList elements)
    {
      this(new TypeParameter(),elements);
    }

  public NewSet (Type baseType, AbstractList elements)
    {
      _kind = Type.SET;
      _baseType = baseType;
      if (!(elements == null || elements.isEmpty()))
        {
          _elements = new Expression[elements.size()];

          for (int i = _elements.length; i--> 0;)
            _elements[i] = (Expression)elements.get(i);
        }
    }

  private NewSet (Type baseType, Expression[] elements)
    {
      _kind = Type.SET;
      _baseType = baseType;
      _elements = elements;
    }

  protected NewCollection newCollection (Type baseType, Expression[] elements)
    {
      return new NewSet(baseType,elements);
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      for (int i = numberOfSubexpressions(); i--> 0;)
        _elements[i].typeCheck(_baseType,typeChecker);

      typeChecker.typeCheck(this,new SetType(_baseType));
    }

  public final void compile (Compiler compiler)
    {
      if (_elements != null)
        {
          for (int i = _elements.length; i--> 0;)
            _elements[i].compile(compiler);

          compiler.generate(new PushValueInt(_elements.length));
        }

      switch (((SetType)_checkedType).baseType().boxSort())
        {
        case Type.INT_SORT:
          compiler.generate(_elements == null ? Instruction.PUSH_SET_I
                                              : Instruction.MAKE_SET_I);
          return;
        case Type.REAL_SORT:
          compiler.generate(_elements == null ? Instruction.PUSH_SET_R
                                              : Instruction.MAKE_SET_R);
          return;
        case Type.OBJECT_SORT:
          compiler.generate(_elements == null ? Instruction.PUSH_SET_O
                                              : Instruction.MAKE_SET_O);
        }         
    }

  public final String toString ()
    {
      if (_elements == null)
        return "new set{"+(_checkedType == null ? "" : _checkedType.toString()) + "}";

      StringBuilder buf = new StringBuilder("set{");

      for (int i = 0; i < _elements.length; i++)
        buf.append(_elements[i]+(i==_elements.length-1 ? "}" : ","));

      return buf.toString();
    }
}
