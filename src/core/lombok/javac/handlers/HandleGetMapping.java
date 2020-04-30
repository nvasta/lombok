package lombok.javac.handlers;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import lombok.core.AST;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import org.mangosdk.spi.ProviderFor;
import org.springframework.web.bind.annotation.GetMapping;

import static lombok.javac.handlers.JavacHandlerUtil.chainDots;


/**
 * The HandleRequestMapping Class
 *
 * The handler for the spring boot annotation
 * {@link org.springframework.web.bind.annotation.RequestMapping} which deals
 * with the automated definitions (based on roles and attributes) for the
 * {@link org.springframework.security.access.prepost.PreAuthorize}.
 *
 * @author nva
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleGetMapping extends JavacAnnotationHandler<GetMapping> {

    /**
     * The main function of the handler that deals with the spring boot
     * {@link org.springframework.web.bind.annotation.RequestMapping} annotation
     * and adds {@link org.springframework.security.access.prepost.PreAuthorize}
     * based on the auto-generated privilege names.
     *
     * @param annotation The actual annotation - use this object to retrieve the annotation parameters.
     * @param ast The javac AST node representing the annotation.
     * @param annotationNode The Lombok AST wrapper around the 'ast' parameter. You can use this object
     * to travel back up the chain (something javac AST can't do) to the parent of the annotation, as well
     */
    @Override
    public void handle(AnnotationValues<GetMapping> annotation, JCTree.JCAnnotation ast, JavacNode annotationNode) {
        // Get the parent node of the annotation
        JavacNode parentNode = annotationNode.directUp();

        // The parent node should always be a function - i.e. the controller endpoint
        if(parentNode.getKind() == AST.Kind.METHOD) {
            GetMapping getMapping = annotation.getInstance();
            JavacTreeMaker controllerClassTreeMaker = parentNode.getTreeMaker();
            addPreAuthAnnotations(getMapping, annotationNode, parentNode, controllerClassTreeMaker);
        }

    }

    /**
     * Adds the {@link org.springframework.security.access.prepost.PreAuthorize}
     * annotation to the provided method javac node.
     *
     * @param getMapping        The GetMapping annotation
     * @param annotationNode    The annotation javac node
     * @param controllerMethod  The controller method javac node
     * @param treeMaker         The javac tree maker
     */
    private void addPreAuthAnnotations(GetMapping getMapping, JavacNode annotationNode, JavacNode controllerMethod, JavacTreeMaker treeMaker) {
        // Get the JC nodes of the controller and the annotated method
        JavacNode controllerClass = JavacHandlerUtil.upToTypeNode(controllerMethod);
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) controllerClass.get();
        JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) controllerMethod.get();

        /*
        // TODO: This could be a usefull starting point
        //ClassLoader.getSystemResourceAsStream("pom.properties");

        // Get the request mapping annotation from the parent class
        String urlPrefix = "";
        for(JCTree.JCAnnotation jcAnnotation : classDecl.getModifiers().getAnnotations()) {
            if(jcAnnotation.getAnnotationType().toString().contains("RequestMapping")) {
                urlPrefix = jcAnnotation.getArguments().stream().collect(Collectors.toList()).toString();
            }
        }

        // Get the method annotation info
        String url = Arrays.asList(requestMapping.path())
                .stream()
                .findFirst()
                .orElse(null);
        */

        // Add the annotation
        JCTree.JCExpression preAuthoriseType = chainDots(controllerMethod, "org", "springframework", "security", "access", "prepost", "PreAuthorize");
        JCTree.JCExpression valueExpr = treeMaker.Assign(treeMaker.Ident(annotationNode.toName("value")), treeMaker.Literal("hasAnyRole('ROLES_OASES_USER')"));
        JCTree.JCAnnotation annotation = treeMaker.Annotation(preAuthoriseType, List.of(valueExpr));
        methodDecl.mods.annotations = methodDecl.mods.annotations.append(annotation);
    }

}
