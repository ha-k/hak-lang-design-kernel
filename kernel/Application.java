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
 * This class represents functional applications <i>&agrave; la</i> &lambda;-calculus.
 * More precisely, it represents multiple-argument applications. This enables an
 * application to handle many arguments without having to be "curryed" into a
 * composition of single-argument applications.
 */
public class Application extends ProtoExpression
{
  protected Expression _function;
  protected Expression[] _arguments;

  protected Expression _checkedFunction;
  protected Expression[] _checkedArguments;

  protected boolean _noCurrying = false;

  protected Application ()
    {
    }

  public Application (Expression function, Expression[] arguments)
    {
      _function = function;
      _arguments = arguments;
    }

  public Application (Expression function, Expression arg1, Expression arg2)
    {
      Expression[] arguments = { arg1, arg2 };
      _function = function;
      _arguments = arguments;
    }

  public Application (Expression function, Expression argument)
    {
      _function = function;
      _arguments = new Expression[1];
      _arguments[0] = argument;
    }

  public Application (Expression function, AbstractList arguments)
    {
      _function = function;
      if (!arguments.isEmpty())
        {
          _arguments = new Expression[arguments.size()];
          for (int i=_arguments.length; i-->0;)
            _arguments[i] = (Expression)arguments.get(i);
        }
      else
        {
          _arguments = new Expression[1];
          _arguments[0] = Constant.VOID;
        }
    }

  public final Application flatten ()
    {
      if (!_noCurrying && _function instanceof Application)
        {
          Application app = (Application)_function;
          int arity = app.arity();
          Expression[] args = new Expression[arity+_arguments.length];

          for (int i = 0; i<arity; i++)
            args[i] = app.argument(i);

          for (int i = arity; i< args.length; i++)
            args[i] = _arguments[i-arity];

          _function = app.function();
          _arguments = args;
        }

      return this;
    }

  public final Expression copy ()
    {
      Expression[] arguments = new Expression[_arguments.length];
      for (int i=arguments.length; i-->0;)
        arguments[i] = _arguments[i].copy();

      return new Application(_function.copy(),arguments);
    }

  public final Expression typedCopy ()
    {
      Expression[] arguments = new Expression[_arguments.length];
      for (int i=arguments.length; i-->0;)
        arguments[i] = _arguments[i].typedCopy();

      return new Application(_function.typedCopy(),arguments).addTypes(this);
    }

  /**
   * Returns <tt>true</tt> iff this application is a slicing filter for the specified
   * parameter; <i>i.e.</i>, iff it is of the form <tt>x.s == e</tt>, where <tt>x</tt>
   * is the parameter, and <tt>x</tt> does not occur free in <tt>e</tt>.
   */
  public final boolean isSlicing (Tables tables, Parameter parameter) throws UndefinedEqualityException
    {
      if (!_function.isEquality(tables) || arity() != 2)
        return false;

      if (_arguments[0] instanceof TupleProjection
          && !_arguments[1].containsFreeName(parameter.name())
          // NB: this next test must come last as it has a side effet if true!
          && ((TupleProjection)_arguments[0]).slicesParameter(parameter))
        return true;

      if (_arguments[1] instanceof TupleProjection
          && !_arguments[0].containsFreeName(parameter.name())
          // NB: this next test must come last as it has a side effet if true!
          && ((TupleProjection)_arguments[1]).slicesParameter(parameter))
        {
          Expression tmp = _arguments[0];
          _arguments[0]  = _arguments[1];
          _arguments[1]  = tmp;
          return true;
        }

      return false;      
    }

