package io.swagger.codegen.languages;

import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.SupportingFile;
import io.swagger.models.Operation;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;

import java.util.*;

public class SpringBootSpecCodegen extends JavaClientCodegen implements CodegenConfig {
    protected String invokerPackage = "io.swagger.api";
    protected String groupId = "io.swagger";
    protected String artifactId = "swagger-spring-boot-spec";
    protected String artifactVersion = "1.0.0";
    protected String sourceFolder = "src/main/java";
    protected String title = "Generated Server Spec";

    protected String configPackage = "";

    public SpringBootSpecCodegen() {
        super();

        outputFolder = "generated-code/javaSpringBoot";
        modelTemplateFiles.put("model.mustache", ".java");
        apiTemplateFiles.put("api.mustache", ".java");
        apiTemplateFiles.put("test.mustache", "Tests.java");
        templateDir = "JavaSpringBoot";

        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList(
                        "String",
                        "boolean",
                        "Boolean",
                        "Double",
                        "Integer",
                        "Long",
                        "Float")
        );
    }

    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    public String getName() {
        return "spring-boot";
    }

    public String getHelp() {
        return "Generates a Java Spring-boot application specification.";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        invokerPackage = (String)additionalProperties.get("invokerPackage");
        apiPackage = String.format("%s.api", invokerPackage);
        modelPackage = String.format("%s.model", invokerPackage);
        configPackage = String.format("%s.configuration", invokerPackage);
        additionalProperties.put("title", title);
        additionalProperties.put("apiPackage", apiPackage);
        additionalProperties.put("configPackage", configPackage);

        supportingFiles.clear();
        supportingFiles.add(new SupportingFile("pom.mustache", "", "pom-swagger.xml"));
    }

    @Override
    public String getTypeDeclaration(Property p) {
        if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            Property inner = ap.getItems();
            return getSwaggerType(p) + "<" + getTypeDeclaration(inner) + ">";
        } else if (p instanceof MapProperty) {
            MapProperty mp = (MapProperty) p;
            Property inner = mp.getAdditionalProperties();

            return getTypeDeclaration(inner);
        }
        return super.getTypeDeclaration(p);
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co, Map<String, List<CodegenOperation>> operations) {
        Map<String, Object> extensions = operation.getVendorExtensions();
        String routerControllerKey = "x-swagger-router-controller";
        String basePath = resourcePath;
        if (basePath.startsWith("/")) {
            basePath = basePath.substring(1);
        }
        int pos = basePath.indexOf("/");
        if (pos > 0) {
            basePath = basePath.substring(0, pos);
        }
        if (extensions.containsKey(routerControllerKey)) {
            basePath = ((String)extensions.get(routerControllerKey)).toLowerCase();
        }

        if (basePath == "") {
            basePath = "default";
        } else {
            if (co.path.startsWith("/" + basePath)) {
                co.path = co.path.substring(("/" + basePath).length());
            }
            co.subresourceOperation = !co.path.isEmpty();
        }
        List<CodegenOperation> opList = operations.get(basePath);
        if (opList == null) {
            opList = new ArrayList<CodegenOperation>();
            operations.put(basePath, opList);
        }
        opList.add(co);
        co.baseName = basePath;
    }

    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        if (operations != null) {
            List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
            for (CodegenOperation operation : ops) {
                if (operation.returnType == null) {
                    operation.returnType = "Void";
                } else if (operation.returnType.startsWith("List")) {
                    String rt = operation.returnType;
                    int end = rt.lastIndexOf(">");
                    if (end > 0) {
                        operation.returnType = rt.substring("List<".length(), end);
                        operation.returnContainer = "List";
                    }
                } else if (operation.returnType.startsWith("Map")) {
                    String rt = operation.returnType;
                    int end = rt.lastIndexOf(">");
                    if (end > 0) {
                        operation.returnType = rt.substring("Map<".length(), end);
                        operation.returnContainer = "Map";
                    }
                } else if (operation.returnType.startsWith("Set")) {
                    String rt = operation.returnType;
                    int end = rt.lastIndexOf(">");
                    if (end > 0) {
                        operation.returnType = rt.substring("Set<".length(), end);
                        operation.returnContainer = "Set";
                    }
                }
            }
        }
        return objs;
    }
}

