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
 * 对比两个文件夹的差异(用于比较文件夹中java文件的变化)
 * 如果dir1中有新增的java文件需要列出来(dir2中有新增的java文件忽略掉)
 * 如果某个java文件两个目录都有但是内容有变化需要列出来
 *
 * 例如:
 * dir1=/Users/tong/Projects/epmyg-android/app/src/main/java
 * dir2=/Users/tong/Projects/epmyg-android/app/src/main/java/build-fastdex/java
 *
 * dir1下面比dir2多了一个com/tong/CActivity.java
 * dir2下面比dir1多了一个com/tong/OldActivity.java
 * dir1和dir2都存在一个com/tong/AActivity.java并且内容存在变化
 *
 * 上面这种情况，输出的结果应该为
 * com/tong/CActivity.java
 * com/tong/AActivity.java
 *
 * 注意输出的结果要把根目录去掉，不能输出这种结果/Users/tong/Projects/epmyg-android/app/src/main/java/com/tong/CActivity.java
 */
public interface FolderComparator {
    Set<String> compare(Project project,File buildDir,File dir1,File dir2)
}
