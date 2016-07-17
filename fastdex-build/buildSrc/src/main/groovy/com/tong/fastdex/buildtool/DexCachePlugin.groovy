package com.tong.fastdex.buildtool

import com.android.build.gradle.api.BaseVariant
import com.tong.fastdex.buildtool.utils.FastDexUtils
import com.tong.fastdex.buildtool.utils.FolderComparator
import com.tong.fastdex.buildtool.utils.FolderComparatorFactory;
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.compile.JavaCompile

/**
 * 缓存dex,hook android打包流程并应用已缓存的dex
 * Created by tong on 16/7/13.
 */
class DexCachePlugin implements Plugin<Project>,TaskExecutionListener  {
    public static final String PROJECT_NAME = "fastdex"
    public static final String CACHE_DIR_NAME = 'build-' + PROJECT_NAME
    public static final boolean DEBUG = false;

    private File cacheDir;
    private String bootTaskName;
    private Project appProject;

    File getCacheDir() {
        return cacheDir
    }

    File getSourceDir() {
        return new File(cacheDir,"compile_source")
    }

    void dlog(String msg) {
        if (DEBUG) {
            println('==='+ PROJECT_NAME.toUpperCase() + ': ' + msg)
        }
    }

    void log(String msg) {
        println('==='+ PROJECT_NAME.toUpperCase() + ': ' + msg)
    }

    void esureCacheDir() {
        dlog('esureCacheDir')
        File cacheDir = getCacheDir();

        if (!cacheDir.exists()) {
            dlog('create dir: ' + cacheDir.getAbsolutePath())
            cacheDir.mkdir()
        }
    }