  /**
   * Returns <tt>true</tt> iff this application is a "hidden" filter for the specified
   * parameter; <i>i.e.</i>, iff it is of the form <tt>s(x) == e</tt>, where <tt>x</tt>
   * is the parameter, and <tt>x</tt> does not occur free in <tt>e</tt>.
   */
  public final boolean isHiddenSlicing (Tables tables, Parameter parameter)
    throws UndefinedEqualityException
    {
      if (!_function.isEquality(tables) || arity() != 2)
        return false;

      if (_arguments[0] instanceof Application
          && ((Application)_arguments[0]).isUnaryTupleProjection()
          && ((Application)_arguments[0]).slicesParameter(parameter)
          && !_arguments[1].containsFreeName(parameter.name()))
        {
          _arguments[0] = ((Application)_arguments[0]).rebuildTupleProjection();
          return true;
        }

      if(_arguments[1] instanceof Application
          && ((Application)_arguments[1]).isUnaryTupleProjection()
          && ((Application)_arguments[1]).slicesParameter(parameter)
          && !_arguments[0].containsFreeName(parameter.name()))
        {
          Expression tmp = _arguments[0];
          _arguments[0]  = ((Application)_arguments[1]).rebuildTupleProjection();
          _arguments[1]  = tmp;
          return true;
        }

      return false;      
    }

  /**
   * Returns <tt>true</tt> iff this is of the form <tt>p1(p2(...pk(x)...))</tt> where
   * <tt>x</tt> is a local occurrence of the specified parameter, and the <tt>pi</tt>s
   * are unary tuple projections.
   */
  public final boolean slicesParameter (Parameter parameter)
    {
      if (_arguments[0] instanceof Application
          && ((Application)_arguments[0]).isUnaryTupleProjection())
        return ((Application)_arguments[0]).slicesParameter(parameter);

      if (_arguments[0] instanceof Local)
        return ((Local)_arguments[0]).name() == parameter.name();

      return false;
    }

  /**
   * Rebuilds an explicit tuple projection from this when it is one in a "hidden" form.
   */
  final TupleProjection rebuildTupleProjection ()
    {
      TupleProjection projection = null;
      Expression tuple = null;
      String field = ((Global)function()).name();

      if (_arguments[0] instanceof Local)
        {
          tuple = new DummyLocal(((Local)_arguments[0]).parameter());
          ((Local)tuple).parameter().setType(_arguments[0].type().actualType());
        }
      else
        {
          tuple = ((Application)_arguments[0]).rebuildTupleProjection();
          tuple.setType(_arguments[0].type().actualType());
        }

      projection = new TupleProjection(tuple,field);

      projection.setPosition(field);
      projection.setType(type().actualType());

      return projection;
    }    

  /**
   * Returns <tt>true</tt> iff this application is a selector filter for the specified
   * parameter; <i>i.e.</i>, iff it is of the form <tt>x == e</tt>, where <tt>x</tt>
   * is the parameter, and <tt>x</tt> does not occur free in <tt>e</tt>.
   */
  public final boolean isSelector (Tables tables, Parameter parameter)
    throws UndefinedEqualityException
    {
      if (!_function.isEquality(tables) || arity() != 2)
        return false;

      if (_arguments[0] instanceof Dummy
          && !_arguments[1].containsFreeName(parameter.name()))
        return true;

      if (_arguments[1] instanceof Dummy
          && !_arguments[0].containsFreeName(parameter.name()))
        {
          Expression tmp = _arguments[0];
          _arguments[0]  = _arguments[1];
          _arguments[1]  = tmp;
          return true;
        }

      return false;      
    }

  /**
   * When this application is a slicing equality, this undoes the name-sanitizing of
   * the "dummy local" parameter occurrence of the projection back to a plain dummy.
   * This is used by a <a href="Comprehension.html"><tt>Comprehension</tt></a> when
   * a selector filter for a generator has been identified - thus rendering all its
   * slicings useless, which then must be reintegrated as simple filter conditions.
   */
  public final Expression undoDummyLocal ()
    {
      TupleProjection projection = (TupleProjection)_arguments[0];

      while (projection.tuple() instanceof TupleProjection)
        projection = (TupleProjection)projection.tuple();

      DummyLocal variable = (DummyLocal)projection.tuple();
      projection.setSubexpression(0,new Dummy(variable.name()).addTypes(variable));

      return this;
    }      

