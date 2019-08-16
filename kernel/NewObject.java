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
 * This is the class of 'new' object expressions.
 */
public class NewObject extends Constant
{
  public NewObject (Type type)
    {
      setType(type);
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      if (!(_type instanceof ClassType))
        typeChecker.error(locate(new TypingErrorException("bad class type: "+_type)));

      ClassType type = (ClassType)_type;

      for (int i=type.arity(); i-->0;)
        typeChecker.disallowVoid(type.argument(i),this,"class type instantiation");
    }

  public final void compile (Compiler compiler)
    {
      compiler.generate(new PushNewObject((ClassType)_type));
    }

  public final String toString ()
    {
      return "new "+_type;
    }
}
