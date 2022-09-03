package consulo.java.manifest.editor;

import javax.annotation.Nonnull;

import consulo.fileEditor.FileEditorPolicy;
import consulo.fileEditor.FileEditorProvider;
import org.jdom.Element;
import org.osmorc.manifest.lang.ManifestFileType;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorState;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author VISTALL
 * @since 12:29/03.05.13
 */
public class ManifestEditorProvider implements FileEditorProvider {
  public static final String EDITOR_ID = ManifestEditorProvider.class.getName();

  @Override
  public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
    return file.getFileType() instanceof ManifestFileType;
  }

  @Nonnull
  @Override
  public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file) {
    return new ManifestEditor(project, file);
  }

  @Override
  public void disposeEditor(@Nonnull FileEditor editor) {
    Disposer.dispose(editor);
  }

  @Nonnull
  @Override
  public FileEditorState readState(@Nonnull Element sourceElement, @Nonnull Project project, @Nonnull VirtualFile file) {
    return FileEditorState.INSTANCE;
  }

  @Override
  public void writeState(@Nonnull FileEditorState state, @Nonnull Project project, @Nonnull Element targetElement) {
  }

  @Nonnull
  @Override
  public String getEditorTypeId() {
    return EDITOR_ID;
  }

  @Nonnull
  @Override
  public FileEditorPolicy getPolicy() {
    return FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR;
  }
}
