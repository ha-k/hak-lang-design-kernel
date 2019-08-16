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
 * This is the class denoting bags.
 */

public class NewBag extends NewCollection
{
  public NewBag ()
    {
      _kind = Type.BAG;
      _baseType = new TypeParameter();
    }  

  public NewBag (Type baseType)
    {
      _kind = Type.BAG;
      _baseType = baseType;
    }  

  public NewBag (AbstractList elements)
    {
      this(new TypeParameter(),elements);
    }

  public NewBag (Type baseType, AbstractList elements)
    {
      _kind = Type.BAG;
      _baseType = baseType;
      if (!(elements == null || elements.isEmpty()))
        {
          _elements = new Expression[elements.size()];

          for (int i = _elements.length; i--> 0;)
            _elements[i] = (Expression)elements.get(i);
        }
    }

  private NewBag (Type baseType, Expression[] elements)
    {
      _kind = Type.BAG;
      _baseType = baseType;
      _elements = elements;
    }

  protected NewCollection newCollection (Type baseType, Expression[] elements)
    {
      return new NewBag(baseType,elements);
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      for (int i = numberOfSubexpressions(); i--> 0;)
        _elements[i].typeCheck(_baseType,typeChecker);

      typeChecker.typeCheck(this,new BagType(_baseType));
    }

  public final void compile (Compiler compiler)
    {
      if (_elements != null)
        {
          for (int i = _elements.length; i--> 0;)
            _elements[i].compile(compiler);

          compiler.generate(new PushValueInt(_elements.length));
        }

      switch (((BagType)_checkedType).baseType().boxSort())
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
//         case Type.INT_SORT:
//           compiler.generate(_elements == null ? Instruction.PUSH_BAG_I
//                                               : Instruction.MAKE_BAG_I);
//           return;
//         case Type.REAL_SORT:
//           compiler.generate(_elements == null ? Instruction.PUSH_BAG_R
//                                               : Instruction.MAKE_BAG_R);
//           return;
//         case Type.OBJECT_SORT:
//           compiler.generate(_elements == null ? Instruction.PUSH_BAG_O
//                                               : Instruction.MAKE_BAG_O);
        }         
    }

  public final String toString ()
    {
      if (_elements == null)
        return "new bag{"+(_checkedType == null ? "" : _checkedType.toString()) + "}";

      StringBuilder buf = new StringBuilder("bag{");

      for (int i = 0; i < _elements.length; i++)
        buf.append(_elements[i]+(i==_elements.length-1 ? "}" : ","));

      return buf.toString();
    }
}
