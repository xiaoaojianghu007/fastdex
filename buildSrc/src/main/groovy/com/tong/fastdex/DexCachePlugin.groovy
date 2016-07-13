package com.tong.fastdex;

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Copy

/**
 * 缓存dex,hook android打包流程并应用已缓存的dex
 * Created by tong on 16/7/13.
 */
class DexCachePlugin implements Plugin<Project>,TaskExecutionListener  {
    public static final String PROJECT_NAME = "fastdex"
    private static final String CACHE_DIR_NAME = 'build-' + PROJECT_NAME
    private static final boolean DEBUG = false;

    private File cacheDir;
    private String bootTaskName;
    private Project appProject;

    File getCacheDir() {
        return cacheDir
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
        File cacheDir = getCacheDir();

        if (!cacheDir.exists()) {
            cacheDir.mkdir()
        }
    }

    List<String> scanKeepMainDexList(Project project) {
        List<String> keepMainDexList = new ArrayList()
        keepMainDexList.add("android/support/multidex/**")

        //dlog('hookJar ext: ' + project.fastdex.rootPackage)

        if (project.fastdex.rootPackage == null || "".equals(project.fastdex.rootPackage.trim())) {
            dlog('scanKeepMainDexList: use applicationId: ' + project.android.defaultConfig.applicationId)
            keepMainDexList.add(project.android.defaultConfig.applicationId.replaceAll("\\.","/") + "/**")
        }
        else {
            dlog('scanKeepMainDexList: use rootPackage: ' + project.fastdex.rootPackage)
            keepMainDexList.add(project.fastdex.rootPackage.replaceAll("\\.","/") + "/**")
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
            log("has cached dex: " + dexFile.getAbsolutePath())
            return true
        }
        //dlog('not found cached dex')
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
        unzipDir.delete()
        unzipDir.mkdirs()

        List<String> mainDexList = scanKeepMainDexList(project)

        project.copy {
            from project.zipTree(zipFile)

            for (String pattern : mainDexList) {
                log('hookJar: ' + pattern)
                include pattern
            }
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
    }

    void apply(Project project) {
        project.extensions.create(PROJECT_NAME, AppExtension.class, project)
        dlog('ext: ' + project.fastdex.rootPackage)

        project.gradle.addListener(new TimeListener())
        appProject = project;
        project.gradle.addListener(this)
        cacheDir = new File(project.getProjectDir(),'/' + CACHE_DIR_NAME);

        def bootTaskName = project.gradle.startParameter.taskNames[0]
        dlog('bootTaskName: ' + bootTaskName)

        project.task('cleanDex', group: PROJECT_NAME, description: 'Clean all dex',type: Delete,dependsOn: 'clean') {
            dlog('cleanDex')
            delete getCacheDir()
        }

        project.task('cacheDex', group: PROJECT_NAME, description: 'Build all dex',type: Copy,dependsOn: ['cleanDex','transformClassesWithDexForDebug']) {
            esureCacheDir();
            dlog('buildDexTask')
            from('build/intermediates/transforms/dex/debug/folders/1000/1f/main/')
            into(getCacheDir())

            include('classes3.dex')
            rename('classes3.dex','classes4.dex')

            include('classes2.dex')
            rename('classes2.dex','classes3.dex')

            include('classes.dex')
            rename('classes.dex','classes2.dex')
        }
    }
}