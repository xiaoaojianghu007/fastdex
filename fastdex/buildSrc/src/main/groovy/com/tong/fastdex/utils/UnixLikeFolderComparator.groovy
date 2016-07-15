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
package com.tong.fastdex.utils

import org.gradle.api.Project

/**
 * linux、mac下使用shell脚本(fastdex_folder_compare.sh)进行比较,涉及的命令有diff、grep、awk、echo
 */
public class UnixLikeFolderComparator implements FolderComparator {
    Set<String> compare(Project project,File dir1,File dir2) {
        Set<String> result = new HashSet<>()
        String cmd = "sh ${project.getProjectDir()}/fastdex_folder_compare.sh ${project.getProjectDir()}/src/main/java ${project.getProjectDir()}/build-fastdex/java"
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
}
