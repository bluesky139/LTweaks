package li.lingfeng.ltweaks.compiler;

import com.google.auto.service.AutoService;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.xml.parsers.DocumentBuilderFactory;

import li.lingfeng.ltweaks.lib.ResLoad;
import li.lingfeng.ltweaks.lib.XposedLoad;
import li.lingfeng.ltweaks.lib.ZygoteLoad;

/**
 * Created by smallville on 2016/12/22.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "li.lingfeng.ltweaks.lib.ZygoteLoad",
        "li.lingfeng.ltweaks.lib.XposedLoad",
        "li.lingfeng.ltweaks.lib.ResLoad"
})
public class XposedProcessor extends AbstractProcessor {

    private Messager mMessager;
    private String mAppPath;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if (annotations.size() != 3) {
            return false;
        }

        try {
            Iterator<TypeElement> iterator = (Iterator<TypeElement>) annotations.iterator();
            TypeElement resTypeElement = iterator.next();
            TypeElement xposedTypeElement = iterator.next();
            TypeElement zygoteTypeElement = iterator.next();
            generateXposedLoader(xposedTypeElement, zygoteTypeElement, resTypeElement, env);
            generatePrefKeys();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void generateXposedLoader(TypeElement xposedTypeElement, TypeElement zygoteTypeElement, TypeElement resTypeElement, RoundEnvironment env) throws Exception {
        JavaFileObject genFile = processingEnv.getFiler().createSourceFile("li.lingfeng.ltweaks.xposed.XposedLoader");
        String genFilePath = genFile.toUri().toString();
        mMessager.printMessage(Diagnostic.Kind.NOTE, "XposedProcessor is generating " + genFilePath);
        Writer writer = genFile.openWriter();
        mAppPath = genFilePath.substring(0, genFilePath.lastIndexOf("/build/generated/source/apt/"));
        mMessager.printMessage(Diagnostic.Kind.NOTE, "XposedProcessor mAppPath = " + mAppPath);

        writer.write("package li.lingfeng.ltweaks.xposed;\n\n");
        writer.write("public class XposedLoader extends Xposed {\n\n");

        // Generate for zygote modules
        mMessager.printMessage(Diagnostic.Kind.NOTE, "XposedProcessor zygoteElement " + zygoteTypeElement.toString());
        writer.write("    @Override\n");
        writer.write("    protected void addZygoteModules() {\n");
        for (Element element_ : env.getElementsAnnotatedWith(zygoteTypeElement)) {
            TypeElement element = (TypeElement) element_;
            ZygoteLoad load = element.getAnnotation(ZygoteLoad.class);
            mMessager.printMessage(Diagnostic.Kind.NOTE, "XposedProcessor " + element.getSimpleName());
            writer.write("        addZygoteModule(" + element.getQualifiedName() + ".class);\n");
        }
        writer.write("    }\n");

        // Generate for all packages
        mMessager.printMessage(Diagnostic.Kind.NOTE, "XposedProcessor xposedTypeElement " + xposedTypeElement.toString() + " for all packages");
        writer.write("    @Override\n");
        writer.write("    protected void addModulesForAll() {\n");
        for (Element element_ : env.getElementsAnnotatedWith(xposedTypeElement)) {
            TypeElement element = (TypeElement) element_;
            XposedLoad load = element.getAnnotation(XposedLoad.class);
            if (load.packages().length == 0) {
                mMessager.printMessage(Diagnostic.Kind.NOTE, "XposedProcessor " + element.getSimpleName());
                writer.write("        addModuleForAll(" + element.getQualifiedName() + ".class);\n");
            }
        }
        writer.write("    }\n");

        // Generate by package names
        mMessager.printMessage(Diagnostic.Kind.NOTE, "XposedProcessor xposedTypeElement " + xposedTypeElement.toString());
        writer.write("    @Override\n");
        writer.write("    protected void addModules() {\n");
        for (Element element_ : env.getElementsAnnotatedWith(xposedTypeElement)) {
            TypeElement element = (TypeElement) element_;
            XposedLoad load = element.getAnnotation(XposedLoad.class);
            for (String packageName : load.packages()) {
                mMessager.printMessage(Diagnostic.Kind.NOTE, "XposedProcessor " + packageName + " -> " + element.getSimpleName());
                writer.write("        addModule(\"" + packageName + "\", " + element.getQualifiedName() + ".class);\n");
            }
        }
        writer.write("    }\n");

        // Generate by pref keys
        Map<Integer, String> keys = getKeyMap();
        writer.write("    @Override\n");
        writer.write("    protected void addModulePrefs() {\n");
        for (Element element_ : env.getElementsAnnotatedWith(xposedTypeElement)) {
            TypeElement element = (TypeElement) element_;
            XposedLoad load = element.getAnnotation(XposedLoad.class);
            for (int prefId : load.prefs()) {
                String key = keys.get(prefId);
                mMessager.printMessage(Diagnostic.Kind.NOTE, "XposedProcessor " + key + " -> " + element.getSimpleName());
                writer.write("        addModulePref(" + element.getQualifiedName() + ".class, \"" + key + "\");\n");
            }
        }
        writer.write("    }\n");

        // Generate for res modules
        mMessager.printMessage(Diagnostic.Kind.NOTE, "XposedProcessor resTypeElement " + resTypeElement.toString());
        writer.write("    @Override\n");
        writer.write("    protected void addResModules() {\n");
        for (Element element_ : env.getElementsAnnotatedWith(resTypeElement)) {
            TypeElement element = (TypeElement) element_;
            ResLoad load = element.getAnnotation(ResLoad.class);
            for (String packageName : load.packages()) {
                mMessager.printMessage(Diagnostic.Kind.NOTE, "XposedProcessor " + packageName + " -> " + element.getSimpleName());
                writer.write("        addResModule(\"" + packageName + "\", " + element.getQualifiedName() + ".class);\n");
            }
        }
        writer.write("    }\n");

        writer.write("}");
        writer.close();
    }

    private void generatePrefKeys() throws Exception {
        JavaFileObject genFile = processingEnv.getFiler().createSourceFile("li.lingfeng.ltweaks.prefs.PrefKeys");
        Writer writer = genFile.openWriter();
        mMessager.printMessage(Diagnostic.Kind.NOTE, "XposedProcessor is generating " + genFile.toUri().toString());

        writer.write("package li.lingfeng.ltweaks.prefs;\n\n");
        writer.write("import android.util.SparseArray;\n\n");
        writer.write("public class PrefKeys {\n\n");

        Map<Integer, String> keys = getKeyMap();
        writer.write("\tprivate static SparseArray<String> id2valueMap = new SparseArray<String>(" + keys.size() + ") {{\n");
        for (Map.Entry<Integer, String> kv : keys.entrySet()) {
            writer.write("\t\tput(" + kv.getKey() + ", \"" + kv.getValue() + "\");\n");
        }
        writer.write("\t}};\n");

        writer.write("\tpublic static String getById(int id) {\n");
        writer.write("\t\treturn id2valueMap.get(id);\n");
        writer.write("\t}\n");

        writer.write("}");
        writer.close();
    }

    private Map<Integer, String> getKeyMap() throws Exception {
        Map<Integer, String> ids = new HashMap<>();  // int id -> string name, read from R.java
        Map<String, String> keys = new HashMap<>();  // string name -> string value, read from pref_keys.xml

        // Read R.java to get string id -> string name
        TypeElement rElement = processingEnv.getElementUtils().getTypeElement("li.lingfeng.ltweaks.R");
        List<TypeElement> inners = ElementFilter.typesIn(rElement.getEnclosedElements());
        TypeElement stringElement = inners.stream().filter(o -> o.getSimpleName().toString().equals("string")).iterator().next();
        List<VariableElement> idElements = ElementFilter.fieldsIn(stringElement.getEnclosedElements());
        for (VariableElement idElement : idElements) {
            if (!idElement.getSimpleName().toString().startsWith("key_")) {
                continue;
            }
            mMessager.printMessage(Diagnostic.Kind.NOTE, "R.string." + idElement.getSimpleName() + " -> " + String.format("0x%x", idElement.getConstantValue()));
            ids.put((int) idElement.getConstantValue(), idElement.getSimpleName().toString());
        }

        // Read pref_keys.xml to get string name -> string value
        String xmlPath = mAppPath + "/src/main/res/values/pref_keys.xml";
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlPath);
        NodeList nodes = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < nodes.getLength(); ++i) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) nodes.item(i);
                String key = element.getAttribute("name");
                if (!key.startsWith("key_")) {
                    continue;
                }
                String value = element.getFirstChild().getNodeValue();
                mMessager.printMessage(Diagnostic.Kind.NOTE, "Key " + key + " -> " + value);
                keys.put(key, value);
            }
        }

        // Combine string id -> string value
        Map<Integer, String> id2valueMap = new HashMap<>();
        for (Integer id : ids.keySet()) {
            String name = ids.get(id);
            String value = keys.get(name);
            id2valueMap.put(id, value);
        }
        return id2valueMap;
    }
}
