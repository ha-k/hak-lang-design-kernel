//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Wed Jun 20 14:29:51 2012 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

import hlt.language.util.Stack;

/**
 * An <tt>Enclosure</tt> is the stack of scopes that are enclosing a
 * given subexpression. Besides stack operations, it provides the
 * method <tt>setLocalInfo(Local)</tt> that computes and sets the de
 * Bruijn index for the specified <tt>Local</tt> according to its
 * runtime sort by looking it up the stack of scopes' parameters as
 * it walks up through the enclosure from the subexpression's
 * occurrence to its outermost enclosing expression's root.  Along
 * the way, the <tt>Local</tt> is recorded in each enclosing
 * abstraction in which it occurs free - this is needed to compute
 * the maximal closure frame size to allocate for each abstraction.
 */

public class Enclosure extends Stack
{
  final void setLocalInfo (Local local)
    {
      for (int i = size(); i-->0;)
        {
          Scope scope = (Scope)get(i);
          Parameter[] parameters = scope.parameters();

          for (int j = parameters.length; j-->0;)
            {
              if (parameters[j].boxSort() == local.boxSort())
                local.incOffset();
              if (parameters[j].name() == local.name())
                return;
            }

          if (scope instanceof Abstraction)
            ((Abstraction)scope).addToFrame(local);
        }
    }

  public String toString ()
    {
      StringBuilder buf = new StringBuilder("[ ");

      for (int i = size(); i-->0;)
        {
          Scope scope = (Scope)get(i);

          for (int j=scope.parameters().length-1; j>= 0; j--)
            buf.append(scope.parameter(j)+(j==0?"":" "));

          if (i > 0) buf.append(" | ");
        }

      buf.append(" ]");

      return buf.toString();
    }
}