  public final boolean noCurrying ()
    {
      return _noCurrying;
    }

  public final Application setNoCurrying ()
    {
      return setNoCurrying(true);
    }

  public final Application setNoCurrying (boolean flag)
    {
      _noCurrying = flag;
      return this;
    }

  public int numberOfSubexpressions ()
    {
      return _arguments.length+1;
    }

  public final Expression subexpression (int n) throws NoSuchSubexpressionException
    {
      if (n == 0)
        return _function;

      if (n > 0 && n <= _arguments.length)
        return _arguments[n-1];

      throw new NoSuchSubexpressionException(this,n);
    }

  public final Expression setSubexpression (int n, Expression expression) throws NoSuchSubexpressionException
    {
      if (n == 0)
        _function = expression;
      else
        if (n > 0 && n <= _arguments.length)
          _arguments[n-1] = expression;
        else
          throw new NoSuchSubexpressionException(this,n);

      return this;
    }

  public final Expression function ()
    {
      return _checkedFunction == null ? _function : _checkedFunction;
    }

  public final Expression[] arguments ()
    {
      return _checkedArguments == null ? _arguments : _checkedArguments;
    }

  public final Expression argument (int n)
    {
      return arguments()[n];
    }

  public final void setFunction (Expression function)
    {
      _function = function;
    }

  public final void setArguments (Expression[] arguments)
    {
      _arguments = arguments;
    }

  public final int arity ()
    {
      return arguments().length;
    }

  public final void setCheckedType ()
    {
      if (setCheckedTypeLocked()) return;
      _function.setCheckedType();

      for (int i=_arguments.length; i-->0;)
        _arguments[i].setCheckedType();

      setCheckedType(type().copy());
    }

  public final void setCheckedType (Type type)
    {
      _checkedFunction = _function;
      _checkedArguments = _arguments;
      _checkedType = type;
    }

  public final void typeCheck (Type type, TypeChecker typeChecker) throws TypingErrorException
    {
      typeCheck(typeChecker);
      typeChecker.unify(_type,type,this);
    }
    
  public void typeCheck (TypeChecker typeChecker) throws TypingErrorException
    {
      if (typeCheckLocked()) return;

      Type[] argumentTypes = new Type[arity()];

      for (int i=arity(); i-->0;)
        {
          _arguments[i].typeCheck(typeChecker);
          argumentTypes[i] = _arguments[i].typeRef();
        }

      FunctionType functionType = new FunctionType(argumentTypes,_type).setNoCurrying(_noCurrying);
      _function.typeCheck(functionType,typeChecker);

      int arity = ((FunctionType)_function.type()).arity();

      if (arity < arity())
        _curryTypeCheck(arity,typeChecker);
    }
  
  protected final void _curryTypeCheck (int depth, TypeChecker typeChecker) throws TypingErrorException
    {
      Expression[] actualArguments = new Expression[depth];

      for (int i=depth; i-->0;)
        actualArguments[i] = _arguments[i]/*.copy()*/;

      Expression[] remainingArguments = new Expression[arity()-depth];

      for (int i=remainingArguments.length; i-->0;)
        remainingArguments[i] = _arguments[i+depth]/*.copy()*/;

      typeChecker.trail(this,_function,_arguments);
      _function = new Application(_function/*.copy()*/,actualArguments);
      _arguments = remainingArguments;

      _typeCheckLocked = false;
      typeCheck(typeChecker);
    }

  public final boolean isTupleProjection ()
    {
      return function() instanceof Global
          && ((Global)function()).codeEntry().isProjection();
    }
    
  public final boolean isUnaryTupleProjection ()
    {
      return isTupleProjection()
          && arity() == 1;
    }
    
  public final boolean isFieldApplication ()
    {
      return (function() instanceof Global
          && ((Global)function()).codeEntry().isField());
    }
    
