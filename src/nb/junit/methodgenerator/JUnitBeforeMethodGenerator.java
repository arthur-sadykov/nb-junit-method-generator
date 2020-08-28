package nb.junit.methodgenerator;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.GeneratorUtilities;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreeUtilities;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.netbeans.spi.editor.codegen.CodeGeneratorContextProvider;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

public class JUnitBeforeMethodGenerator implements CodeGenerator {

    private static final String DISPLAY_NAME = "JUnit @Before method";
    private final JTextComponent component;
    private TreeMaker make;
    private TreePath pathToCaretPosition;
    private WorkingCopy workingCopy;

    /**
     *
     * @param context containing JTextComponent and possibly other items registered by
     *                {@link CodeGeneratorContextProvider}
     */
    private JUnitBeforeMethodGenerator(Lookup context) { // Good practice is not to save Lookup outside ctor
        component = context.lookup(JTextComponent.class);
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    /**
     * This will be invoked when user chooses this Generator from Insert Code dialog
     */
    @Override
    public void invoke() {
        JavaSource javaSource = getJavaSource();
        try {
            javaSource.runModificationTask(copy -> {
                workingCopy = copy;
                workingCopy.toPhase(Phase.RESOLVED);
                make = workingCopy.getTreeMaker();
                pathToCaretPosition = getPathToCaretPosition();
                if (isCaretInsideClassBody(pathToCaretPosition)) {
                    MethodTree testMethod = makeMethodTree();
                    insertTestMethodIntoClass(testMethod);
                }
            }).commit();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private JavaSource getJavaSource() {
        JavaSource javaSource = JavaSource.forDocument(component.getDocument());
        return Objects.requireNonNull(javaSource, () -> "javaSource must be non-null");
    }

    private MethodTree makeMethodTree() {
        return make.Method(
                make.Modifiers(
                        Collections.singleton(Modifier.PUBLIC),
                        List.of(make.Annotation(make.Type("org.junit.Before"),
                                Collections.emptyList()))),
                "setUp",
                make.PrimitiveType(TypeKind.VOID),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                make.Block(Collections.emptyList(), false),
                null);
    }

    private TreePath getPathToCaretPosition() {
        TreeUtilities treeUtilities = workingCopy.getTreeUtilities();
        return treeUtilities.pathFor(component.getCaretPosition());
    }

    private boolean isCaretInsideClassBody(TreePath pathToCaretPosition) {
        return TreeUtilities.CLASS_TREE_KINDS.contains(pathToCaretPosition.getLeaf().getKind());
    }

    private void insertTestMethodIntoClass(MethodTree testMethod) {
        ClassTree classTree = (ClassTree) pathToCaretPosition.getLeaf();
        ClassTree newClassTree = GeneratorUtilities.get(workingCopy)
                .insertClassMember(classTree, testMethod, component.getCaretPosition());
        workingCopy.rewrite(classTree, newClassTree);
    }

    @MimeRegistration(mimeType = "text/x-java", service = CodeGenerator.Factory.class, position = 5000)
    public static class Factory implements CodeGenerator.Factory {

        @Override
        public List<? extends CodeGenerator> create(Lookup context) {
            return Collections.singletonList(new JUnitBeforeMethodGenerator(context));
        }
    }
}
