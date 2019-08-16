//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
// PLEASE DO NOT EDIT WITHOUT THE EXPLICIT CONSENT OF THE AUTHOR! \\
//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

package hlt.language.design.kernel;

/**
 * @version     Last modified on Wed Jun 20 14:29:51 2012 by hak
 * @author      <a href="mailto:hak@acm.org">Hassan A&iuml;t-Kaci</a>
 * @copyright   &copy; <a href="http://www.hassan-ait-kaci.net/">by the author</a>
 */

public class Sanitizer
{
  public static final Expression sanitizeNames (Expression e)
    {
      e.linkScopeTree(null);
      return e.sanitizeNames(new ParameterStack());
    }

  public static final Expression sanitizeNames (Expression e, ParameterStack stack)
    {
      e.linkScopeTree(null);
      return e.sanitizeNames(stack);
    }

  public static final void sanitizeSorts (Expression e)
    {
      e.sanitizeSorts(new Enclosure());
    }
}