  public void compile (Compiler compiler)
    {
       FunctionType functionType = (FunctionType)_checkedFunction.checkedType();

      if (_compileBuiltIn(functionType,compiler))
        return;

      for (int i=_checkedArguments.length; i-->0;)
        _compileArgument(i,functionType,compiler);

      if (isFieldApplication())
        {
          DefinedEntry entry = ((Global)_checkedFunction).definedEntry();
          compiler.generate(_getField(entry));

          if (arity() > 1)
            compiler.generate(new Apply(functionType).curryObject());
        }
      else
        if (isTupleProjection())
          {
            DefinedEntry entry = ((Global)_checkedFunction).definedEntry();
            compiler.generate(entry.projection());

            if (arity() > 1)
              compiler.generate(new Apply(functionType).curryObject());
          }
        else
          {
            _checkedFunction.compile(compiler);
            compiler.generate(_checkedFunction instanceof Scope ? new Enter(functionType)
                                                                : new Apply(functionType));
          }

      _padResultIfNeeded(functionType,compiler);
    }

  protected final FieldInstruction _getField (DefinedEntry entry)
    {
      switch (entry.fieldSort())
        {
        case Type.INT_SORT:
          return new GetIntField(entry);
        case Type.REAL_SORT:
          return new GetRealField(entry);
        case Type.OBJECT_SORT:
          return new GetObjectField(entry);
        }
      return null;
    }

  protected final boolean _compileBuiltIn (FunctionType functionType, Compiler compiler)
    {
      if (_checkedFunction instanceof Global)
        {
          CodeEntry entry = ((Global)_checkedFunction).checkedCodeEntry();

          if (entry.isBuiltIn())
            {
              BuiltinEntry builtinEntry = (BuiltinEntry)entry;
              Instruction builtin = builtinEntry.builtIn();
              FunctionType type = (FunctionType)(entry.type().copy());

              if (_checkedArguments.length == type.arity())
                {
                  if (builtin.isDummy())
                    return _compileDummyBuiltin(builtin,functionType,compiler);

                  for (int i=_checkedArguments.length-1; i>=0; i--)
                    {
                      _checkedArguments[i].compile(compiler);
                      _padArgumentIfNeeded(i,functionType,compiler);
                    }

                  compiler.generate(builtin);
                  _padResultIfNeeded(functionType,compiler);
                }
              else // necessarily: _checkedArguments.length < arity())
                _compileCurryedBuiltIn(functionType,compiler);

              return true;
            }
        }

      return false;
    }

