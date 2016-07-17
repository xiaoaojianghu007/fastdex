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

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project

import java.util.regex.Matcher
import java.util.regex.Pattern

public class FastDexUtils {
    /**
     * 解析manifest中Application对应的类名(<application android:name="xxx" >)
     * @param manifest
     * @return
     */
    public static String getApplicationClassName(File manifest) {
        StringBuilder sb = new StringBuilder();
        if (manifest.exists() && manifest.isFile()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(manifest)));
            String line = null;
            while((line = br.readLine()) != null){
                sb.append(line);
            }
            br.close();
        }

        sb = new StringBuilder(sb.toString().replaceAll("(?s)<!--.*?-->",""));

        Pattern pattern = Pattern.compile("<application[^>]{1,}>");
        Pattern pattern2 = Pattern.compile("android:name\\s*=\\s*\\\"[^\\\"]{1,}\\\"");
        Matcher matcher = pattern.matcher(sb.toString());
        if (matcher.find()) {
            String str = matcher.group(0);

            Matcher matcher2 = pattern2.matcher(str);

            if (matcher2.find()) {
                String result = matcher2.group(0).replaceAll("\\s", "").trim().substring("android:name=\"".length()).replaceAll("\"","");
                return result;
            }
        }
        return "";
    }

    /**
     * 解析manifest中package节点值(<manifest package="xxx" >)
     * @param manifest
     * @return
     */
    public static String getManifestPackageName(File manifest) {
        StringBuilder sb = new StringBuilder();
        if (manifest.exists() && manifest.isFile()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(manifest)));
            String line = null;
            while((line = br.readLine()) != null){
                sb.append(line);
            }
            br.close();
        }

        sb = new StringBuilder(sb.toString().replaceAll("(?s)<!--.*?-->",""));

        Pattern pattern = Pattern.compile("<manifest[^>]{1,}>");
        Pattern pattern2 = Pattern.compile("package\\s*=\\s*\\\"[^\\\"]{1,}\\\"");
        Matcher matcher = pattern.matcher(sb.toString());
        if (matcher.find()) {
            String str = matcher.group(0);

            Matcher matcher2 = pattern2.matcher(str);

            if (matcher2.find()) {
                String result = matcher2.group(0).replaceAll("\\s", "").trim().substring("package=\"".length()).replaceAll("\"","");
                return result;
            }
        }
        return "";
    }


    /**
     * 删除文件或目录
     * @param rootFile
     * @return
     */
    public static boolean deleteFile(File rootFile) {
        if (rootFile == null || !rootFile.exists()) {
            return
        }
        boolean result = rootFile.delete()
        if (!result) {
            if (rootFile.isDirectory()) {
                //删除失败尝试删除子目录
                File[] dirs = rootFile.listFiles();
                if (dirs != null) {
                    result = true;
                    for (int i = 0;i < dirs.length;i++) {
                        File file = dirs[i];
                        boolean res = file.delete()
                        if (!res) {
                            result = res
                        }
                    }
                }
            }
        }
        if (!result) {
            String cmd = ""
            if (Os.isFamily(Os.FAMILY_MAC) || Os.isFamily(Os.FAMILY_UNIX)) {
                cmd = "rm -rf ${rootFile.getAbsolutePath()}"
            } else if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                cmd = "del/f/s/q ${rootFile.getAbsolutePath()}"
            }

            def process = cmd.execute()
            int status = process.waitFor()
            if (status == 0) {
                result = true
            }
            process.destroy()
        }

        return result
    }

    /**
     *
     * @param project
     * @return
     */
    public static File getManifestFile(Project project) {
        //TODO 暂时使用默认的路径
        File manifest = new File(project.getBuildDir(),"/intermediates/manifests/full/debug/AndroidManifest.xml")
        return manifest
    }

    public static String readUTF8Content(File file) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream()
        if (file.exists() && file.isFile()) {
            FileInputStream fis = new FileInputStream(file)
            byte[] buffer = new byte[1024]
            int len = -1
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer,0,len)
            }
        }

        String content = new String(bos.toByteArray());
        bos.close();
        return content;
    }
}