    Set<String> scanKeepMainDexList(Project project) {
        Map<String,String> properties = new HashMap<>()
        File configFile = new File(getCacheDir(),"fasetdex-config.properties")
        configFile.eachLine {line->
            if (line != null && !line.startsWith("#")) {
                String[] keyValue = line.split("=")
                properties.put(keyValue[0].trim(),keyValue[1].trim())
            }
        }

        String packageName = properties.get("package.name")
        Set<String> keepMainDexList = new HashSet<>()
        keepMainDexList.add("android/support/multidex/**")
        keepMainDexList.add(packageName.replaceAll("\\.","/") + "/R.class")
        keepMainDexList.add(packageName.replaceAll("\\.","/") + "/R\$**.class")

        FolderComparator folderComparator = null
        dlog('scanKeepMainDexList incremental: ' + project.fastdex.incremental)
        if (project.fastdex.incremental
                && (folderComparator = FolderComparatorFactory.createFolderComparator(project)) != null) {
            //增量更新
            Set<String> diffFileSet =
                    folderComparator.compare(project,getCacheDir(),new File(project.getProjectDir(),"src/main/java"),new File(getCacheDir(),"java"))
            if (diffFileSet != null) {
                for (String path : diffFileSet) {
                    keepMainDexList.add(path.replaceAll(".java",".class"))
                }
            }
        }
        else {
            dlog('hookJar ext: ' + project.fastdex.rootPackage)
            if (project.fastdex.rootPackage == null || "".equals(project.fastdex.rootPackage.trim())) {
                dlog('scanKeepMainDexList: use applicationId: ' + project.android.defaultConfig.applicationId)
                keepMainDexList.add(project.android.defaultConfig.applicationId.replaceAll("\\.","/") + "/**")
            }
            else {
                dlog('scanKeepMainDexList: use rootPackage: ' + project.fastdex.rootPackage)
                keepMainDexList.add(project.fastdex.rootPackage.replaceAll("\\.","/") + "/**")
            }
        }
        //是否存在${projectdir}/keep_main_dex_list.txt
        File rulesFile = new File(project.getProjectDir(),PROJECT_NAME + "_keep_main_dex.txt");
        if (rulesFile.exists() && rulesFile.isFile()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(rulesFile)));
            String line = null;
            while((line = br.readLine()) != null){
                if (line != null && !line.startsWith("#")) {
                    keepMainDexList.add(line.replaceAll("\\.","/"))
                }
            }
            br.close();
        }
        return keepMainDexList;
    }

    boolean hasCachedDex() {
        File dexFile = null;
        for (int point = 2;(dexFile = new File(getCacheDir(),"classes" + point + ".dex")).exists();point++) {
            dlog("has cached dex")
            return true
        }
        return false
    }

    void hookJar(Project project) {
        dlog('hookJar transformClassesWithJarMergingForDebug')

        File zipFile = new File(project.getBuildDir(),"/intermediates/transforms/jarMerging/debug/jars/1/1f/combined.jar")
        if (!zipFile.exists()) {
            dlog('hookJar file not found: ' + zipFile.getAbsolutePath())
            return
        }

        File unzipDir = new File(getCacheDir(),"combined")
        FastDexUtils.deleteFile(unzipDir)
        if (unzipDir.exists()) {
            throw new IllegalStateException("del combined dir fail: " + unzipDir.getAbsolutePath())
        }
        unzipDir.mkdirs()

        Set<String> mainDexList = scanKeepMainDexList(project)

        project.copy {
            from project.zipTree(zipFile)

            for (String pattern : mainDexList) {
                log('hookJar main_dex_pattern: ' + pattern)
                include pattern
            }
            into unzipDir
        }

        project.copy {
            from new File(getCacheDir(),"classes")
            into unzipDir
        }

        zipFile.delete()
        project.ant.zip(baseDir: unzipDir, destFile: zipFile)
    }

    void hookDex(Project project) {
        dlog('hookDex transformClassesWithDexForDebug')

        project.copy {
            from(getCacheDir())
            into('build/intermediates/transforms/dex/debug/folders/1000/1f/main/')

            File dexFile = null;
            for (int point = 2;(dexFile = new File(getCacheDir(),"classes" + point + ".dex")).exists();point++) {
                log("use cache dex: " + dexFile)
                include(dexFile.getName())
            }
        }
    }

    void replaceManifestApplicationClassName(Project project) {
        dlog("replace minifest application class name")
        File manifestFile = FastDexUtils.getManifestFile(project)
        String content = FastDexUtils.readUTF8Content(manifestFile)

        Map<String,String> properties = new HashMap<>()
        File configFile = new File(getCacheDir(),"fasetdex-config.properties")
        configFile.eachLine {line->
            if (line != null && !line.startsWith("#")) {
                String[] keyValue = line.split("=")
                properties.put(keyValue[0].trim(),keyValue[1].trim())
            }
        }
        String originApplicationClassName = properties.get("origin.application.name")
        String delegateApplicationClassName = properties.get("delegate.application.name")

        dlog("originApplicationClassName: ${originApplicationClassName}")
        dlog("delegateApplicationClassName: ${delegateApplicationClassName}")
        content = content.replaceAll(originApplicationClassName,delegateApplicationClassName)

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(manifestFile)))
        writer.write(content)
        writer.flush()
        writer.close()
    }

    @Override
    void beforeExecute(Task task) {

    }

    @Override
    void afterExecute(Task task, TaskState state) {
        if ('transformClassesWithJarMergingForDebug'.equals(task.name)) {
            //hook /intermediates/transforms/jarMerging/debug/jars/1/1f/combined.jar
            if (hasCachedDex()) {
                hookJar(appProject)
            }
        }
        else if ('transformClassesWithDexForDebug'.equals(task.name)) {
            if (hasCachedDex()) {
                hookDex(appProject)
            }
        }
        else if ('processDebugManifest'.equals(task.name)) {
            if (hasCachedDex()) {
                replaceManifestApplicationClassName(appProject)
            }
        }
        else if ('assembleDebug'.equals(task.name)) {
            if (hasCachedDex()) {
                log("***请使用5.0以上的设备调试，否则会报类找不到***")
            }
        }
    }

    void apply(Project project) {
        if (project.getProjectDir().equals(project.getRootDir())) {
            return
        }
        project.extensions.create(PROJECT_NAME, AppExtension.class, project)

        project.gradle.addListener(new TimeListener())
        appProject = project;
        project.gradle.addListener(this)
        cacheDir = new File(project.getProjectDir(),'/' + CACHE_DIR_NAME);

        def bootTaskName = project.gradle.startParameter.taskNames[0]
        dlog('bootTaskName: ' + bootTaskName + ' projectDir: ' + project.getProjectDir())

//        project.afterEvaluate {
//            Task processDebugManifest = project.tasks["processDebugManifest"]
//            processDebugManifest.doLast {
//                dlog('=processDebugManifest doLast')
//            }
//        }


        def sourceCompatibility = '1.7'
        def targetCompatibility = '1.7'

        project.afterEvaluate {
            if (!project.android.hasProperty('applicationVariants')) return
            project.android.applicationVariants.all { BaseVariant variant ->
                if (variant.buildType.name == 'debug') {
                    variant.javaCompile.doFirst { JavaCompile it2 ->
                        sourceCompatibility = it2.sourceCompatibility
                        targetCompatibility = it2.targetCompatibility

                        dlog("sourceCompatibility ${sourceCompatibility}")
                        dlog("targetCompatibility ${targetCompatibility}")
                    }
                }
            }
        }

        project.task('cleanDex', group: PROJECT_NAME, description: 'Clean all dex',type: Delete,dependsOn: 'clean') {
            project.afterEvaluate {
                dlog('cleanDex')
                delete getCacheDir()
            }
        }

        project.task('cacheDex', group: PROJECT_NAME, description: 'Build all dex',dependsOn: ['cleanDex','transformClassesWithDexForDebug']) {
            doLast {
                dlog('cacheDex doLast')
                esureCacheDir()

                //缓存dex
                project.copy {
                    from(new File(project.getBuildDir(), "/intermediates/transforms/dex/debug/folders/1000/1f/main/"))
                    into(getCacheDir())

                    include('classes3.dex')
                    rename('classes3.dex', 'classes4.dex')

                    include('classes2.dex')
                    rename('classes2.dex', 'classes3.dex')

                    include('classes.dex')
                    rename('classes.dex', 'classes2.dex')
                }

                dlog('==project.fastdex.incremental: ' + project.fastdex.incremental)
                //对source目录做快照
                new File(getCacheDir(), "/java").delete()
                if (project.fastdex.incremental) {
                    //如果开启增量更新，对sourceSet对快照
                    project.copy {
                        from(new File(project.getProjectDir(), 'src/main/java'))
                        into(new File(getCacheDir(), "/java"))
                    }
                }


                //解析数据，写入到配置文件里
                File manifestFile = FastDexUtils.getManifestFile(project)
                String applicationClassName = FastDexUtils.getApplicationClassName(manifestFile)
                String packageName = FastDexUtils.getManifestPackageName(manifestFile)

                File propertiesFile = new File(getCacheDir(),"fasetdex-config.properties")
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(propertiesFile)))

                writer.writeLine("#<application android:name=\"\${value}\" >")
                writer.writeLine("origin.application.name=${applicationClassName}")
                writer.writeLine("package.name=${packageName}")
                writer.writeLine("delegate.application.name=fastdex_classes.FastDexDelegateApplication")
                writer.writeLine("delegate.application.class=fastdex_classes/FastDexDelegateApplication.class")
                writer.close()


                //创建source目录
                File delegateAppDir = new File(getSourceDir(), "fastdex_classes")
                delegateAppDir.mkdirs();

                File dalegateAppFile = new File(delegateAppDir, "FastDexDelegateApplication.java")

                def printWriter = dalegateAppFile.newPrintWriter()
                printWriter.write("package fastdex_classes;\n" +
                        "\n" +
                        "import android.app.Application;\n" +
                        "import android.app.Instrumentation;\n" +
                        "import android.content.Context;\n" +
                        "import android.util.Log;\n" +
                        "import java.lang.reflect.Field;\n" +
                        "import java.lang.reflect.Method;\n" +
                        "import android.support.multidex.MultiDexApplication;\n" +
                        "\n" +
                        "/**\n" +
                        " * Created by tong on 16/7/16.\n" +
                        " */\n" +
                        "public class FastDexDelegateApplication extends MultiDexApplication {\n" +
                        "    private static final String TAG = \"FASTDEX\";\n" +
                        "\n" +
                        "    private String originApplicationClassName = \"${applicationClassName}\";\n" +
                        "\n" +
                        "    public void onCreate() {\n" +
                        "        super.onCreate();\n" +
                        "\n" +
                        "        Log.d(TAG, \"originApplicationClassName: \" + originApplicationClassName);\n" +
                        "        Log.d(TAG, \"currentApplicationClassName: \" + FastDexDelegateApplication.class);\n" +
                        "\n" +
                        "        try {\n" +
                        "            Class e = Class.forName(originApplicationClassName);\n" +
                        "            Application originApplication = Instrumentation.newApplication(e, (Context) this);\n" +
                        "            Instrumentation appInstrumentation = this.getInstrumentation();\n" +
                        "            appInstrumentation.callApplicationOnCreate(originApplication);\n" +
                        "        } catch (Exception var5) {\n" +
                        "            var5.printStackTrace();\n" +
                        "        }\n" +
                        "\n" +
                        "    }\n" +
                        "\n" +
                        "    private Instrumentation getInstrumentation() {\n" +
                        "        try {\n" +
                        "            Class e = Class.forName(\"android.app.ActivityThread\");\n" +
                        "            Method method = e.getMethod(\"currentActivityThread\", new Class[0]);\n" +
                        "            Object thread = method.invoke((Object)null, (Object[])null);\n" +
                        "            Field field = e.getDeclaredField(\"mInstrumentation\");\n" +
                        "            field.setAccessible(true);\n" +
                        "            return (Instrumentation)field.get(thread);\n" +
                        "        } catch (Exception var5) {\n" +
                        "            var5.printStackTrace();\n" +
                        "            return null;\n" +
                        "        }\n" +
                        "    }\n" +
                        "}")
                printWriter.flush()
                printWriter.close()


                //编译代理类
                dlog("compile FastDexDelegateApplication.java")
                new File(getCacheDir(), "classes").mkdirs()

                def cf = project.android.defaultConfig
                def androidJar = new File(project.android.getSdkDirectory(), "platforms/android-${cf.targetSdkVersion.getApiLevel()}/android.jar")

                File multidexJarDir = new File(project.getBuildDir(), "/intermediates/exploded-aar/com.android.support/multidex")
                String multidexJarVersion = multidexJarDir.listFiles()[0].getName()
                File multidexJar = new File(multidexJarDir, "${multidexJarVersion}/jars/classes.jar")
                project.ant.javac(
                        srcdir: getSourceDir(),
                        source: sourceCompatibility,
                        target: targetCompatibility,
                        destdir: new File(getCacheDir(), "classes"),
                        bootclasspath: androidJar.getAbsolutePath(),
                        classpath: multidexJar.getAbsolutePath())

                replaceManifestApplicationClassName(project)
            }
        }
    }
}