  protected boolean _compileDummyBuiltin (Instruction instruction, FunctionType functionType,
                                          Compiler compiler)
    {
      if (instruction == Instruction.DUMMY_AND)
        {
          And and = new And(_checkedArguments[0],_checkedArguments[1]);
          and.setCheckedType(checkedType());
          and.compile(compiler);
          return true;
        }

      if (instruction == Instruction.DUMMY_OR)
        {
          Or or = new Or(_checkedArguments[0],_checkedArguments[1]);
          or.setCheckedType(checkedType());
          or.compile(compiler);
          return true;
        }

      if (instruction == Instruction.DUMMY_SIZE)
        {
          _checkedArguments[0].compile(compiler);
          if (((ArrayType)_checkedArguments[0].checkedType()).isMap())
            compiler.generate(Instruction.MAP_SIZE);
          else
            compiler.generate(Instruction.ARRAY_SIZE);
          return true;
        }

      if (instruction == Instruction.DUMMY_EQU || instruction == Instruction.DUMMY_NEQ)
        {
          if (_checkedArguments[1].sort() == Type.VOID_SORT)
            if (instruction == Instruction.DUMMY_EQU)
              compiler.generate(Instruction.PUSH_TRUE);
            else
              compiler.generate(Instruction.PUSH_FALSE);
          else
            {
              _checkedArguments[1].compile(compiler);

              if (_checkedArguments[1].checkedType().isBoxedType())
                if (_checkedArguments[1].sort() == Type.INT_SORT
                    || _checkedArguments[1].sort() == Type.REAL_SORT)
                  compiler.generateUnwrapper(_checkedArguments[1].sort());

              _checkedArguments[0].compile(compiler);

              if (_checkedArguments[0].checkedType().isBoxedType())
                if (_checkedArguments[0].sort() == Type.INT_SORT
                    || _checkedArguments[0].sort() == Type.REAL_SORT)
                  compiler.generateUnwrapper(_checkedArguments[0].sort());

              if (instruction == Instruction.DUMMY_EQU)
                switch (_checkedArguments[0].sort())
                  {
                  case Type.INT_SORT:
                    compiler.generate(Instruction.EQU_II);
                    break;
                  case Type.REAL_SORT:
                    compiler.generate(Instruction.EQU_RR);
                    break;
                  default:
                    compiler.generate(Instruction.EQU_OO);
                  }
              else
                switch (_checkedArguments[0].sort())
                  {
                  case Type.INT_SORT:
                    compiler.generate(Instruction.NEQ_II);
                    break;
                  case Type.REAL_SORT:
                    compiler.generate(Instruction.NEQ_RR);
                    break;
                  default:
                    compiler.generate(Instruction.NEQ_OO);
                  }
            }

          _padResultIfNeeded(functionType,compiler);
          return true;
        }

      if (instruction == Instruction.DUMMY_STRCON)
        {
          _checkedArguments[1].compile(compiler);
          _compileArgument(0,functionType,compiler);

          compiler.generate(new StringConcatenation(_checkedArguments[0].checkedType()));
          return true;
        }

      if (instruction == Instruction.DUMMY_WRITE)
        {
          _checkedArguments[0].compile(compiler);
          switch (_checkedArguments[0].boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(new WriteInt(_checkedArguments[0].checkedType()));
              break;
            case Type.REAL_SORT:
              compiler.generate(Instruction.WRITE_R);
              break;
            default:
              compiler.generate(new WriteObject(_checkedArguments[0].checkedType()));
            }
          _padResultIfNeeded(functionType,compiler);
          return true;
        }

      if (instruction == Instruction.DUMMY_SET_ADD || instruction == Instruction.DUMMY_SET_RMV)
        {
          _checkedArguments[1].compile(compiler);
          _checkedArguments[0].compile(compiler);

          switch (_checkedArguments[0].boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(instruction == Instruction.DUMMY_SET_ADD
                                ? Instruction.SET_ADD_I
                                : Instruction.SET_RMV_I);
              break;
            case Type.REAL_SORT:
              compiler.generate(instruction == Instruction.DUMMY_SET_ADD
                                ? Instruction.SET_ADD_R
                                : Instruction.SET_RMV_R);
              break;
            default:
              compiler.generate(instruction == Instruction.DUMMY_SET_ADD
                                ? Instruction.SET_ADD_O
                                : Instruction.SET_RMV_O);
            }
          return true;
        }

      if (instruction == Instruction.DUMMY_BELONGS)
        {
          _checkedArguments[1].compile(compiler);
          _checkedArguments[0].compile(compiler);

          switch (_checkedArguments[0].boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(Instruction.BELONGS_I);
              break;
            case Type.REAL_SORT:
              compiler.generate(Instruction.BELONGS_R);
              break;
            default:
              compiler.generate(Instruction.BELONGS_O);
            }
          return true;
        }

      if (instruction == Instruction.DUMMY_FIRST)
        {
          _checkedArguments[0].compile(compiler);

          switch (((SetType)_checkedArguments[0].checkedType()).baseType().boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(Instruction.FIRST_I);
              break;
            case Type.REAL_SORT:
              compiler.generate(Instruction.FIRST_R);
              break;
            default:
              compiler.generate(Instruction.FIRST_O);
            }
          return true;
        }

      if (instruction == Instruction.DUMMY_LAST)
        {
          _checkedArguments[0].compile(compiler);

          switch (((SetType)_checkedArguments[0].checkedType()).baseType().boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(Instruction.LAST_I);
              break;
            case Type.REAL_SORT:
              compiler.generate(Instruction.LAST_R);
              break;
            default:
              compiler.generate(Instruction.LAST_O);
            }
          return true;
        }

      if (instruction == Instruction.DUMMY_ORD)
        {
          _checkedArguments[1].compile(compiler);
          _checkedArguments[0].compile(compiler);

          switch (_checkedArguments[1].boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(Instruction.ORD_I);
              break;
            case Type.REAL_SORT:
              compiler.generate(Instruction.ORD_R);
              break;
            default:
              compiler.generate(Instruction.ORD_O);
            }
          return true;
        }

      if (instruction == Instruction.DUMMY_NEXT)
        {
          _checkedArguments[1].compile(compiler);
          _checkedArguments[0].compile(compiler);

          switch (_checkedArguments[1].boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(Instruction.NEXT_I);
              break;
            case Type.REAL_SORT:
              compiler.generate(Instruction.NEXT_R);
              break;
            default:
              compiler.generate(Instruction.NEXT_O);
            }
          return true;
        }
        
      if (instruction == Instruction.DUMMY_NEXT_OFFSET)
        {
          _checkedArguments[2].compile(compiler);  
          _checkedArguments[1].compile(compiler);
          _checkedArguments[0].compile(compiler);

          switch (_checkedArguments[1].boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(Instruction.NEXT_I_OFFSET);
              break;
            case Type.REAL_SORT:
              compiler.generate(Instruction.NEXT_R_OFFSET);
              break;
            default:
              compiler.generate(Instruction.NEXT_O_OFFSET);
            }
          return true;
        }

      if (instruction == Instruction.DUMMY_NEXT_C)
        {
          _checkedArguments[1].compile(compiler);
          _checkedArguments[0].compile(compiler);

          switch (_checkedArguments[1].boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(Instruction.NEXT_C_I);
              break;
            case Type.REAL_SORT:
              compiler.generate(Instruction.NEXT_C_R);
              break;
            default:
              compiler.generate(Instruction.NEXT_C_O);
            }
          return true;
        }

      if (instruction == Instruction.DUMMY_NEXT_C_OFFSET)
        {
          _checkedArguments[2].compile(compiler);  
          _checkedArguments[1].compile(compiler);
          _checkedArguments[0].compile(compiler);

          switch (_checkedArguments[1].boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(Instruction.NEXT_C_I_OFFSET);
              break;
            case Type.REAL_SORT:
              compiler.generate(Instruction.NEXT_C_R_OFFSET);
              break;
            default:
              compiler.generate(Instruction.NEXT_C_O_OFFSET);
            }
          return true;
        }

      if (instruction == Instruction.DUMMY_PREV)
        {
          _checkedArguments[1].compile(compiler);
          _checkedArguments[0].compile(compiler);

          switch (_checkedArguments[1].boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(Instruction.PREV_I);
              break;
            case Type.REAL_SORT:
              compiler.generate(Instruction.PREV_R);
              break;
            default:
              compiler.generate(Instruction.PREV_O);
            }
          return true;
        }

      if (instruction == Instruction.DUMMY_PREV_OFFSET)
        {
          _checkedArguments[2].compile(compiler);
          _checkedArguments[1].compile(compiler);            
          _checkedArguments[0].compile(compiler);

          switch (_checkedArguments[1].boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(Instruction.PREV_I_OFFSET);
              break;
            case Type.REAL_SORT:
              compiler.generate(Instruction.PREV_R_OFFSET);
              break;
            default:
              compiler.generate(Instruction.PREV_O_OFFSET);
            }
          return true;
        }

      if (instruction == Instruction.DUMMY_PREV_C)
        {
          _checkedArguments[1].compile(compiler);
          _checkedArguments[0].compile(compiler);

          switch (_checkedArguments[1].boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(Instruction.PREV_C_I);
              break;
            case Type.REAL_SORT:
              compiler.generate(Instruction.PREV_C_R);
              break;
            default:
              compiler.generate(Instruction.PREV_C_O);
            }
          return true;
        }

      if (instruction == Instruction.DUMMY_PREV_C_OFFSET)
        {
          _checkedArguments[2].compile(compiler);
          _checkedArguments[1].compile(compiler);            
          _checkedArguments[0].compile(compiler);

          switch (_checkedArguments[1].boxSort())
            {
            case Type.INT_SORT:
              compiler.generate(Instruction.PREV_C_I_OFFSET);
              break;
            case Type.REAL_SORT:
              compiler.generate(Instruction.PREV_C_R_OFFSET);
              break;
            default:
              compiler.generate(Instruction.PREV_C_O_OFFSET);
            }
          return true;
        }
        

      return true;
    }

