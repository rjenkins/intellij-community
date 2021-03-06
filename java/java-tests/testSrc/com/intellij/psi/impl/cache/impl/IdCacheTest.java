package com.intellij.psi.impl.cache.impl;

import com.intellij.JavaTestUtil;
import com.intellij.codeInsight.CodeInsightTestCase;
import com.intellij.ide.todo.TodoConfiguration;
import com.intellij.ide.todo.TodoIndexPatternProvider;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.impl.cache.impl.id.IdIndex;
import com.intellij.psi.impl.cache.impl.todo.TodoIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.TodoAttributes;
import com.intellij.psi.search.TodoPattern;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.indexing.FileBasedIndex;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class IdCacheTest extends CodeInsightTestCase{

  private VirtualFile myRootDir;
  private File myCacheFile;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    final FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
    fileBasedIndex.requestRebuild(IdIndex.NAME);
    fileBasedIndex.requestRebuild(TodoIndex.NAME);

    String root = JavaTestUtil.getJavaTestDataPath()+ "/psi/impl/cache/";

    PsiTestUtil.removeAllRoots(myModule, JavaSdkImpl.getMockJdk17());
    myRootDir = PsiTestUtil.createTestProjectStructure(myProject, myModule, root, myFilesToDelete);

    myCacheFile = FileUtil.createTempFile("cache", "");
    myCacheFile.delete();
    myFilesToDelete.add(myCacheFile);
  }

  public void testBuildCache() throws Exception {
    checkCache(CacheManager.SERVICE.getInstance(myProject));
  }

  public void testLoadCacheNoTodo() throws Exception {

    final CacheManager cache = CacheManager.SERVICE.getInstance(myProject);

    checkResult(new String[]{"1.java", "2.java"}, convert(cache.getFilesWithWord("b", UsageSearchContext.ANY, GlobalSearchScope.projectScope(myProject), false)));
  }

  public void testUpdateCache1() throws Exception {
    myRootDir.createChildData(null, "4.java");
    Thread.sleep(1000);
    checkCache(CacheManager.SERVICE.getInstance(myProject));
  }

  public void testUpdateCache2() throws Exception {
    VirtualFile child = myRootDir.findChild("1.java");
    VfsUtil.saveText(child, "xxx");

    PsiDocumentManager.getInstance(myProject).commitAllDocuments();
    FileDocumentManager.getInstance().saveAllDocuments();

    final CacheManager cache = CacheManager.SERVICE.getInstance(myProject);
    final GlobalSearchScope scope = GlobalSearchScope.projectScope(myProject);
    checkResult(new String[] {"1.java"}, convert(cache.getFilesWithWord("xxx", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{}, convert(cache.getFilesWithWord("a", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"2.java"}, convert(cache.getFilesWithWord("b", UsageSearchContext.ANY,scope, false)));
    checkResult(new String[]{"2.java", "3.java"}, convert(cache.getFilesWithWord("c", UsageSearchContext.ANY,scope, false)));
    checkResult(new String[]{"2.java", "3.java"}, convert(cache.getFilesWithWord("d", UsageSearchContext.ANY,scope, false)));
    checkResult(new String[]{"3.java"}, convert(cache.getFilesWithWord("e", UsageSearchContext.ANY,scope, false)));

    checkResult(new String[]{"3.java"}, convert(cache.getFilesWithTodoItems()));
    assertEquals(0, cache.getTodoCount(myRootDir.findChild("1.java"), TodoIndexPatternProvider.getInstance()));
    assertEquals(0, cache.getTodoCount(myRootDir.findChild("2.java"), TodoIndexPatternProvider.getInstance()));
    assertEquals(2, cache.getTodoCount(myRootDir.findChild("3.java"), TodoIndexPatternProvider.getInstance()));
  }

  public void testUpdateCache3() throws Exception {
    VirtualFile child = myRootDir.findChild("1.java");
    child.delete(null);

    final CacheManager cache2 = CacheManager.SERVICE.getInstance(myProject);
    final GlobalSearchScope scope = GlobalSearchScope.projectScope(myProject);
    checkResult(ArrayUtil.EMPTY_STRING_ARRAY, convert(cache2.getFilesWithWord("xxx", UsageSearchContext.ANY, scope, false)));
    checkResult(ArrayUtil.EMPTY_STRING_ARRAY, convert(cache2.getFilesWithWord("a", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"2.java"}, convert(cache2.getFilesWithWord("b", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"2.java", "3.java"}, convert(cache2.getFilesWithWord("c", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"2.java", "3.java"}, convert(cache2.getFilesWithWord("d", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"3.java"}, convert(cache2.getFilesWithWord("e", UsageSearchContext.ANY, scope, false)));

    checkResult(new String[]{"3.java"}, convert(cache2.getFilesWithTodoItems()));
    assertEquals(0, cache2.getTodoCount(myRootDir.findChild("2.java"), TodoIndexPatternProvider.getInstance()));
    assertEquals(2, cache2.getTodoCount(myRootDir.findChild("3.java"), TodoIndexPatternProvider.getInstance()));
  }

  public void testUpdateCacheNoTodo() throws Exception {
    myRootDir.createChildData(null, "4.java");
    final GlobalSearchScope scope = GlobalSearchScope.projectScope(myProject);
    final CacheManager cache = CacheManager.SERVICE.getInstance(myProject);
    checkResult(new String[]{"1.java", "2.java"}, convert(cache.getFilesWithWord("b", UsageSearchContext.ANY, scope, false)));
  }

  public void testUpdateOnTodoChange() throws Exception {
    TodoPattern pattern = new TodoPattern("newtodo", TodoAttributes.createDefault(), true);
    TodoPattern[] oldPatterns = TodoConfiguration.getInstance().getTodoPatterns();
    TodoConfiguration.getInstance().setTodoPatterns(new TodoPattern[]{pattern});

    try{
      final CacheManager cache = CacheManager.SERVICE.getInstance(myProject);
      checkResult(new String[]{"2.java"}, convert(cache.getFilesWithTodoItems()));
      assertEquals(0, cache.getTodoCount(myRootDir.findChild("1.java"), TodoIndexPatternProvider.getInstance()));
      assertEquals(1, cache.getTodoCount(myRootDir.findChild("2.java"), TodoIndexPatternProvider.getInstance()));
      assertEquals(0, cache.getTodoCount(myRootDir.findChild("3.java"), TodoIndexPatternProvider.getInstance()));
    }
    finally{
      TodoConfiguration.getInstance().setTodoPatterns(oldPatterns);
    }
  }

  public void testFileModification() throws Exception {
    final CacheManager cache = CacheManager.SERVICE.getInstance(myProject);
    checkCache(cache);

    VirtualFile child = myRootDir.findChild("1.java");

    checkCache(cache);

    VfsUtil.saveText(child, "xxx");
    PsiDocumentManager.getInstance(myProject).commitAllDocuments();

    final GlobalSearchScope scope = GlobalSearchScope.projectScope(myProject);
    checkResult(new String[] {"1.java"}, convert(cache.getFilesWithWord("xxx", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{}, convert(cache.getFilesWithWord("a", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"2.java"}, convert(cache.getFilesWithWord("b", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"2.java", "3.java"}, convert(cache.getFilesWithWord("c", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"2.java", "3.java"}, convert(cache.getFilesWithWord("d", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"3.java"}, convert(cache.getFilesWithWord("e", UsageSearchContext.ANY, scope, false)));

    checkResult(new String[]{"3.java"}, convert(cache.getFilesWithTodoItems()));
    assertEquals(0, cache.getTodoCount(myRootDir.findChild("1.java"), TodoIndexPatternProvider.getInstance()));
    assertEquals(0, cache.getTodoCount(myRootDir.findChild("2.java"), TodoIndexPatternProvider.getInstance()));
    assertEquals(2, cache.getTodoCount(myRootDir.findChild("3.java"), TodoIndexPatternProvider.getInstance()));
  }

  public void testFileDeletion() throws Exception {
    final CacheManager cache = CacheManager.SERVICE.getInstance(myProject);
    checkCache(cache);

    VirtualFile child = myRootDir.findChild("1.java");
    child.delete(null);

    final GlobalSearchScope scope = GlobalSearchScope.projectScope(myProject);
    checkResult(new String[]{}, convert(cache.getFilesWithWord("xxx", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{}, convert(cache.getFilesWithWord("a", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"2.java"}, convert(cache.getFilesWithWord("b", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"2.java", "3.java"}, convert(cache.getFilesWithWord("c", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"2.java", "3.java"}, convert(cache.getFilesWithWord("d", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"3.java"}, convert(cache.getFilesWithWord("e", UsageSearchContext.ANY, scope, false)));

    checkResult(new String[]{"3.java"}, convert(cache.getFilesWithTodoItems()));
    assertEquals(0, cache.getTodoCount(myRootDir.findChild("2.java"), TodoIndexPatternProvider.getInstance()));
    assertEquals(2, cache.getTodoCount(myRootDir.findChild("3.java"), TodoIndexPatternProvider.getInstance()));
  }

  public void testFileCreation() throws Exception {
    final CacheManager cache = CacheManager.SERVICE.getInstance(myProject);
    checkCache(cache);

    VirtualFile child = myRootDir.createChildData(null, "4.java");
    VfsUtil.saveText(child, "xxx //todo");
    PsiDocumentManager.getInstance(myProject).commitAllDocuments();

    final GlobalSearchScope scope = GlobalSearchScope.projectScope(myProject);
    checkResult(new String[]{"4.java"}, convert(cache.getFilesWithWord("xxx", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"1.java"}, convert(cache.getFilesWithWord("a", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"1.java", "2.java"}, convert(cache.getFilesWithWord("b", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"1.java", "2.java", "3.java"}, convert(cache.getFilesWithWord("c", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"2.java", "3.java"}, convert(cache.getFilesWithWord("d", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"3.java"}, convert(cache.getFilesWithWord("e", UsageSearchContext.ANY, scope, false)));

    checkResult(new String[]{"1.java", "3.java", "4.java"}, convert(cache.getFilesWithTodoItems()));
    assertEquals(1, cache.getTodoCount(myRootDir.findChild("1.java"), TodoIndexPatternProvider.getInstance()));
    assertEquals(0, cache.getTodoCount(myRootDir.findChild("2.java"), TodoIndexPatternProvider.getInstance()));
    assertEquals(2, cache.getTodoCount(myRootDir.findChild("3.java"), TodoIndexPatternProvider.getInstance()));
    assertEquals(1, cache.getTodoCount(myRootDir.findChild("4.java"), TodoIndexPatternProvider.getInstance()));
  }

  public void testCrash() throws Exception {
    final CacheManager cache = CacheManager.SERVICE.getInstance(myProject);
    cache.getFilesWithWord("xxx", UsageSearchContext.ANY, GlobalSearchScope.projectScope(myProject), false);
    System.gc();
  }

  private void checkCache(CacheManager cache) {
    final GlobalSearchScope scope = GlobalSearchScope.projectScope(myProject);
    checkResult(ArrayUtil.EMPTY_STRING_ARRAY, convert(cache.getFilesWithWord("xxx", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"1.java"}, convert(cache.getFilesWithWord("a", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"1.java", "2.java"}, convert(cache.getFilesWithWord("b", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"1.java", "2.java", "3.java"}, convert(cache.getFilesWithWord("c", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"2.java", "3.java"}, convert(cache.getFilesWithWord("d", UsageSearchContext.ANY, scope, false)));
    checkResult(new String[]{"3.java"}, convert(cache.getFilesWithWord("e", UsageSearchContext.ANY, scope, false)));

    checkResult(new String[]{"1.java", "3.java"}, convert(cache.getFilesWithTodoItems()));
    assertEquals(1, cache.getTodoCount(myRootDir.findChild("1.java"), TodoIndexPatternProvider.getInstance()));
    assertEquals(0, cache.getTodoCount(myRootDir.findChild("2.java"), TodoIndexPatternProvider.getInstance()));
    assertEquals(2, cache.getTodoCount(myRootDir.findChild("3.java"), TodoIndexPatternProvider.getInstance()));
  }

  private static VirtualFile[] convert(PsiFile[] psiFiles) {
    final VirtualFile[] files = new VirtualFile[psiFiles.length];
    for (int idx = 0; idx < psiFiles.length; idx++) {
      files[idx] = psiFiles[idx].getVirtualFile();
    }
    return files;
  }
  
  private static void checkResult(String[] expected, VirtualFile[] result){
    assertEquals(expected.length, result.length);
    
    Arrays.sort(expected);
    Arrays.sort(result, new Comparator() {
      @Override
      public int compare(Object o1, Object o2) {
        VirtualFile file1 = (VirtualFile)o1;
        VirtualFile file2 = (VirtualFile)o2;
        return file1.getName().compareTo(file2.getName());
      }
    });

    for(int i = 0; i < expected.length; i++){
      String name = expected[i];
      assertEquals(name, result[i].getName());
    }
  }
}
