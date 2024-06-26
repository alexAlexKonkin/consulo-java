package com.intellij.roots.libraries;

import consulo.application.ApplicationManager;
import consulo.content.OrderRootType;
import consulo.content.RootProvider;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablesRegistrar;
import com.intellij.testFramework.IdeaTestCase;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

/**
 *  @author dsl
 */
public abstract class LibraryTest extends IdeaTestCase {
  public void testModification() throws Exception {
    final LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable();
    final Library library = libraryTable.createLibrary("NewLibrary");
    final boolean[] listenerNotifiedOnChange = new boolean[1];
    library.getRootProvider().addRootSetChangedListener(new RootProvider.RootSetChangedListener() {
      @Override
      public void rootSetChanged(RootProvider wrapper) {
        listenerNotifiedOnChange[0] = true;
      }

    });
    final Library.ModifiableModel modifyableModel = library.getModifiableModel();
    modifyableModel.addRoot("file://x.jar", OrderRootType.CLASSES);
    modifyableModel.addRoot("file://x-src.jar", OrderRootType.SOURCES);
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        modifyableModel.commit();
      }
    });
    assertTrue(listenerNotifiedOnChange[0]);

    listenerNotifiedOnChange[0] = false;

    final Library.ModifiableModel modifyableModel1 = library.getModifiableModel();
    modifyableModel1.setName("library");
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        modifyableModel1.commit();
      }
    });
    assertFalse(listenerNotifiedOnChange[0]);

    final Element element = new Element("root");
    library.writeExternal(element);
    assertEquals("<root><library name=\"library\"><CLASSES><root url=\"file://x.jar\" /></CLASSES><JAVADOC /><SOURCES><root url=\"file://x-src.jar\" /></SOURCES></library></root>",
            new XMLOutputter().outputString(element));

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        libraryTable.removeLibrary(library);
      }
    });
  }
}