  protected final void _compileArgument (int i, FunctionType functionType, Compiler compiler)
    {
      if (functionType.domain(i).kind() == Type.FUNCTION)
        _checkedArguments[i].pad((FunctionType)functionType.domain(i)).compile(compiler);
      else
        _checkedArguments[i].compile(compiler);

      _padArgumentIfNeeded(i,functionType,compiler);
    }

  protected final void _padArgumentIfNeeded (int i, FunctionType functionType, Compiler compiler)
    {
      if (functionType.domainIsBoxed(i))
        {
          if (!_checkedArguments[i].checkedType().isBoxedType())
            compiler.generateWrapper(_checkedArguments[i].sort());
        }
      else
        if (_checkedArguments[i].checkedType().isBoxedType())
          compiler.generateUnwrapper(_checkedArguments[i].sort());       
    }

  protected final void _padResultIfNeeded (FunctionType functionType, Compiler compiler)
    {
      if (functionType.rangeIsBoxed())
        {
          if (!_checkedType.isBoxedType())
            compiler.generateUnwrapper(sort());
        }
      else
        if (_checkedType.isBoxedType())
          compiler.generateWrapper(sort());
    }

  protected final void _compileCurryedBuiltIn (FunctionType curryedType, Compiler compiler)
    {
      FunctionType curryedRange = (FunctionType)curryedType.range();
      FunctionType uncurryedType = curryedType.uncurry();

      int actualArity   = curryedType.arity();
      int missingArity  = curryedRange.arity();
      int expectedArity = actualArity + missingArity;

      Parameter [] dummyParameters = new Parameter [missingArity];
      Local     [] dummyArguments  = new Local     [missingArity];
      Expression[] newArguments    = new Expression[expectedArity];

      for (int i=0; i<missingArity; i++)
        {
          dummyParameters[i] = new Parameter();
          dummyParameters[i].setCheckedType(curryedRange.domain(i));
          dummyArguments[i] = new Local(dummyParameters[i]);
        }

      ((Global)_checkedFunction).resetCheckedType(uncurryedType);
      Application application = new Application(_checkedFunction,newArguments);
      application.setCheckedType(curryedRange.range());
      Abstraction abstraction = new Abstraction(dummyParameters,application);
      abstraction.setNonExitable();
      abstraction.setSortedArities();

      int intArity = abstraction.intArity();
      int realArity = abstraction.realArity();
      int objectArity = abstraction.objectArity();
      int ia = 0, ra = 0, oa = 0;

      for (int i=0; i<missingArity; i++)
        {
          switch (dummyParameters[i].boxSort())
            {
            case Type.INT_SORT:
              dummyArguments[i].setOffset(intArity-1-(ia++));
              break;
            case Type.REAL_SORT:
              dummyArguments[i].setOffset(realArity-1-(ra++));
              break;
            default:
              dummyArguments[i].setOffset(objectArity-1-(oa++));
            }

          newArguments[actualArity+i] = dummyArguments[i];
        }
      
      for (int i=0; i<actualArity; i++)
        newArguments[i] = _checkedArguments[i].shiftOffsets(ia,ra,oa);

      abstraction.compile(compiler);
    }

  public String toString ()
    {
      String s = function() + "(";

      for (int i=0; i<arity(); i++)
        {
          s += argument(i);
          if (i < arity()-1) s += ",";
        }

      return s + ")";      
    }

}
