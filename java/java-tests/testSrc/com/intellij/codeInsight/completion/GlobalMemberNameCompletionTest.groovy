package com.intellij.codeInsight.completion;

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.util.ArrayUtil
import com.intellij.codeInsight.lookup.LookupElementPresentation;

/**
 * @author peter
 */
public class GlobalMemberNameCompletionTest extends LightCodeInsightFixtureTestCase {

  public void testMethodName() throws Exception {
    myFixture.addClass("""
package foo;

public class Foo {
  public static int abcmethod() {}
  static void methodThatsNotVisible() {}
}
""")

    doTest "class Bar {{ abcm<caret> }}", true, """import static foo.Foo.abcmethod;

class Bar {{ abcmethod()<caret> }}"""
  }

  public void testFieldName() throws Exception {
    myFixture.addClass("""
package foo;

public class Foo {
  public static int abcfield = 2
  static final int fieldThatsNotVisible = 3
}
""")

    doTest "class Bar {{ abcf<caret> }}", true, """import static foo.Foo.abcfield;

class Bar {{ abcfield<caret> }}"""
  }

  public void testFieldNameQualified() throws Exception {
    myFixture.addClass("""
package foo;

public class Foo {
  public static int abcfield = 2
  static final int fieldThatsNotVisible = 3
}
""")

    doTest "class Bar {{ abcf<caret> }}", false, """import foo.Foo;

class Bar {{ Foo.abcfield<caret> }}"""
  }

  public void testFieldNamePresentation() {
    myFixture.addClass("""
package foo;

public class Foo {
  public static int abcfield = 2
  static final int fieldThatsNotVisible = 3
}
""")
    myFixture.configureByText "a.java", "class Bar {{ abcf<caret> }}"
    def element = myFixture.complete(CompletionType.CLASS_NAME)[0]
    def presentation = new LookupElementPresentation()
    element.renderElement(presentation)
    assert 'Foo.abcfield' == presentation.itemText
    assert ' (foo)' == presentation.tailText
    assert 'int' == presentation.typeText
  }

  public void testQualifiedMethodName() throws Exception {
    myFixture.addClass("""
package foo;

public class Foo {
  public static int abcmethod() {}
}
""")

    doTest "class Bar {{ abcm<caret> }}", false, """import foo.Foo;

class Bar {{ Foo.abcmethod()<caret> }}"""
  }

  public void testIfThereAreAlreadyStaticImportsWithThatClass() throws Exception {
    myFixture.addClass("""
package foo;

public class Foo {
  public static int anotherMethod(int a) {}
  public static int abcmethod() {}
  void methodThatsNotVisible() {}
}
""")

    doTest """import static foo.Foo.abcmethod;

class Bar {{ abcmethod(); anoMe<caret> }}""", false,
           """import static foo.Foo.abcmethod;
import static foo.Foo.anotherMethod;

class Bar {{ abcmethod(); anotherMethod(<caret>) }}"""
  }


  @Override protected void tearDown() {
    CodeInsightSettings.instance.EXCLUDED_PACKAGES = ArrayUtil.EMPTY_STRING_ARRAY
    super.tearDown()
  }

  public void testExcludeClassFromCompletion() throws Exception {
    myFixture.addClass("""package foo;
      public class Foo {
        public static int abcmethod() {}
      }
    """)
    myFixture.addClass("""package foo;
      public class Excl {
        public static int abcmethod2() {}
      }
    """)

    CodeInsightSettings.instance.EXCLUDED_PACKAGES = ["foo.Excl"] as String[]

    doTest "class Bar {{ abcm<caret> }}", true, """import static foo.Foo.abcmethod;

class Bar {{ abcmethod()<caret> }}"""
  }

  public void testExcludeMethodFromCompletion() throws Exception {
    myFixture.addClass("""package foo;
      public class Foo {
        public static int abcmethod1() {}
        public static int abcmethodExcluded() {}
      }
    """)

    CodeInsightSettings.instance.EXCLUDED_PACKAGES = ["foo.Foo.abcmethodExcluded"] as String[]

    doTest "class Bar {{ abcm<caret> }}", true, """import static foo.Foo.abcmethod1;

class Bar {{ abcmethod1()<caret> }}"""
  }

  public void testMergeOverloads() throws Exception {
    myFixture.addClass("""package foo;
      public class Foo {
        public static int abcmethod(int a) {}
        public static int abcmethod(boolean a) {}
        public static int abcmethod1(boolean a) {}
      }
    """)

    myFixture.configureByText("a.java", "class Bar {{ abcm<caret> }}")
    myFixture.complete(CompletionType.CLASS_NAME)
    assertOrderedEquals myFixture.lookupElementStrings, "abcmethod", "abcmethod1"
  }

  public void testMethodFromTheSameClass() {
    myFixture.configureByText("a.java", """
class A {
  static void foo() {}

  static void goo() {
    f<caret>
  }
}
""")
    def element = myFixture.complete(CompletionType.CLASS_NAME)[0]
    def presentation = new LookupElementPresentation()
    element.renderElement(presentation)
    assert 'foo' == presentation.itemText
    myFixture.type '\n'
    myFixture.checkResult '''
class A {
  static void foo() {}

  static void goo() {
    foo();<caret>
  }
}
'''
  }

  private void doTest(String input, boolean importStatic, String output) {
    myFixture.configureByText("a.java", input)

    def item = assertOneElement(myFixture.complete(CompletionType.CLASS_NAME))
    if (importStatic) {
      item.'as'(StaticallyImportable).shouldBeImported = true
    }
    myFixture.type('\n')
    myFixture.checkResult output
  }

}
