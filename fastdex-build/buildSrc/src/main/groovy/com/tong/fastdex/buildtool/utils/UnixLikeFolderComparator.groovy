/*
 * Copyright 2015-present wequick.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.tong.fastdex.buildtool.utils

import org.gradle.api.Project

/**
 * linux、mac下使用shell脚本(fastdex_folder_compare.sh)进行比较,涉及的命令有diff、grep、awk、echo
 */
public class UnixLikeFolderComparator implements FolderComparator {
    private static final String SCRIPT_NAME = "fastdex_folder_compare.sh";

    Set<String> compare(Project project,File buildDir,File dir1,File dir2) {
        checkScriptFile(buildDir)
        Set<String> result = new HashSet<>()
        String cmd = "sh ${buildDir}/${SCRIPT_NAME} ${project.getProjectDir()}/src/main/java ${project.getProjectDir()}/build-fastdex/java"
        println(cmd)
        def process = cmd.execute()
        int status = process.waitFor()
        if (status == 0) {
            String exeResult = process.in.text
            println("==folder diff res: " + exeResult)
            BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(exeResult.getBytes())));
            String line = null;
            while ((line = br.readLine()) != null){
                if (line != null) {
                    result.add(line)
                }
            }
            br.close();
        }
        process.destroy()
        return result;
    }

    void checkScriptFile(File buildDir) {
        File script = new File(buildDir,SCRIPT_NAME)

        if (script.exists() && script.isFile()) {
            return
        }

        /**

         #!/bin/bash

         #sh compare.sh src/main/java build-fastdex/java
         # 对比文件夹的变化执行结果如下(diff -qr build-fastdex/java src/main/java | grep '\.java')
         # Only in src/main/java/com/dx168/epmyg/activity: AboutWebviewActivity.java
         # Only in build-fastdex/java/com/dx168/epmyg/activity: BindPhoneActivity.java
         # Files build-fastdex/java/com/dx168/epmyg/activity/BuyActivity.java and src/main/java/com/dx168/epmyg/activity/BuyActivity.java differ

         #
         #内容有变化的会被列出来
         #dir1中新增的文件也会被列出来
         #

         IFS=$'\n'
         dir1=$1
         dir2=$2
         for item in $(diff -qr ${dir1} ${dir2} | grep '\.java')
         do
            desc=$(echo ${item} | awk '{print $1}')
            if [ ${desc} == 'Files' ] ;then
                res=$(echo ${item} | awk '{print $2}')
                echo ${res##*src/main/java/}
            fi

            if [ ${desc} == 'Only' ]; then
                dir=$(echo ${item} | awk '{print $3}')
                filename=$(echo ${item} | awk '{print $4}')
                if [[ ${dir} =~ ${dir1} ]];then
                    res="${dir%:}/${filename}"
                    echo ${res##*src/main/java/}
                fi
            fi
         done
         */

        def printWriter = script.newPrintWriter()
        printWriter.write('#!/bin/bash\n' +
                '\n' +
                '#sh compare.sh src/main/java build-fastdex/java\n' +
                '# 对比文件夹的变化执行结果如下(diff -qr build-fastdex/java src/main/java | grep \'\\.java\')\n' +
                '# Only in src/main/java/com/dx168/epmyg/activity: AboutWebviewActivity.java\n' +
                '# Only in build-fastdex/java/com/dx168/epmyg/activity: BindPhoneActivity.java\n' +
                '# Files build-fastdex/java/com/dx168/epmyg/activity/BuyActivity.java and src/main/java/com/dx168/epmyg/activity/BuyActivity.java differ\n' +
                '\n' +
                '#\n' +
                '#内容有变化的会被列出来\n' +
                '#dir1中新增的文件也会被列出来\n' +
                '#\n' +
                '\n' +
                'IFS=$\'\\n\'\n' +
                'dir1=$1\n' +
                'dir2=$2\n' +
                'for item in $(diff -qr ${dir1} ${dir2} | grep \'\\.java\')\n' +
                'do \n' +
                '\tdesc=$(echo ${item} | awk \'{print $1}\')\n' +
                '\tif [ ${desc} == \'Files\' ] ;then\n' +
                '\t\tres=$(echo ${item} | awk \'{print $2}\')\n' +
                '\t\techo ${res##*src/main/java/}\n' +
                '\tfi\n' +
                '\n' +
                '\tif [ ${desc} == \'Only\' ]; then\n' +
                '\t\tdir=$(echo ${item} | awk \'{print $3}\')\n' +
                '\t\tfilename=$(echo ${item} | awk \'{print $4}\')\n' +
                '\t\tif [[ ${dir} =~ ${dir1} ]];then\n' +
                '\t\t\tres="${dir%:}/${filename}"\n' +
                '\t\t\techo ${res##*src/main/java/}\n' +
                '\t\tfi\n' +
                '\tfi\n' +
                'done')
        printWriter.flush()
        printWriter.close()
    }
}
