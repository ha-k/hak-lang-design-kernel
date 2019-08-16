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

import hlt.language.util.Stack;
import hlt.language.util.ArrayList;

import java.util.Iterator;

public class Global extends ProtoExpression
{
  private Symbol _symbol;

  private CodeEntry _codeEntry;
  private CodeEntry _checkedCodeEntry;
  private Type _filter = new TypeParameter();

  private Tables _tables;

  public Global (Tables tables, String name)
    {
      _symbol = (_tables = tables).symbol(name);
    }

  public Global (Tables tables, String name, Type type)
    {
      this(tables,name);
      addType(type);      
    }

  public Global (Symbol symbol)
    {
      _symbol = symbol;
    }

  public Global (Tables tables, Symbol symbol)
    {
      _tables = tables;
      _symbol = symbol;
    }

  public final Expression copy ()
    {
      return new Global(_tables,_symbol);
    }

  public final Expression typedCopy ()
    {
      return new Global(_tables,_symbol).addTypes(this);
    }

  public static final Global dummyIndexSet ()
    {
      return new Global(Symbol.INDEX_SET);
    }

  public static final Global dummyIndexable ()
    {
      return new Global(Symbol.INDEXABLE);
    }

  public static final Global dummyCollection ()
    {
      return new Global(Symbol.COLLECTION);
    }

  public final Global globalConstant (Type type)
    {
      setCodeEntry(symbol().getCodeEntry(type));
      setCheckedType(type);
      return this;
    }

  public final Global globalConstant (Type domain, Type range)
    {
      return globalConstant(new FunctionType(domain,range));
    }

  public final Type filter ()
    {
      return _filter.value();
    }

  public final Type sieve ()
    {
      Type filter = filter();
      return filter.kind() == Type.PARAMETER ? type() : filter;
    }

  public final String name ()
    {
      return _symbol.name();
    }

  public final boolean containsFreeName (String name)
    {
      return name == name();
    }

  public final DefinedEntry definedEntry ()
    {
      return (DefinedEntry)_checkedCodeEntry;
    }

  public final CodeEntry checkedCodeEntry ()
    {
      return _checkedCodeEntry;
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      _checkedType = type().copy();
      _checkedCodeEntry = _codeEntry;
    }

  public final void setCheckedType (Type type)
    {
      _type = type;
      setCheckedType();
    }

  public final void resetCheckedType (Type type)
    {
      _checkedType = type;
    }

  public final Symbol symbol ()
    {
      return _symbol;
    }

  public final CodeEntry codeEntry ()
    {
      return _checkedCodeEntry == null ? _codeEntry : _checkedCodeEntry;
    }

  public final void setCodeEntry (CodeEntry codeEntry)
    {
      _codeEntry = codeEntry;
    }

  public final boolean isDefined ()
    {
      return _checkedCodeEntry != null;
    }

  private final ArrayList _eligibles ()
    {
      ArrayList typeTable = _symbol.typeTable();

      if (type().kind() == Type.PARAMETER)
        return typeTable;

      ArrayList eligibles = new ArrayList(typeTable.size());
      
      for (Iterator i=typeTable.iterator(); i.hasNext();)
        {
          CodeEntry eligible = (CodeEntry)i.next();
          if (eligible.type().copy().unify(type().copy()))
            eligibles.add(eligible);
        }

      return eligibles;
    }

  public final ArrayList viableTypes ()
    {
      ArrayList typeTable = _eligibles();

      if (filter().kind() == Type.PARAMETER)
        return typeTable;

      ArrayList viableTypes = new ArrayList(typeTable.size());
      
      for (Iterator i=typeTable.iterator(); i.hasNext();)
        {
          CodeEntry candidate = (CodeEntry)i.next();
          if (candidate.type().copy().unify(filter().copy()))
            viableTypes.add(candidate);
        }

      return viableTypes;
    }

  public final void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      if (!_symbol.isDefined())
        typeChecker.error(new TypingErrorException("undefined symbol '"+_symbol+"'"),this);

      typeChecker.prove(new GlobalTypingGoal(this));
    }

  public final void typeCheck (Type type, TypeChecker typeChecker) throws TypingErrorException
    {
      typeChecker.prune(this,type,this);
      typeCheck(typeChecker);
      typeChecker.unify(typeRef(),type,this);
    }
    
  public final void compile (Compiler compiler)
    {
      if (_checkedCodeEntry.isBuiltIn())
        {
          if (_checkedCodeEntry.type().kind() == Type.FUNCTION)
            _compileCurryedBuiltin(compiler);
          else
            compiler.generate(((BuiltinEntry)_checkedCodeEntry).builtIn());
          return;
        }

      if (definedEntry().isInlinable())
        compiler.inline(definedEntry().code());
      else
        compiler.generate(new Call(definedEntry()));
    }

  private final void _compileCurryedBuiltin(Compiler compiler)
    {
      FunctionType type = (FunctionType)_checkedType;
      int arity = type.arity();
      
      Parameter[]  dummyParameters = new Parameter [arity];
      Local[]      dummyArguments  = new Local     [arity];

      for (int i=0; i<arity; i++)
        {
          dummyParameters[i] = new Parameter();
          dummyParameters[i].setCheckedType(type.domain(i));
          dummyArguments[i] = new Local(dummyParameters[i]);
        }

      Application application = new Application(this,dummyArguments);
      application.setCheckedType(type.range());
      Abstraction abstraction = new Abstraction(dummyParameters,application);
      abstraction.setNonExitable();
      abstraction.setSortedArities();

      int intArity = abstraction.intArity();
      int realArity = abstraction.realArity();
      int objectArity = abstraction.objectArity();
      int ia = 0, ra = 0, oa = 0;

      for (int i=0; i<arity; i++)
        switch (dummyParameters[i].boxSort())
          {
          case Type.INT_SORT:
            dummyArguments[i].setOffset(intArity-1-(ia++));
            break;
          case Type.REAL_SORT:
            dummyArguments[i].setOffset(realArity-1-(ra++));
            break;
          case Type.OBJECT_SORT:
            dummyArguments[i].setOffset(objectArity-1-(oa++));
          }

      abstraction.compile(compiler);
    }

  public final boolean equals (Object other)
    {
      if (this == other)
        return true;

      if (!(other instanceof Global))
        return false;

      return _symbol.equals(((Global)other).symbol());
    }

  public final String toString ()
    {
      return name();
    }
}
